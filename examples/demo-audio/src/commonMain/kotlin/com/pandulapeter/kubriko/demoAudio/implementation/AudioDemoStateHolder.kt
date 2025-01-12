package com.pandulapeter.kubriko.demoAudio.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.demoAudio.implementation.managers.AudioDemoManager
import com.pandulapeter.kubriko.shared.StateHolder

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
    override val kubriko = Kubriko.newInstance(
        musicManager,
        soundManager,
        audioDemoManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )

    override fun dispose() = kubriko.dispose()
}

private const val LOG_TAG = "Audio"