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

import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.mentionPathsOf
import com.pandulapeter.kubrikoCreator.context.mentionedPathOf
import com.pandulapeter.kubrikoCreator.context.pagesMentioning
import com.pandulapeter.kubrikoCreator.context.writeSummary
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
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
 *   once it finished (whether it succeeded or not), still holding the change lock.
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
     * Applies [command] in the background, for user interface callbacks, reporting a rejection through [onRejected].
     * A change that needs no disk or git access shows before this returns, unless an AI tool call holds the change lock, so
     * text fields never lag behind what was typed.
     */
    public fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {}) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { execute(invocation).report(onSuccess) }
    }

    internal suspend fun execute(invocation: Invocation<*>): CommandOutcome = execute(invocation, origin = null, isByAi = false)

    /**
     * Applies [invocation] for an AI client ([isByAi]) or the user. A change that leaves no result of its own returns,
     * for an AI client, the revision it made and what it changed ([writeSummary]).
     */
    private suspend fun execute(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): CommandOutcome = changes.withLock {
        try {
            applyChange(invocation, origin, isByAi)
        } finally {
            deliverSceneChanges()
        }
    }

    private suspend fun applyChange(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): CommandOutcome {
        validate(invocation)?.let { return CommandOutcome.Rejected(it) }
        if (isByAi) invocation.validateByAi(context)?.let { return CommandOutcome.Rejected(it) }
        // A delete leaves nothing to find the pages that mentioned it by (its mentions become plain text), so they are found before it.
        val deleted = invocation.deletedSubject.takeIf { isByAi }
        val mentionedBefore = if (deleted == null) emptyList() else mentionPathsOrNone { context.pagesMentioning(listOfNotNull(context.mentionedPathOf(deleted))) }
        val applied = try {
            invocation.apply(context)
        } catch (exception: Exception) {
            return CommandOutcome.Rejected(failureOf(exception))
        }
        applied.failure?.let { return CommandOutcome.Rejected(it) }
        if (applied.isRewritingFiles) _history.value = CommandHistory()
        val target = invocation.target(context)
        val revision = changeLog.record(invocation.commandName, target, origin, isByAi)
        applied.inverse?.let { inverse ->
            record(HistoryEntry(revert = inverse.copy(isRestoring = true), reapply = invocation.copy(isRestoring = true), conversationId = origin?.conversationId, revision = revision, isByAi = isByAi).withContentHashes())
        }
        if (!isByAi || applied.result != JsonNull) return CommandOutcome.Success(applied.result)
        val mentionedIn = mentionedBefore + mentionPathsOrNone { context.pagesMentioning(context.mentionPathsOf(invocation.targets(context))) }
        return CommandOutcome.Success(JsonObject(context.writeSummary(revision, invocation.commandName, target, mentionedIn.distinct()) + applied.details.orEmpty()))
    }

    /**
     * Runs the command called [name] with [arguments] decoded into its input: what an AI tool call does. Changes enter
     * the undo history like the user's own.
     *
     * @param origin Which of the assistant's conversations made the call, if one did.
     */
    public suspend fun call(name: String, arguments: JsonObject, origin: AiStepOrigin? = null, clientName: String? = null): CommandOutcome {
        val command = registry[name] ?: return CommandOutcome.Rejected(Rejection(CommandProblem.UNKNOWN, name))
        return call(name, command, arguments, origin, clientName)
    }

    /**
     * What applying [invocation] would do to each usage of what it deletes ([deleteWithUsages]), read while no change
     * is being made, or null if it deletes nothing.
     */
    public suspend fun usagesOf(invocation: Invocation<*>): List<Usage>? = invocation.deletedSubject?.let { subject -> changes.withLock { context.usagesOf(subject) } }

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
     *   once its Scenes are delivered, still holding the change lock, so a later step's announcement can't come first.
     */
    private suspend fun step(isUndo: Boolean, isUsers: Boolean): CommandOutcome? = changes.withLock {
        var stepped: Stepped? = null
        try {
            stepped = stepHistory(isUndo)
            stepped?.outcome
        } finally {
            try {
                deliverSceneChanges()
            } finally {
                if (isUsers) stepped?.broughtBack?.let(_broughtBack::tryEmit)
            }
        }
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
        try {
            val outcome = when (command) {
                is ReadCommand -> if (command.readsProject) changes.withLock { command.read(context, input) } else command.read(context, input)
                is WriteCommand -> execute(Invocation(command, input), origin, isByAi = true)
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
            publish(step.copy(target = runCatching { command.target(context, input) }.getOrNull() ?: step.target, isRunning = false, rejection = rejection))
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

    private fun deliverSceneChanges() {
        context.scenes.takeChanges().takeIf { it.isNotEmpty() }?.let(onSceneChanges)
    }

    private fun rejectInput(name: String, command: EditorCommand<*>, exception: Exception, origin: AiStepOrigin?, clientName: String?): CommandOutcome {
        val rejection = Rejection(CommandProblem.INPUT_INVALID, InputProblem.describe(exception.message))
        publish(AiStep(id = nextAiStepId++, toolName = name, target = null, isRunning = false, rejection = rejection, origin = origin, clientName = clientName))
        return CommandOutcome.Rejected(rejection)
    }

    private fun publish(step: AiStep) = _aiSteps.update { steps ->
        (if (steps.any { it.id == step.id }) steps.map { if (it.id == step.id) step else it } else steps + step).takeLast(AI_STEP_COUNT)
    }

    private fun CommandOutcome.report(onSuccess: () -> Unit = {}) = when (this) {
        is CommandOutcome.Success -> onSuccess()
        is CommandOutcome.Rejected -> onRejected(rejection)
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
