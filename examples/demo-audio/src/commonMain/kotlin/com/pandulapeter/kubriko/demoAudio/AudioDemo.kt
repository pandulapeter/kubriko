package com.pandulapeter.kubriko.demoAudio

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
) {
    stateHolder as AudioDemoStateHolderImpl
    KubrikoViewport(
        kubriko = stateHolder.kubriko,
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