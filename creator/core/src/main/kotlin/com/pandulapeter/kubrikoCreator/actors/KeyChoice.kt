/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A key that Key pressed Events and held-key conditions react to: any of [composeKeys], by the name of its constant in
 * `androidx.compose.ui.input.key.Key`. One that isn't among them (written into a file by hand) is rejected by the
 * commands and left out of the generated code.
 */
@Serializable(with = KeyChoice.Serializer::class)
@JvmInline
public value class KeyChoice(
    public val composeKeyName: String,
) : Comparable<KeyChoice> {

    public val isKnown: Boolean get() = composeKeyName in composeKeys

    /**
     * Keys are ordered like [all], so the common ones come first.
     */
    override fun compareTo(other: KeyChoice): Int = compareValuesBy(this, other) { order[it.composeKeyName] ?: Int.MAX_VALUE }

    public companion object {

        /**
         * How the AI tools describe a key.
         */
        public const val DESCRIPTION: String = "named like Compose's Key constants: DirectionLeft to DirectionDown, Spacebar, Enter, Escape, ShiftLeft, " +
            "A to Z, Zero to Nine, F1 to F12, and the rest."

        public val LEFT: KeyChoice = KeyChoice("DirectionLeft")
        public val RIGHT: KeyChoice = KeyChoice("DirectionRight")
        public val UP: KeyChoice = KeyChoice("DirectionUp")
        public val DOWN: KeyChoice = KeyChoice("DirectionDown")
        public val SPACE: KeyChoice = KeyChoice("Spacebar")
        public val ENTER: KeyChoice = KeyChoice("Enter")
        public val W: KeyChoice = KeyChoice("W")
        public val A: KeyChoice = KeyChoice("A")
        public val S: KeyChoice = KeyChoice("S")
        public val D: KeyChoice = KeyChoice("D")

        /**
         * The keys games use most, which lead every list, in the order older projects had them.
         */
        public val common: List<KeyChoice> = listOf(LEFT, RIGHT, UP, DOWN, SPACE, ENTER, W, A, S, D) + listOf(
            "ShiftLeft", "ShiftRight", "CtrlLeft", "CtrlRight", "AltLeft", "AltRight", "Escape", "Tab", "Backspace",
        ).map(::KeyChoice)

        /**
         * Every key: [common] ones first, then the rest in the order Compose declares them.
         */
        public val all: List<KeyChoice> = common + composeKeys.keys.map(::KeyChoice).filterNot { it in common }

        private val order = all.withIndex().associate { (index, key) -> key.composeKeyName to index }

        /**
         * The names older projects saved, before every key could be chosen.
         */
        private val legacyNames = mapOf(
            "left" to LEFT,
            "right" to RIGHT,
            "up" to UP,
            "down" to DOWN,
            "space" to SPACE,
            "enter" to ENTER,
        )

        /**
         * The key called [name]: its Compose name in any case, or an older project's name for it.
         */
        public fun of(name: String): KeyChoice = legacyNames[name] ?: composeKeys.keys.firstOrNull { it.equals(name, ignoreCase = true) }?.let(::KeyChoice) ?: KeyChoice(name)
    }

    internal object Serializer : KSerializer<KeyChoice> {

        override val descriptor = PrimitiveSerialDescriptor("KeyChoice", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: KeyChoice) = encoder.encodeString(value.composeKeyName)

        override fun deserialize(decoder: Decoder) = of(decoder.decodeString())
    }
}
