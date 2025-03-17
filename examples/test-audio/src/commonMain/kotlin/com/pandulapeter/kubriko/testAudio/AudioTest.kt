/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testAudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolder
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolderImpl

fun createAudioTestStateHolder(
    webRootPathName: String,
    isLoggingEnabled: Boolean,
): AudioTestStateHolder = AudioTestStateHolderImpl(
    webRootPathName = webRootPathName,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun AudioTest(
    modifier: Modifier = Modifier,
    stateHolder: AudioTestStateHolder = createAudioTestStateHolder(
        webRootPathName = "",
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as AudioTestStateHolderImpl
    KubrikoViewport(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .windowInsetsPadding(windowInsets),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}