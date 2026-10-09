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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import org.jetbrains.compose.resources.StringResource

/**
 * An example's overlay: the info panel with [description] at the top, and a controls panel that the
 * [ExpandControlsButton] in the bottom-end corner expands and collapses.
 *
 * [panel] receives the modifier that places it above the button and must apply it to its root.
 */
@Composable
fun ExpandableControlsOverlay(
    windowInsets: WindowInsets,
    description: StringResource,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    panel: @Composable (modifier: Modifier) -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(windowInsets)
        .padding(16.dp),
) {
    InfoPanel(
        stringResource = description,
        isVisible = LocalInfoPanelVisibility.current,
    )
    Spacer(
        modifier = Modifier.weight(1f),
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        this@Column.AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                panel(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                )
            }
        }
        ExpandControlsButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            isExpanded = isExpanded,
            onToggle = onToggle,
        )
    }
}
