/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry

internal class FakeProjectRunner(
    var result: ProjectRunner.RunResult = ProjectRunner.RunResult(isSuccessful = true, console = emptyList()),
) : ProjectRunner {
    val calls = mutableListOf<String>()

    override suspend fun build() = result.also { calls += "build" }

    override suspend fun play(durationInMilliseconds: Long) = result.also { calls += "play $durationInMilliseconds" }

    companion object {
        fun failing(vararg console: ConsoleEntry) = FakeProjectRunner(ProjectRunner.RunResult(isSuccessful = false, console = console.toList()))
    }
}
