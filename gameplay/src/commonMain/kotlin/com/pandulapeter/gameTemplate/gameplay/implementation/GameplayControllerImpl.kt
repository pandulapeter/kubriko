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
        Engine.get().stateManager.isFocused.onEach { isFocused ->
            if (!isFocused) {
                Engine.get().stateManager.updateIsRunning(false)
            }
        }.launchIn(this)
    }

    private const val RECTANGLE_SIZE = 100f
    private const val RECTANGLE_DISTANCE = 100f
    private const val RECTANGLE_COUNT = 50

    override fun start() {
        Engine.get().gameObjectManager.register(
            (0..360).let { angleRange ->
                (50..100).let { sizeRange ->
                    (-80..80).let { offsetRange ->
                        (-RECTANGLE_COUNT..RECTANGLE_COUNT).flatMap { x ->
                            (-RECTANGLE_COUNT..RECTANGLE_COUNT).map { y ->
                                StaticBox(
                                    color = Color.hsv(angleRange.random().toFloat(), 0.2f, 0.9f),
                                    edgeSize = RECTANGLE_SIZE * (sizeRange.random() / 100f),
                                    position = Offset(x * RECTANGLE_DISTANCE + offsetRange.random(), y * RECTANGLE_DISTANCE + offsetRange.random()),
                                    rotationDegrees = angleRange.random().toFloat(),
                                )
                            }
                        }
                    }
                }
            } + (0..360).let { angleRange ->
                (50..100).let { sizeRange ->
                    (-80..80).let { offsetRange ->
                        (-RECTANGLE_COUNT..RECTANGLE_COUNT).flatMap { x ->
                            (-RECTANGLE_COUNT..RECTANGLE_COUNT).map { y ->
                                DynamicBox(
                                    color = Color.hsv(angleRange.random().toFloat(), 0.2f, 0.9f),
                                    edgeSize = RECTANGLE_SIZE * (sizeRange.random() / 100f),
                                    position = Offset(x * RECTANGLE_DISTANCE + offsetRange.random(), y * RECTANGLE_DISTANCE + offsetRange.random()),
                                    rotationDegrees = angleRange.random().toFloat(),
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    override fun updateIsRunning(isRunning: Boolean) = Engine.get().stateManager.updateIsRunning(isRunning)
}