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

/**
 * The sound and music groups of a module whose [resources] these are, each as a tree of the real folders of its
 * `composeResources/files` folders ([AudioFolders]): its files by their paths from `composeResources` (one row for the
 * copies of a file in several source sets, which the commands change together), in their folders, and the empty
 * folders among [folders] the group shows ([AudioFolders.ofKind]); below the folder new files go to while it holds all
 * of them ([FolderTreeGroupState.root]), so a project keeping its sounds in `sounds` shows them right in the group,
 * while one with a folder beside it shows both.
 */
internal fun audioTreesOf(resources: Map<ResourceGroup, List<ProjectResource>>, folders: Set<String>): Map<ResourceGroup, FolderTreeGroupState> {
    val paths = AudioFiles.of(resources)
    return listOf(ResourceGroup.SOUNDS, ResourceGroup.MUSIC).associateWith { group ->
        val kind = checkNotNull(group.resourceKind)
        val items = (if (kind == ResourceKind.SOUND) paths.sounds else paths.music).map { it to ResourceKind.audioFolderOf(it) }
        val groupFolders = AudioFolders.ofKind(kind, folders, paths)
        val root = rootOf(items.map { it.second }, groupFolders, checkNotNull(ResourceKind.defaultAudioFolderOf(kind)))
        FolderTreeGroupState(items, groupFolders, root = root)
    }
}

/**
 * The group of the sidebar whose folders are those of this group of files, for sounds and music.
 */
internal val ResourceGroup.audioFolderGroup: FolderGroup?
    get() = resourceKind?.let(FolderGroup::of)

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
