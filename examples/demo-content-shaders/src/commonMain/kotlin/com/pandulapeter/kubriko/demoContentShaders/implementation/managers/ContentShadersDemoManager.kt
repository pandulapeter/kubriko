/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation.managers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersState
import com.pandulapeter.kubriko.demoContentShaders.implementation.actors.ColorfulBox
import com.pandulapeter.kubriko.demoContentShaders.implementation.ui.ContentShadersOverlay
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.times
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.ComicShader
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import com.pandulapeter.kubriko.shaders.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class ContentShadersDemoManager : Manager() {

    private val _state = MutableStateFlow(ContentShadersState())
    val state = _state.asStateFlow()
    private val actorManager by manager<ActorManager>()
    private val smoothPixelationShader by lazy { SmoothPixelationShader() }
    private val vignetteShader by lazy { VignetteShader() }
    private val blurShader by lazy { BlurShader() }
    private val rippleShader by lazy { RippleShader() }
    private val chromaticAberrationShader by lazy { ChromaticAberrationShader() }
    private val comicShader by lazy { ComicShader() }
    private val _areControlsExpanded = MutableStateFlow(false)
    val areControlsExpanded = _areControlsExpanded.asStateFlow()

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
                        shouldDrawBorder = { !state.value.isComicShaderEnabled },
                    )
                }
            }
        )
        state.onEach { state ->
            actorManager.remove(actorManager.allActors.value.filterIsInstance<Shader<*>>())
            actorManager.add(
                buildList {
                    if (state.isSmoothPixelationShaderEnabled) {
                        add(smoothPixelationShader)
                    }
                    if (state.isVignetteShaderEnabled) {
                        add(vignetteShader)
                    }
                    if (state.isComicShaderEnabled) {
                        add(comicShader)
                    }
                    if (state.isBlurShaderEnabled) {
                        add(blurShader)
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
    override fun Composable(windowInsets: WindowInsets) = ContentShadersOverlay(
        windowInsets = windowInsets,
        areControlsExpanded = areControlsExpanded.collectAsState().value,
        state = state.collectAsState().value,
        onStateChanged = ::onStateChanged,
        onControlsToggled = ::toggleControlsExpanded,
    )

    fun onStateChanged(state: ContentShadersState) = _state.update { state }

    fun toggleControlsExpanded() = _areControlsExpanded.update { !it }
}
