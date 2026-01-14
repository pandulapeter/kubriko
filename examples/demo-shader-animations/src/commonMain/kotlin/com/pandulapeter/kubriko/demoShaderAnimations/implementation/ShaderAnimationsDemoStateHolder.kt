/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsState
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.alpha
import kubriko.examples.demo_shader_animations.generated.resources.clouds
import kubriko.examples.demo_shader_animations.generated.resources.collapse_controls
import kubriko.examples.demo_shader_animations.generated.resources.color
import kubriko.examples.demo_shader_animations.generated.resources.cover
import kubriko.examples.demo_shader_animations.generated.resources.dark
import kubriko.examples.demo_shader_animations.generated.resources.description
import kubriko.examples.demo_shader_animations.generated.resources.ether
import kubriko.examples.demo_shader_animations.generated.resources.expand_controls
import kubriko.examples.demo_shader_animations.generated.resources.focus
import kubriko.examples.demo_shader_animations.generated.resources.frequency
import kubriko.examples.demo_shader_animations.generated.resources.gradient
import kubriko.examples.demo_shader_animations.generated.resources.hide_code
import kubriko.examples.demo_shader_animations.generated.resources.ic_brush
import kubriko.examples.demo_shader_animations.generated.resources.ic_code
import kubriko.examples.demo_shader_animations.generated.resources.light
import kubriko.examples.demo_shader_animations.generated.resources.noodle
import kubriko.examples.demo_shader_animations.generated.resources.scale
import kubriko.examples.demo_shader_animations.generated.resources.shaders_not_supported
import kubriko.examples.demo_shader_animations.generated.resources.show_code
import kubriko.examples.demo_shader_animations.generated.resources.sky_1
import kubriko.examples.demo_shader_animations.generated.resources.sky_2
import kubriko.examples.demo_shader_animations.generated.resources.speed
import kubriko.examples.demo_shader_animations.generated.resources.warp

sealed interface ShaderAnimationsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_brush).value != null
                && preloadedImageVector(Res.drawable.ic_code).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.shaders_not_supported).value.isNotBlank()
                && preloadedString(Res.string.expand_controls).value.isNotBlank()
                && preloadedString(Res.string.collapse_controls).value.isNotBlank()
                && preloadedString(Res.string.show_code).value.isNotBlank()
                && preloadedString(Res.string.hide_code).value.isNotBlank()
                && preloadedString(Res.string.gradient).value.isNotBlank()
                && preloadedString(Res.string.ether).value.isNotBlank()
                && preloadedString(Res.string.noodle).value.isNotBlank()
                && preloadedString(Res.string.clouds).value.isNotBlank()
                && preloadedString(Res.string.warp).value.isNotBlank()
                && preloadedString(Res.string.scale).value.isNotBlank()
                && preloadedString(Res.string.speed).value.isNotBlank()
                && preloadedString(Res.string.dark).value.isNotBlank()
                && preloadedString(Res.string.light).value.isNotBlank()
                && preloadedString(Res.string.cover).value.isNotBlank()
                && preloadedString(Res.string.alpha).value.isNotBlank()
                && preloadedString(Res.string.sky_1).value.isNotBlank()
                && preloadedString(Res.string.sky_2).value.isNotBlank()
                && preloadedString(Res.string.focus).value.isNotBlank()
                && preloadedString(Res.string.frequency).value.isNotBlank()
                && preloadedString(Res.string.color).value.isNotBlank()
    }
}

internal class ShaderAnimationsDemoStateHolderImpl(
    isLoggingEnabled: Boolean,
) : ShaderAnimationsDemoStateHolder {

    val shaderManager = ShaderManager.newInstance()
    val shaderAnimationDemoHolders = ShaderAnimationDemoType.entries.associateWith {
        when (it) {
            ShaderAnimationDemoType.CLOUD -> ShaderAnimationDemoHolder(
                shader = CloudShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "cloud",
                isLoggingEnabled = isLoggingEnabled,
            )

            ShaderAnimationDemoType.ETHER -> ShaderAnimationDemoHolder(
                shader = EtherShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "ether",
                isLoggingEnabled = isLoggingEnabled,
            )

            ShaderAnimationDemoType.GRADIENT -> ShaderAnimationDemoHolder(
                shader = GradientShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "gradient",
                isLoggingEnabled = isLoggingEnabled,
            )

            ShaderAnimationDemoType.NOODLE -> ShaderAnimationDemoHolder(
                shader = NoodleShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "noodle",
                isLoggingEnabled = isLoggingEnabled,
            )

            ShaderAnimationDemoType.WARP -> ShaderAnimationDemoHolder(
                shader = WarpShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "warp",
                isLoggingEnabled = isLoggingEnabled,
            )
        }
    }.toPersistentMap()
    private val _selectedDemoType = MutableStateFlow(ShaderAnimationDemoType.entries.first())
    val selectedDemoType = _selectedDemoType.asStateFlow()
    private val _controlsState = MutableStateFlow(ControlsState.COLLAPSED)
    val controlsState = _controlsState.asStateFlow()
    override val kubriko = selectedDemoType.map { shaderAnimationDemoHolders[it]?.kubriko }

    fun onSelectedDemoTypeChanged(selectedDemoType: ShaderAnimationDemoType) = _selectedDemoType.update { selectedDemoType }

    fun onControlsStateChanged(controlsState: ControlsState) = _controlsState.update { controlsState }

    override fun navigateBack(
        isInFullscreenMode: Boolean,
        onFullscreenModeToggled: () -> Unit,
    ) = (controlsState.value != ControlsState.COLLAPSED).also {
        if (it) {
            onControlsStateChanged(ControlsState.COLLAPSED)
        }
    }

    override fun dispose() = shaderAnimationDemoHolders.values.forEach { it.kubriko.dispose() }
}