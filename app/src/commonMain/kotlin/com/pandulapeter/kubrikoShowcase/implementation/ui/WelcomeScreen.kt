package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.welcome_message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WelcomeScreen(
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier.padding(16.dp),
    style = MaterialTheme.typography.bodySmall,
    text = stringResource(Res.string.welcome_message),
)