package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.filter_logs
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
    isLowPriorityEnabled: Boolean,
    onLowPriorityToggled: () -> Unit,
    isMediumPriorityEnabled: Boolean,
    onMediumPriorityToggled: () -> Unit,
    isHighPriorityEnabled: Boolean,
    onHighPriorityToggled: () -> Unit,
    areFiltersApplied: Boolean,
    onFiltersClicked: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
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
        onClick = onFiltersClicked,
    )
}

@Composable
private fun Icon(
    isSmall: Boolean = false,
    drawableResource: DrawableResource,
    stringResource: StringResource,
    onClick: () -> Unit,
) = Image(
    modifier = Modifier
        .size(24.dp)
        .clickable(onClick = onClick)
        .padding(if (isSmall) 4.dp else 2.dp),
    colorFilter = ColorFilter.tint(LocalContentColor.current),
    painter = painterResource(drawableResource),
    contentDescription = stringResource(stringResource),
)