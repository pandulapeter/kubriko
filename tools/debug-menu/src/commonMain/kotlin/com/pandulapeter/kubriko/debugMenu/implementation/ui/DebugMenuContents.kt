package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import kotlin.math.roundToInt

@Composable
internal fun DebugMenuContents(
    modifier: Modifier = Modifier,
    debugMenuMetadata: DebugMenuMetadata,
    onIsDebugOverlayEnabledChanged: () -> Unit,
) = Column(
    modifier = modifier.width(100.dp).padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        style = TextStyle.Default.copy(fontSize = 10.sp),
        text = "FPS: ${debugMenuMetadata.fps.roundToInt()}\n" +
                "Total Actors: ${debugMenuMetadata.totalActorCount}\n" +
                "Visible within viewport: ${debugMenuMetadata.visibleActorWithinViewportCount}\n" +
                "Play time in seconds: ${debugMenuMetadata.playTimeInSeconds}"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = debugMenuMetadata.isDebugOverlayEnabled,
                onClick = onIsDebugOverlayEnabledChanged,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "Debug overlay",
            style = TextStyle.Default.copy(fontSize = 10.sp),
        )
        Switch(
            checked = debugMenuMetadata.isDebugOverlayEnabled,
            onCheckedChange = { onIsDebugOverlayEnabledChanged() },
        )
    }
}