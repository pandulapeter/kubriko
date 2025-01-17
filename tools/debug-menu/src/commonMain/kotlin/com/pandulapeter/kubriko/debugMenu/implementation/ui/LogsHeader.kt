/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.implementation.InternalDebugMenu
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.uiComponents.TextInput
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.clear_logs
import kubriko.tools.debug_menu.generated.resources.filter_logs
import kubriko.tools.debug_menu.generated.resources.ic_clear
import kubriko.tools.debug_menu.generated.resources.ic_filter_off
import kubriko.tools.debug_menu.generated.resources.ic_filter_on
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_high_off
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_high_on
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_low_off
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_low_on
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_medium_off
import kubriko.tools.debug_menu.generated.resources.ic_log_filter_medium_on
import kubriko.tools.debug_menu.generated.resources.log_importance_high
import kubriko.tools.debug_menu.generated.resources.log_importance_low
import kubriko.tools.debug_menu.generated.resources.log_importance_medium
import kubriko.tools.debug_menu.generated.resources.logs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LogsHeader(
    modifier: Modifier = Modifier,
    isLowPriorityEnabled: Boolean,
    onLowPriorityToggled: () -> Unit,
    isMediumPriorityEnabled: Boolean,
    onMediumPriorityToggled: () -> Unit,
    isHighPriorityEnabled: Boolean,
    onHighPriorityToggled: () -> Unit,
    areFiltersApplied: Boolean,
) = AnimatedContent(
    targetState = InternalDebugMenu.isEditingFilter.collectAsState().value,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    contentAlignment = Alignment.Center,
) { isEditingFilter ->
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditingFilter) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                val filterText = InternalDebugMenu.filter.collectAsState().value
                TextInput(
                    modifier = Modifier.fillMaxWidth(),
                    value = filterText,
                    onValueChanged = InternalDebugMenu::onFilterUpdated,
                )
                if (filterText.isEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth().alpha(0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        text = stringResource(Res.string.filter_logs),
                    )
                }
            }
            Icon(
                isSmall = true,
                drawableResource = if (areFiltersApplied) Res.drawable.ic_filter_on else Res.drawable.ic_filter_off,
                stringResource = Res.string.filter_logs,
                onClick = InternalDebugMenu::toggleIsEditingFilter,
            )
        } else {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                text = stringResource(Res.string.logs),
            )
            Icon(
                drawableResource = if (isLowPriorityEnabled) Res.drawable.ic_log_filter_low_on else Res.drawable.ic_log_filter_low_off,
                stringResource = Res.string.log_importance_low,
                onClick = onLowPriorityToggled,
            )
            Icon(
                drawableResource = if (isMediumPriorityEnabled) Res.drawable.ic_log_filter_medium_on else Res.drawable.ic_log_filter_medium_off,
                stringResource = Res.string.log_importance_medium,
                onClick = onMediumPriorityToggled,
            )
            Icon(
                drawableResource = if (isHighPriorityEnabled) Res.drawable.ic_log_filter_high_on else Res.drawable.ic_log_filter_high_off,
                stringResource = Res.string.log_importance_high,
                onClick = onHighPriorityToggled,
            )
            Icon(
                isSmall = true,
                drawableResource = if (areFiltersApplied) Res.drawable.ic_filter_on else Res.drawable.ic_filter_off,
                stringResource = Res.string.filter_logs,
                onClick = InternalDebugMenu::toggleIsEditingFilter,
            )
            Icon(
                isEnabled = Logger.logs.collectAsState().value.isNotEmpty(),
                drawableResource = Res.drawable.ic_clear,
                stringResource = Res.string.clear_logs,
                onClick = Logger::clearLogs,
            )
        }
    }
}

@Composable
private fun Icon(
    isSmall: Boolean = false,
    isEnabled: Boolean = true,
    drawableResource: DrawableResource,
    stringResource: StringResource,
    onClick: () -> Unit,
) = Image(
    modifier = Modifier
        .size(24.dp)
        .clickable(enabled = isEnabled, onClick = onClick)
        .alpha(if (isEnabled) 1f else 0.5f)
        .padding(if (isSmall) 4.dp else 2.dp),
    colorFilter = ColorFilter.tint(LocalContentColor.current),
    painter = painterResource(drawableResource),
    contentDescription = stringResource(stringResource),
)