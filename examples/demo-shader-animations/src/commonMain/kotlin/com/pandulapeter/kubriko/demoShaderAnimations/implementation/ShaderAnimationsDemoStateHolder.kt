package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsState
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

sealed interface ShaderAnimationsDemoStateHolder : StateHolder

internal class ShaderAnimationsDemoStateHolderImpl : ShaderAnimationsDemoStateHolder {
    val shaderManager = ShaderManager.newInstance()
    val shaderAnimationDemoHolders = ShaderAnimationDemoType.entries.associateWith {
        when (it) {
            ShaderAnimationDemoType.CLOUD -> ShaderAnimationDemoHolder(
                shader = CloudShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "cloud"
            )

            ShaderAnimationDemoType.ETHER -> ShaderAnimationDemoHolder(
                shader = EtherShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "ether"
            )

            ShaderAnimationDemoType.GRADIENT -> ShaderAnimationDemoHolder(
                shader = GradientShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "gradient"
            )

            ShaderAnimationDemoType.NOODLE -> ShaderAnimationDemoHolder(
                shader = NoodleShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "noodle"
            )

            ShaderAnimationDemoType.WARP -> ShaderAnimationDemoHolder(
                shader = WarpShader(),
                updater = { shader, state -> shader.updateState(state) },
                nameForLogging = "warp"
            )
        }
    }.toPersistentMap()
    private val _selectedDemoType = MutableStateFlow(ShaderAnimationDemoType.entries.first())
    val selectedDemoType = _selectedDemoType.asStateFlow()
    private val _controlsState = MutableStateFlow(ControlsState.COLLAPSED)
    val controlsState = _controlsState.asStateFlow()
    override val kubriko =  selectedDemoType.map { shaderAnimationDemoHolders[it]?.kubriko }

    fun onSelectedDemoTypeChanged(selectedDemoType: ShaderAnimationDemoType) = _selectedDemoType.update { selectedDemoType }

    fun onControlsStateChanged(controlsState: ControlsState) = _controlsState.update { controlsState }

    override fun dispose() = shaderAnimationDemoHolders.values.forEach { it.kubriko.dispose() }
}