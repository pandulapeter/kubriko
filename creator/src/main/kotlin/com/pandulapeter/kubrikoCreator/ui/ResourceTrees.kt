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
import com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore

/**
 * The sprite, sound, and music groups of a module whose [resources] these are, each as a tree of its folders: the
 * sprites' virtual ones ([spriteFolders], [SpriteFolderStore]), and the sounds' and music's the real ones of its
 * `composeResources/files` folders ([AudioFolders]). Its files are listed by their paths from `composeResources` (one row
 * for the copies of a file in several source sets, which the commands change together), in their folders, and the other
 * images of `drawable` (in a qualified folder, which the game doesn't take for sprites) by their paths from the project's
 * root, outside every folder. A sound or music group shows the empty folders among [audioFolders] it holds
 * ([AudioFolders.ofKind]), below the folder new files go to while it holds all of them ([FolderTreeGroupState.root]), so a
 * project keeping its sounds in `sounds` shows them right in the group, while one with a folder beside it shows both.
 */
internal fun resourceTreesOf(
    resources: Map<ResourceGroup, List<ProjectResource>>,
    audioFolders: Set<String>,
    spriteFolders: SpriteFolderStore.State = SpriteFolderStore.State(),
): Map<ResourceGroup, FolderTreeGroupState> {
    val paths = AudioFiles.of(resources)
    val audioTrees = listOf(ResourceGroup.SOUNDS, ResourceGroup.MUSIC).associateWith { group ->
        val kind = checkNotNull(group.resourceKind)
        val items = (if (kind == ResourceKind.SOUND) paths.sounds else paths.music).map { it to ResourceKind.audioFolderOf(it) }
        val groupFolders = AudioFolders.ofKind(kind, audioFolders, paths)
        val root = rootOf(items.map { it.second }, groupFolders, checkNotNull(ResourceKind.defaultAudioFolderOf(kind)))
        FolderTreeGroupState(items, groupFolders, root = root)
    }
    val sprites = resources[ResourceGroup.SPRITES].orEmpty()
        .map { resource -> resourcePathOf(resource)?.let { it to spriteFolders.folderOf[ResourceKind.nameOf(it)].orEmpty() } ?: (resource.relativePath to "") }
        .distinctBy { it.first }
    return audioTrees + (ResourceGroup.SPRITES to FolderTreeGroupState(sprites, spriteFolders.folders))
}

/**
 * The group of the sidebar whose folders are those of this group of files, for sprites, sounds, and music.
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
