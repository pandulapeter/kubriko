/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation.managers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoContentShaders.implementation.actors.ColorfulBox
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.times
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import com.pandulapeter.kubriko.shaders.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.chromatic_aberration
import kubriko.examples.demo_content_shaders.generated.resources.ripple
import kubriko.examples.demo_content_shaders.generated.resources.smooth_pixelation
import kubriko.examples.demo_content_shaders.generated.resources.vignette
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal class ContentShadersDemoManager : Manager() {

    private val state = MutableStateFlow(State())
    private val actorManager by manager<ActorManager>()
    private val viewportManager by manager<ViewportManager>()
    private val smoothPixelationShader by lazy { SmoothPixelationShader() }
    private val vignetteShader by lazy { VignetteShader() }
    private val rippleShader by lazy { RippleShader() }
    private val chromaticAberrationShader by lazy { ChromaticAberrationShader() }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(
            (-10..10).flatMap { y ->
                (-10..10).map { x ->
                    ColorfulBox(
                        initialPosition = SceneOffset(
                            x = x * 100.sceneUnit,
                            y = y * 100.sceneUnit,
                        ),
                        hue = (0..360).random().toFloat(),
                    )
                }
            }
        )
        state.onEach { state ->
            actorManager.remove(smoothPixelationShader, vignetteShader, rippleShader, chromaticAberrationShader)
            actorManager.add(
                buildList {
                    if (state.isSmoothPixelationShaderEnabled) {
                        add(smoothPixelationShader)
                    }
                    if (state.isVignetteShaderEnabled) {
                        add(vignetteShader)
                    }
                    if (state.isRippleShaderEnabled) {
                        add(rippleShader)
                    }
                    if (state.isChromaticAberrationShaderEnabled) {
                        add(chromaticAberrationShader)
                    }
                }
            )
        }.launchIn(scope)
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) = Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets)
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Controls(
                modifier = Modifier.width(280.dp),
                state = state.collectAsState().value,
                onStateChanged = { state.value = it },
            )
        }
    }

    @Composable
    private fun Controls(
        modifier: Modifier = Modifier,
        state: State,
        onStateChanged: (State) -> Unit,
    ) = Column(
        modifier = modifier,
    ) {
        Toggle(
            name = Res.string.smooth_pixelation,
            isChecked = state.isSmoothPixelationShaderEnabled,
            onCheckedChanged = { onStateChanged(state.copy(isSmoothPixelationShaderEnabled = !state.isSmoothPixelationShaderEnabled)) }
        )
        Toggle(
            name = Res.string.vignette,
            isChecked = state.isVignetteShaderEnabled,
            onCheckedChanged = { onStateChanged(state.copy(isVignetteShaderEnabled = !state.isVignetteShaderEnabled)) }
        )
        Toggle(
            name = Res.string.ripple,
            isChecked = state.isRippleShaderEnabled,
            onCheckedChanged = { onStateChanged(state.copy(isRippleShaderEnabled = !state.isRippleShaderEnabled)) }
        )
        Toggle(
            name = Res.string.chromatic_aberration,
            isChecked = state.isChromaticAberrationShaderEnabled,
            onCheckedChanged = { onStateChanged(state.copy(isChromaticAberrationShaderEnabled = !state.isChromaticAberrationShaderEnabled)) }
        )
    }

    @Composable
    private fun Toggle(
        name: StringResource,
        isChecked: Boolean,
        onCheckedChanged: () -> Unit,
    ) = Row(
        modifier = Modifier
            .selectable(
                selected = isChecked,
                onClick = onCheckedChanged,
            )
            .padding(start = 8.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(name),
        )
        Switch(
            modifier = Modifier.scale(0.6f).height(24.dp),
            checked = isChecked,
            onCheckedChange = { onCheckedChanged() },
        )
    }

    private data class State(
        val isSmoothPixelationShaderEnabled: Boolean = false,
        val isVignetteShaderEnabled: Boolean = true,
        val isRippleShaderEnabled: Boolean = true,
        val isChromaticAberrationShaderEnabled: Boolean = true,
    )
}