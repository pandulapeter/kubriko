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

import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsState
import com.pandulapeter.kubriko.shaders.ShaderManager
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
