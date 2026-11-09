/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers
import com.pandulapeter.kubrikoCreator.actors.NameFlaw
import java.text.Normalizer

/**
 * The rules for what a new project may be called, and the identifiers derived from its name. The name is shown to players,
 * so it may hold any letters; what goes into paths, Gradle, and code is folded to the letters a to z.
 */
internal object ProjectNaming {

    /**
     * Letters that don't fold to a to z by dropping their accents.
     */
    private val foldedLetters = mapOf('ß' to "ss", 'æ' to "ae", 'œ' to "oe", 'ø' to "o", 'đ' to "d", 'ð' to "d", 'ł' to "l", 'þ' to "th", 'ı' to "i")

    /**
     * Why [name] can't be a project's name, or null if it can.
     */
    fun validateName(name: String): TemplateProblem? = name.trim().let { trimmed ->
        when {
            trimmed.isEmpty() -> TemplateProblem.NAME_EMPTY
            !Character.isLetter(trimmed.codePointAt(0)) -> TemplateProblem.NAME_FIRST_CHARACTER
            !areAllowed(trimmed) -> TemplateProblem.NAME_CHARACTERS
            else -> null
        }
    }

    /**
     * Whether every character of [name] is a letter, a digit, a space, a dash, an underscore, or an accent written on a
     * letter, read by code point.
     */
    private fun areAllowed(name: String): Boolean = !NameFlaw.anyCodePoint(name) { codePoint, isAccentOnLetter ->
        !isAccentOnLetter && !Character.isLetterOrDigit(codePoint) && codePoint != ' '.code && codePoint != '_'.code && codePoint != '-'.code
    }

    /**
     * Why [packageName] can't be a project's package, or null if it can.
     */
    fun validatePackageName(packageName: String): TemplateProblem? = if (packageName.isEmpty()) TemplateProblem.PACKAGE_NAME_PARTS else packageName.split('.').let { segments ->
        segments.firstNotNullOfOrNull { segment ->
            when (NameFlaw.of(segment, isFirstAllowed = { it in 'a'..'z' }, isAllowed = { it in 'a'..'z' || it in '0'..'9' || it == '_' })) {
                NameFlaw.EMPTY -> TemplateProblem.PACKAGE_NAME_PART_EMPTY
                NameFlaw.LETTER_BEYOND_A_TO_Z -> TemplateProblem.PACKAGE_NAME_LETTER_BEYOND_A_TO_Z
                NameFlaw.FIRST_CHARACTER -> TemplateProblem.PACKAGE_NAME_FIRST_CHARACTER
                NameFlaw.CHARACTER -> TemplateProblem.PACKAGE_NAME_CHARACTERS
                null -> if (segment in KotlinIdentifiers.hardKeywords) TemplateProblem.PACKAGE_NAME_KEYWORD else null
            }
        } ?: if (segments.size < 2) TemplateProblem.PACKAGE_NAME_PARTS else null
    }

    /**
     * The folder and Gradle name of a project: "My Game" becomes "my-game", and "Kalandjáték" "kalandjatek". It starts
     * with a letter even when the name's letters fold to none ("Игра 2" becomes "game-2").
     */
    fun projectId(name: String) = folded(name).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.joinToString("-").let { id ->
        when {
            id.isEmpty() -> "game"
            id.first() !in 'a'..'z' -> "game-$id"
            else -> id
        }
    }

    /**
     * The package suggested for a project: "My Game" becomes "com.example.mygame".
     */
    fun defaultPackageName(name: String) = "com.example." + folded(name).filter { it in 'a'..'z' || it in '0'..'9' }
        .dropWhile { it in '0'..'9' }
        .ifEmpty { "game" }
        .let { if (it in KotlinIdentifiers.hardKeywords) "${it}game" else it }

    /**
     * [name] in lowercase, with every letter that has an accent written without it.
     */
    private fun folded(name: String) = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
        .filter { !NameFlaw.isAccent(it.code) && !NameFlaw.isSelector(it.code) }
        .fold(StringBuilder()) { result, character -> result.append(foldedLetters[character] ?: character) }
        .toString()
}
