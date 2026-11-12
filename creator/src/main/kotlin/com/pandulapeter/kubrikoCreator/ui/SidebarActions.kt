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
import androidx.compose.runtime.rememberCoroutineScope
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.MoveFolder
import com.pandulapeter.kubrikoCreator.commands.folders.MoveSpriteToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.resources.AddResource
import com.pandulapeter.kubrikoCreator.commands.resources.CreateSprite
import com.pandulapeter.kubrikoCreator.commands.resources.DeleteResource
import com.pandulapeter.kubrikoCreator.commands.resources.ImportSpriteFrames
import com.pandulapeter.kubrikoCreator.commands.resources.RenameResource
import com.pandulapeter.kubrikoCreator.commands.resources.newResourcePathsOf
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.MovePage
import com.pandulapeter.kubrikoCreator.screenplay.DocumentNaming
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceCreation
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_from_frames_choose
import org.jetbrains.compose.resources.stringResource

/**
 * Where the sidebar's items and folders are dragged: a dropped one moves to its folder through a command.
 */
@Composable
internal fun rememberSidebarDragState(commands: CommandExecutor, creation: WorkspaceCreation) = remember(commands, creation) {
    SidebarDragState().apply {
        onMoveToFolder = { group, name, folder ->
            when (group) {
                FolderGroup.SCENES -> commands.dispatch(Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name)))))
                FolderGroup.PAGES -> commands.dispatch(Invocation(MovePage, MovePage.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name)))))
                FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> commands.dispatch(Invocation(MoveToFolder, MoveToFolder.Input(name, folder)))
                FolderGroup.SPRITES -> commands.dispatch(Invocation(MoveSpriteToFolder, MoveSpriteToFolder.Input(name, folder)))
                FolderGroup.SOUNDS, FolderGroup.MUSIC -> commands.dispatch(Invocation(RenameResource, RenameResource.Input(name, ResourceKind.inAudioFolder(name, folder))))
                null -> creation.moveDocument(name, DocumentNaming.pathOf(folder, name.substringAfterLast('/')))
            }
        }
        onMoveFolder = { group, path, folder -> commands.dispatch(Invocation(MoveFolder, MoveFolder.Input(group, path, folder))) }
    }
}

/**
 * What the sidebar's groups with folders do, each through the command for it; a delete waits for its confirmation
 * ([WorkspaceUiState.dispatch]), but for an empty folder's.
 */
@Composable
internal fun rememberFolderTreeActions(
    creation: WorkspaceCreation,
    commands: CommandExecutor,
    selection: SelectionController,
    uiState: WorkspaceUiState,
) = remember(creation, commands, selection, uiState) {
    FolderTreeActions(
        onSelected = { group, name ->
            when (group) {
                FolderGroup.SCENES -> selection.openScene(name)
                FolderGroup.PAGES -> selection.openPage(name)
                FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> selection.selectType(name)
                FolderGroup.SPRITES -> if (ResourceKind.of(name) == ResourceKind.SPRITE) selection.openSprite(name)
                FolderGroup.SOUNDS, FolderGroup.MUSIC -> Unit
            }
        },
        onSelectionCleared = selection::clear,
        onCreateItem = { group, name ->
            when (group) {
                FolderGroup.ACTOR_TYPES -> creation.createActorType(name)
                FolderGroup.MANAGER_TYPES -> creation.createManagerType(name)
                FolderGroup.SCENES -> creation.createScene(name)
                FolderGroup.PAGES -> creation.createPage(name)
                FolderGroup.SPRITES, FolderGroup.SOUNDS, FolderGroup.MUSIC -> null
            }
        },
        onCreateFolder = { group, path -> commands.dispatchIfValid(Invocation(CreateFolder, CreateFolder.Input(group, path))) },
        onRenameFolder = { group, path, newName -> commands.dispatchIfValid(Invocation(RenameFolder, RenameFolder.Input(group, path, newName))) },
        onDeleteFolder = { group, path, isEmpty ->
            val invocation = folderDeleteOf(group, path, isEmpty)
            if (invocation.input.keepsContents) commands.dispatch(invocation) else uiState.dispatch(invocation = invocation, onSuccess = {}, dispatch = commands::dispatch)
        },
        onRenameItem = { group, name, newName ->
            when (group) {
                FolderGroup.ACTOR_TYPES -> Invocation(RenameActorType, RenameActorType.Input(name, newName))
                FolderGroup.MANAGER_TYPES -> Invocation(RenameManagerType, RenameManagerType.Input(name, newName))
                FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(FolderPaths.parentOf(name), newName)))
                FolderGroup.PAGES -> Invocation(MovePage, MovePage.Input(name, FolderPaths.child(FolderPaths.parentOf(name), newName)))
                FolderGroup.SPRITES, FolderGroup.SOUNDS, FolderGroup.MUSIC -> resourceRenameOf(name, newName)
            }?.let(commands::dispatchIfValid)
        },
        onDeleteItem = { group, name ->
            uiState.dispatch(
                invocation = when (group) {
                    FolderGroup.ACTOR_TYPES -> Invocation(DeleteActorType, DeleteActorType.Input(name))
                    FolderGroup.MANAGER_TYPES -> Invocation(DeleteManagerType, DeleteManagerType.Input(name))
                    FolderGroup.SCENES -> Invocation(DeleteScene, DeleteScene.Input(name))
                    FolderGroup.PAGES -> Invocation(DeletePage, DeletePage.Input(name))
                    FolderGroup.SPRITES, FolderGroup.SOUNDS, FolderGroup.MUSIC -> Invocation(DeleteResource, DeleteResource.Input(name))
                },
                onSuccess = {},
                dispatch = commands::dispatch,
            )
        },
        onFindUsages = { group, name -> uiState.findUsages(usageSubjectOf(group, name)) },
    )
}

