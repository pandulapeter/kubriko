package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SmallButton(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    onButtonPressed: () -> Unit,
    contentDescription: StringResource?,
) = FloatingActionButton(
    modifier = modifier.size(40.dp),
    onClick = onButtonPressed,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription?.let { stringResource(it) },
    )
}