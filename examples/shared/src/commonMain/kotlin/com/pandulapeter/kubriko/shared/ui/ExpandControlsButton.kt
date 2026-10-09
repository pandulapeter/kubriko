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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import kubriko.examples.shared.generated.resources.Res
import kubriko.examples.shared.generated.resources.collapse_controls
import kubriko.examples.shared.generated.resources.expand_controls
import kubriko.examples.shared.generated.resources.ic_brush
import org.jetbrains.compose.resources.stringResource

/**
 * The brush button that expands and collapses an example's controls panel.
 */
@Composable
fun ExpandControlsButton(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) = FloatingButton(
    modifier = modifier,
    icon = Res.drawable.ic_brush,
    isSelected = isExpanded,
    contentDescription = stringResource(if (isExpanded) Res.string.collapse_controls else Res.string.expand_controls),
    onButtonPressed = onToggle,
)
