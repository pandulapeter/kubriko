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
): Map<FolderGroup, FolderTreeGroupState> = remember(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName) {
    folderTreeGroups(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName)
}

/**
 * What the sidebar's groups with folders show: the type groups for projects that opted in to them, and the Scenes.
 *
 * @param openSceneName The Scene open in the center, if any.
 */
internal fun folderTreeGroups(
    types: List<BlockTypeDefinition>?,
    typeFolders: Map<BlockTypeKind, Set<String>>,
    selectedTypeName: String?,
    sceneNames: List<String>,
    sceneFolders: Set<String>,
    openSceneName: String?,
): Map<FolderGroup, FolderTreeGroupState> = buildMap {
    if (types != null) {
        put(FolderGroup.ACTOR_TYPES, FolderTreeGroupState(types.filterIsInstance<ActorTypeDefinition>().map { it.name to it.folder }, typeFolders[FolderGroup.ACTOR_TYPES.kind].orEmpty(), selectedTypeName))
        put(FolderGroup.MANAGER_TYPES, FolderTreeGroupState(types.filterIsInstance<ManagerTypeDefinition>().map { it.name to it.folder }, typeFolders[FolderGroup.MANAGER_TYPES.kind].orEmpty(), selectedTypeName))
    }
    put(FolderGroup.SCENES, FolderTreeGroupState(sceneNames.map { it to FolderPaths.parentOf(it) }, sceneFolders, openSceneName))
}
