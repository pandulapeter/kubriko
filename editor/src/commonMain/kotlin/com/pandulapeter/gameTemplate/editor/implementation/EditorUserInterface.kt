package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleClick
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseDrag
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ClickableText
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ColorfulPropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.RotatablePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.VisiblePropertyEditors
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import kotlin.math.roundToInt

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.weight(1f),
        ) {
            EngineCanvas(
                modifier = Modifier
                    .weight(1f)
                    .handleMouseMove()
                    .handleMouseZoom()
                    .handleMouseDrag()
                    .handleClick()
                    .background(Color.White),
            )
            GameObjectPanel(
                data = EditorController.selectedGameObject.collectAsState().value,
            )
        }
        MetadataIndicatorRow(
            gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
            mouseWorldPosition = EditorController.mouseWorldPosition.collectAsState().value,
        )
    }
}

@Composable
private fun GameObjectPanel(
    data: Pair<GameObject?, Boolean>,
) = Column(
    modifier = Modifier
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .width(200.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    data.first.let { gameObject ->
        if (gameObject != null) {
            ClickableText(
                onClick = EditorController::unselectGameObject,
                text = "Unselect",
            )
            ClickableText(
                onClick = EditorController::deleteGameObject,
                text = "Delete",
            )
            Divider()
        }
        if (gameObject is Colorful) {
            ColorfulPropertyEditors(gameObject to data.second)
        }
        if (gameObject is Rotatable) {
            RotatablePropertyEditors(gameObject to data.second)
        }
        if (gameObject is Visible) {
            VisiblePropertyEditors(gameObject to data.second)
        }
    }
}

@Composable
private fun MetadataIndicatorRow(
    gameObjectCount: Int,
    mouseWorldPosition: Offset,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Text(
        style = MaterialTheme.typography.caption,
        text = "Object count: $gameObjectCount",
    )
    Text(
        style = MaterialTheme.typography.caption,
        text = "${mouseWorldPosition.x.roundToInt()}:${mouseWorldPosition.y.roundToInt()}",
    )
}