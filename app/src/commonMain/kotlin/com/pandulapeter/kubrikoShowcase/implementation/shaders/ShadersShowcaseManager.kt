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
    private val fractalShader by lazy { FractalShader() }
    val fractalState get() = fractalShader.state
    private val cloudShader by lazy { CloudShader() }
    val cloudState get() = cloudShader.state
    private val warpShader by lazy { WarpShader() }
    val warpState get() = warpShader.state
    private val gradientShader by lazy { GradientShader() }
    val gradientState get() = gradientShader.state

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { demoType ->
            actorManager.removeAll()
            actorManager.add(
                when (demoType) {
                    ShaderDemoType.FRACTAL -> fractalShader
                    ShaderDemoType.CLOUDS -> cloudShader
                    ShaderDemoType.WARP -> warpShader
                    ShaderDemoType.GRADIENT -> gradientShader
                }
            )
        }.launchIn(scope)
    }

    fun setSelectedDemoType(demoType: ShaderDemoType) = _demoType.update { demoType }

    fun setCloudState(state: CloudShader.State) = cloudShader.updateState(state)

    fun setFractalState(state: FractalShader.State) = fractalShader.updateState(state)

    fun setWarpState(state: WarpShader.State) = warpShader.updateState(state)

    fun setGradientState(state: GradientShader.State) = gradientShader.updateState(state)
}