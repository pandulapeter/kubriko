package com.pandulapeter.kubriko.shared.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
) = FloatingActionButton(
    modifier = modifier.size(40.dp),
    containerColor = if (isSelected) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
    onClick = onButtonPressed,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
    )
}