/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoType
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsContainer
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.shaders_not_supported
import org.jetbrains.compose.resources.stringResource

fun createShaderAnimationsDemoStateHolder(
    isLoggingEnabled: Boolean,
): ShaderAnimationsDemoStateHolder = ShaderAnimationsDemoStateHolderImpl(
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun ShaderAnimationsDemo(
    modifier: Modifier = Modifier,
    stateHolder: ShaderAnimationsDemoStateHolder = createShaderAnimationsDemoStateHolder(
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ShaderAnimationsDemoStateHolderImpl
    if (stateHolder.shaderManager.areShadersSupported) {
        val selectedDemoType = stateHolder.selectedDemoType.collectAsState().value
        val controlsState = stateHolder.controlsState.collectAsState().value
        Column(
            modifier = modifier.fillMaxSize(),
        ) {
            SecondaryScrollableTabRow(
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.secondary,
                selectedTabIndex = selectedDemoType.ordinal,
                divider = {},
            ) {
                ShaderAnimationDemoType.entries.forEach { demoType ->
                    Tab(
                        modifier = Modifier.height(42.dp),
                        text = { Text(stringResource(demoType.nameStringResource)) },
                        selected = demoType == selectedDemoType,
                        onClick = { stateHolder.onSelectedDemoTypeChanged(demoType) }
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = selectedDemoType,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.Center,
                ) { demoType ->
                    KubrikoViewport(
                        modifier = Modifier.fillMaxSize(),
                        kubriko = stateHolder.shaderAnimationDemoHolders[demoType]!!.kubriko,
                    )
                }
                ControlsContainer(
                    modifier = Modifier.windowInsetsPadding(windowInsets).align(Alignment.BottomEnd).padding(16.dp),
                    state = selectedDemoType to controlsState,
                    onIsExpandedChanged = stateHolder::onControlsStateChanged,
                    shaderAnimationDemoHolders = stateHolder.shaderAnimationDemoHolders,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(0.75f).align(Alignment.Center),
                textAlign = TextAlign.Center,
                text = stringResource(Res.string.shaders_not_supported),
            )
        }
    }
}