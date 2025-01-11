package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.launch
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.collision_masks
import kubriko.tools.debug_menu.generated.resources.logs_empty
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun DebugMenuContents(
    windowInsets: WindowInsets,
    debugMenuMetadata: DebugMenuMetadata,
    logs: List<Logger.Entry>,
    onIsDebugOverlayEnabledChanged: () -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) = LazyColumn(
    modifier = Modifier
        .width(100.dp)
        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Right)),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = windowInsets.only(WindowInsetsSides.Vertical).asPaddingValues(),
    state = lazyListState,
) {
    item("metadata") {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(
                    top = 8.dp,
                    bottom = 4.dp,
                ),
            style = MaterialTheme.typography.labelSmall,
            text = "FPS: ${debugMenuMetadata.fps.roundToInt()}\n" +
                    "Total Actors: ${debugMenuMetadata.totalActorCount}\n" +
                    "Visible within viewport: ${debugMenuMetadata.visibleActorWithinViewportCount}\n" +
                    "Play time in seconds: ${debugMenuMetadata.playTimeInSeconds}"
        )
    }
    item("collisionMasksSwitch") {
        Row(
            modifier = Modifier.selectable(
                selected = debugMenuMetadata.isDebugOverlayEnabled,
                onClick = onIsDebugOverlayEnabledChanged,
            ).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.collision_masks),
                style = MaterialTheme.typography.bodySmall,
            )
            Switch(
                modifier = Modifier.scale(0.6f).height(24.dp),
                checked = debugMenuMetadata.isDebugOverlayEnabled,
                onCheckedChange = { onIsDebugOverlayEnabledChanged() },
            )
        }
    }
    item("logsHeader") {
        LogsHeader(
            isLowPriorityEnabled = DebugMenu.isLowPriorityEnabled.collectAsState().value,
            onLowPriorityToggled = DebugMenu::onLowPriorityToggled,
            isMediumPriorityEnabled = DebugMenu.isMediumPriorityEnabled.collectAsState().value,
            onMediumPriorityToggled = DebugMenu::onMediumPriorityToggled,
            isHighPriorityEnabled = DebugMenu.isHighPriorityEnabled.collectAsState().value,
            onHighPriorityToggled = DebugMenu::onHighPriorityToggled,
            areFiltersApplied = false, // TODO
            onFiltersClicked = {},
        )
    }
    items(
        items = logs,
        key = { "log_${it.id}" }
    ) {
        LogEntry(
            modifier = Modifier.animateItem(),
            entry = it
        )
    }
    if (logs.isEmpty()) {
        item("logsEmptyState") {
            Text(
                modifier = Modifier.animateItem()
                    .padding(horizontal = 8.dp)
                    .padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                text = stringResource(Res.string.logs_empty)
            )
        }
    }
    item("bottomSpacer") {
        Spacer(modifier = Modifier.height(8.dp))
    }
}