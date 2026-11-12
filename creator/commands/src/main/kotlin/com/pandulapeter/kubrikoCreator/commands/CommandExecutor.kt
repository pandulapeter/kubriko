/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.SearchResults
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.findUsages
import com.pandulapeter.kubrikoCreator.context.mentionPathsOf
import com.pandulapeter.kubrikoCreator.context.pagesMentioning
import com.pandulapeter.kubrikoCreator.context.pagesMentioningDeleted
import com.pandulapeter.kubrikoCreator.context.searchProject
import com.pandulapeter.kubrikoCreator.context.writeSummary
import com.pandulapeter.kubrikoCreator.foundation.reportProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Runs commands against the open project and keeps the history of the changes they made, which undo and redo walk.
 * Changes are applied one at a time, in the order they were requested. Used from the main thread only: the history, the AI
 * steps, and the controllers it changes aren't synchronized; [com.pandulapeter.kubrikoCreator.mcp.McpToolServer] hops
 * there for tool calls.
 *
 * @param onSceneChanges Receives what each command, undo, or redo changed in the Scene files, as the Scenes recorded it,
 *   once it finished (whether it succeeded or not), still holding the change lock. Each change is handed over once, so
 *   it follows every one even when a part of it fails. What it throws never hides what the command threw, nor fails
 *   one that was made: it goes to [scope]'s handler then ([reportProblem]).
 */
