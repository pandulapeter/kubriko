package com.pandulapeter.kubriko.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SmallButton(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    onButtonPressed: () -> Unit,
    contentDescription: StringResource?,
    contentColor: Color? = null,
) {
    FloatingActionButton(
        modifier = modifier.size(40.dp),
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary,
        onClick = onButtonPressed,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription?.let { stringResource(it) },
        )
    }
}