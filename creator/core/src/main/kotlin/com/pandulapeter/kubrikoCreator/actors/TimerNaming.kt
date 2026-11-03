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
public object TimerNaming {

    /**
     * The name a new Timer gets when none is in use yet.
     */
    public const val DEFAULT_NAME: String = "timer"

    private val namePattern = Regex("[a-z][A-Za-z0-9]*")

    /**
     * The fields a Timer's name ends up in end with these, so no Property may.
     */
    internal val fieldSuffixes = listOf("RemainingInMilliseconds", "PeriodInMilliseconds")

    public fun isValid(name: String): Boolean = namePattern.matches(name) && name !in KotlinIdentifiers.hardKeywords

    /**
     * The name a Timer numbered [number] in older projects (Timers 1 to 8) keeps.
     */
    internal fun ofNumber(number: Int) = "timer$number"

    internal fun remainingField(timer: String) = timer + fieldSuffixes[0]

    internal fun periodField(timer: String) = timer + fieldSuffixes[1]

    internal fun hookName(timer: String) = "on" + timer.replaceFirstChar { it.uppercase() } + "Finished"
}
