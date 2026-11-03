/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioFilesTest {

    private fun resource(group: ResourceGroup, path: String) = ProjectResource(group, File(path), path)

    @Test
    fun onlyFilesTheGameCanReadByTheirPathAreListedByThatPath() {
        val files = AudioFiles.of(
            mapOf(
                ResourceGroup.SOUNDS to listOf(
                    resource(ResourceGroup.SOUNDS, "src/commonMain/composeResources/files/sounds/pop.wav"),
                    resource(ResourceGroup.SOUNDS, "src/commonMain/composeResources/files/hit.wav"),
                    resource(ResourceGroup.SOUNDS, "src/commonMain/composeResources/drawable/beep.wav"),
                ),
                ResourceGroup.MUSIC to listOf(resource(ResourceGroup.MUSIC, "src/desktopMain/composeResources/files/music/theme.mp3")),
            ),
        )
        assertEquals(AudioFiles(sounds = listOf("files/hit.wav", "files/sounds/pop.wav"), music = listOf("files/music/theme.mp3")), files)
        assertEquals(AudioFiles(), AudioFiles.of(emptyMap()))
    }
}