public class CommandExecutor(
    public val context: CommandContext,
    private val scope: CoroutineScope,
    private val onRejected: (Rejection) -> Unit = {},
    private val registry: CommandRegistry = CommandRegistry.editor,
    private val onSceneChanges: (List<SceneChange>) -> Unit = {},
) {
    private val _history = MutableStateFlow(CommandHistory())
    public val history: StateFlow<CommandHistory> = _history.asStateFlow()
    private val _aiSteps = MutableStateFlow(emptyList<AiStep>())

    /**
     * The newest tool calls of AI clients ([call]), oldest first, each updated when it finishes.
     */
    public val aiSteps: StateFlow<List<AiStep>> = _aiSteps.asStateFlow()
    private var nextAiStepId = 0

    /**
     * Numbers the AI's next steps from [next] on, if no step took that number yet, so that a project's steps go on from where
     * its last session left off rather than from 1 every time Kubriko Creator starts. It never numbers two steps alike.
     */
    public fun continueAiStepsFrom(next: Int) {
        if (next > nextAiStepId) nextAiStepId = next
    }
    private val _broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = BROUGHT_BACK_BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * The Actors each [undo] or [redo] put back into a Scene (an undone removal), once their Scene was written, so that the
     * Scene editor can select them as it selects a placed one. Undoing an AI turn ([undoTurn]) isn't the user's step, so
     * it leaves the selection alone.
     */
    public val broughtBack: SharedFlow<ActorsBroughtBack> = _broughtBack.asSharedFlow()
    private val changes = Mutex()

    /**
     * The project's revision and its newest changes, for telling an agent what changed between two of its turns.
     */
    public val changeLog: ChangeLog = ChangeLog()

    /**
     * Why [invocation] can't be applied right now, or null if it can.
     */
    public fun validate(invocation: Invocation<*>): Rejection? = context.rejectChanges() ?: invocation.validate(context)

    /**
     * Applies [invocation] unless it is rejected, calling [onSuccess] once it was applied.
     *
     * @return Why it was rejected, or null if it is being applied.
     */
    public fun dispatchIfValid(invocation: Invocation<*>, onSuccess: () -> Unit = {}): Rejection? {
        validate(invocation)?.let { return it }
        dispatch(invocation, onSuccess)
        return null
    }

    /**
     * Applies [invocation] unless it is rejected, as [dispatchIfValid] does, calling [onSuccess] once it was applied and
     * [onFailure] if applying it fails after all, for a caller that waits for either (a save before closing).
     *
     * @return Why it was rejected, or null if it is being applied.
     */
    public fun dispatchIfValidWatching(invocation: Invocation<*>, onSuccess: () -> Unit, onFailure: () -> Unit): Rejection? {
        validate(invocation)?.let { return it }
        launch(invocation, onSuccess, onFailure)
        return null
    }

    /**
     * Applies [command] in the background, for user interface callbacks, reporting a rejection through [onRejected].
     * A change that needs no disk or git access shows before this returns, unless an AI tool call holds the change lock, so
     * text fields never lag behind what was typed.
     */
    public fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {}): Unit = launch(invocation, onSuccess = onSuccess)

    /**
     * What every dispatch shares: applies [invocation] in the background, starting right away, and reports how it went.
     */
    private fun launch(invocation: Invocation<*>, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { execute(invocation).report(onSuccess, onFailure) }
    }

    internal suspend fun execute(invocation: Invocation<*>): CommandOutcome = execute(invocation, origin = null, isByAi = false).outcome

    /**
     * Applies [invocation] for an AI client ([isByAi]) or the user. A change that leaves no result of its own returns,
     * for an AI client, the revision it made and what it changed ([writeSummary]).
     */
    private suspend fun execute(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): AppliedChange = changes.withLock {
        deliveringSceneChanges { applyChange(invocation, origin, isByAi) }
    }

    private suspend fun applyChange(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): AppliedChange {
        validate(invocation)?.let { return AppliedChange(CommandOutcome.Rejected(it)) }
        if (isByAi) invocation.validateByAi(context)?.let { return AppliedChange(CommandOutcome.Rejected(it)) }
        // A delete leaves nothing to find the pages that mentioned it by (its mentions become plain text), so they are found before it.
        val deleted = invocation.deletedSubject.takeIf { isByAi }
        val mentionedBefore = if (deleted == null) emptyList() else mentionPathsOrNone { context.pagesMentioningDeleted(deleted) }
        val applied = try {
            invocation.apply(context)
        } catch (exception: Exception) {
            return AppliedChange(CommandOutcome.Rejected(failureOf(exception)))
        }
        applied.failure?.let { return AppliedChange(CommandOutcome.Rejected(it)) }
        if (applied.isRewritingFiles) _history.value = CommandHistory()
        val target = applied.target ?: applied.broughtBack?.let { CommandTarget.Scene(it.scene, it.actors.map { actor -> actor.id }) } ?: invocation.target(context)
        val revision = changeLog.record(invocation.commandName, target, origin, isByAi)
        applied.inverse?.let { inverse ->
            record(HistoryEntry(revert = inverse.copy(isRestoring = true), reapply = invocation.copy(isRestoring = true), conversationId = origin?.conversationId, revision = revision, isByAi = isByAi).withContentHashes())
        }
        if (!isByAi || applied.result != JsonNull) return AppliedChange(CommandOutcome.Success(applied.result), target)
        val mentionedIn = mentionedBefore + mentionPathsOrNone { context.pagesMentioning(context.mentionPathsOf(invocation.targets(context))) }
        return AppliedChange(CommandOutcome.Success(JsonObject(context.writeSummary(revision, invocation.commandName, target, mentionedIn.distinct()) + applied.details.orEmpty())), target)
    }

    /**
     * What applying a change came to, and what it turned out to be about, if it was made.
     */
    private class AppliedChange(
        val outcome: CommandOutcome,
        val target: CommandTarget? = null,
    )

    /**
     * Runs the command called [name] with [arguments] decoded into its input: what an AI tool call does. Changes enter
     * the undo history like the user's own.
     *
     * @param origin Which of the assistant's conversations made the call, if one did.
     */
    public suspend fun call(name: String, arguments: JsonObject, origin: AiStepOrigin? = null, clientName: String? = null): CommandOutcome {
        val command = registry[name] ?: return CommandOutcome.Rejected(registry.missing(name))
        return call(name, command, arguments, origin, clientName)
    }

    /**
     * What applying [invocation] would do to each usage of what it deletes ([deleteWithUsages]), read while no change
     * is being made, or null if it deletes nothing.
     */
    public suspend fun usagesOf(invocation: Invocation<*>): List<Usage>? = invocation.deletedSubject?.let { subject -> changes.withLock { context.usagesOf(subject) } }

    /**
     * Every place that refers to [subject] (`find_usages`), read while no change is being made.
     */
    public suspend fun findUsages(subject: RelationSubject): List<Usage> = changes.withLock { context.findUsages(subject) }

    /**
     * What [query] finds in the project's texts (`search_project`), at most [limit] hits, read while no change is being made.
     */
    public suspend fun searchTexts(query: String, limit: Int): SearchResults = changes.withLock { context.searchProject(query, isSearchingNames = false, limit = limit) }

    public fun undo(): Unit = stepForUser(isUndo = true)

    public fun redo(): Unit = stepForUser(isUndo = false)

    private fun stepForUser(isUndo: Boolean) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { step(isUndo, isUsers = true)?.report() }
    }

    /**
     * Reverts the newest change, or makes the newest reverted one again. A step that no longer applies, or whose type or
     * Scene changed outside Kubriko Creator since, or that throws, is dropped from the history.
     *
     * @return What happened, or null if there was nothing to step.
     */
    public suspend fun step(isUndo: Boolean): CommandOutcome? = step(isUndo, isUsers = false)

    /**
     * @param isUsers Whether the user stepped ([undo], [redo]), whose step announces what it brought back ([broughtBack])
     *   once its Scenes were delivered (or failed to be, since the Scene editor selects them only once its Scene shows
     *   them), still holding the change lock, so a later step's announcement can't come first.
     */
    private suspend fun step(isUndo: Boolean, isUsers: Boolean): CommandOutcome? = changes.withLock {
        val stepped = deliveringSceneChanges { stepHistory(isUndo) }
        if (isUsers) stepped?.broughtBack?.let(_broughtBack::tryEmit)
        stepped?.outcome
    }

    private class Stepped(val outcome: CommandOutcome, val broughtBack: ActorsBroughtBack? = null)

    private suspend fun stepHistory(isUndo: Boolean): Stepped? {
        val history = _history.value
        val entry = (if (isUndo) history.undoable else history.redoable).lastOrNull() ?: return null
        val invocation = if (isUndo) entry.revert else entry.reapply
        val remaining = if (isUndo) history.copy(undoable = history.undoable.dropLast(1)) else history.copy(redoable = history.redoable.dropLast(1))
        context.rejectChanges()?.let { return Stepped(CommandOutcome.Rejected(it)) }
        changedOutside(entry)?.let { name ->
            _history.value = remaining
            return Stepped(CommandOutcome.Rejected(Rejection(CommandProblem.CHANGED_OUTSIDE, name)))
        }
        val rejection = invocation.validate(context)
        if (rejection != null) {
            _history.value = remaining
            return Stepped(CommandOutcome.Rejected(rejection))
        }
        val applied = try {
            invocation.apply(context)
        } catch (exception: Exception) {
            _history.value = remaining
            return Stepped(CommandOutcome.Rejected(failureOf(exception)))
        }
        applied.failure?.let {
            _history.value = remaining
            return Stepped(CommandOutcome.Rejected(it))
        }
        val revision = changeLog.record(invocation.commandName, invocation.target(context), step = if (isUndo) ChangeRecord.Step.UNDO else ChangeRecord.Step.REDO)
        val stepped = applied.inverse?.copy(isRestoring = true)?.let { inverse ->
            if (isUndo) HistoryEntry(invocation, inverse, entry.conversationId, revision, isByAi = entry.isByAi) else HistoryEntry(inverse, invocation, entry.conversationId, revision, isByAi = entry.isByAi)
        }?.withContentHashes()
        _history.value = when {
            stepped == null -> remaining
            isUndo -> remaining.copy(redoable = remaining.redoable + stepped)
            else -> remaining.copy(undoable = remaining.undoable + stepped)
        }
        return Stepped(CommandOutcome.Success(applied.result), applied.broughtBack)
    }

    /**
     * Reverts what the assistant's conversation at the tool address [conversationId] changed after [sinceRevision], as
     * long as its changes are the newest: a later change of anyone else's stops it, so nothing else is reverted with them.
     *
     * @return How many of the conversation's changes are still there after it.
     */
    public suspend fun undoTurn(conversationId: String, sinceRevision: Int): Int {
        while (true) {
            val newest = _history.value.undoable.lastOrNull()
            if (newest == null || newest.conversationId != conversationId || newest.revision <= sinceRevision) break
            if (step(isUndo = true) !is CommandOutcome.Success) break
        }
        return _history.value.undoable.count { it.conversationId == conversationId && it.revision > sinceRevision }
    }

    private suspend fun <I> call(name: String, command: EditorCommand<I>, arguments: JsonObject, origin: AiStepOrigin?, clientName: String?): CommandOutcome {
        val input = try {
            json.decodeFromJsonElement(command.inputSerializer, arguments)
        } catch (exception: SerializationException) {
            return rejectInput(name, command, exception, origin, clientName)
        } catch (exception: IllegalArgumentException) {
            return rejectInput(name, command, exception, origin, clientName)
        }
        val step = AiStep(id = nextAiStepId++, toolName = name, target = command.target(context, input), isRunning = true, changesProject = command is WriteCommand, origin = origin, clientName = clientName)
        publish(step)
        var rejection: Rejection? = null
        var appliedTarget: CommandTarget? = null
        try {
            val outcome = when (command) {
                is ReadCommand -> if (command.readsProject) changes.withLock { command.read(context, input) } else command.read(context, input)
                is WriteCommand -> execute(Invocation(command, input), origin, isByAi = true).also { appliedTarget = it.target }.outcome
                is ActionCommand -> command.run(context, input, origin)
            }
            rejection = (outcome as? CommandOutcome.Rejected)?.rejection
            return outcome
        } catch (exception: CancellationException) {
            rejection = Rejection(CommandProblem.TURN_CANCELLED)
            throw exception
        } catch (throwable: Throwable) {
            rejection = Rejection(CommandProblem.INTERNAL, throwable.toString())
            throw throwable
        } finally {
            publish(step.copy(target = appliedTarget ?: runCatching { command.target(context, input) }.getOrNull() ?: step.target, isRunning = false, rejection = rejection))
        }
    }

    /**
     * What [read] reads on the I/O dispatcher, or nothing when it fails: which pages mention a change only helps the agent,
     * and the change is made all the same.
     */
    private suspend fun <T> mentionPathsOrNone(read: () -> Collection<T>): Collection<T> = try {
        withContext(context.ioDispatcher) { read() }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Why a command that threw wasn't applied. A cancellation isn't a rejection, so it is thrown on; an `Error`, which
     * isn't caught, reaches the scope's handler.
     */
    private fun failureOf(exception: Exception): Rejection {
        if (exception is CancellationException) throw exception
        return Rejection(CommandProblem.FAILED, exception.message ?: exception.toString())
    }

    private suspend fun HistoryEntry.withContentHashes(): HistoryEntry {
        val hashes = context.contentHashesOf(revert.targets(context) + reapply.targets(context))
        return copy(contentHashes = hashes.mapNotNull { (target, hash) -> hash?.let { target to it } }.toMap())
    }

    /**
     * The name of the type or Scene [entry] touches that changed since it was made or stepped, if one did. One that can't
     * be read right now isn't taken for changed.
     */
    private suspend fun changedOutside(entry: HistoryEntry): String? {
        if (entry.contentHashes.isEmpty()) return null
        val current = context.contentHashesOf(entry.contentHashes.keys)
        val changed = entry.contentHashes.entries.firstOrNull { (target, hash) -> current[target].let { it != null && it != hash } }?.key
        return when (changed) {
            is CommandTarget.BlockType -> changed.name
            is CommandTarget.Scene -> changed.name
            is CommandTarget.Page -> changed.name
            is CommandTarget.Document -> changed.path
            else -> null
        }
    }

    /**
     * Runs [change], then hands over the Scene changes it made, even when it throws. A delivery that fails never hides
     * what the change threw, nor fails one that was made: it is added to what the change threw as suppressed, or
     * reported when the change was made or cancelled, since a cancellation's suppressed are never read. A cancelled
     * delivery is no problem, unless it timed out; after a change that was made, it stops only a caller that was cancelled.
     */
    private suspend inline fun <T> deliveringSceneChanges(change: () -> T): T {
        val result = try {
            change()
        } catch (throwable: Throwable) {
            // The coroutine's own cancellation may come back from the delivery, and can't suppress itself.
            deliverSceneChanges()?.takeIf { it !== throwable }?.let { failure ->
                when {
                    failure is CancellationException && failure !is TimeoutCancellationException -> Unit
                    throwable is CancellationException -> scope.reportProblem(failure)
                    else -> throwable.addSuppressed(failure)
                }
            }
            throw throwable
        }
        deliverSceneChanges()?.let { failure ->
            if (failure is CancellationException) currentCoroutineContext().ensureActive()
            // A cancellation the caller didn't share is no problem, unless it is a timeout.
            scope.reportProblem(failure)
        }
        return result
    }

    /**
     * Hands over the Scene changes made since the last call.
     *
     * @return What [onSceneChanges] threw, if it did.
     */
    private fun deliverSceneChanges(): Throwable? = try {
        context.scenes.takeChanges().takeIf { it.isNotEmpty() }?.let(onSceneChanges)
        null
    } catch (failure: Throwable) {
        failure
    }

    private fun rejectInput(name: String, command: EditorCommand<*>, exception: Exception, origin: AiStepOrigin?, clientName: String?): CommandOutcome {
        val rejection = Rejection(CommandProblem.INPUT_INVALID, InputProblem.describe(exception.message))
        publish(AiStep(id = nextAiStepId++, toolName = name, target = null, isRunning = false, rejection = rejection, origin = origin, clientName = clientName))
        return CommandOutcome.Rejected(rejection)
    }

    private fun publish(step: AiStep) = _aiSteps.update { steps ->
        (if (steps.any { it.id == step.id }) steps.map { if (it.id == step.id) step else it } else steps + step).takeLast(AI_STEP_COUNT)
    }

    private fun CommandOutcome.report(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) = when (this) {
        is CommandOutcome.Success -> onSuccess()
        is CommandOutcome.Rejected -> {
            onRejected(rejection)
            onFailure()
        }
    }

    private fun record(entry: HistoryEntry) {
        val undoable = _history.value.undoable
        val previous = undoable.lastOrNull()
        _history.value = CommandHistory(
            undoable = if (previous != null && _history.value.redoable.isEmpty() && previous.conversationId == entry.conversationId && previous.isByAi == entry.isByAi && previous.reapply.mergesWith(entry.reapply)) {
                undoable.dropLast(1) + entry.copy(revert = previous.revert, contentHashes = previous.contentHashes + entry.contentHashes)
            } else {
                (undoable + entry).takeLast(HISTORY_SIZE)
            },
        )
    }

    private companion object {
        const val HISTORY_SIZE = 200
        const val AI_STEP_COUNT = 500
        const val BROUGHT_BACK_BUFFER = 16
        val json = Json { ignoreUnknownKeys = false }
    }
}
