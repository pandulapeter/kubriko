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

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.AudioFolders
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.VirtualFolderStore
import com.pandulapeter.kubrikoCreator.project.VirtualFolders

/**
 * The sprite, font, sound, and music groups of a module whose [resources] these are, each as a tree of its folders: the
 * sprites' and fonts' virtual ones ([virtualFolders], [VirtualFolderStore]), and the sounds' and music's the real ones of its
 * `composeResources/files` folders ([AudioFolders]). Its files are listed by their paths from `composeResources` (one row
 * for the copies of a file in several source sets, which the commands change together), in their folders, and the other
 * images of `drawable` and fonts of `font` (in a qualified folder, which the game doesn't take for sprites or fonts) by
 * their paths from the project's root, outside every folder. A sound or music group shows the empty folders among [audioFolders] it holds
 * ([AudioFolders.ofKind]), below the folder new files go to while it holds all of them ([FolderTreeGroupState.root]), so a
 * project keeping its sounds in `sounds` shows them right in the group, while one with a folder beside it shows both.
 */
internal fun resourceTreesOf(
    resources: Map<ResourceGroup, List<ProjectResource>>,
    audioFolders: Set<String>,
    virtualFolders: VirtualFolderStates = VirtualFolderStates(),
): Map<ResourceGroup, FolderTreeGroupState> {
    val paths = AudioFiles.of(resources)
    val audioTrees = listOf(ResourceGroup.SOUNDS, ResourceGroup.MUSIC).associateWith { group ->
        val kind = checkNotNull(group.resourceKind)
        val items = (if (kind == ResourceKind.SOUND) paths.sounds else paths.music).map { it to ResourceKind.audioFolderOf(it) }
        val groupFolders = AudioFolders.ofKind(kind, audioFolders, paths)
        val root = rootOf(items.map { it.second }, groupFolders, checkNotNull(ResourceKind.defaultAudioFolderOf(kind)))
        FolderTreeGroupState(items, groupFolders, root = root)
    }
    return audioTrees +
        (ResourceGroup.SPRITES to virtualTreeOf(resources[ResourceGroup.SPRITES].orEmpty(), virtualFolders.sprites)) +
        (ResourceGroup.FONTS to virtualTreeOf(resources[ResourceGroup.FONTS].orEmpty(), virtualFolders.fonts))
}

/**
 * The tree of [resources], a group with virtual [folders]: each file by its path from `composeResources` in its folder,
 * or, when the game doesn't take it for one of the group's, by its path from the project's root outside every folder.
 */
private fun virtualTreeOf(resources: List<ProjectResource>, folders: VirtualFolders): FolderTreeGroupState {
    val items = resources
        .map { resource -> resourcePathOf(resource)?.let { it to folders.folderOf[ResourceKind.nameOf(it)].orEmpty() } ?: (resource.relativePath to "") }
        .distinctBy { it.first }
    return FolderTreeGroupState(items, folders.folders)
}

/**
 * The group of the sidebar whose folders are those of this group of files, for sprites, fonts, sounds, and music.
 */
internal val ResourceGroup.folderGroup: FolderGroup?
    get() = resourceKind?.let(FolderGroup::of)

/**
 * What tells apart an image of `drawable` the game doesn't take for a sprite, by its [relativePath] from the project's
 * root: its source set and its folder (`androidMain/drawable-hdpi`), since copies of it may be in several of either.
 */
internal fun otherImageDetailOf(relativePath: String): String {
    val segments = relativePath.split('/')
    val sourceSet = segments.indexOf("src").takeIf { it >= 0 && it + 1 < segments.lastIndex }?.let { segments[it + 1] }
    return listOfNotNull(sourceSet, segments.getOrNull(segments.lastIndex - 1)).joinToString("/")
}

/**
 * Whether a folder is shown in this tree: one besides its [FolderTreeGroupState.root] and those holding it.
 */
internal val FolderTreeGroupState.hasShownFolders: Boolean
    get() = folders.any { root.isEmpty() || (it != root && FolderPaths.isWithin(it, root)) }

/**
 * [defaultFolder] when it exists and every one of [itemFolders] and [folders] is in it, so it holds everything the group
 * shows; none otherwise.
 */
private fun rootOf(itemFolders: List<String>, folders: Set<String>, defaultFolder: String): String {
    val exists = defaultFolder in folders || itemFolders.any { FolderPaths.isWithin(it, defaultFolder) }
    val holdsEverything = itemFolders.all { FolderPaths.isWithin(it, defaultFolder) } && folders.all { FolderPaths.isWithin(it, defaultFolder) }
    return if (exists && holdsEverything) defaultFolder else ""
}
