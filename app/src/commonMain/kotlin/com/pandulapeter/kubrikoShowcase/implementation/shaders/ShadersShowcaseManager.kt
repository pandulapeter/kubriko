package com.pandulapeter.kubrikoShowcase.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.collection.CloudShader
import com.pandulapeter.kubriko.shader.collection.FractalShader
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
    private val _cloudProperties = MutableStateFlow(CloudShader.Properties())
    val cloudProperties = _cloudProperties.asStateFlow()
    private val _fractalProperties = MutableStateFlow(FractalShader.Properties())
    val fractalProperties = _fractalProperties.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { actorManager.removeAll() }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = actorManager.add(
        when (demoType.value) {
            ShaderDemoType.FRACTAL -> FractalShader(
                properties = fractalProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )

            ShaderDemoType.CLOUDS -> CloudShader(
                properties = cloudProperties.value.copy(
                    time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                ),
            )
        }
    )

    fun setSelectedDemoType(demoType: ShaderDemoType) = _demoType.update { demoType }

    fun setCloudProperties(properties: CloudShader.Properties) = _cloudProperties.update { properties }

    fun setFractalProperties(properties: FractalShader.Properties) = _fractalProperties.update { properties }
}