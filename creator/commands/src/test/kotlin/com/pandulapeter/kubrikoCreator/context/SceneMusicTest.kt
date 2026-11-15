/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
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

class SceneMusicTest {

    @OptIn(EditorMutation::class)
    @Test
    fun eachScenesMusicIsReadFromItsSettingsAndAGoneOneIsAMissingReference() = runTest {
        val context = commandContext(audioFiles = AudioFiles(music = listOf("files/theme.mp3")))
        fun settings(music: String) = SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, SceneSettingsState.withChanges(music = music), "settings")
        context.scenes.write("main", SceneDocument.format(listOf(settings("files/theme.mp3"))))
        context.scenes.write("boss", SceneDocument.format(listOf(settings("files/boss.mp3"))))
        context.scenes.write("quiet", SceneDocument.format(listOf(settings(""))))
        context.scenes.write("bare", "[]")
        assertEquals(mapOf("main" to "files/theme.mp3", "boss" to "files/boss.mp3"), context.sceneCounts().music)
        assertEquals(
            listOf(HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Scene("boss"), "Scene boss", usage = "files/boss.mp3")),
            context.missingReferences(),
        )
    }

    @OptIn(EditorMutation::class)
    @Test
    fun aSpritesUsagesReadNoSceneWhileAMusicFilesListTheScenesPlayingIt() = runTest {
        val rootDirectory = testDirectory()
        commandContext(rootDirectory = rootDirectory).scenes.write(
            "main",
            SceneDocument.format(listOf(SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, SceneSettingsState.withChanges(music = "files/theme.mp3"), "settings"))),
        )
        var sceneReads = 0
        val context = commandContext(rootDirectory = rootDirectory, sceneReadText = { file -> sceneReads++; file.readText() })
        assertEquals(emptyList(), context.findUsages(RelationSubject.Resource("files/ball.png")))
        assertEquals(0, sceneReads)
        assertEquals(1, context.findUsages(RelationSubject.Resource("files/theme.mp3")).size)
        assertEquals(1, sceneReads)
    }

    @OptIn(EditorMutation::class)
    @Test
    fun theSoundsAndMusicAScenesActorsSaveAreKeptApartByTheKindOfTheirProperty() = runTest {
        val enemy = ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Sound("cry"), PropertyDefinition.Music("tune")))
        val context = commandContext(enemy)
        context.scenes.write("level", SceneDocument.format(listOf(SceneActorEntry("Enemy", """{"cry":"files/music/theme.mp3","tune":"files/music/theme.mp3"}""", "enemy"))))
        val counts = context.sceneCounts(listOf(enemy))
        assertEquals(mapOf("level" to SavedAudio(sounds = setOf("files/music/theme.mp3"), music = setOf("files/music/theme.mp3"))), counts.savedAudio)
        assertEquals(mapOf("level" to mapOf("files/music/theme.mp3" to 2)), counts.savedReferences)
    }
}
