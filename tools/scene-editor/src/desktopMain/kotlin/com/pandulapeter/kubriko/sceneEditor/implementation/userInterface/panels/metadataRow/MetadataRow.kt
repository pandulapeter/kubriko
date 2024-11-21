package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.metadataRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.roundToInt


@Composable
internal fun MetadataRow(
    modifier: Modifier = Modifier,
    totalActorCount: Int,
    mouseSceneOffset: SceneOffset,
) = EditorSurface(
    modifier = modifier,
) {
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
            text = "Actors: $totalActorCount",
        )
        EditorText(
            text = "${mouseSceneOffset.x.raw.roundToInt()}:${mouseSceneOffset.y.raw.roundToInt()}",
        )
    }
}