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

import com.pandulapeter.kubrikoCreator.context.writeSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_turn_cancelled
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_unknown
import kubriko.creator.generated.resources.error_mcp_internal

/**
 * Runs commands against the open project and keeps the history of the changes they made, which undo and redo walk.
 * Changes are applied one at a time, in the order they were requested. Used from the main thread only: the history, the AI
 * steps, and the controllers it changes aren't synchronized; [com.pandulapeter.kubrikoCreator.mcp.McpToolServer] hops
 * there for tool calls.
 */
internal class CommandExecutor(
    val context: CommandContext,
    private val scope: CoroutineScope,
    private val onRejected: (Rejection) -> Unit = {},
    private val registry: CommandRegistry = CommandRegistry.editor,
) {
    private val _history = MutableStateFlow(CommandHistory())
    val history = _history.asStateFlow()
    private val _aiSteps = MutableStateFlow(emptyList<AiStep>())

    /**
     * The newest tool calls of AI clients ([call]), oldest first, each updated when it finishes.
     */
    val aiSteps = _aiSteps.asStateFlow()
    private var nextAiStepId = 0
    private val changes = Mutex()

    /**
     * The project's revision and its newest changes, for telling an agent what changed between two of its turns.
     */
    val changeLog = ChangeLog()

    /**
     * Why [invocation] can't be applied right now, or null if it can.
     */
    fun validate(invocation: Invocation<*>) = context.rejectChanges() ?: invocation.validate(context)

    /**
     * Applies [command] in the background, for user interface callbacks, reporting a rejection through [onRejected].
     * A change that needs no disk or git access shows before this returns, so text fields never lag behind what was typed.
     */
    fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {}) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { execute(invocation).report(onSuccess) }
    }

    suspend fun execute(invocation: Invocation<*>): CommandOutcome = execute(invocation, origin = null, isByAi = false)

    /**
     * Applies [invocation] for an AI client ([isByAi]) or the user. A change that leaves no result of its own returns,
     * for an AI client, the revision it made and what it changed ([writeSummary]).
     */
    private suspend fun execute(invocation: Invocation<*>, origin: AiStepOrigin?, isByAi: Boolean): CommandOutcome = changes.withLock {
        validate(invocation)?.let { return@withLock CommandOutcome.Rejected(it) }
        val applied = invocation.apply(context)
        applied.failure?.let { return@withLock CommandOutcome.Rejected(it) }
        if (applied.isRewritingFiles) _history.value = CommandHistory()
        val target = invocation.target(context)
        val revision = changeLog.record(invocation.title, target, origin, isByAi)
        applied.inverse?.let { inverse ->
            record(HistoryEntry(revert = inverse.copy(isRestoring = true), reapply = invocation.copy(isRestoring = true), conversationId = origin?.conversationId, revision = revision, isByAi = isByAi))
        }
        CommandOutcome.Success(if (isByAi && applied.result == JsonNull) context.writeSummary(revision, invocation.title, target) else applied.result)
    }

    /**
     * Runs the command called [name] with [arguments] decoded into its input: what an AI tool call does. Changes enter
     * the undo history like the user's own.
     *
     * @param origin Which of the assistant's conversations made the call, if one did.
     */
    suspend fun call(name: String, arguments: JsonObject, origin: AiStepOrigin? = null, clientName: String? = null): CommandOutcome {
        val command = registry[name] ?: return CommandOutcome.Rejected(Rejection(Res.string.error_command_unknown, name))
        return call(name, command, arguments, origin, clientName)
    }

    fun undo() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { step(isUndo = true)?.report() }
    }

    fun redo() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { step(isUndo = false)?.report() }
    }

    /**
     * Reverts the newest change, or makes the newest reverted one again. A step that no longer applies (because the
     * project changed outside Kubriko Creator, for example) is dropped from the history.
     *
     * @return What happened, or null if there was nothing to step.
     */
    suspend fun step(isUndo: Boolean): CommandOutcome? = changes.withLock {
        val history = _history.value
        val entry = (if (isUndo) history.undoable else history.redoable).lastOrNull() ?: return@withLock null
        val invocation = if (isUndo) entry.revert else entry.reapply
        val remaining = if (isUndo) history.copy(undoable = history.undoable.dropLast(1)) else history.copy(redoable = history.redoable.dropLast(1))
        context.rejectChanges()?.let { return@withLock CommandOutcome.Rejected(it) }
        val rejection = invocation.validate(context)
        if (rejection != null) {
            _history.value = remaining
            return@withLock CommandOutcome.Rejected(rejection)
        }
        val applied = invocation.apply(context)
        applied.failure?.let {
            _history.value = remaining
            return@withLock CommandOutcome.Rejected(it)
        }
        val revision = changeLog.record(invocation.title, invocation.target(context), step = if (isUndo) ChangeRecord.Step.UNDO else ChangeRecord.Step.REDO)
        val stepped = applied.inverse?.copy(isRestoring = true)?.let { inverse ->
            if (isUndo) HistoryEntry(invocation, inverse, entry.conversationId, revision, isByAi = entry.isByAi) else HistoryEntry(inverse, invocation, entry.conversationId, revision, isByAi = entry.isByAi)
        }
        _history.value = when {
            stepped == null -> remaining
            isUndo -> remaining.copy(redoable = remaining.redoable + stepped)
            else -> remaining.copy(undoable = remaining.undoable + stepped)
        }
        CommandOutcome.Success(applied.result)
    }

    /**
     * Reverts what the assistant's conversation at the tool address [conversationId] changed after [sinceRevision], as
     * long as its changes are the newest: a later change of anyone else's stops it, so nothing else is reverted with them.
     *
     * @return How many of the conversation's changes are still there after it.
     */
    suspend fun undoTurn(conversationId: String, sinceRevision: Int): Int {
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
        val step = AiStep(id = nextAiStepId++, toolName = name, title = command.title, target = command.target(context, input), isRunning = true, origin = origin, clientName = clientName)
        publish(step)
        var rejection: Rejection? = null
        try {
            val outcome = when (command) {
                is ReadCommand -> command.read(context, input)
                is WriteCommand -> execute(Invocation(command, input), origin, isByAi = true)
                is ActionCommand -> command.run(context, input)
            }
            rejection = (outcome as? CommandOutcome.Rejected)?.rejection
            return outcome
        } catch (exception: CancellationException) {
            rejection = Rejection(Res.string.assistant_turn_cancelled)
            throw exception
        } catch (throwable: Throwable) {
            rejection = Rejection(Res.string.error_mcp_internal, throwable.toString())
            throw throwable
        } finally {
            publish(step.copy(target = runCatching { command.target(context, input) }.getOrNull() ?: step.target, isRunning = false, rejection = rejection))
        }
    }

    private fun rejectInput(name: String, command: EditorCommand<*>, exception: Exception, origin: AiStepOrigin?, clientName: String?): CommandOutcome {
        val rejection = Rejection(Res.string.error_command_input_invalid, InputProblem.describe(exception.message))
        publish(AiStep(id = nextAiStepId++, toolName = name, title = command.title, target = null, isRunning = false, rejection = rejection, origin = origin, clientName = clientName))
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
                undoable.dropLast(1) + entry.copy(revert = previous.revert.mergedWith(entry.revert))
            } else {
                (undoable + entry).takeLast(HISTORY_SIZE)
            },
        )
    }

    private companion object {
        const val HISTORY_SIZE = 200
        const val AI_STEP_COUNT = 500
        val json = Json { ignoreUnknownKeys = false }
    }
}
