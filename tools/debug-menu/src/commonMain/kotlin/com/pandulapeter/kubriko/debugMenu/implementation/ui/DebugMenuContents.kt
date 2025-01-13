package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import com.pandulapeter.kubriko.logger.Logger
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
    shouldUseVerticalLayout: Boolean,
    lazyListState: LazyListState = rememberLazyListState(),
) = Row(
    modifier = Modifier
        .width(120.dp + windowInsets.only(WindowInsetsSides.Right).asPaddingValues().calculateRightPadding(LocalLayoutDirection.current))
        .windowInsetsPadding(windowInsets.only(if (shouldUseVerticalLayout) WindowInsetsSides.Right else WindowInsetsSides.Horizontal)),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    if (!shouldUseVerticalLayout) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Metadata(
                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 4.dp,
                ),
                debugMenuMetadata = debugMenuMetadata,
            )
            CollisionMasksSwitch(
                debugMenuMetadata = debugMenuMetadata,
                onIsDebugOverlayEnabledChanged = onIsDebugOverlayEnabledChanged,
            )
            LogsHeader(
                modifier = Modifier.padding(vertical = 4.dp),
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
    }
    LazyColumn(
        modifier = if (shouldUseVerticalLayout) Modifier else Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = windowInsets.only(WindowInsetsSides.Vertical).asPaddingValues().let {
            PaddingValues(
                start = it.calculateStartPadding(LocalLayoutDirection.current),
                top = it.calculateTopPadding() + 8.dp,
                end = it.calculateEndPadding(LocalLayoutDirection.current),
                bottom = it.calculateBottomPadding() + 8.dp,
            )
        },
        state = lazyListState,
    ) {
        if (shouldUseVerticalLayout) {
            item("metadata") {
                Metadata(
                    debugMenuMetadata = debugMenuMetadata,
                )
            }
            item("collisionMasksSwitch") {
                CollisionMasksSwitch(
                    debugMenuMetadata = debugMenuMetadata,
                    onIsDebugOverlayEnabledChanged = onIsDebugOverlayEnabledChanged,
                )
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
    }
}

@Composable
private fun Metadata(
    modifier: Modifier = Modifier,
    debugMenuMetadata: DebugMenuMetadata,
) = Text(
    modifier = modifier
        .padding(horizontal = 8.dp)
        .padding(bottom = 2.dp),
    style = MaterialTheme.typography.labelSmall,
    text = "FPS: ${debugMenuMetadata.fps.roundToInt()}\n" +
            "Total Actors: ${debugMenuMetadata.totalActorCount}\n" +
            "Visible within viewport: ${debugMenuMetadata.visibleActorWithinViewportCount}\n" +
            "Play time in seconds: ${debugMenuMetadata.playTimeInSeconds}"
)

@Composable
private fun CollisionMasksSwitch(
    debugMenuMetadata: DebugMenuMetadata,
    onIsDebugOverlayEnabledChanged: () -> Unit,
) = Row(
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