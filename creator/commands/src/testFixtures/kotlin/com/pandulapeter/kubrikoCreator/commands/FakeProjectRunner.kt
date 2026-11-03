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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry

public class FakeProjectRunner(
    var result: ProjectRunner.RunResult = ProjectRunner.RunResult(isSuccessful = true, console = emptyList()),
    var step: GameStep = GameStep.Done(console = emptyList()),
    var targets: PlayTargets = PlayTargets(available = listOf(PlayTarget.DESKTOP), chosen = PlayTarget.DESKTOP),
) : ProjectRunner {
    val calls = mutableListOf<String>()

    override suspend fun build() = result.also { calls += "build" }

    override suspend fun playTargets() = targets.also { calls += "targets" }

    override suspend fun play(durationInMilliseconds: Long, target: PlayTarget) = result.also { calls += "play $durationInMilliseconds" + target.suffix }

    override suspend fun startGame(firstScene: String?, target: PlayTarget) =
        result.also { calls += (if (firstScene == null) "start" else "start $firstScene") + target.suffix }

    override suspend fun stopGame() = result.also { calls += "stop" }

    override suspend fun pressKeys(keys: List<String>, durationInMilliseconds: Long) = step.also { calls += "press ${keys.joinToString("+")} $durationInMilliseconds" }

    override suspend fun wait(durationInMilliseconds: Long) = step.also { calls += "wait $durationInMilliseconds" }

    override suspend fun captureScreen() = step.also { calls += "screenshot" }

    private val PlayTarget.suffix get() = if (this == PlayTarget.DESKTOP) "" else " on $id"

    companion object {
        fun failing(vararg console: ConsoleEntry) = FakeProjectRunner(ProjectRunner.RunResult(isSuccessful = false, console = console.toList()))
    }
}
