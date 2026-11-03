/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginReason
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryInUseTest {
    private val root = testDirectory()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun theLibrariesAreThoseTheSourcesNameWithWhoNamesThem() {
        val collision = PluginUse(KubrikoArtifact.COLLISION, PluginReason.COLLISION_EVENT, "Ball", "Paddle")
        val libraries = LibraryInUse.of(
            sources = mapOf(
                "src/commonMain/kotlin/game/Game.kt" to "import com.pandulapeter.kubriko.serialization.SerializationManager\nimport com.pandulapeter.kubriko.physics.PhysicsManager",
                "src/commonMain/kotlin/game/generated/Ball.kt" to "import com.pandulapeter.kubriko.collision.Collidable\nimport com.pandulapeter.kubriko.serialization.Serializable",
                "src/commonMain/kotlin/generated/Mine.kt" to "val physics = com.pandulapeter.kubriko.physics.RigidBody::class",
            ),
            uses = listOf(collision),
        )
        assertEquals(
            listOf(
                LibraryInUse(KubrikoArtifact.ENGINE),
                LibraryInUse(KubrikoArtifact.COLLISION, uses = listOf(collision), isUsedByGeneratedCode = true),
                LibraryInUse(KubrikoArtifact.PHYSICS, handWrittenFiles = listOf("src/commonMain/kotlin/game/Game.kt"), isUsedByGeneratedCode = true),
                LibraryInUse(KubrikoArtifact.SERIALIZATION, handWrittenFiles = listOf("src/commonMain/kotlin/game/Game.kt"), isUsedByGeneratedCode = true),
                LibraryInUse(KubrikoArtifact.CREATOR_API),
            ),
            libraries,
        )
        assertTrue(libraries.first().isAlwaysUsed)
    }

    @Test
    fun readingFindsTheSourcesAndTheScenesMusic() {
        File(root, "src/commonMain/kotlin/game/Sounds.kt").apply { parentFile.mkdirs() }.writeText("import com.pandulapeter.kubriko.audioPlayback.SoundManager\n")
        File(root, "src/commonMain/kotlin/game/Notes.txt").writeText("com.pandulapeter.kubriko.physics\n")
        val scene = File(root, "src/commonMain/composeResources/files/scenes/levels/1-1.json").apply { parentFile.mkdirs() }
        scene.writeText(SceneDocument.format(listOf(SceneActorEntry("SceneSettings", SceneSettingsState.withChanges(music = "files/music/level.mp3")))))
        File(scene.parentFile, "1-2.json").writeText(SceneDocument.format(emptyList()))
        val scenes = SceneStore(root) { emptyList() }
        val libraries = LibraryInUse.read(root, listOf(ActorTypeDefinition(name = "Ball", sprite = "drawable/ball.png")), scenes)
        assertEquals(listOf(KubrikoArtifact.ENGINE, KubrikoArtifact.AUDIO_PLAYBACK, KubrikoArtifact.CREATOR_API), libraries.map { it.artifact })
        assertEquals(
            LibraryInUse(
                KubrikoArtifact.AUDIO_PLAYBACK,
                uses = listOf(PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.SCENE_MUSIC, "levels/1-1", "files/music/level.mp3")),
                handWrittenFiles = listOf("src/commonMain/kotlin/game/Sounds.kt"),
            ),
            libraries[1],
        )
    }
}
