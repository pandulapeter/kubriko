/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.creating_project
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreatingProjectScreen(
    directoryName: String,
    onCancel: () -> Unit,
) = ProgressScreen(
    text = stringResource(Res.string.creating_project, directoryName),
    onCancel = onCancel,
)
