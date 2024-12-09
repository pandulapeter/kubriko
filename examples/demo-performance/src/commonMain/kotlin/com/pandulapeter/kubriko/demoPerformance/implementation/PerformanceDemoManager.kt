package com.pandulapeter.kubriko.demoPerformance.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_performance.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class PerformanceDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
) : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        serializationManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
        actorManager.allActors
            .filter { it.isNotEmpty() }
            .onEach {
                delay(100)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap() = scope.launch {
        try {
            val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
            sceneJson?.update { json } ?: processJson(json)
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        _shouldShowLoadingIndicator.update { true }
        actorManager.removeAll()
        val deserializedActors = serializationManager.deserializeActors(json)
        actorManager.add(deserializedActors)
    }

    companion object {
        const val SCENE_NAME = "scene_performance_test.json"
    }
}