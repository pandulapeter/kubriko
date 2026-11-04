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
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.MovePage
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceCreation

/**
 * Where the sidebar's items are dragged: a dropped one moves to its folder through a command.
 */
@Composable
internal fun rememberSidebarDragState(commands: CommandExecutor) = remember(commands) {
    SidebarDragState().apply {
        onMoveToFolder = { group, name, folder ->
            commands.dispatch(
                when (group) {
                    FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name))))
                    FolderGroup.PAGES -> Invocation(MovePage, MovePage.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name))))
                    FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> Invocation(MoveToFolder, MoveToFolder.Input(name, folder))
                },
            )
        }
    }
}

/**
 * What the sidebar's groups with folders do, each through the command for it; a delete waits for its confirmation
 * ([WorkspaceUiState.dispatch]).
 */
@Composable
internal fun rememberFolderTreeActions(
    creation: WorkspaceCreation,
    commands: CommandExecutor,
    selection: SelectionController,
    uiState: WorkspaceUiState,
) = remember(creation, commands, selection, uiState) {
    fun dispatchIfValid(invocation: Invocation<*>): Rejection? {
        commands.validate(invocation)?.let { return it }
        commands.dispatch(invocation)
        return null
    }
    FolderTreeActions(
        onSelected = { group, name ->
            when (group) {
                FolderGroup.SCENES -> selection.openScene(name)
                FolderGroup.PAGES -> selection.openPage(name)
                FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> selection.selectType(name)
            }
        },
        onSelectionCleared = selection::clear,
        onCreateItem = { group, name ->
            when (group) {
                FolderGroup.ACTOR_TYPES -> creation.createActorType(name)
                FolderGroup.MANAGER_TYPES -> creation.createManagerType(name)
                FolderGroup.SCENES -> creation.createScene(name)
                FolderGroup.PAGES -> creation.createPage(name)
            }
        },
        onCreateFolder = { group, path -> dispatchIfValid(Invocation(CreateFolder, CreateFolder.Input(group, path))) },
        onRenameFolder = { group, path, newName -> dispatchIfValid(Invocation(RenameFolder, RenameFolder.Input(group, path, newName))) },
        onDeleteFolder = { group, path -> commands.dispatch(Invocation(DeleteFolder, DeleteFolder.Input(group, path))) },
        onRenameItem = { group, name, newName ->
            dispatchIfValid(
                when (group) {
                    FolderGroup.ACTOR_TYPES -> Invocation(RenameActorType, RenameActorType.Input(name, newName))
                    FolderGroup.MANAGER_TYPES -> Invocation(RenameManagerType, RenameManagerType.Input(name, newName))
                    FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(FolderPaths.parentOf(name), newName)))
                    FolderGroup.PAGES -> Invocation(MovePage, MovePage.Input(name, FolderPaths.child(FolderPaths.parentOf(name), newName)))
                },
            )
        },
        onDeleteItem = { group, name ->
            uiState.dispatch(
                invocation = when (group) {
                    FolderGroup.ACTOR_TYPES -> Invocation(DeleteActorType, DeleteActorType.Input(name))
                    FolderGroup.MANAGER_TYPES -> Invocation(DeleteManagerType, DeleteManagerType.Input(name))
                    FolderGroup.SCENES -> Invocation(DeleteScene, DeleteScene.Input(name))
                    FolderGroup.PAGES -> Invocation(DeletePage, DeletePage.Input(name))
                },
                onSuccess = {},
                dispatch = commands::dispatch,
            )
        },
    )
}
