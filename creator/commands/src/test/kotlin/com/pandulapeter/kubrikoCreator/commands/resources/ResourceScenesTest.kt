/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceScenesTest {

    @OptIn(EditorMutation::class)
    @Test
    fun withoutAudioPropertiesOnlyAMusicRenameReadsTheScenes() = runTest {
        val rootDirectory = testDirectory()
        commandContext(rootDirectory = rootDirectory).scenes.write(
            "main",
            SceneDocument.format(listOf(SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, SceneSettingsState.withChanges(music = "files/theme.mp3"), "settings"))),
        )
        var sceneReads = 0
        val context = commandContext(rootDirectory = rootDirectory, sceneReadText = { file -> sceneReads++; file.readText() })
        assertEquals(emptyMap(), context.scenesWithAudioReplaced(listOf("main"), "files/hit.wav", "files/bump.wav"))
        assertEquals(0, sceneReads)
        assertEquals(setOf("main"), context.scenesWithAudioReplaced(listOf("main"), "files/theme.mp3", "files/tune.mp3").keys)
        assertEquals(1, sceneReads)
    }
}
