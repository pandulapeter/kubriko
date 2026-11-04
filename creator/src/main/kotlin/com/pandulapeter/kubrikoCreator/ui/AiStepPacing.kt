/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.commands.AiStep
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The AI step the editor follows, each one shown for at least a while, so that steps taken in quick succession don't
 * make the editor flash between them: a newer step waits until the one shown has been for long enough, and only the
 * newest of those waiting is shown then. The step shown finishing replaces it at once, since only then is its target final.
 */
internal class AiStepPacing(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    var shown: AiStep? by mutableStateOf(null)
        private set
    private var shownSince: TimeMark? = null

    /**
     * Shows [step] once the step shown has been for [delayInMillis]. Cancelled while it waits, it shows nothing.
     */
    suspend fun show(step: AiStep?, delayInMillis: Int) {
        if (step == null || step == shown) return
        if (step.id != shown?.id) {
            shownSince?.let { delay(delayInMillis.milliseconds - it.elapsedNow()) }
            shownSince = timeSource.markNow()
        }
        shown = step
    }
}

/**
 * The newest of [aiSteps] that changes the project, as [AiStepPacing] shows it.
 */
@Composable
internal fun rememberFollowedAiStep(aiSteps: List<AiStep>, delayInMillis: Int): AiStep? {
    val pacing = remember { AiStepPacing() }
    val newest = aiSteps.newestChange()
    LaunchedEffect(newest, delayInMillis) { pacing.show(newest, delayInMillis) }
    return pacing.shown
}
