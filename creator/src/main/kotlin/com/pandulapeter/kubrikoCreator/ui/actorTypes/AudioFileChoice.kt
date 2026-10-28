/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A choice of one of the project's sound or music files, shown by their path inside the `files` folder.
 */
@Composable
internal fun AudioFileChoice(
    path: String,
    paths: List<String>,
    placeholder: StringResource,
    onSelected: (String) -> Unit,
) = MenuButton(
    label = path.ifEmpty { null }?.let(::audioFileLabel) ?: stringResource(placeholder),
    options = paths,
    optionText = { audioFileLabel(it) },
    onSelected = onSelected,
)

internal fun audioFileLabel(path: String) = path.removePrefix("files/")
