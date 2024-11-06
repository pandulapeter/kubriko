package com.pandulapeter.kubrikoPerformanceTest.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serializationManager.SerializationManager
import com.pandulapeter.kubriko.shaderManager.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaderManager.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaderManager.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoPerformanceTest.implementation.actors.KeyboardInputListener
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kubriko.examples.test_performance.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

// TODO: There should be a simpler way of drawing a background than making this Manager an Actor.
internal class GameplayManager : Manager(), KeyboardInputAware, Visible, Unique {

    private lateinit var actorManager: ActorManager
    private lateinit var serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>
    lateinit var stateManager: StateManager
        private set
    lateinit var viewportManager: ViewportManager
        private set
    override var position = SceneOffset.Zero
    override var boundingBox = SceneSize.Zero
    override val drawingOrder = Float.MAX_VALUE

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        serializationManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
        loadMap(SCENE_NAME)
    }

    override fun onAdd(kubriko: Kubriko) {
        viewportManager.cameraPosition.onEach { position = it }.launchIn(scope)
        combine(
            viewportManager.size,
            viewportManager.cameraPosition,
            viewportManager.scaleFactor,
        ) { viewportSize, viewportCenter, viewportScaleFactor ->
            val viewportTopLeft = Offset.Zero.toSceneOffset(
                viewportCenter = viewportCenter,
                viewportSize = viewportSize,
                viewportScaleFactor = viewportScaleFactor,
            )
            val viewportBottomRight = Offset(viewportSize.width, viewportSize.height).toSceneOffset(
                viewportCenter = viewportCenter,
                viewportSize = viewportSize,
                viewportScaleFactor = viewportScaleFactor,
            )
            viewportBottomRight to viewportTopLeft
        }.onEach { (viewportBottomRight, viewportTopLeft) ->
            boundingBox = (viewportBottomRight - viewportTopLeft).let { SceneSize(it.x + 50f.scenePixel, it.y + 50f.scenePixel) }
        }.launchIn(scope)
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = Color.White,
        size = boundingBox.raw,
    )

    override fun onKeyReleased(key: Key) = when (key) {
        Key.Escape, Key.Back, Key.Backspace -> stateManager.updateIsRunning(!stateManager.isRunning.value)
        else -> Unit
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = scope.launch {
        try {
            val actors = listOf(
                this@GameplayManager,
                ChromaticAberrationShader(),
                VignetteShader(),
                SmoothPixelationShader(),
                KeyboardInputListener(),
            ) + serializationManager.deserializeActors(Res.readBytes("files/scenes/$mapName.json").decodeToString())
            actorManager.add(actors = actors.toTypedArray())
        } catch (_: MissingResourceException) {
        }
    }

    companion object {
        const val SCENE_NAME = "scene_performance_test"
    }
}