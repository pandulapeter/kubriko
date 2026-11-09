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

/**
 * The first rule a name made of a limited set of characters breaks, told apart so that a rejection says what is actually
 * wrong with it rather than restating every rule.
 */
public enum class NameFlaw {
    EMPTY,

    /**
     * A letter outside a to z, such as an accented one, which names in code and in files shared between systems can't hold.
     */
    LETTER_BEYOND_A_TO_Z,
    FIRST_CHARACTER,
    CHARACTER,
    ;

    public companion object {

        /**
         * The flaw of [name], whose first character has to pass [isFirstAllowed] and every other one [isAllowed], or null
         * if it has none. Both see only the letters a to z (in either case), since every other letter is a flaw of its own.
         */
        public fun of(name: String, isFirstAllowed: (Char) -> Boolean, isAllowed: (Char) -> Boolean): NameFlaw? = when {
            name.isEmpty() -> EMPTY
            hasLetterBeyondAToZ(name) -> LETTER_BEYOND_A_TO_Z
            !isFirstAllowed(name.first()) -> FIRST_CHARACTER
            name.drop(1).any { !isAllowed(it) } -> CHARACTER
            else -> null
        }

        /**
         * Whether [character] is one of the letters a to z (in either case) or a digit from 0 to 9.
         */
        public fun isLetterOrDigitAToZ(character: Char): Boolean = character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9'

        /**
         * Whether [text] holds a letter outside a to z, or a mark written on one (an accent typed as a character of its
         * own, or a vowel sign of a script such as Devanagari), read by code point, so letters beyond the Basic
         * Multilingual Plane count too.
         */
        public fun hasLetterBeyondAToZ(text: String): Boolean = text.codePoints().anyMatch { codePoint ->
            (Character.isLetter(codePoint) && codePoint !in 'a'.code..'z'.code && codePoint !in 'A'.code..'Z'.code) || isMark(codePoint)
        }

        /**
         * Whether [codePoint] is a mark, which is written on the letter before it.
         */
        public fun isMark(codePoint: Int): Boolean = when (Character.getType(codePoint).toByte()) {
            Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
            else -> false
        }
    }
}
