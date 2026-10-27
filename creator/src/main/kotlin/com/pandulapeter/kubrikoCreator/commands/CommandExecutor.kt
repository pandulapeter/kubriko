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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_unknown

/**
 * Runs commands against the open project and keeps the history of the changes they made, which undo and redo walk.
 * Changes are applied one at a time, in the order they were requested.
 */
internal class CommandExecutor(
    private val context: CommandContext,
    private val scope: CoroutineScope,
    private val onRejected: (Rejection) -> Unit = {},
    private val registry: CommandRegistry = CommandRegistry.editor,
) {
    private val _history = MutableStateFlow(CommandHistory())
    val history = _history.asStateFlow()
    private val changes = Mutex()

    /**
     * Why [invocation] can't be applied right now, or null if it can.
     */
    fun validate(invocation: Invocation<*>) = invocation.validate(context)

    /**
     * Applies [command] in the background, for user interface callbacks, reporting a rejection through [onRejected].
     * A change that needs no disk or git access shows before this returns, so text fields never lag behind what was typed.
     */
    fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {}) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { execute(invocation).report(onSuccess) }
    }

    suspend fun execute(invocation: Invocation<*>): CommandOutcome = changes.withLock {
        invocation.validate(context)?.let { return@withLock CommandOutcome.Rejected(it) }
        val applied = invocation.apply(context)
        applied.inverse?.let { inverse -> record(HistoryEntry(revert = inverse, reapply = invocation)) }
        CommandOutcome.Success(applied.result)
    }

    /**
     * Runs the command called [name] with [arguments] decoded into its input: what an AI tool call does. Changes enter
     * the undo history like the user's own.
     */
    suspend fun call(name: String, arguments: JsonObject): CommandOutcome {
        val command = registry[name] ?: return CommandOutcome.Rejected(Rejection(Res.string.error_command_unknown, name))
        return call(command, arguments)
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
        val rejection = invocation.validate(context)
        if (rejection != null) {
            _history.value = remaining
            return@withLock CommandOutcome.Rejected(rejection)
        }
        val applied = invocation.apply(context)
        val stepped = applied.inverse?.let { inverse ->
            if (isUndo) HistoryEntry(revert = invocation, reapply = inverse) else HistoryEntry(revert = inverse, reapply = invocation)
        }
        _history.value = when {
            stepped == null -> remaining
            isUndo -> remaining.copy(redoable = remaining.redoable + stepped)
            else -> remaining.copy(undoable = remaining.undoable + stepped)
        }
        CommandOutcome.Success(applied.result)
    }

    private suspend fun <I> call(command: EditorCommand<I>, arguments: JsonObject): CommandOutcome {
        val input = try {
            json.decodeFromJsonElement(command.inputSerializer, arguments)
        } catch (exception: SerializationException) {
            return CommandOutcome.Rejected(Rejection(Res.string.error_command_input_invalid, exception.message.orEmpty()))
        } catch (exception: IllegalArgumentException) {
            return CommandOutcome.Rejected(Rejection(Res.string.error_command_input_invalid, exception.message.orEmpty()))
        }
        return when (command) {
            is ReadCommand -> command.read(context, input)
            is WriteCommand -> execute(Invocation(command, input))
        }
    }

    private fun CommandOutcome.report(onSuccess: () -> Unit = {}) = when (this) {
        is CommandOutcome.Success -> onSuccess()
        is CommandOutcome.Rejected -> onRejected(rejection)
    }

    private fun record(entry: HistoryEntry) {
        val undoable = _history.value.undoable
        val previous = undoable.lastOrNull()
        val mergeKey = entry.reapply.mergeKey
        _history.value = CommandHistory(
            undoable = if (mergeKey != null && previous != null && previous.reapply.command == entry.reapply.command && previous.reapply.mergeKey == mergeKey && _history.value.redoable.isEmpty()) {
                undoable.dropLast(1) + previous.copy(reapply = entry.reapply)
            } else {
                (undoable + entry).takeLast(HISTORY_SIZE)
            },
        )
    }

    private companion object {
        const val HISTORY_SIZE = 200
        val json = Json { ignoreUnknownKeys = false }
    }
}
