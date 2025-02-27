/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testAudio.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.implementation.managers.AudioTestManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.test_audio.generated.resources.Res
import kubriko.examples.test_audio.generated.resources.ic_loop_off
import kubriko.examples.test_audio.generated.resources.ic_loop_on
import kubriko.examples.test_audio.generated.resources.ic_pause
import kubriko.examples.test_audio.generated.resources.ic_play
import kubriko.examples.test_audio.generated.resources.ic_stop

sealed interface AudioTestStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = preloadedImageVector(Res.drawable.ic_loop_off).value != null
                && preloadedImageVector(Res.drawable.ic_loop_on).value != null
                && preloadedImageVector(Res.drawable.ic_pause).value != null
                && preloadedImageVector(Res.drawable.ic_play).value != null
                && preloadedImageVector(Res.drawable.ic_stop).value != null
    }
}

internal class AudioTestStateHolderImpl : AudioTestStateHolder {
    private val musicManager = MusicManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val soundManager = SoundManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val audioTestManager = AudioTestManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            musicManager,
            soundManager,
            audioTestManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun stopMusic() = audioTestManager.stopMusicBeforeDispose()

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Audio"