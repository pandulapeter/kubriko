package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleClick
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleDragAndPan
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox
import kotlin.math.roundToInt

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetadataIndicator(
                gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
                mouseWorldPosition = EditorController.mouseWorldPosition.collectAsState().value,
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            EngineCanvas(
                modifier = Modifier
                    .weight(1f)
                    .handleMouseZoom()
                    .handleMouseMove()
                    .handleDragAndPan()
                    .handleClick()
                    .background(Color.White),
            )
            GameObjectPanel(
                gameObject = EditorController.selectedGameObject.collectAsState().value,
            )
        }
    }
}

@Composable
private fun GameObjectPanel(
    gameObject: GameObject?,
) = Column(
    modifier = Modifier
        .fillMaxHeight()
        .width(160.dp)
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    if (gameObject != null) {
        Text(
            modifier = Modifier.clickable(onClick = EditorController::unselectGameObject).padding(8.dp),
            text = "Unselect"
        )
        Text(
            modifier = Modifier.clickable(onClick = EditorController::deleteGameObject).padding(8.dp),
            text = "Delete"
        )
        when (gameObject) {
            is StaticBox -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = gameObject.color)
                        .clickable {
                            EditorController.updateSelectedGameObject(
                                gameObject.copy(
                                    color = Color.hsv((0..360).random().toFloat(), 0.2f, 0.9f)
                                )
                            )
                        }
                )
                Slider(
                    value = gameObject.edgeSize,
                    onValueChange = {
                        EditorController.updateSelectedGameObject(
                            gameObject.copy(
                                edgeSize = it
                            )
                        )
                    },
                    valueRange = 50f..150f
                )
            }
        }
    }
}

@Composable
private fun MetadataIndicator(
    gameObjectCount: Int,
    mouseWorldPosition: Offset,
) = Text(
    text = "GameObjects: $gameObjectCount\n" +
            "Mouse: ${mouseWorldPosition.x.roundToInt()}|${mouseWorldPosition.y.roundToInt()}",
)