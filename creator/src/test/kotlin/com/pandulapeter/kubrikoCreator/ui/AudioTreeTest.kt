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

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioTreeTest {
    private fun resources(sounds: List<String> = emptyList(), music: List<String> = emptyList(), sourceSet: String = "commonMain") = mapOf(
        ResourceGroup.SOUNDS to sounds.map { resource(ResourceGroup.SOUNDS, it, sourceSet) },
        ResourceGroup.MUSIC to music.map { resource(ResourceGroup.MUSIC, it, sourceSet) },
    )

    private fun resource(group: ResourceGroup, path: String, sourceSet: String) = "src/$sourceSet/composeResources/$path".let { ProjectResource(group, File(it), it) }

    @Test
    fun theFolderNewFilesGoToIsHiddenWhileItHoldsEverything() {
        val trees = audioTreesOf(resources(sounds = listOf("files/sounds/hit.wav", "files/sounds/enemies/roar.wav"), music = listOf("files/music/theme.mp3")), setOf("sounds", "sounds/enemies", "music"))
        val sounds = trees.getValue(ResourceGroup.SOUNDS)
        assertEquals("sounds", sounds.root)
        assertEquals(listOf(FolderTreeRow.Folder("sounds/enemies", 0, isExpanded = true, isEmpty = false), FolderTreeRow.Item("files/sounds/enemies/roar.wav", 1), FolderTreeRow.Item("files/sounds/hit.wav", 0)), rowsOf(sounds))
        assertEquals(listOf(FolderTreeRow.Item("files/music/theme.mp3", 0)), rowsOf(trees.getValue(ResourceGroup.MUSIC)))
    }

    @Test
    fun aFolderBesideItShowsEverything() {
        val sounds = audioTreesOf(resources(sounds = listOf("files/sounds/hit.wav", "files/effects/boom.wav")), setOf("sounds", "effects")).getValue(ResourceGroup.SOUNDS)
        assertEquals("", sounds.root)
        assertEquals(listOf("effects", "sounds"), rowsOf(sounds).filterIsInstance<FolderTreeRow.Folder>().map { it.path })
        val empty = audioTreesOf(resources(sounds = listOf("files/sounds/hit.wav")), setOf("sounds", "empty")).getValue(ResourceGroup.SOUNDS)
        assertEquals("", empty.root)
    }

    @Test
    fun aFileInSeveralSourceSetsIsOneRow() {
        val both = resources(sounds = listOf("files/sounds/hit.wav")).getValue(ResourceGroup.SOUNDS) + resource(ResourceGroup.SOUNDS, "files/sounds/hit.wav", "desktopMain")
        val sounds = audioTreesOf(mapOf(ResourceGroup.SOUNDS to both), setOf("sounds")).getValue(ResourceGroup.SOUNDS)
        assertEquals(listOf("files/sounds/hit.wav" to "sounds"), sounds.items)
    }

    @Test
    fun withNothingThereIsNoRoot() {
        val trees = audioTreesOf(emptyMap(), emptySet())
        assertEquals("", trees.getValue(ResourceGroup.SOUNDS).root)
        assertEquals(false, trees.getValue(ResourceGroup.SOUNDS).hasShownFolders)
        assertEquals(true, audioTreesOf(emptyMap(), setOf("sounds", "sounds/new")).getValue(ResourceGroup.SOUNDS).hasShownFolders)
        assertEquals(false, audioTreesOf(emptyMap(), setOf("sounds")).getValue(ResourceGroup.SOUNDS).hasShownFolders)
    }

    private fun rowsOf(state: FolderTreeGroupState) = folderTreeRows(state.items, state.folders, collapsed = emptySet()).trimmedTo(state.root)

    @Test
    fun theFoldersNewFilesGoToStayWithTheirOwnGroupEvenEmpty() {
        val trees = audioTreesOf(emptyMap(), setOf("sounds", "music", "music/boss"))
        assertEquals(setOf("sounds"), trees.getValue(ResourceGroup.SOUNDS).folders)
        assertEquals("sounds", trees.getValue(ResourceGroup.SOUNDS).root)
        assertEquals(setOf("music", "music/boss"), trees.getValue(ResourceGroup.MUSIC).folders)
        assertEquals("music", trees.getValue(ResourceGroup.MUSIC).root)
    }
}
