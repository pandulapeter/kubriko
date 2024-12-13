package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LargeButton(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource,
    onButtonPressed: () -> Unit,
) = FloatingActionButton(
    modifier = modifier.height(40.dp),
    onClick = onButtonPressed,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(title),
        )
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = stringResource(title),
        )
    }
}