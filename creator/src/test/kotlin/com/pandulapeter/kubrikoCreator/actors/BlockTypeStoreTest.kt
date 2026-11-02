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

import org.junit.Assume.assumeFalse
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockTypeStoreTest {
    private val root = createTempDirectory().toFile()
    private val generated = File(root, "src/commonMain/kotlin/com/example/game/generated")
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge))),
    )

    private fun optIn(settings: String = """{ "packageName": "com.example.game" }""") = File(root, ".kubriko/project.json").apply {
        parentFile.mkdirs()
        writeText(settings)
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aProjectWithoutSettingsHasNoStore() {
        assertNull(BlockTypeStore.open(root))
    }

    @Test
    fun savingWritesTheDefinitionItsClassAndTheRegistry() {
        optIn()
        val store = assertNotNull(BlockTypeStore.open(root))
        store.saveAndRegenerate(ball)
        assertTrue(File(root, ".kubriko/actors/Ball.json").isFile)
        assertEquals(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), File(generated, "Ball.kt").readText())
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "Ball.metadata,")
    }

    @Test
    fun typesLiveInFoldersAndMovingOneLeavesNoCopyBehind() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball.withFolder("toys/round"))
        assertTrue(File(root, ".kubriko/actors/toys/round/Ball.json").isFile)
        assertEquals(listOf(ball.withFolder("toys/round")), store.load().types)
        assertEquals(setOf("toys", "toys/round"), store.folders(BlockTypeKind.ACTOR))
        store.saveAndRegenerate(ball.withFolder("toys"))
        assertFalse(File(root, ".kubriko/actors/toys/round/Ball.json").exists())
        assertEquals(listOf(ball.withFolder("toys")), store.load().types)
        store.createFolder(BlockTypeKind.MANAGER, "rules")
        assertEquals(setOf("rules"), store.folders(BlockTypeKind.MANAGER))
        // A folder with a type in it stays; an empty one goes, with its empty folders.
        store.deleteFolder(BlockTypeKind.ACTOR, "toys")
        assertEquals(setOf("toys", "toys/round"), store.folders(BlockTypeKind.ACTOR))
        store.deleteAndRegenerate("Ball")
        store.deleteFolder(BlockTypeKind.ACTOR, "toys")
        assertEquals(emptySet(), store.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun savedDefinitionsLoadBackSortedByName() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE, color = 0xFF000000)
        store.saveAndRegenerate(wall)
        store.saveAndRegenerate(ball)
        assertEquals(listOf(ball, wall), store.load().types)
    }

    @Test
    fun savingAgainOverwritesTheDefinitionAndItsClass() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        store.saveAndRegenerate(ball.copy(shape = ActorShape.RECTANGLE))
        assertEquals(listOf(ball.copy(shape = ActorShape.RECTANGLE)), store.load().types)
        assertContains(File(generated, "Ball.kt").readText(), "drawRect(")
    }

    @Test
    fun deletingRemovesTheDefinitionAndItsClassAndUpdatesTheRegistry() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        store.deleteAndRegenerate("Ball")
        assertTrue(store.load().types.isEmpty())
        assertFalse(File(generated, "Ball.kt").exists())
        assertFalse("Ball" in File(generated, "GeneratedActorTypes.kt").readText())
    }

    @Test
    fun regeneratingRestoresEveryGeneratedFile() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        generated.deleteRecursively()
        store.regenerateAll()
        assertTrue(File(generated, "Ball.kt").isFile)
        assertTrue(File(generated, "GeneratedActorTypes.kt").isFile)
    }

    @Test
    fun regeneratingAProjectWithoutActorTypesStillWritesTheRegistryAndTheSceneSettings() {
        optIn()
        BlockTypeStore.open(root)!!.regenerateAll()
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "        SceneSettings.metadata,\n")
        assertTrue(File(generated, "SceneSettings.kt").isFile)
    }

    @Test
    fun theGeneratedPathIsRelativeToTheProject() {
        optIn()
        assertEquals("src/commonMain/kotlin/com/example/game/generated", BlockTypeStore.open(root)!!.generatedPath)
    }

    @Test
    fun theSourceFolderComesFromTheSettings() {
        optIn("""{ "packageName": "io.game", "sourceDirectory": "shared/src/commonMain/kotlin" }""")
        BlockTypeStore.open(root)!!.saveAndRegenerate(ball)
        assertTrue(File(root, "shared/src/commonMain/kotlin/io/game/generated/Ball.kt").isFile)
    }

    @Test
    fun definitionsAreStoredAsReadableJsonThatSurvivesUnknownFields() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(root, ".kubriko/actors/Ball.json")
        val text = file.readText()
        assertContains(text, "\n    \"name\": \"Ball\",")
        assertContains(text, "\"type\": \"bounceOffEdge\"")
        assertContains(text, "\"color\": \"#6060AA\"")
        assertFalse("null" in text, text)
        file.writeText(text.replace("\"name\": \"Ball\",", "\"name\": \"Ball\", \"addedLater\": true,"))
        assertEquals(listOf(ball), store.load().types)
    }

    @Test
    fun collisionEventsCompileOnlyWhileTheirOtherActorTypeExists() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ActorTypeDefinition(name = "Paddle"))
        store.saveAndRegenerate(ball.copy(events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle", blocks = listOf(BlockDefinition.BounceOffOther)))))
        assertContains(File(generated, "Ball.kt").readText(), "is Paddle ->")
        store.deleteAndRegenerate("Paddle")
        assertFalse("Paddle" in File(generated, "Ball.kt").readText())
        store.saveAndRegenerate(ActorTypeDefinition(name = "Paddle"))
        assertContains(File(generated, "Ball.kt").readText(), "is Paddle ->")
    }

    @Test
    fun generatedCodeChangedByHandIsKeptAndReported() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        assertEquals(emptySet(), store.saveAndRegenerate(ball))
        val file = File(generated, "Ball.kt")
        val editedByHand = file.readText().replace("override val body = state.body", "override val body = state.body // tuned")
        file.writeText(editedByHand)
        assertEquals(setOf("Ball"), store.saveAndRegenerate(ball.copy(shape = ActorShape.RECTANGLE)))
        assertEquals(editedByHand, file.readText())
        assertEquals(setOf("Ball"), store.regenerateAll().changedByHand)
    }

    @Test
    fun generatedCodeCheckedOutWithWindowsLineBreaksIsNeitherReportedNorRewritten() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        assertEquals(emptySet(), store.saveAndRegenerate(ball))
        val file = File(generated, "Ball.kt")
        file.writeText(file.readText().replace("\n", "\r\n"))
        assertEquals(emptySet(), store.regenerateAll().changedByHand)
        assertEquals(emptySet(), BlockTypeStore.open(root)!!.regenerateAll().changedByHand)
        assertContains(file.readText(), "\r\n")
    }

    @Test
    fun theGeneratedCodeOfATypeDeletedOutsideCreatorIsDeleted() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        store.saveAndRegenerate(ActorTypeDefinition(name = "Wall"))
        File(root, ".kubriko/actors/Wall.json").delete()
        val regenerated = store.regenerateAll()
        assertEquals(listOf("src/commonMain/kotlin/com/example/game/generated/Wall.kt"), regenerated.removedLeftovers)
        assertFalse(File(generated, "Wall.kt").exists())
        assertTrue(File(generated, "Ball.kt").isFile)
        assertTrue(File(generated, "GeneratedScenes.kt").isFile)
        assertTrue(File(generated, "KubrikoWiring.kt").isFile)
        assertEquals(emptyList(), store.regenerateAll().removedLeftovers)
    }

    @Test
    fun theGeneratedCodeOfADeletedTypeIsDeletedEvenWhenTheStoreDidntWriteIt() {
        optIn()
        BlockTypeStore.open(root)!!.saveAndRegenerate(ActorTypeDefinition(name = "Wall"))
        File(root, ".kubriko/actors/Wall.json").delete()
        assertEquals(1, BlockTypeStore.open(root)!!.regenerateAll().removedLeftovers.size)
        assertFalse(File(generated, "Wall.kt").exists())
    }

    @Test
    fun theGeneratedCodeOfADeletedTypeChangedByHandIsKeptAndReported() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ActorTypeDefinition(name = "Wall"))
        val file = File(generated, "Wall.kt").apply { appendText("// mine\n") }
        val handWritten = File(generated, "Helpers.kt").apply { writeText("package com.example.game.generated\n") }
        File(root, ".kubriko/actors/Wall.json").delete()
        val regenerated = store.regenerateAll()
        assertEquals(emptyList(), regenerated.removedLeftovers)
        assertEquals(emptySet(), regenerated.changedByHand)
        assertEquals(
            setOf("src/commonMain/kotlin/com/example/game/generated/Helpers.kt", "src/commonMain/kotlin/com/example/game/generated/Wall.kt"),
            regenerated.leftoversChangedByHand,
        )
        assertTrue(file.isFile)
        assertTrue(handWritten.isFile)
        assertEquals(regenerated.leftoversChangedByHand, store.regenerateAll().leftoversChangedByHand)
        file.delete()
        handWritten.delete()
        assertEquals(emptySet(), store.regenerateAll().leftoversChangedByHand)
    }

    @Test
    fun theAudioFileIsntALeftover() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.regenerateAll()
        val audio = File(generated, "GeneratedAudio.kt").apply { writeText("// mine\n") }
        assertEquals(emptySet(), store.regenerateAll().leftoversChangedByHand)
        assertTrue(audio.isFile)
    }

    @Test
    fun changesMadeByHandCanBeDiscarded() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(generated, "Ball.kt")
        file.appendText("// tuned\n")
        assertEquals(emptySet(), store.regenerateAll(overwriting = setOf("Ball")).changedByHand)
        assertEquals(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), file.readText())
    }

    @Test
    fun aRegistryChangedByHandIsKept() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val registry = File(generated, "GeneratedActorTypes.kt")
        registry.appendText("// mine\n")
        store.saveAndRegenerate(ActorTypeDefinition(name = "Wall"))
        assertContains(registry.readText(), "// mine")
    }

    @Test
    fun codeGeneratedBeforeChecksumsIsStillUpdated() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(generated, "Ball.kt")
        file.writeText("// Generated by Kubriko Creator from .kubriko/actors/Ball.json. Edit it in Kubriko Creator.\npackage old\n")
        assertEquals(emptySet(), store.regenerateAll().changedByHand)
        assertContains(file.readText(), "package com.example.game.generated")
    }

    @Test
    fun managerTypesAreKeptApartAndWiredIn() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")), texts = listOf(ScreenTextDefinition("{score}")))
        store.saveAndRegenerate(ball)
        store.saveAndRegenerate(game)
        assertTrue(File(root, ".kubriko/managers/Game.json").isFile)
        assertEquals(listOf(ball, game), store.load().types)
        assertEquals(ManagerTypeCodeGenerator.generateManagerType(game, "com.example.game", listOf("Ball"), listOf(game)), File(generated, "Game.kt").readText())
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "        Game(isInSceneEditor),\n")
        assertEquals(ScenesCodeGenerator.generate("com.example.game"), File(generated, "GeneratedScenes.kt").readText())
        assertEquals(SceneSettingsCodeGenerator.generate("com.example.game"), File(generated, "SceneSettings.kt").readText())
        store.deleteAndRegenerate("Game")
        assertFalse(File(root, ".kubriko/managers/Game.json").exists())
        assertFalse(File(generated, "Game.kt").exists())
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "        val managers = listOf<Manager>(\n        )\n")
    }

    @Test
    fun actorTypesAreRegeneratedWhenAManagerTheyUseChanges() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val brick = ActorTypeDefinition("Brick", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game")))))
        store.saveAndRegenerate(brick)
        assertContains(File(generated, "Brick.kt").readText(), "// Choose a Property for this Block.")
        store.saveAndRegenerate(ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score"))))
        assertContains(File(generated, "Brick.kt").readText(), "gameManager.score += 1f")
    }

    @Test
    fun theAudioManagerIsWrittenWhileTheGamePlaysAudioAndRemovedAfterwards() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val audio = File(generated, "GeneratedAudio.kt")
        store.saveAndRegenerate(ball)
        assertFalse(audio.exists())
        val hit = ball.copy(events = ball.events + EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound("files/sounds/hit.wav"))))
        store.saveAndRegenerate(hit)
        assertEquals(AudioCodeGenerator.generate("com.example.game", setOf("files/sounds/hit.wav"), emptySet()), audio.readText())
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "SoundManager.newInstance()")
        store.saveAndRegenerate(ball)
        assertFalse(audio.exists())
        assertFalse("SoundManager" in File(generated, "KubrikoWiring.kt").readText())
    }

    @Test
    fun aSceneWithMusicMakesTheGamePlayAudio() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val scenes = File(root, "src/commonMain/composeResources/files/scenes").apply { mkdirs() }
        File(scenes, "main.json").writeText("""[{"typeId":"SceneSettings","state":"{\"music\":\"files/music/theme.mp3\"}"}]""")
        File(scenes, "broken.json").writeText("not a Scene")
        store.saveAndRegenerate(ball)
        assertContains(File(generated, "GeneratedAudio.kt").readText(), "            \"files/music/theme.mp3\",\n")
        File(scenes, "main.json").writeText("""[{"typeId":"SceneSettings","state":"{\"music\":\"\"}"}]""")
        store.regenerateAll()
        assertFalse(File(generated, "GeneratedAudio.kt").exists())
    }

    @Test
    fun anAudioManagerChangedByHandIsKept() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball.copy(events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.StopMusic)))))
        File(generated, "GeneratedAudio.kt").appendText("// Mine\n")
        store.saveAndRegenerate(ball)
        assertContains(File(generated, "GeneratedAudio.kt").readText(), "// Mine")
    }

    @Test
    fun regeneratingLeavesUnchangedFilesAlone() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(generated, "Ball.kt")
        file.setLastModified(1_000_000L)
        store.regenerateAll()
        assertEquals(1_000_000L, file.lastModified())
    }

    @Test
    fun aWiringChangedByHandIsLeftAlone() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        File(generated, "KubrikoWiring.kt").appendText("// Mine\n")
        store.saveAndRegenerate(ManagerTypeDefinition("Game"))
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "// Mine")
    }

    @Test
    fun codeThatDidNotChangeIsNotWrittenOrReadAgain() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(generated, "Ball.kt")
        val lastModified = file.lastModified()
        store.regenerate(store.load().types)
        assertEquals(lastModified, file.lastModified())
        store.save(ball.copy(shape = ActorShape.RECTANGLE))
        store.regenerate(store.load().types)
        assertContains(file.readText(), "drawRect")
    }

    @Test
    fun aGeneratedFileThatCouldntBeWrittenIsWrittenByTheNextRegeneration() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.saveAndRegenerate(ball)
        val file = File(generated, "Ball.kt")
        val changed = ball.copy(shape = ActorShape.RECTANGLE)
        generated.setWritable(false)
        assumeFalse(generated.canWrite())
        assertFails { store.regenerate(listOf(changed)) }
        generated.setWritable(true)
        store.regenerate(listOf(changed))
        assertContains(file.readText(), "drawRect")
    }

    @Test
    fun anUnreadableFileIsReportedByPathWhileTheOthersLoad() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        File(root, ".kubriko/actors/bricks/Brick.json").apply { parentFile.mkdirs() }.writeText("{ \"name\": ")
        File(root, ".kubriko/managers/Game.json").apply { parentFile.mkdirs() }.writeText("{}")
        val loaded = store.load()
        assertEquals(listOf(ball), loaded.types)
        assertEquals(listOf(".kubriko/actors/bricks/Brick.json", ".kubriko/managers/Game.json"), loaded.unreadableFiles.map { it.path })
        assertTrue(loaded.unreadableFiles.all { (it.problem as BlockTypeFileProblem.Unparsable).message.let { message -> message.isNotBlank() && '\n' !in message } })
    }

    @Test
    fun aTypeNamedOtherThanItsFileIsReportedRatherThanLoaded() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        File(root, ".kubriko/actors/Ball.json").renameTo(File(root, ".kubriko/actors/Puck.json"))
        val loaded = store.load()
        assertEquals(emptyList(), loaded.types)
        assertEquals(listOf(UnreadableBlockTypeFile(".kubriko/actors/Puck.json", BlockTypeFileProblem.NameMismatch("Ball"))), loaded.unreadableFiles)
        assertEquals("Puck", loaded.unreadableFiles.single().name)
    }

    @Test
    fun namesThatCantBeClassNamesAreReported() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        File(root, ".kubriko/actors/my enemy.json").apply { parentFile.mkdirs() }.writeText("""{ "name": "my enemy" }""")
        File(root, ".kubriko/managers/Color.json").apply { parentFile.mkdirs() }.writeText("""{ "name": "Color" }""")
        assertEquals(
            listOf(
                UnreadableBlockTypeFile(".kubriko/actors/my enemy.json", BlockTypeFileProblem.InvalidName("my enemy")),
                UnreadableBlockTypeFile(".kubriko/managers/Color.json", BlockTypeFileProblem.ReservedName("Color")),
            ),
            store.load().unreadableFiles,
        )
    }

    @Test
    fun aTypeWithAnInvalidPropertyNameIsUnreadable() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball.copy(properties = listOf(PropertyDefinition.Number("2x"))))
        val loaded = store.load()
        assertEquals(listOf(UnreadableBlockTypeFile(".kubriko/actors/Ball.json", BlockTypeFileProblem.InvalidPropertyName("2x"))), loaded.unreadableFiles)
        assertTrue(loaded.types.none { it.name == "Ball" })
        assertFails { store.save(ball) }
    }

    @Test
    fun aTypeWithAReservedPropertyNameIsUnreadable() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("velocity"))))
        assertEquals(listOf(UnreadableBlockTypeFile(".kubriko/managers/Game.json", BlockTypeFileProblem.InvalidPropertyName("velocity"))), store.load().unreadableFiles)
    }

    @Test
    fun aTypeWithTwoPropertiesDifferingOnlyInCaseIsUnreadable() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball.copy(properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Number("Score"))))
        assertEquals(listOf(UnreadableBlockTypeFile(".kubriko/actors/Ball.json", BlockTypeFileProblem.InvalidPropertyName("score"))), store.load().unreadableFiles)
    }

    @Test
    fun aTypeWithAPropertyListedTwiceIsUnreadable() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball.copy(properties = listOf(PropertyDefinition.Number("hp"), PropertyDefinition.Toggle("hp"))))
        assertEquals(listOf(UnreadableBlockTypeFile(".kubriko/actors/Ball.json", BlockTypeFileProblem.InvalidPropertyName("hp"))), store.load().unreadableFiles)
    }

    @Test
    fun aChildRepeatingItsParentsPropertyLoads() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val enemy = ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Number("hp", initialValue = 3f)))
        val bat = ActorTypeDefinition(name = "Bat", parent = "Enemy", properties = listOf(PropertyDefinition.Number("hp", initialValue = 1f)))
        store.save(enemy)
        store.save(bat)
        val loaded = store.load()
        assertEquals(listOf(bat, enemy), loaded.types)
        assertEquals(emptyList(), loaded.unreadableFiles)
    }

    @Test
    fun typesOfOneNameAreAllReportedAndNoneIsDeletedBySaving() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        File(root, ".kubriko/actors/Ball.json").copyTo(File(root, ".kubriko/actors/toys/Ball.json"))
        File(root, ".kubriko/managers/BALL.json").apply { parentFile.mkdirs() }.writeText("""{ "name": "BALL" }""")
        val wall = ActorTypeDefinition(name = "Wall")
        store.save(wall)
        val loaded = store.load()
        assertEquals(listOf(wall), loaded.types)
        assertEquals(
            listOf(
                UnreadableBlockTypeFile(".kubriko/actors/Ball.json", BlockTypeFileProblem.DuplicateName(listOf(".kubriko/actors/toys/Ball.json", ".kubriko/managers/BALL.json"))),
                UnreadableBlockTypeFile(".kubriko/actors/toys/Ball.json", BlockTypeFileProblem.DuplicateName(listOf(".kubriko/actors/Ball.json", ".kubriko/managers/BALL.json"))),
                UnreadableBlockTypeFile(".kubriko/managers/BALL.json", BlockTypeFileProblem.DuplicateName(listOf(".kubriko/actors/Ball.json", ".kubriko/actors/toys/Ball.json"))),
            ),
            loaded.unreadableFiles,
        )
        store.save(wall.withFolder("walls"))
        assertTrue(File(root, ".kubriko/actors/toys/Ball.json").isFile)
        assertTrue(File(root, ".kubriko/actors/Ball.json").isFile)
    }

    @Test
    fun nothingIsRegeneratedWhileAFileCantBeRead() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        File(root, ".kubriko/actors/Brick.json").writeText("not JSON")
        assertEquals(emptySet(), store.regenerateAll().changedByHand)
        assertFalse(File(generated, "Ball.kt").exists())
    }

    @Test
    fun anUnreadableFileIsNeverWrittenOverOrDeleted() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val broken = File(root, ".kubriko/actors/toys/Ball.json").apply { parentFile.mkdirs() }.apply { writeText("not JSON") }
        store.load()
        assertFails { store.save(ball.withFolder("toys")) }
        store.save(ball)
        store.delete("Ball")
        assertEquals("not JSON", broken.readText())
        assertFalse(File(root, ".kubriko/actors/Ball.json").exists())
    }

    @Test
    fun aMovedTypeLeavesNoOldFile() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.load()
        store.save(ball.withFolder("a"))
        store.save(ball.withFolder("b"))
        assertFalse(File(root, ".kubriko/actors/a/Ball.json").exists())
        assertTrue(File(root, ".kubriko/actors/b/Ball.json").isFile)
        store.save(ball.withFolder("a"))
        assertFalse(File(root, ".kubriko/actors/b/Ball.json").exists())
        assertEquals(listOf(ball.withFolder("a")), store.load().types)
    }

    @Test
    fun deleteRemovesEveryFileOfTheName() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.load()
        store.save(ball)
        store.save(ManagerTypeDefinition(name = "Ball"))
        store.delete("Ball")
        assertFalse(File(root, ".kubriko/actors/Ball.json").exists())
        assertFalse(File(root, ".kubriko/managers/Ball.json").exists())
    }

    @Test
    fun aStoreUsedBeforeLoadingStillFindsOldFiles() {
        optIn()
        BlockTypeStore.open(root)!!.save(ball.withFolder("old"))
        val store = BlockTypeStore.open(root)!!
        store.save(ball.withFolder("new"))
        assertFalse(File(root, ".kubriko/actors/old/Ball.json").exists())
        assertTrue(File(root, ".kubriko/actors/new/Ball.json").isFile)
    }

    @Test
    fun aFileCopiedInAfterLoadingIsLeftForTheNextLoadToReport() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        store.load()
        File(root, ".kubriko/actors/Ball.json").copyTo(File(root, ".kubriko/actors/toys/Ball.json"))
        store.save(ball.withFolder("balls"))
        assertTrue(File(root, ".kubriko/actors/toys/Ball.json").isFile)
        assertFalse(File(root, ".kubriko/actors/Ball.json").exists())
        assertEquals(
            listOf(".kubriko/actors/balls/Ball.json", ".kubriko/actors/toys/Ball.json"),
            store.load().unreadableFiles.map { it.path },
        )
    }

    @Test
    fun anUnreadableFileIsNeverDeletedThroughTheIndex() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val broken = File(root, ".kubriko/actors/toys/Ball.json").apply { parentFile.mkdirs() }.apply { writeText("not JSON") }
        store.load()
        store.save(ball.withFolder("balls"))
        store.save(ball)
        store.delete("Ball")
        assertEquals("not JSON", broken.readText())
        assertFalse(File(root, ".kubriko/actors/balls/Ball.json").exists())
        assertFalse(File(root, ".kubriko/actors/Ball.json").exists())
    }

    private fun BlockTypeStore.saveAndRegenerate(definition: BlockTypeDefinition): Set<String> {
        save(definition)
        return regenerateAll().changedByHand
    }

    private fun BlockTypeStore.deleteAndRegenerate(name: String): Set<String> {
        delete(name)
        return regenerateAll().changedByHand
    }
}
