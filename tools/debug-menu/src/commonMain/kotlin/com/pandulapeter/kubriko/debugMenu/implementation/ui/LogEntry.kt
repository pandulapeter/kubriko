package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.logger.Logger

@Composable
internal fun LogEntry(
    modifier: Modifier = Modifier,
    entry: Logger.Entry
) = Text(
    modifier = modifier
        .padding(horizontal = 8.dp)
        .padding(top = 2.dp),
    style = MaterialTheme.typography.labelSmall,
    color = getColor(entry.source),
    text = entry.source.let { source ->
        if (source == null) entry.message else "${entry.source}: ${entry.message}"
    }
)

@Composable
private fun getColor(source: String?) = if (source == null) LocalContentColor.current else Color.hsv(
    hue = source.toHue(),
    saturation = 0.8f,
    value = if (isSystemInDarkTheme()) 0.9f else 0.7f,
)

private fun String.toHue(): Float {
    val hash = substringAfterLast('@').hashCode()
    val positiveHash = hash.toLong() and 0xFFFFFFFFL
    return (positiveHash % 360).toFloat()
}