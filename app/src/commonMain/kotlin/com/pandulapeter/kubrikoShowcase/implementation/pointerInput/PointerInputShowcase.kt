package com.pandulapeter.kubrikoShowcase.implementation.pointerInput

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PointerInputShowcase() = Text(
    modifier = Modifier.padding(16.dp),
    style = MaterialTheme.typography.bodySmall,
    text = "Work in progress",
)