/**
 * What the sidebar's documents do, each through the command for it; deleting one, or adding it to the screenplay, waits
 * for its confirmation ([WorkspaceUiState.dispatch]), where the page a document becomes can still be renamed. A document
 * moved while it is shown stays shown, and one added to the screenplay is shown as its page.
 */
@Composable
internal fun rememberDocumentActions(
    creation: WorkspaceCreation,
    commands: CommandExecutor,
    selection: SelectionController,
    uiState: WorkspaceUiState,
) = remember(creation, commands, selection, uiState) {
    DocumentActions(
        onOpen = selection::openDocument,
        onCreate = creation::createDocument,
        onRename = { path, newName -> creation.moveDocument(path, DocumentNaming.pathOf(DocumentNaming.folderOf(path), newName)) },
        onDelete = { path -> uiState.dispatch(Invocation(DeleteDocument, DeleteDocument.Input(path)), onSuccess = {}, dispatch = commands::dispatch) },
        onAddToScreenplay = { path ->
            val wasShown = selection.current.value == WorkspaceSelection.Document(path)
            val page = freePageName(DocumentNaming.pageNameOf(path)) { commands.context.screenplay.existingName(it) != null }
            uiState.confirmDocumentChange(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input(path, page))) { confirmed ->
                if (wasShown) (confirmed.input as? AddDocumentToScreenplay.Input)?.let { selection.openPage(it.page) }
            }
        },
    )
}

/**
 * What the sidebar's sprites, sounds, and music groups do, each through the command for it: files chosen, dropped, or
 * pasted are added under their own names ([newResourcePathsOf]). The clipboard is read off the main thread, in a scope
 * that outlives the sidebar.
 */
@Composable
internal fun rememberResourceActions(commands: CommandExecutor, selection: SelectionController? = null): ResourceActions {
    val scope = rememberCoroutineScope()
    val chooseTitles = ResourceKind.entries.associateWith { stringResource(it.addLabel) }
    val framesTitle = stringResource(Res.string.sprite_from_frames_choose)
    return remember(commands, selection, scope, chooseTitles, framesTitle) {
        val add = { sources: List<ResourceSource> ->
            val paths = commands.context.newResourcePathsOf(sources.map { it.name })
            sources.zip(paths).forEach { (source, path) -> if (path != null) commands.dispatch(Invocation(AddResource, AddResource.Input(source.file.path, path))) }
        }
        ResourceActions(
            onChoose = { kind -> add(pickResourceFiles(kind, chooseTitles.getValue(kind))) },
            onAdd = add,
            onPaste = { scope.launch { withContext(Dispatchers.IO) { pastedResources() }.takeIf { it.isNotEmpty() }?.let(add) } },
            onCreateSprite = { name, width, height ->
                val path = ResourceKind.pathOf(ResourceKind.SPRITE, name, "png")
                commands.dispatchIfValid(Invocation(CreateSprite, CreateSprite.Input(path, width, height))).also { if (it == null) selection?.openSprite(path) }
            },
            onImportFrames = {
                val files = pickFrameFiles(framesTitle)
                commands.context.newResourcePathsOf(listOfNotNull(files.firstOrNull()?.let { frameSpriteNameOf(files) + ".png" })).firstOrNull()?.let { path ->
                    if (commands.dispatchIfValid(Invocation(ImportSpriteFrames, ImportSpriteFrames.Input(files.map { it.path }, path))) == null) selection?.openSprite(path)
                }
            },
        )
    }
}

/**
 * The rename of the sprite, sound, or music file at [path] to [newName] in its own folder, or null when that is its name
 * already.
 */
internal fun resourceRenameOf(path: String, newName: String): Invocation<RenameResource.Input>? =
    ResourceKind.renamed(path, newName).takeIf { it != path }?.let { Invocation(RenameResource, RenameResource.Input(path, it)) }

/**
 * [name], or the first of `name 2`, `name 3`, and so on that [isTaken] doesn't take, as a page a document becomes is
 * offered under (a project's `README.md` meets the screenplay's own first page).
 */
internal fun freePageName(name: String, isTaken: (String) -> Boolean): String =
    generateSequence(1) { it + 1 }.map { if (it == 1) name else "$name $it" }.first { !isTaken(it) }

/**
 * The delete of the folder at [path] of [group]: a folder shown [isEmpty] takes nothing with it, so it goes without a
 * confirmation, but as a delete of the folder alone, which the command refuses if something was put in it since.
 */
internal fun folderDeleteOf(group: FolderGroup, path: String, isEmpty: Boolean) = Invocation(DeleteFolder, DeleteFolder.Input(group, path, keepsContents = isEmpty))
