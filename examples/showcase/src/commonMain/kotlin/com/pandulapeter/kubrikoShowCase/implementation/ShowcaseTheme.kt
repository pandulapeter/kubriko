package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
internal fun ShowcaseTheme(
    content: @Composable () -> Unit
) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    content = content,
)