/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersState
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.Panel
import com.pandulapeter.kubriko.uiComponents.SmallSwitch
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.blur
import kubriko.examples.demo_content_shaders.generated.resources.chromatic_aberration
import kubriko.examples.demo_content_shaders.generated.resources.collapse_controls
import kubriko.examples.demo_content_shaders.generated.resources.comic
import kubriko.examples.demo_content_shaders.generated.resources.description
import kubriko.examples.demo_content_shaders.generated.resources.expand_controls
import kubriko.examples.demo_content_shaders.generated.resources.ic_brush
import kubriko.examples.demo_content_shaders.generated.resources.ripple
import kubriko.examples.demo_content_shaders.generated.resources.smooth_pixelation
import kubriko.examples.demo_content_shaders.generated.resources.vignette
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContentShadersOverlay(
    windowInsets: WindowInsets,
    areControlsExpanded: Boolean,
    state: ContentShadersState,
    onStateChanged: (ContentShadersState) -> Unit,
    onControlsToggled: () -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(windowInsets)
        .padding(16.dp),
) {
    InfoPanel(
        stringResource = Res.string.description,
        isVisible = StateHolder.isInfoPanelVisible.value,
    )
    Spacer(
        modifier = Modifier.weight(1f),
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        this@Column.AnimatedVisibility(
            visible = areControlsExpanded,
            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Panel(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                ) {
                    Controls(
                        modifier = Modifier.width(220.dp),
                        state = state,
                        onStateChanged = onStateChanged,
                    )
                }
            }
        }
        FloatingButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            icon = Res.drawable.ic_brush,
            isSelected = areControlsExpanded,
            contentDescription = stringResource(if (areControlsExpanded) Res.string.collapse_controls else Res.string.expand_controls),
            onButtonPressed = onControlsToggled,
        )
    }
}

@Composable
private fun Controls(
    modifier: Modifier = Modifier,
    state: ContentShadersState,
    onStateChanged: (ContentShadersState) -> Unit,
) = Column(
    modifier = modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = 16.dp),
) {
    SmallSwitch(
        title = stringResource(Res.string.chromatic_aberration),
        isChecked = state.isChromaticAberrationShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isChromaticAberrationShaderEnabled = !state.isChromaticAberrationShaderEnabled)) }
    )
    SmallSwitch(
        title = stringResource(Res.string.ripple),
        isChecked = state.isRippleShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isRippleShaderEnabled = !state.isRippleShaderEnabled)) }
    )
    SmallSwitch(
        title = stringResource(Res.string.blur),
        isChecked = state.isBlurShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isBlurShaderEnabled = !state.isBlurShaderEnabled)) }
    )
    SmallSwitch(
        title = stringResource(Res.string.comic),
        isChecked = state.isComicShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isComicShaderEnabled = !state.isComicShaderEnabled)) }
    )
    SmallSwitch(
        title = stringResource(Res.string.vignette),
        isChecked = state.isVignetteShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isVignetteShaderEnabled = !state.isVignetteShaderEnabled)) }
    )
    SmallSwitch(
        title = stringResource(Res.string.smooth_pixelation),
        isChecked = state.isSmoothPixelationShaderEnabled,
        onCheckedChanged = { onStateChanged(state.copy(isSmoothPixelationShaderEnabled = !state.isSmoothPixelationShaderEnabled)) }
    )
}
