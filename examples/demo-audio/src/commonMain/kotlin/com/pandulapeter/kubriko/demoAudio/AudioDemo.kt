package com.pandulapeter.kubriko.demoAudio

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoAudio.implementation.AudioDemoStateHolder
import com.pandulapeter.kubriko.demoAudio.implementation.AudioDemoStateHolderImpl

fun createAudioDemoStateHolder(): AudioDemoStateHolder = AudioDemoStateHolderImpl()

@Composable
fun AudioDemo(
    modifier: Modifier = Modifier,
    stateHolder: AudioDemoStateHolder = createAudioDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as AudioDemoStateHolderImpl
    DebugMenu(
        modifier = modifier,
        windowInsets = windowInsets,
        kubriko = stateHolder.kubriko,
        buttonAlignment = null,
    ) {
        KubrikoViewport(
            modifier = modifier.windowInsetsPadding(windowInsets),
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
    }
}