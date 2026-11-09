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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.audio_files_not_read
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A choice of one of the project's sound or music files, shown by their path inside the `files` folder.
 *
 * @param paths The files to choose from, or null while they aren't known, when none is marked as missing.
 * @param placeholder What is shown while nothing is chosen, which is offered as the first option, choosing nothing, if
 *   [canChooseNone].
 * @param noneYet Shown in the menu when the project has no such files, saying how to add some.
 */
@Composable
internal fun AudioFileChoice(
    path: String,
    paths: List<String>?,
    placeholder: StringResource,
    noneYet: StringResource,
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
        hint = audioFilesHint(paths, noneYet)?.let { stringResource(it) },
    )
}

internal fun audioFileLabel(path: String) = path.removePrefix("files/")

/**
 * What the menu of [paths] says besides its options: how to add files ([noneYet]) when the project has none, and that
 * they are still being looked for while they aren't known, rather than that there are none.
 */
internal fun audioFilesHint(paths: List<String>?, noneYet: StringResource): StringResource? = when {
    paths == null -> Res.string.audio_files_not_read
    paths.isEmpty() -> noneYet
    else -> null
}
