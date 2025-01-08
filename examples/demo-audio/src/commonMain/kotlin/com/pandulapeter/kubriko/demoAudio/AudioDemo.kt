package com.pandulapeter.kubriko.demoAudio

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.demoAudio.implementation.AudioDemoManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

@Composable
fun AudioDemo(
    modifier: Modifier = Modifier,
    stateHolder: AudioDemoStateHolder = createAudioDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as AudioDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.windowInsetsPadding(windowInsets),
        kubriko = stateHolder.kubriko,
        windowInsets = windowInsets,
    )
}

sealed interface AudioDemoStateHolder : ExampleStateHolder

fun createAudioDemoStateHolder(): AudioDemoStateHolder = AudioDemoStateHolderImpl()

internal class AudioDemoStateHolderImpl : AudioDemoStateHolder {
    val kubriko = Kubriko.newInstance(
        MusicManager.newInstance(),
        SoundManager.newInstance(),
        AudioDemoManager(),
    )

    override fun dispose() = kubriko.dispose()
}