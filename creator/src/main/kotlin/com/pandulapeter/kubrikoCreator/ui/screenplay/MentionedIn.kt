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
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import kotlinx.coroutines.Dispatchers
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
 * The [MentionedIn] of the pages of [screenplay], read off the main thread again after every change to them.
 */
@Composable
internal fun rememberMentionedIn(screenplay: ScreenplayStore, onPageSelected: (String) -> Unit): MentionedIn {
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val revision by screenplay.revision.collectAsState()
    val index = produceState(MentionIndex(), revision) {
        value = withContext(Dispatchers.IO) {
            runCatching { MentionIndex.of(screenplay.scanNames().mapNotNull { name -> screenplay.read(name)?.let { name to it } }.toMap()) }.getOrDefault(value)
        }
    }.value
    return MentionedIn(screenplay.rootDirectory, index) { currentOnPageSelected(it) }
}
