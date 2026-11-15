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

        /**
         * [keyNames] in the order of the key list ([KeyChoice]).
         */
        fun order(keyNames: Collection<String>): List<String> = (pressed + released).mapNotNull { it.trigger.key }.distinct().sorted().map { it.composeKeyName }.filter { it in keyNames }
    }

    /**
     * A Manager's override of [function] (`onKeyPressed` or `onKeyReleased`) calling [events]' functions by their key,
     * or nothing when none of them has a function ([callOf]); a Manager hears keys while the game is paused too.
     */
    fun StringBuilder.appendKeyDispatch(
        function: String,
        events: List<EventDefinition>,
        callOf: (List<EventDefinition>) -> String?,
    ) {
        val calls = callsByKey(events, callOf)
        if (calls.isEmpty()) return
        appendLine()
        appendLine("    override fun $function(key: Key) {")
        appendLine("        when (key) {")
        calls.forEach { (key, call) -> appendLine("            Key.$key -> $call") }
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * An Actor's overrides of `onKeyPressed` and `onKeyReleased`, which pair each key's release with a press the Actor
     * heard, after the lines of [pressGuards] (while the game runs) and [releaseGuards]: a released Event then runs even
     * while the game is paused, so what a press started always ends, but never for a press the Actor didn't hear (one in
     * the Scene editor, whose game never runs, or before it was added). Each key with a released Event keeps whether its
     * press was heard in a field of its own, so pairing allocates nothing.
     */
    fun StringBuilder.appendPairedKeyDispatches(
        keys: KeyEvents,
        callOf: (List<EventDefinition>) -> String?,
        pressGuards: List<String>,
        releaseGuards: List<String>,
    ) {
        val pressedCalls = callsByKey(keys.pressed, callOf).toMap()
        val releasedCalls = callsByKey(keys.released, callOf).toMap()
        val pressedKeys = keys.order(pressedCalls.keys + releasedCalls.keys)
        if (pressedKeys.isEmpty()) return
        releasedCalls.keys.forEach { key ->
            appendLine()
            appendLine("    /**")
            appendLine("     * Whether the Actor heard $key pressed while the game ran, which its release ends.")
            appendLine("     */")
            appendLine("    private var ${pressedField(key)} = false")
        }
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        pressGuards.forEach(::appendLine)
        appendLine("        when (key) {")
        pressedKeys.forEach { key ->
            val lines = listOfNotNull(releasedCalls[key]?.let { "${pressedField(key)} = true" }, pressedCalls[key])
            if (lines.size == 1) {
                appendLine("            Key.$key -> ${lines.single()}")
            } else {
                appendLine("            Key.$key -> {")
                lines.forEach { appendLine("                $it") }
                appendLine("            }")
            }
        }
        appendLine("        }")
        appendLine("    }")
        if (releasedCalls.isEmpty()) return
        appendLine()
        appendLine("    override fun onKeyReleased(key: Key) {")
        releaseGuards.forEach(::appendLine)
        appendLine("        when (key) {")
        keys.order(releasedCalls.keys).forEach { key ->
            appendLine("            Key.$key -> if (${pressedField(key)}) {")
            appendLine("                ${pressedField(key)} = false")
            appendLine("                ${releasedCalls.getValue(key)}")
            appendLine("            }")
        }
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * Starts with an underscore, which no Property's name can, so it clashes with none.
     */
    private fun pressedField(key: String) = "_wasPressed$key"

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
