/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoType
import com.pandulapeter.kubriko.shared.ui.ExpandControlsButton
import com.pandulapeter.kubriko.shared.ui.LocalInfoPanelVisibility
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.Panel
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.description
import kubriko.examples.demo_shader_animations.generated.resources.hide_code
import kubriko.examples.demo_shader_animations.generated.resources.ic_code
import kubriko.examples.demo_shader_animations.generated.resources.show_code
import org.jetbrains.compose.resources.stringResource

private val MaximumWidth = 300.dp

@Composable
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    selectedDemoType: ShaderAnimationDemoType,
    controlsState: ControlsState,
    onControlsStateChanged: (ControlsState) -> Unit,
    getCode: (ShaderAnimationDemoType) -> String,
    getControls: (ShaderAnimationDemoType) -> @Composable () -> Unit,
) = Column(
    modifier = modifier,
) {
    InfoPanel(
        stringResource = Res.string.description,
        isVisible = LocalInfoPanelVisibility.current,
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        val cardAlpha: Float by animateFloatAsState(
            targetValue = if (controlsState == ControlsState.COLLAPSED) 0f else 1f,
            animationSpec = tween(),
        )
        val cardEndPaddingMultiplier: Float by animateFloatAsState(
            targetValue = if (controlsState == ControlsState.EXPANDED_CODE) 1f else 0f,
            animationSpec = tween(),
        )
        Panel(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .alpha(cardAlpha)
                .padding(end = 48.dp * cardEndPaddingMultiplier),
        ) {
            AnimatedContent(
                targetState = selectedDemoType to controlsState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentAlignment = Alignment.TopStart,
                label = "transformControls"
            ) { targetState ->
                when (targetState.second) {
                    ControlsState.COLLAPSED -> Unit
                    ControlsState.EXPANDED_CODE -> Code(
                        code = getCode(targetState.first),
                    )

                    ControlsState.EXPANDED_CONTROLS -> Controls(
                        controls = getControls(targetState.first),
                    )
                }
            }
        }
        ControlButtons(
            modifier = Modifier.align(Alignment.BottomEnd),
            controlsState = controlsState,
            onControlsStateChanged = onControlsStateChanged,
        )
    }
}

@Composable
private fun ControlButtons(
    modifier: Modifier,
    controlsState: ControlsState,
    onControlsStateChanged: (ControlsState) -> Unit,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    (controlsState == ControlsState.EXPANDED_CODE).let { isSelected ->
        FloatingButton(
            icon = Res.drawable.ic_code,
            isSelected = isSelected,
            contentDescription = stringResource(if (isSelected) Res.string.hide_code else Res.string.show_code),
            onButtonPressed = { onControlsStateChanged(if (isSelected) ControlsState.COLLAPSED else ControlsState.EXPANDED_CODE) },
        )
    }
    (controlsState == ControlsState.EXPANDED_CONTROLS).let { isExpanded ->
        ExpandControlsButton(
            isExpanded = isExpanded,
            onToggle = { onControlsStateChanged(if (isExpanded) ControlsState.COLLAPSED else ControlsState.EXPANDED_CONTROLS) },
        )
    }
}

@Composable
private fun Code(
    code: String,
) = Text(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
        .padding(
            horizontal = 16.dp,
        ),
    style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
    ),
    text = code,
)

@Composable
private fun Controls(
    controls: @Composable () -> Unit,
) = Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .width(MaximumWidth)
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    controls()
    Spacer(modifier = Modifier.height(16.dp))
}
