/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Numbers the AI's steps per project rather than per run of Kubriko Creator: the number the next step takes is kept on this
 * computer (`.kubriko/local/`), read when the project opens, and written after each step. A step taken before it is read
 * keeps its number, and the next ones go on from the higher of the two, so no number is given twice in a session; a
 * write that fails ([JsonFileStore][com.pandulapeter.kubrikoCreator.foundation.JsonFileStore] reports none) only lets
 * the next session start lower.
 */
internal class AiStepNumbering(
    rootDirectory: File,
    commands: CommandExecutor,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) {
    private val store = LocalProjectStore(rootDirectory, FILE_NAME, LocalAiSteps.serializer())

    init {
        scope.launch {
            var kept = withContext(ioDispatcher) { store.read()?.nextStep } ?: 0
            commands.continueAiStepsFrom(kept)
            commands.aiSteps.mapNotNull { steps -> steps.lastOrNull()?.id?.plus(1) }.distinctUntilChanged().conflate().collect { next ->
                if (next > kept) {
                    kept = next
                    withContext(ioDispatcher) { store.write(LocalAiSteps(next)) }
                }
            }
        }
    }

    private companion object {
        const val FILE_NAME = "ai-steps.json"
    }
}

@Serializable
internal data class LocalAiSteps(
    @SerialName("nextStep") val nextStep: Int,
)
