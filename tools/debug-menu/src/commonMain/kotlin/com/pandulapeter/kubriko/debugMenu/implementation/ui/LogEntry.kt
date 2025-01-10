package com.pandulapeter.kubriko.debugMenu.implementation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.logger.Logger

@Composable
internal fun LogEntry(
    entry: Logger.Entry
) = Text(
    modifier = Modifier
        .padding(horizontal = 8.dp)
        .padding(top = 2.dp),
    style = TextStyle.Default.copy(fontSize = 10.sp),
    text = entry.source.let { source ->
        if (source == null) entry.message else "${entry.source}: ${entry.message}"
    }
)