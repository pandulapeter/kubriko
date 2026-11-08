/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.context.mentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Which pages mention what, for the editors to show beside what they edit ([MentionedInRow]), and how to open a page.
 */
internal class MentionedIn(
    private val rootDirectory: File,
    val index: MentionIndex,
    val onPageSelected: (String) -> Unit,
) {
    /**
     * The path of [file], a file of the project, from the project's root, as mentions name it.
     */
    fun pathOf(file: File): String = file.relativeTo(rootDirectory).invariantSeparatorsPath
}

/**
 * The workspace's [MentionedIn], or null outside one.
 */
internal val LocalMentionedIn = compositionLocalOf<MentionedIn?> { null }

/**
 * The [MentionedIn] of the project of [context], opening pages through [selectionController].
 */
@Composable
internal fun rememberMentionedIn(context: CommandContext, selectionController: SelectionController): MentionedIn =
    rememberMentionedIn(context.project.rootDirectory, context.screenplay.revision, context::mentionIndex, selectionController::openPage)

/**
 * The [MentionedIn] of the project in [rootDirectory], whose index [readIndex] reads (blocking, so off the main thread)
 * again after every change to its pages ([revision]), the same instance while the index stays the same.
 */
@Composable
internal fun rememberMentionedIn(
    rootDirectory: File,
    revision: StateFlow<Int>,
    readIndex: () -> MentionIndex,
    onPageSelected: (String) -> Unit,
): MentionedIn {
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentRevision by revision.collectAsState()
    val index = produceState(MentionIndex(), currentRevision) {
        value = withContext(Dispatchers.IO) { runCatching(readIndex).getOrDefault(value) }
    }.value
    return remember(rootDirectory, index) { MentionedIn(rootDirectory, index) { currentOnPageSelected(it) } }
}
