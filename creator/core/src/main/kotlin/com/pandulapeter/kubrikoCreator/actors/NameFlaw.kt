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
         * if it has none. Of the letters, both see only a to z (in either case), since every other letter, and every mark
         * written on a letter, is a flaw of its own ([hasLetterBeyondAToZ]).
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
         * Whether [text] holds a letter outside a to z, or an accent written on a letter (typed as a character of its own,
         * or a vowel sign of a script such as Devanagari), read by code point, so letters beyond the Basic Multilingual
         * Plane count too. Marks that follow no letter, and invisible selectors, are left to the other rules.
         */
        public fun hasLetterBeyondAToZ(text: String): Boolean = anyCodePoint(text) { codePoint, isAccentOnLetter ->
            isAccentOnLetter || (Character.isLetter(codePoint) && codePoint !in 'a'.code..'z'.code && codePoint !in 'A'.code..'Z'.code)
        }

        /**
         * Whether [predicate] holds for any code point of [text], told whether that code point is an accent written on the
         * letter before it (directly, or after other accents on it).
         */
        public inline fun anyCodePoint(text: String, predicate: (codePoint: Int, isAccentOnLetter: Boolean) -> Boolean): Boolean {
            var followsLetter = false
            var index = 0
            while (index < text.length) {
                val codePoint = text.codePointAt(index)
                val isAccentOnLetter = followsLetter && isAccent(codePoint)
                if (predicate(codePoint, isAccentOnLetter)) return true
                if (!isSelector(codePoint)) followsLetter = Character.isLetter(codePoint) || isAccentOnLetter
                index += Character.charCount(codePoint)
            }
            return false
        }

        /**
         * Whether [codePoint] is a mark written on the character before it, other than the invisible ones that only choose
         * how it is shown or sorts ([isSelector]).
         */
        public fun isAccent(codePoint: Int): Boolean = when {
            isSelector(codePoint) -> false
            else -> when (Character.getType(codePoint).toByte()) {
                Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
                else -> false
            }
        }

        /**
         * Whether [codePoint] is an invisible mark that only chooses how the character before it is shown (a variation
         * selector) or how it sorts (the combining grapheme joiner), which neither is nor ends an accent.
         */
        public fun isSelector(codePoint: Int): Boolean = codePoint in 0xFE00..0xFE0F || codePoint in 0xE0100..0xE01EF || codePoint == 0x034F
    }
}
