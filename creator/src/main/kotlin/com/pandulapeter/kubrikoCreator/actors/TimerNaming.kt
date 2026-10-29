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
 * The names of Timers, which Blocks start and stop and an Event reacts to, like GameMaker's alarms, but named and as
 * many as needed: a Timer exists once a Block or an Event names it, and becomes a pair of fields of the generated class.
 */
internal object TimerNaming {

    /**
     * The name a new Timer gets when none is in use yet.
     */
    const val DEFAULT_NAME = "timer"

    private val namePattern = Regex("[a-z][A-Za-z0-9]*")
    private val keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object",
        "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
    )

    /**
     * The fields a Timer's name ends up in end with these, so no Property may.
     */
    val fieldSuffixes = listOf("RemainingInMilliseconds", "PeriodInMilliseconds")

    fun isValid(name: String) = namePattern.matches(name) && name !in keywords

    /**
     * The name a Timer numbered [number] in older projects (Timers 1 to 8) keeps.
     */
    fun ofNumber(number: Int) = "timer$number"

    fun remainingField(timer: String) = timer + fieldSuffixes[0]

    fun periodField(timer: String) = timer + fieldSuffixes[1]

    fun hookName(timer: String) = "on" + timer.replaceFirstChar { it.uppercase() } + "Finished"
}
