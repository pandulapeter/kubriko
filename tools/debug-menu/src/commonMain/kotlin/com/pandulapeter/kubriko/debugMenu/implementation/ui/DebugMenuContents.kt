package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import kotlin.math.roundToInt

@Composable
internal fun DebugMenuContents(
    modifier: Modifier = Modifier,
    debugMenuMetadata: DebugMenuMetadata,
    onIsDebugOverlayEnabledChanged: () -> Unit,
) = LazyColumn(
    modifier = modifier.width(100.dp).padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    item("metadata") {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle.Default.copy(fontSize = 10.sp),
            text = "FPS: ${debugMenuMetadata.fps.roundToInt()}\n" +
                    "Total Actors: ${debugMenuMetadata.totalActorCount}\n" +
                    "Visible within viewport: ${debugMenuMetadata.visibleActorWithinViewportCount}\n" +
                    "Play time in seconds: ${debugMenuMetadata.playTimeInSeconds}"
        )
    }
    item("collisionMasksSwitch") {
        Row(
            modifier = modifier.selectable(
                selected = debugMenuMetadata.isDebugOverlayEnabled,
                onClick = onIsDebugOverlayEnabledChanged,
            ).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Show collision masks",
                style = TextStyle.Default.copy(fontSize = 10.sp),
            )
            Switch(
                modifier = Modifier.scale(0.6f).height(24.dp),
                checked = debugMenuMetadata.isDebugOverlayEnabled,
                onCheckedChange = { onIsDebugOverlayEnabledChanged() },
            )
        }
    }
    item("logsHeader") {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle.Default.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            text = "Logs",
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
    itemsIndexed(
        items = DebugMenu.logs.value,
        key = { index, _ -> "log_$index" }
    ) { _, message ->
        Text(
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 2.dp),
            style = TextStyle.Default.copy(fontSize = 10.sp),
            text = message,
        )
    }
}