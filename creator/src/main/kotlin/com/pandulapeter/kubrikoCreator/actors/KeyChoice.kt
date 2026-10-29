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
internal value class KeyChoice(
    val composeKeyName: String,
) : Comparable<KeyChoice> {

    val isKnown get() = composeKeyName in composeKeys

    /**
     * Keys are ordered like [all], so the common ones come first.
     */
    override fun compareTo(other: KeyChoice) = compareValuesBy(this, other) { order[it.composeKeyName] ?: Int.MAX_VALUE }

    companion object {

        /**
         * How the AI tools describe a key.
         */
        const val DESCRIPTION = "named like its constant in Compose's androidx.compose.ui.input.key.Key: DirectionLeft, DirectionRight, DirectionUp, " +
            "DirectionDown, Spacebar, Enter, Escape, Tab, ShiftLeft, CtrlLeft, A to Z, Zero to Nine, F1 to F12, NumPad0 to NumPad9, and every other one."

        val LEFT = KeyChoice("DirectionLeft")
        val RIGHT = KeyChoice("DirectionRight")
        val UP = KeyChoice("DirectionUp")
        val DOWN = KeyChoice("DirectionDown")
        val SPACE = KeyChoice("Spacebar")
        val ENTER = KeyChoice("Enter")
        val W = KeyChoice("W")
        val A = KeyChoice("A")
        val S = KeyChoice("S")
        val D = KeyChoice("D")

        /**
         * The keys games use most, which lead every list, in the order older projects had them.
         */
        val common = listOf(LEFT, RIGHT, UP, DOWN, SPACE, ENTER, W, A, S, D) + listOf(
            "ShiftLeft", "ShiftRight", "CtrlLeft", "CtrlRight", "AltLeft", "AltRight", "Escape", "Tab", "Backspace",
        ).map(::KeyChoice)

        /**
         * Every key: [common] ones first, then the rest in the order Compose declares them.
         */
        val all = common + composeKeys.keys.map(::KeyChoice).filterNot { it in common }

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
        fun of(name: String) = legacyNames[name] ?: composeKeys.keys.firstOrNull { it.equals(name, ignoreCase = true) }?.let(::KeyChoice) ?: KeyChoice(name)
    }

    object Serializer : KSerializer<KeyChoice> {

        override val descriptor = PrimitiveSerialDescriptor("KeyChoice", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: KeyChoice) = encoder.encodeString(value.composeKeyName)

        override fun deserialize(decoder: Decoder) = of(decoder.decodeString())
    }
}
