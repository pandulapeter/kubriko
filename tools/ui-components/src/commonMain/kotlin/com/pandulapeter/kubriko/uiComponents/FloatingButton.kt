package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun FloatingButton(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    isSelected: Boolean = false,
    contentDescription: String? = null,
    onButtonPressed: () -> Unit,
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.surface else if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    FloatingActionButton(
        modifier = modifier.size(40.dp),
        containerColor = containerColor,
        onClick = onButtonPressed,
    ) {
        Icon(
            painter = painterResource(icon),
            tint = contentColorFor(containerColor),
            contentDescription = contentDescription,
        )
    }
}