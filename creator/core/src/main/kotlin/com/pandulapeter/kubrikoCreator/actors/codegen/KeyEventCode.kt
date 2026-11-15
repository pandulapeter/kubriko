/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.KEYBOARD_INPUT_MANAGER_FIELD

/**
 * How generated classes run the Events that react to keys: Kubriko tells a `KeyboardInputAware` about each key pressed
 * and let go, and a held key is asked about on every frame, which needs no Trait.
 */
internal object KeyEventCode {

    /**
     * The Events of [events] that react to keys, each kind apart, as both generators split them.
     *
     * @param isFamily Whether they are a family's, whose base class runs a held key's hook even without Blocks, since a
     *   child may override it.
     */
    class KeyEvents(events: List<EventDefinition>, isFamily: Boolean = false) {
        val pressed = events.filter { it.type == EventType.KEY_PRESSED }
        val released = events.filter { it.type == EventType.KEY_RELEASED }
        val held = events.filter { it.type == EventType.KEY_HELD }

        /**
         * Whether Kubriko has to tell the type about keys pressed and let go, which the held keys don't need.
         */
        val isKeyboardInputAware = pressed.isNotEmpty() || released.isNotEmpty()

        /**
         * Whether the type asks about a held key every frame: one whose Event has Blocks, or any in a family.
         */
        val holdsKeys = held.any { isFamily || it.blocks.isNotEmpty() }
    }

    /**
     * The override of [function] (`onKeyPressed` or `onKeyReleased`) calling [events]' functions by their key, after
     * the lines of [guards], or nothing when none of them has a function ([callOf]).
     */
    fun StringBuilder.appendKeyDispatch(
        function: String,
        events: List<EventDefinition>,
        callOf: (List<EventDefinition>) -> String?,
        guards: List<String> = emptyList(),
    ) {
        val calls = callsByKey(events, callOf)
        if (calls.isEmpty()) return
        appendLine()
        appendLine("    override fun $function(key: Key) {")
        guards.forEach(::appendLine)
        appendLine("        when (key) {")
        calls.forEach { (key, call) -> appendLine("            Key.$key -> $call") }
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * The lines calling the functions of [events], each while its key is held and [condition] holds, if it is given, at
     * [indent].
     */
    fun StringBuilder.appendHeldKeyCalls(
        events: List<EventDefinition>,
        callOf: (List<EventDefinition>) -> String?,
        indent: String,
        condition: String? = null,
    ) = callsByKey(events, callOf).forEach { (key, call) ->
        appendLine("${indent}if (${condition?.let { "$it && " }.orEmpty()}$KEYBOARD_INPUT_MANAGER_FIELD.isKeyPressed(Key.$key)) $call")
    }

    /**
     * The calls of [events]' functions by the name of their key, in the order of the key list ([KeyChoice]).
     */
    private fun callsByKey(events: List<EventDefinition>, callOf: (List<EventDefinition>) -> String?) = events
        .groupBy { checkNotNull(it.trigger.key) }
        .toSortedMap()
        .mapNotNull { (key, events) -> callOf(events)?.let { key.composeKeyName to it } }
}
