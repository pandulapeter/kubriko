/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoAudio.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.demoAudio.implementation.managers.AudioDemoManager
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AudioDemoStateHolder : StateHolder

internal class AudioDemoStateHolderImpl : AudioDemoStateHolder {
    private val musicManager = MusicManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val soundManager = SoundManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val audioDemoManager = AudioDemoManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            musicManager,
            soundManager,
            audioDemoManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun stopMusic() = audioDemoManager.stopMusicBeforeDispose()

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Audio"