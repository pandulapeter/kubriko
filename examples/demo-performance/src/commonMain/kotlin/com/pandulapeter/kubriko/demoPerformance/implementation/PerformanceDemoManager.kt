package com.pandulapeter.kubriko.demoPerformance.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.shader.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shader.collection.RippleShader
import com.pandulapeter.kubriko.shader.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shader.collection.VignetteShader
import kotlinx.coroutines.flow.MutableStateFlow
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
) : Manager(), Unique, Overlay {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    override val overlayDrawingOrder = Float.MIN_VALUE
    private var overlayAlpha = 1f

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        serializationManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (actorManager.allActors.value.size > 1 && overlayAlpha > 0) {
            overlayAlpha -= 0.003f * deltaTimeInMillis
        }
    }

    override fun DrawScope.drawToViewport() {
        if (overlayAlpha > 0f) {
            drawRect(
                color = Color.Black.copy(alpha = overlayAlpha),
                size = size,
                topLeft = Offset.Zero,
            )
        }
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
        overlayAlpha = 1f
        actorManager.removeAll()
        actorManager.add(this)
        val deserializedActors = serializationManager.deserializeActors(json)
        actorManager.add(
            listOf(
                SmoothPixelationShader(),
                RippleShader(layerIndex = 0),
                ChromaticAberrationShader(layerIndex = 0),
                VignetteShader(),
            ) + deserializedActors
        )
    }

    companion object {
        const val SCENE_NAME = "scene_performance_test.json"
    }
}