package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorText
import kotlin.math.roundToInt


@Composable
internal fun MetadataIndicatorPanel(
    gameObjectCount: Int,
    mouseWorldPosition: Offset,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    EditorText(
        text = "Object count: $gameObjectCount",
    )
    EditorText(
        text = "${mouseWorldPosition.x.roundToInt()}:${mouseWorldPosition.y.roundToInt()}",
    )
}