package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.metadataRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorText
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlin.math.roundToInt


@Composable
internal fun MetadataRow(
    gameObjectCount: Int,
    mouseWorldCoordinates: WorldCoordinates,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    Divider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorText(
            text = "Instances: $gameObjectCount",
        )
        EditorText(
            text = "${mouseWorldCoordinates.x.roundToInt()}:${mouseWorldCoordinates.y.roundToInt()}",
        )
    }
}