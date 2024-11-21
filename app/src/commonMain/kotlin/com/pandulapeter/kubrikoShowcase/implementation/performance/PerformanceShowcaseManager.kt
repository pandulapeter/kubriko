package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
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
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.KeyboardInputListener
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kubriko.app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException
import kotlin.math.abs

internal class PerformanceShowcaseManager(
    private val mapJson: StateFlow<String>?,
) : Manager(), KeyboardInputAware, Visible, Unique, Overlay {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>
    lateinit var stateManager: StateManager
        private set
    lateinit var viewportManager: ViewportManager
        private set
    override var position = SceneOffset.Zero
    override var boundingBox = SceneSize.Zero
    override val drawingOrder = Float.MAX_VALUE
    private var overlayAlpha = 1f

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        serializationManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
        mapJson.let {
            it?.onEach(::processJson)?.launchIn(scope) ?: loadMap()
        }
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        boundingBox = (viewportManager.topLeft.value - viewportManager.bottomRight.value).let {
            SceneSize((abs(it.x.raw) + 50).scenePixel, (abs(it.y.raw) + 50).scenePixel)
        }
        position = viewportManager.cameraPosition.value
        if (actorManager.allActors.value.size > 1) {
            actorManager.add(
                RippleShader((metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
            )
            if (overlayAlpha > 0) {
                overlayAlpha -= 0.003f * deltaTimeInMillis
            }
        }
    }

    // TODO: There should be a simpler way of drawing a background than making this Manager an Actor.
    override fun DrawScope.draw() {
        if (overlayAlpha < 1f) {
            drawRect(
                color = Color.White,
                size = boundingBox.raw,
            )
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
            processJson(Res.readBytes("files/scenes/$SCENE_NAME.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        actorManager.removeAll()
        actorManager.add(this)
        val deserializedActors = serializationManager.deserializeActors(json)
        val allActors = listOf(
            KeyboardInputListener(),
            ChromaticAberrationShader(),
            VignetteShader(),
            SmoothPixelationShader()
        ) + deserializedActors
        actorManager.add(actors = allActors.toTypedArray())
    }

    companion object {
        const val SCENE_NAME = "scene_performance_test"
    }
}