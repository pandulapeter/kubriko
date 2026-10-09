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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersState
import com.pandulapeter.kubriko.shared.ui.ExpandableControlsOverlay
import com.pandulapeter.kubriko.uiComponents.Panel
import com.pandulapeter.kubriko.uiComponents.SmallSwitch
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.blur
import kubriko.examples.demo_content_shaders.generated.resources.chromatic_aberration
import kubriko.examples.demo_content_shaders.generated.resources.comic
import kubriko.examples.demo_content_shaders.generated.resources.description
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
) = ExpandableControlsOverlay(
    windowInsets = windowInsets,
    description = Res.string.description,
    isExpanded = areControlsExpanded,
    onToggle = onControlsToggled,
) { modifier ->
    Panel(
        modifier = modifier,
    ) {
        Controls(
            modifier = Modifier.width(220.dp),
            state = state,
            onStateChanged = onStateChanged,
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
