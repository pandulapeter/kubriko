package com.pandulapeter.kubrikoShowcase.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.collection.CloudShader
import com.pandulapeter.kubriko.shader.collection.FractalShader
import com.pandulapeter.kubriko.shader.collection.GradientShader
import com.pandulapeter.kubriko.shader.collection.WarpShader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class ShadersShowcaseManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private val _demoType = MutableStateFlow(ShaderDemoType.FRACTAL)
    val demoType = _demoType.asStateFlow()
    private val _cloudState = MutableStateFlow(CloudShader.State())
    val cloudProperties = _cloudState.asStateFlow()
    private val _fractalState = MutableStateFlow(FractalShader.State())
    val fractalProperties = _fractalState.asStateFlow()
    private val _warpState = MutableStateFlow(WarpShader.State())
    val warpProperties = _warpState.asStateFlow()
    private val _gradientState = MutableStateFlow(GradientShader.State())
    val gradientProperties = _gradientState.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { actorManager.removeAll() }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = actorManager.add(
        when (demoType.value) {
            ShaderDemoType.FRACTAL -> FractalShader(
                initialState = fractalProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )

            ShaderDemoType.CLOUDS -> CloudShader(
                initialState = cloudProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )

            ShaderDemoType.WARP -> WarpShader(
                initialState = warpProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )

            ShaderDemoType.GRADIENT -> GradientShader(
                initialState = gradientProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )
        }
    )

    fun setSelectedDemoType(demoType: ShaderDemoType) = _demoType.update { demoType }

    fun setCloudState(properties: CloudShader.State) = _cloudState.update { properties }

    fun setFractalState(properties: FractalShader.State) = _fractalState.update { properties }

    fun setWarpState(properties: WarpShader.State) = _warpState.update { properties }

    fun setGradientState(properties: GradientShader.State) = _gradientState.update { properties }
}