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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController

/**
 * Where the sidebar's types and Scenes are dragged: a dropped one moves to its folder through a command.
 */
@Composable
internal fun rememberSidebarDragState(controller: WorkspaceController) = remember(controller) {
    SidebarDragState().apply {
        onMoveToFolder = { group, name, folder ->
            controller.commands.dispatch(
                when (group) {
                    FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name))))
                    else -> Invocation(MoveToFolder, MoveToFolder.Input(name, folder))
                },
            )
        }
    }
}

/**
 * What the sidebar's groups with folders do, each through the command for it.
 */
@Composable
internal fun rememberFolderTreeActions(controller: WorkspaceController) = remember(controller) {
    FolderTreeActions(
        onSelected = { group, name ->
            if (group == FolderGroup.SCENES) {
                controller.resources.value[ResourceGroup.SCENES].orEmpty().firstOrNull { SceneStore.nameOf(it.file) == name }?.let { scene ->
                    controller.blockTypes?.select(null)
                    controller.sceneEditing.open(scene)
                }
            } else {
                controller.sceneEditing.close()
                controller.blockTypes?.select(name)
            }
        },
        onSelectionCleared = {
            controller.sceneEditing.close()
            controller.blockTypes?.select(null)
        },
        onCreateItem = { group, name ->
            when (group) {
                FolderGroup.ACTOR_TYPES -> controller.createActorType(name)
                FolderGroup.MANAGER_TYPES -> controller.createManagerType(name)
                FolderGroup.SCENES -> controller.createScene(name)
            }
        },
        onCreateFolder = { group, path -> controller.dispatchIfValid(Invocation(CreateFolder, CreateFolder.Input(group, path))) },
        onRenameFolder = { group, path, newName -> controller.dispatchIfValid(Invocation(RenameFolder, RenameFolder.Input(group, path, newName))) },
        onDeleteFolder = { group, path -> controller.commands.dispatch(Invocation(DeleteFolder, DeleteFolder.Input(group, path))) },
    )
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
