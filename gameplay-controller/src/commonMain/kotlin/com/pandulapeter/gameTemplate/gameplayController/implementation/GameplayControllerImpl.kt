package com.pandulapeter.gameTemplate.gameplayController.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardZoomState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.directionState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.zoomState
import com.pandulapeter.gameTemplate.gameplayController.GameplayController
import com.pandulapeter.gameTemplate.gameplayController.models.Metadata
import com.pandulapeter.gameTemplate.gameplayObjects.Character
import com.pandulapeter.gameTemplate.gameplayObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameplayObjects.Marker
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal object GameplayControllerImpl : GameplayController, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val isRunning = Engine.get().stateManager.isRunning
    override val metadata = combine(
        Engine.get().metadataManager.fps,
        Engine.get().metadataManager.totalGameObjectCount,
        Engine.get().metadataManager.visibleGameObjectCount,
        Engine.get().metadataManager.runtimeInMilliseconds,
    ) { fps, totalGameObjectCount, visibleGameObjectCount, runtimeInMilliseconds ->
        Metadata(
            fps = fps,
            totalGameObjectCount = totalGameObjectCount,
            visibleGameObjectCount = visibleGameObjectCount,
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())
    private val character = Character(Offset.Zero)

    init {
        Engine.get().stateManager.isFocused
            .filterNot { it }
            .onEach { Engine.get().stateManager.updateIsRunning(false) }
            .launchIn(this)
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyPressed
            .onEach(::handleKeyPressed)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        start()
    }

    private const val RECTANGLE_SIZE = 100f
    private const val RECTANGLE_DISTANCE = 100f
    private const val RECTANGLE_COUNT = 50

    private fun start() {
        Engine.get().gameObjectManager.add(
            listOf(true, false).let { booleanRange ->
                (0..360).let { angleRange ->
                    (50..100).let { sizeRange ->
                        (50..150).let { scaleRange ->
                            (-80..80).let { offsetRange ->
                                (-RECTANGLE_COUNT..RECTANGLE_COUNT).flatMap { x ->
                                    (-RECTANGLE_COUNT..RECTANGLE_COUNT).map { y ->
                                        if (booleanRange.random()) StaticBox(
                                            color = Color.hsv(angleRange.random().toFloat(), 0.2f, 0.9f),
                                            edgeSize = RECTANGLE_SIZE * (sizeRange.random() / 100f),
                                            position = Offset(x * RECTANGLE_DISTANCE + offsetRange.random(), y * RECTANGLE_DISTANCE + offsetRange.random()),
                                            rotationDegrees = angleRange.random().toFloat(),
                                        ) else DynamicBox(
                                            color = Color.hsv(angleRange.random().toFloat(), 0.2f, 0.9f),
                                            edgeSize = RECTANGLE_SIZE * (sizeRange.random() / 100f),
                                            position = Offset(x * RECTANGLE_DISTANCE + offsetRange.random(), y * RECTANGLE_DISTANCE + offsetRange.random()),
                                            rotationDegrees = angleRange.random().toFloat(),
                                            scaleFactor = scaleRange.random() / 100f,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } + character + Marker(Offset.Zero, true)
        )
    }

    override fun updateIsRunning(isRunning: Boolean) = Engine.get().stateManager.updateIsRunning(isRunning)

    private fun handleKeys(keys: Set<Key>) {
        if (isRunning.value) {
            character.move(keys.directionState)
            Engine.get().viewportManager.multiplyScaleFactor(
                when (keys.zoomState) {
                    KeyboardZoomState.NONE -> 1f
                    KeyboardZoomState.ZOOM_IN -> 1.02f
                    KeyboardZoomState.ZOOM_OUT -> 0.98f
                }
            )
        }
    }

    private fun handleKeyPressed(key: Key) {
        if (isRunning.value) {
            when (key) {
                Key.Spacebar -> character.triggerExplosion()
            }
        }
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> Engine.get().stateManager.updateIsRunning(!isRunning.value)
        }
    }
}