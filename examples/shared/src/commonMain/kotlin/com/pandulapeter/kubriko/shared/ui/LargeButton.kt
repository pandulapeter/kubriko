package com.pandulapeter.kubriko.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeButton(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource? = null,
    isEnabled: Boolean = true,
    onButtonPressed: () -> Unit,
) = CompositionLocalProvider(LocalRippleConfiguration provides if (isEnabled) LocalRippleConfiguration.current else null) {
    FloatingActionButton(
        modifier = modifier.height(40.dp).alpha(if (isEnabled) 1f else 0.5f),
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        onClick = onButtonPressed,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(title),
                )
            }
            Text(
                modifier = Modifier.padding(end = 8.dp, start = if (icon == null) 8.dp else 0.dp),
                text = stringResource(title),
            )
        }
    }
}