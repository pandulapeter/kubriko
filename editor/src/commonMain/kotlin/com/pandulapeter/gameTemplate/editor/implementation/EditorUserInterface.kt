package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleClick
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseDrag
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ClickableText
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ColorfulPropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.GameObjectTypeRadioButton
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.RotatablePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ScalablePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.VisiblePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.selectedGameObjectHighlight
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_delete
import game.editor.generated.resources.ic_locate
import org.jetbrains.compose.resources.painterResource
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
                editorSelectedGameObjectHighlight = { selectedGameObjectHighlight(it) },
            )
            GameObjectPanel(
                data = EditorController.selectedGameObject.collectAsState().value,
                selectedGameObjectType = EditorController.selectedGameObjectType.collectAsState().value,
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
    selectedGameObjectType: Class<out GameObject>
) = LazyColumn(
    modifier = Modifier
        .fillMaxHeight()
        .width(200.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    data.first.let { gameObject ->
        if (gameObject == null) {
            items(
                items = EditorController.supportedGameObjectTypes.keys.toList(),
                key = { "typeRadioButton_${it.name}" },
            ) { gameObjectType ->
                GameObjectTypeRadioButton(
                    gameObjectType = gameObjectType,
                    selectedGameObjectType = selectedGameObjectType,
                    onSelected = { EditorController.selectGameObjectType(gameObjectType) }
                )
            }
        } else {
            item(key = "selectedTypeTitle") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.overline,
                        text = gameObject::class.java.simpleName,
                    )
                    Icon(
                        modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = EditorController::locateGameObject).padding(4.dp),
                        painter = painterResource(Res.drawable.ic_locate),
                        contentDescription = "Locate"
                    )
                    Icon(
                        modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = EditorController::deleteGameObject).padding(4.dp),
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = "Delete"
                    )
                }
                Divider()
            }
            if (gameObject is Colorful) {
                item(key = "propertyColorful") {
                    ColorfulPropertyEditors(Modifier.animateItem(), gameObject to data.second)
                }
            }
            if (gameObject is Rotatable) {
                item(key = "propertyRotatable") {
                    RotatablePropertyEditors(Modifier.animateItem(), gameObject to data.second)
                }
            }
            if (gameObject is Scalable) {
                item(key = "propertyScalable") {
                    ScalablePropertyEditors(Modifier.animateItem(), gameObject to data.second)
                }
            }
            if (gameObject is Visible) {
                item(key = "propertyVisible") {
                    VisiblePropertyEditors(Modifier.animateItem(), gameObject to data.second)
                }
            }
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