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
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * What the sidebar's groups with folders show ([folderTreeGroups]), computed again only when one of these changes.
 *
 * @param types The project's block-built types, or null if it hasn't opted in to them.
 */
@Composable
internal fun rememberFolderTreeGroups(
    types: List<BlockTypeDefinition>?,
    typeFolders: Map<BlockTypeKind, Set<String>>,
    selectedTypeName: String?,
    sceneNames: List<String>,
    sceneFolders: Set<String>,
    openSceneName: String?,
    pages: FolderTreeGroupState,
): Map<FolderGroup, FolderTreeGroupState> = remember(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName, pages) {
    folderTreeGroups(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName, pages)
}

/**
 * What the sidebar's groups with folders show: the screenplay's pages, the type groups for projects that opted in to
 * them, and the Scenes.
 *
 * @param openSceneName The Scene open in the center, if any.
 * @param pages The pages with their folders and the one open in the center ([pageGroupState]).
 */
internal fun folderTreeGroups(
    types: List<BlockTypeDefinition>?,
    typeFolders: Map<BlockTypeKind, Set<String>>,
    selectedTypeName: String?,
    sceneNames: List<String>,
    sceneFolders: Set<String>,
    openSceneName: String?,
    pages: FolderTreeGroupState = FolderTreeGroupState(emptyList(), emptySet(), null),
): Map<FolderGroup, FolderTreeGroupState> = buildMap {
    put(FolderGroup.PAGES, pages)
    if (types != null) {
        put(FolderGroup.ACTOR_TYPES, FolderTreeGroupState(types.filterIsInstance<ActorTypeDefinition>().map { it.name to it.folder }, typeFolders[FolderGroup.ACTOR_TYPES.kind].orEmpty(), selectedTypeName))
        put(FolderGroup.MANAGER_TYPES, FolderTreeGroupState(types.filterIsInstance<ManagerTypeDefinition>().map { it.name to it.folder }, typeFolders[FolderGroup.MANAGER_TYPES.kind].orEmpty(), selectedTypeName))
    }
    put(FolderGroup.SCENES, FolderTreeGroupState(sceneNames.map { it to FolderPaths.parentOf(it) }, sceneFolders, openSceneName))
}

/**
 * What the Screenplay group shows: the pages [names], each in the folder its path names, the [folders], and the page
 * [openPage] open in the center.
 */
internal fun pageGroupState(names: List<String>, folders: Set<String>, openPage: String?) =
    FolderTreeGroupState(names.map { it to PageNaming.folderOf(it) }, folders, openPage)
