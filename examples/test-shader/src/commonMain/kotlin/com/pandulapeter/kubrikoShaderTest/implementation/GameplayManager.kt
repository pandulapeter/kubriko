package com.pandulapeter.kubrikoShaderTest.implementation

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

internal class GameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private val _demoType = MutableStateFlow(DemoType.CLOUDS)
    val demoType = _demoType.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        demoType.onEach { actorManager.removeAll() }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = actorManager.add(
        when (demoType.value) {
            DemoType.CLOUDS -> CloudShader(
                time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
            )

            DemoType.FRACTAL -> FractalShader(
                time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
            )
        }
    )

    fun setSelectedDemoType(demoType: DemoType) = _demoType.update { demoType }
}