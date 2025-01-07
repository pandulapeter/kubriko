package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
internal fun SpaceSquadronIconButton(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    onButtonPressed: () -> Unit,
    onPointerEnter: (() -> Unit),
    contentDescription: StringResource?,
) {
    val alpha = remember { mutableStateOf(IDLE_BUTTON_ALPHA) }
    FloatingActionButton(
        modifier = modifier
            .size(40.dp)
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
                            PointerEventType.Press -> {
                                alpha.value = 1f
                            }

                            PointerEventType.Release,
                            PointerEventType.Exit -> {
                                alpha.value = IDLE_BUTTON_ALPHA
                            }
                        }
                    }
                }
            },
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        contentColor = if (alpha.value == IDLE_BUTTON_ALPHA) MaterialTheme.colorScheme.onPrimary else Color.White,
        onClick = onButtonPressed,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription?.let { stringResource(it) },
        )
    }
}