package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.filter_logs
import kubriko.tools.debug_menu.generated.resources.ic_filter_off
import kubriko.tools.debug_menu.generated.resources.log_priority_high
import kubriko.tools.debug_menu.generated.resources.log_priority_low
import kubriko.tools.debug_menu.generated.resources.log_priority_medium
import kubriko.tools.debug_menu.generated.resources.logs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LogsHeader() = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        modifier = Modifier.weight(1f).padding(end = 8.dp),
        style = TextStyle.Default.copy(fontSize = 10.sp),
        fontWeight = FontWeight.Bold,
        text = stringResource(Res.string.logs),
    )
    FilterPriorityToggle(
        filterName = Res.string.log_priority_low,
        onClick = {}, // TODO
    )
    FilterPriorityToggle(
        filterName = Res.string.log_priority_medium,
        onClick = {}, // TODO
    )
    FilterPriorityToggle(
        filterName = Res.string.log_priority_high,
        onClick = {}, // TODO
    )
    Image(
        modifier = Modifier
            .size(24.dp)
            .clickable { } // TODO
            .padding(6.dp),
        painter = painterResource(Res.drawable.ic_filter_off),
        contentDescription = stringResource(Res.string.filter_logs),
    )
}

@Composable
private fun FilterPriorityToggle(
    filterName: StringResource,
    onClick: () -> Unit,
) = Text(
    modifier = Modifier
        .defaultMinSize(minWidth = 24.dp)
        .clickable(onClick = onClick)
        .padding(6.dp),
    textAlign = TextAlign.Center,
    style = TextStyle.Default.copy(fontSize = 10.sp),
    text = stringResource(filterName),
)