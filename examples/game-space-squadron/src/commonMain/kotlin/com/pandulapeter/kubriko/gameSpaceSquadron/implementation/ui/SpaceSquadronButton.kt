package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SpaceSquadronButton(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource? = null,
    onButtonPressed: () -> Unit,
    onPointerEnter: (() -> Unit),
) {
    val alpha = remember { mutableStateOf(0.8f) }
    FloatingActionButton(
        modifier = modifier
            .height(40.dp)
            .alpha(alpha.value)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                alpha.value = 1f
                                onPointerEnter()
                            }

                            PointerEventType.Exit -> {
                                alpha.value = 0.8f
                            }
                        }
                    }
                }
            },
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        contentColor = if (alpha.value == 0.8f) MaterialTheme.colorScheme.onPrimary else Color.White,
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