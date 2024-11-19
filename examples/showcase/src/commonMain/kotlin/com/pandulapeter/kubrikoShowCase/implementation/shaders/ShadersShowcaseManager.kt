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
    private val _demoType = MutableStateFlow(ShaderDemoType.CLOUDS)
    val demoType = _demoType.asStateFlow()
    private val _red = MutableStateFlow(2)
    val red = _red.asStateFlow()
    private val _green = MutableStateFlow(5)
    val green = _green.asStateFlow()
    private val _blue = MutableStateFlow(12)
    val blue = _blue.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { actorManager.removeAll() }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = actorManager.add(
        when (demoType.value) {
            ShaderDemoType.CLOUDS -> CloudShader(
                time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
            )

            ShaderDemoType.FRACTAL -> FractalShader(
                time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
                red = red.value,
                green = green.value,
                blue = blue.value,
            )
        }
    )

    fun setSelectedDemoType(demoType: ShaderDemoType) = _demoType.update { demoType }

    fun setRed(red: Int) = _red.update { red }

    fun setGreen(green: Int) = _green.update { green }

    fun setBlue(blue: Int) = _blue.update { blue }
}