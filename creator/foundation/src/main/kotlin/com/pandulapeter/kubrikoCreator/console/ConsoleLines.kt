/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.console

/**
 * What the console shows: its [entries], oldest first, and a [revision] that every change raises, so that two of them
 * are told apart by it alone rather than by comparing their lines, which a game logging the same line every frame would
 * make both slow and wrong.
 *
 * @param firstNumber The number of the first of [entries] among every line ever added, so that a line keeps its number
 *   while older ones are dropped.
 * @param firstLogNumber The number of the first line the game logged ([ConsoleEntry.Log]) since it was last played, if
 *   any, so that only that one opens the console by itself however often the console is shown again.
 * @param latestProblem The newest [ConsoleEntry.Failing] line among [entries], unless a status that isn't an error came
 *   after it (something went well since), kept as lines are added rather than looked for among them.
 */
public data class ConsoleLines(
    val revision: Long = 0,
    val entries: List<ConsoleEntry> = emptyList(),
    val firstNumber: Long = 0,
    val firstLogNumber: Long? = null,
    val latestProblem: ConsoleEntry.Failing? = null,
)
