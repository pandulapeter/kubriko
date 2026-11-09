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
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A choice of one of the project's sound or music files, shown by their path inside the `files` folder.
 *
 * @param paths The files to choose from, or null while they aren't known, when none is marked as missing.
 * @param placeholder What is shown while nothing is chosen, which is offered as the first option, choosing nothing, if
 *   [canChooseNone].
 */
@Composable
internal fun AudioFileChoice(
    path: String,
    paths: List<String>?,
    placeholder: StringResource,
    canChooseNone: Boolean = false,
    onSelected: (String) -> Unit,
) {
    val isMissing = isMissingChoice(path, paths)
    val none = stringResource(placeholder)
    SearchableChoiceButton(
        label = when {
            isMissing -> missingChoiceLabel(audioFileLabel(path))
            else -> path.ifEmpty { null }?.let(::audioFileLabel) ?: none
        },
        options = remember(paths, canChooseNone) { if (canChooseNone) listOf("") + paths.orEmpty() else paths.orEmpty() },
        optionText = { it.ifEmpty { null }?.let(::audioFileLabel) ?: none },
        onSelected = onSelected,
        isMissing = isMissing,
    )
}

internal fun audioFileLabel(path: String) = path.removePrefix("files/")
