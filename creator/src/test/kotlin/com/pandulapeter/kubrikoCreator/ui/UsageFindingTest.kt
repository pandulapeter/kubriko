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

import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kotlin.test.Test
import kotlin.test.assertEquals

class UsageFindingTest {

    @Test
    fun eachSidebarGroupsItemIsTheSubjectItsUsagesAreFoundFor() = assertEquals(
        listOf(
            RelationSubject.Type("Ball"),
            RelationSubject.Type("Game"),
            RelationSubject.Scene("levels/1"),
            RelationSubject.Page("story"),
            RelationSubject.Resource("drawable/ball.png"),
            RelationSubject.Resource("files/sounds/hit.wav"),
            RelationSubject.Resource("files/music/theme.mp3"),
        ),
        listOf(
            FolderGroup.ACTOR_TYPES to "Ball",
            FolderGroup.MANAGER_TYPES to "Game",
            FolderGroup.SCENES to "levels/1",
            FolderGroup.PAGES to "story",
            FolderGroup.SPRITES to "drawable/ball.png",
            FolderGroup.SOUNDS to "files/sounds/hit.wav",
            FolderGroup.MUSIC to "files/music/theme.mp3",
        ).map { (group, name) -> usageSubjectOf(group, name) },
    )
}
