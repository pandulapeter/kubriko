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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.implementation.managers.AudioTestManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AudioTestStateHolder : StateHolder

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