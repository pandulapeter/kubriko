package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.gameplayObjects.Clickable
import com.pandulapeter.gameTemplate.gameplayObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameplayObjects.Marker
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal object EditorController : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val totalGameObjectCount = Engine.get().metadataManager.totalGameObjectCount

    init {
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        start()
    }

    private const val RECTANGLE_SIZE = 100f
    private const val RECTANGLE_DISTANCE = 100f
    private const val RECTANGLE_COUNT = 50
    private const val COORDINATE_SYSTEM_SIZE = 100
    private const val COORDINATE_GRID_SIZE = 100f

    private fun start() {
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
            } + (-COORDINATE_SYSTEM_SIZE..COORDINATE_SYSTEM_SIZE).flatMap { x ->
                (-COORDINATE_SYSTEM_SIZE..COORDINATE_SYSTEM_SIZE).map { y ->
                    Marker(
                        position = Offset(x * COORDINATE_GRID_SIZE, y * COORDINATE_GRID_SIZE),
                        isOrigin = x == 0 && y == 0,
                    )
                }
            }
        )
    }

    fun handleClick(screenCoordinates: Offset) {
        Engine.get().gameObjectManager.findGameObjectsOnScreenCoordinates(screenCoordinates).minByOrNull { it.depth }?.let { (it as? Clickable)?.onClicked()  }
    }
}