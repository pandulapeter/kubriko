package com.pandulapeter.kubriko.demoCustomShaders.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.FractalShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class CustomShadersDemoManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private val _demoType = MutableStateFlow(CustomShaderType.FRACTAL)
    val demoType = _demoType.asStateFlow()
    private val fractalShader by lazy { FractalShader() }
    private val _fractalState by lazy { MutableStateFlow(fractalShader.state) }
    val fractalState get() = _fractalState.asStateFlow()
    private val cloudShader by lazy { CloudShader() }
    private val _cloudState by lazy { MutableStateFlow(cloudShader.state) }
    val cloudState get() = _cloudState.asStateFlow()
    private val warpShader by lazy { WarpShader() }
    private val _warpState by lazy { MutableStateFlow(warpShader.state) }
    val warpState get() = _warpState.asStateFlow()
    private val gradientShader by lazy { GradientShader() }
    private val _gradientState by lazy { MutableStateFlow(gradientShader.state) }
    val gradientState get() = _gradientState.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { demoType ->
            actorManager.removeAll()
            actorManager.add(
                when (demoType) {
                    CustomShaderType.FRACTAL -> fractalShader
                    CustomShaderType.CLOUDS -> cloudShader
                    CustomShaderType.WARP -> warpShader
                    CustomShaderType.GRADIENT -> gradientShader
                }
            )
        }.launchIn(scope)
        fractalState.onEach { fractalShader.updateState(it) }.launchIn(scope)
        cloudState.onEach { cloudShader.updateState(it) }.launchIn(scope)
        warpState.onEach { warpShader.updateState(it) }.launchIn(scope)
        gradientState.onEach { gradientShader.updateState(it) }.launchIn(scope)
    }

    fun setSelectedDemoType(demoType: CustomShaderType) = _demoType.update { demoType }

    fun setCloudState(state: CloudShader.State) = _cloudState.update { state }

    fun setFractalState(state: FractalShader.State) = _fractalState.update { state }

    fun setWarpState(state: WarpShader.State) = _warpState.update { state }

    fun setGradientState(state: GradientShader.State) = _gradientState.update { state }
}