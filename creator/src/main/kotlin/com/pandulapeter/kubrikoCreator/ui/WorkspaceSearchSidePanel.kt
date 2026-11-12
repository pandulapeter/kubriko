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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.search.SearchPanel
import com.pandulapeter.kubrikoCreator.ui.search.rememberUsages
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The workspace's Search panel, after the splitter that resizes it: what refers to what [uiState] asks about, read again
 * after every change, each going to its place when chosen ([openUsage]).
 *
 * @param rootDirectory The project's folder, which the paths of hand-written Kotlin files start from.
 */
@Composable
internal fun WorkspaceSearchSidePanel(
    commands: CommandExecutor,
    selection: SelectionController,
    rootDirectory: File,
    uiState: WorkspaceUiState,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    val subject = uiState.usagesSubject
    val revision = commands.history.collectAsState().value to commands.context.blockTypes?.types?.collectAsState()?.value
    val scope = rememberCoroutineScope()
    SearchPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        usagesSubject = subject,
        usages = rememberUsages(subject, revision, commands::findUsages),
        onUsageSelected = { usage -> scope.launch { openUsage(usage, subject, commands, selection, rootDirectory, uiState) } },
    )
}

/**
 * Goes to where [usage] (of [subject]) is: in the center, with a type's Actors selected in a Scene placing them, or a
 * hand-written Kotlin file in the system's app for it, or Project settings for the game's settings.
 */
private suspend fun openUsage(usage: Usage, subject: RelationSubject?, commands: CommandExecutor, selection: SelectionController, rootDirectory: File, uiState: WorkspaceUiState) {
    val scene = ((usage.place as? UsagePlace.InModel)?.place as? RelationPlace.Scene)?.name
    val actorIds = if (scene != null && subject is RelationSubject.Type) {
        withContext(Dispatchers.IO) { commands.context.scenes.actors(scene)?.filter { it.typeId == subject.name }?.map { it.id }?.filter { it.isNotEmpty() } }.orEmpty()
    } else {
        emptyList()
    }
    if (selection.open(usage, actorIds)) return
    when (val place = usage.place) {
        is UsagePlace.KotlinFile -> withContext(Dispatchers.IO) { runCatching { openInSystemEditor(File(rootDirectory, place.path)) } }
        UsagePlace.GameSettings -> uiState.showProjectSettings()
        is UsagePlace.InModel, is UsagePlace.Markdown, is UsagePlace.ResourceFile -> Unit
    }
}
