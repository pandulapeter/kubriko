/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    text = logEntryText(entry, TimeZone.currentSystemDefault()),
)

// TODO: Allow consumers to override this behavior
@Composable
private fun getColor(source: String?) = if (source == null) LocalContentColor.current else Color.hsv(
    hue = sourceHue(source),
    saturation = 0.2f,
    value = if (isSystemInDarkTheme()) 0.9f else 0.6f,
)

@OptIn(ExperimentalTime::class)
internal fun logEntryText(entry: Logger.Entry, timeZone: TimeZone): String = entry.source.let { source ->
    val timestamp = Instant.fromEpochMilliseconds(entry.timestamp).toLocalDateTime(timeZone).time.let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}." +
            (it.nanosecond / 1_000_000).toString().padStart(3, '0')
    }
    val message = if (source == null) entry.message else "${entry.source}: ${entry.message}"
    val suffix = if (entry.details.isNullOrBlank()) "" else "*"
    "[$timestamp] $message$suffix"
}

internal fun sourceHue(source: String): Float {
    val hash = source.substringAfterLast('@').hashCode()
    val positiveHash = hash.toLong() and 0xFFFFFFFFL
    return (positiveHash % 360).toFloat()
}
