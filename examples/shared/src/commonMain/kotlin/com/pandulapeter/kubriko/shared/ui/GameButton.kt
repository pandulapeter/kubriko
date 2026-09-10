/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A Material [Surface] shaped and elevated like a `FloatingActionButton`, but with the click
 * indication supplied by [LocalIndication] instead of the Material ripple.
 *
 * `FloatingActionButton` builds its own ripple inside [Surface], which reads the ripple alpha from
 * `LocalRippleConfiguration` — a value Material 3 no longer lets themes set. Driving the indication
 * from a nested [clickable] keeps the games' custom [gameRipple] alphas and still paints the state
 * layer above the container but below the content.
 */
@Composable
fun GameButton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (isHovered) HoveredElevation else DefaultElevation,
        animationSpec = if (isHovered) {
            tween(durationMillis = 120, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 120, easing = OutgoingElevationEasing)
        },
    )
    Surface(
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = DefaultElevation,
        shadowElevation = elevation,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                    .defaultMinSize(
                        minWidth = MinimumSize,
                        minHeight = MinimumSize,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

private val DefaultElevation = 6.dp
private val HoveredElevation = 8.dp
private val MinimumSize = 56.dp
private val OutgoingElevationEasing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)
