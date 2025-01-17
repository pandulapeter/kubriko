/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoAudio

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
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
    KubrikoViewport(
        modifier = modifier.windowInsetsPadding(windowInsets),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}