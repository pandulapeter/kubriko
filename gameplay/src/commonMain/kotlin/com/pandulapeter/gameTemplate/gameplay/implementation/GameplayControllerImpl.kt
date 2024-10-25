package com.pandulapeter.gameTemplate.gameplay.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.gameplay.GameplayController
import com.pandulapeter.gameTemplate.gameplay.implementation.gameObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameplay.implementation.gameObjects.StaticBox
import com.pandulapeter.gameTemplate.gameplay.models.Metadata
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
    ) { fps, totalGameObjectCount, visibleGameObjectCount ->
        Metadata(
            fps = fps,
            totalGameObjectCount = totalGameObjectCount,
            visibleGameObjectCount = visibleGameObjectCount,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())

    init {
        Engine.get().stateManager.isFocused
            .filterNot { it }
            .onEach { Engine.get().stateManager.updateIsRunning(false) }
            .launchIn(this)
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
    }

    private const val RECTANGLE_SIZE = 100f
    private const val RECTANGLE_DISTANCE = 100f
    private const val RECTANGLE_COUNT = 50

    override fun start() {
        Engine.get().gameObjectManager.register(
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
            }
        )
    }

    override fun updateIsRunning(isRunning: Boolean) = Engine.get().stateManager.updateIsRunning(isRunning)
}