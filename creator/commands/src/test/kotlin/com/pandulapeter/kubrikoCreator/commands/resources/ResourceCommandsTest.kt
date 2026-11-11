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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceCommandsTest {
    private val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10, 1, 2, 3)
    private val wav = "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.ISO_8859_1)
    private val mp3 = "ID3\u0004rest".toByteArray(Charsets.ISO_8859_1)

    private val resources = "src/commonMain/composeResources"

    private fun TestScope.context(vararg types: ActorTypeDefinition, sprites: List<String> = emptyList(), sounds: List<String> = emptyList(), music: List<String> = emptyList(), report: (ConsoleEntry) -> Unit = {}): CommandContext {
        val root = testDirectory()
        val spriteFiles = sprites.associateWith { path -> File(root, "$resources/$path").also { it.parentFile.mkdirs(); it.writeBytes(png) } }
        sounds.forEach { path -> File(root, "$resources/$path").also { it.parentFile.mkdirs(); it.writeBytes(wav) } }
        music.forEach { path -> File(root, "$resources/$path").also { it.parentFile.mkdirs(); it.writeBytes(mp3) } }
        return commandContext(*types, rootDirectory = root, spriteFiles = SpriteFiles(spriteFiles), audioFiles = AudioFiles(sounds, music), report = report)
    }

    private fun CommandContext.file(path: String) = File(project.rootDirectory, path)

    private fun CommandContext.resource(path: String) = file("$resources/$path")

    private fun sourceFile(name: String, bytes: ByteArray) = File(testDirectory(), name).apply { writeBytes(bytes) }

    private fun CommandContext.filesSnapshot() = project.rootDirectory.walkTopDown().filter { it.isFile }.associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readBytes().toList() }

    @Test
    fun addingCopiesTheFileWhereTheGameFindsIt() = runTest {
        val context = context()
        val source = sourceFile("Ball.png", png)
        val after = context.assertRoundTrip(Invocation(AddResource, AddResource.Input(source.path, "drawable/ball.png"))) { context.filesSnapshot() }
        assertEquals(png.toList(), after["$resources/drawable/ball.png"])
        val sound = sourceFile("hit.WAV", wav)
        assertNull(AddResource.apply(context, AddResource.Input(sound.path, "files/sounds/hit.wav")).failure)
        assertEquals(wav.toList(), context.resource("files/sounds/hit.wav").readBytes().toList())
    }

    @Test
    fun addingRaisesTheFilesRevision() = runTest {
        val context = context()
        val before = context.filesRevision.value
        AddResource.apply(context, AddResource.Input(sourceFile("ball.png", png).path, "drawable/ball.png"))
        assertTrue(context.filesRevision.value > before)
    }

    @Test
    fun addingChecksThePathAndTheFile() = runTest {
        val context = context(sprites = listOf("drawable/ball-red.png"), sounds = listOf("files/sounds/hit.wav"))
        val image = sourceFile("image.png", png)
        fun rejection(path: String, file: String = image.path) = Invocation(AddResource, AddResource.Input(file, path)).validate(context)
        assertEquals(Rejection(CommandProblem.RESOURCE_PATH_INVALID, "drawable/sub/ball.png"), rejection("drawable/sub/ball.png"))
        assertEquals(Rejection(CommandProblem.RESOURCE_PATH_INVALID, "files/scenes/hit.wav"), rejection("files/scenes/hit.wav"))
        assertEquals(Rejection(CommandProblem.RESOURCE_PATH_INVALID, "drawable/ball.gif"), rejection("drawable/ball.gif"))
        assertEquals(Rejection(NameProblem.RESOURCE_CHARACTERS), rejection("drawable/ball red.png"))
        assertEquals(Rejection(NameProblem.RESOURCE_FIRST_CHARACTER), rejection("drawable/_ball.png"))
        assertEquals(Rejection(NameProblem.NAME_LETTER_BEYOND_A_TO_Z), rejection("drawable/labda-é.png"))
        assertEquals(Rejection(NameProblem.PATH_PART_EMPTY), rejection("files/sounds//hit.wav", sourceFile("hit.wav", wav).path))
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/ball_red.png"), rejection("drawable/ball_red.png"))
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/Ball-Red.jpg"), rejection("drawable/Ball-Red.jpg", sourceFile("x.jpg", png).path))
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "files/sounds/HIT.wav"), rejection("files/sounds/HIT.wav", sourceFile("hit.wav", wav).path))
        assertEquals(Rejection(CommandProblem.RESOURCE_SOURCE_EXTENSION, image.path, "png"), rejection("drawable/ball.jpg"))
        assertNull(rejection("drawable/1-up.png"))
    }

    @Test
    fun addingRefusesWhatTheGameCouldNotRead() = runTest {
        val context = context()
        val text = sourceFile("notes.png", "not a picture".toByteArray())
        assertEquals(Rejection(CommandProblem.RESOURCE_CONTENT_MISMATCH, text.path, "png"), AddResource.apply(context, AddResource.Input(text.path, "drawable/notes.png")).failure)
        val missing = File(testDirectory(), "gone.wav").path
        assertEquals(Rejection(CommandProblem.RESOURCE_SOURCE_UNREADABLE, missing), AddResource.apply(context, AddResource.Input(missing, "files/sounds/gone.wav")).failure)
        assertFalse(context.resource("drawable/notes.png").exists())
    }

    @Test
    fun addingNeverWritesOverAFileTheScanDoesNotKnowYet() = runTest {
        val context = context()
        context.resource("drawable/ball.png").apply { parentFile.mkdirs() }.writeBytes(byteArrayOf(1))
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/Ball.png"), AddResource.apply(context, AddResource.Input(sourceFile("b.png", png).path, "drawable/Ball.png")).failure)
        assertEquals(listOf<Byte>(1), context.resource("drawable/ball.png").readBytes().toList())
    }

    @Test
    fun renamingFollowsTypesBlocksScenesAndPages() = runTest {
        val ball = ActorTypeDefinition(
            name = "Ball",
            sprite = "drawable/ball.png",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.PlaySound("files/sounds/hit.wav"), BlockDefinition.PlayMusic("files/music/theme.mp3")))),
        )
        val reported = ArrayList<ConsoleEntry>()
        val context = context(ball, sprites = listOf("drawable/ball.png"), sounds = listOf("files/sounds/hit.wav"), music = listOf("files/music/theme.mp3"), report = { reported += it })
        context.file("$resources/files/scenes/level.json").apply { parentFile.mkdirs() }.writeText(sceneWithMusic("files/music/theme.mp3"))
        context.scenes.refresh()
        context.file("src/commonMain/kotlin/Game.kt").apply { parentFile.mkdirs() }.writeText("val ball = Res.drawable.ball\nval hit = \"files/sounds/hit.wav\"")
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\n![The ball](../$resources/drawable/ball.png)\n\nIt bounces like a [ball](../$resources/drawable/ball.png).\n"))

        assertNull(Invocation(RenameResource, RenameResource.Input("drawable/ball.png", "drawable/orb.png")).validate(context))
        val imagesRevision = context.spriteImagesRevision.value
        assertNull(RenameResource.apply(context, RenameResource.Input("drawable/ball.png", "drawable/orb.png")).failure)
        assertEquals(imagesRevision + 1, context.spriteImagesRevision.value, "the Scene view and the game follow a sprite renamed in Kubriko Creator")
        RenameResource.apply(context, RenameResource.Input("files/sounds/hit.wav", "files/effects/bounce.wav"))
        assertEquals(imagesRevision + 1, context.spriteImagesRevision.value, "a sound is no sprite")
        RenameResource.apply(context, RenameResource.Input("files/music/theme.mp3", "files/music/song.mp3"))

        val renamed = context.blockTypes!!["Ball"] as ActorTypeDefinition
        assertEquals("drawable/orb.png", renamed.sprite)
        assertEquals(listOf(BlockDefinition.PlaySound("files/effects/bounce.wav"), BlockDefinition.PlayMusic("files/music/song.mp3")), renamed.events.single().blocks)
        assertEquals("files/music/song.mp3", context.scenes.actors("level")!!.single().let { SceneSettingsState.read(it.state).music })
        assertTrue(context.resource("drawable/orb.png").isFile)
        assertFalse(context.resource("drawable/ball.png").exists())
        assertTrue(context.resource("files/effects/bounce.wav").isFile)
        val page = context.screenplay.read("README")!!
        assertTrue("![The ball](../$resources/drawable/orb.png)" in page && "[ball](../$resources/drawable/orb.png)" in page, page)
        assertEquals(
            listOf(
                listOf("src/commonMain/kotlin/Game.kt", "drawable/ball.png", "drawable/orb.png"),
                listOf("src/commonMain/kotlin/Game.kt", "files/sounds/hit.wav", "files/effects/bounce.wav"),
            ),
            reported.filterIsInstance<ConsoleEntry.Status>().filter { it.text == CommandProblem.RESOURCE_NAMED_IN_CODE }.map { it.arguments },
        )
    }

    @Test
    fun renamingIsUndoneExactly() = runTest {
        val ball = ActorTypeDefinition(name = "Ball", sprite = "drawable/ball.png")
        val context = context(ball, sprites = listOf("drawable/ball.png"))
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\n![The ball](../$resources/drawable/ball.png)\n"))
        val after = context.assertRoundTrip(Invocation(RenameResource, RenameResource.Input("drawable/ball.png", "drawable/orb.png"))) {
            context.filesSnapshot().filterKeys { !it.startsWith(".kubriko") } to context.blockTypes!!["Ball"]
        }
        assertEquals("drawable/orb.png", (after.second as ActorTypeDefinition).sprite)
    }

    @Test
    fun renamingKeepsWhatTheFileIs() = runTest {
        val context = context(sprites = listOf("drawable/ball.png", "drawable/bat.png"), sounds = listOf("files/sounds/hit.wav"))
        fun rejection(path: String, newPath: String) = Invocation(RenameResource, RenameResource.Input(path, newPath)).validate(context)
        assertEquals(Rejection(CommandProblem.RESOURCE_KIND_CHANGED, "png"), rejection("drawable/ball.png", "drawable/ball.jpg"))
        assertEquals(Rejection(CommandProblem.RESOURCE_KIND_CHANGED, "wav"), rejection("files/sounds/hit.wav", "files/hit.mp3"))
        assertEquals(Rejection(CommandProblem.RESOURCE_KIND_CHANGED, "png"), rejection("drawable/ball.png", "files/ball.png"))
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/Bat.png"), rejection("drawable/ball.png", "drawable/Bat.png"))
        assertEquals(Rejection(LookupProblem.RESOURCE_MISSING_SIMILAR, "drawable/bal.png", "drawable/ball.png"), rejection("drawable/bal.png", "drawable/orb.png"))
        assertNull(rejection("drawable/ball.png", "drawable/Ball.png"))
        assertNull(rejection("files/sounds/hit.wav", "files/hit.wav"))
    }

    @Test
    fun renamingMovesTheFileOfEverySourceSet() = runTest {
        val context = context(sprites = listOf("drawable/ball.png"))
        context.file("src/wasmJsMain/composeResources/drawable/ball.png").apply { parentFile.mkdirs() }.writeBytes(png)
        assertNull(RenameResource.apply(context, RenameResource.Input("drawable/ball.png", "drawable/orb.png")).failure)
        assertTrue(context.file("src/wasmJsMain/composeResources/drawable/orb.png").isFile)
        assertTrue(context.resource("drawable/orb.png").isFile)
    }

    @Test
    fun deletingCleansUpWhatRefersToIt() = runTest {
        val ball = ActorTypeDefinition(
            name = "Ball",
            sprite = "drawable/ball.png",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.PlayMusic("files/music/theme.mp3"), BlockDefinition.Log("hi")))),
        )
        val context = context(ball, sprites = listOf("drawable/ball.png"), music = listOf("files/music/theme.mp3"))
        context.file("$resources/files/scenes/level.json").apply { parentFile.mkdirs() }.writeText(sceneWithMusic("files/music/theme.mp3"))
        context.scenes.refresh()
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nThe [theme](../$resources/files/music/theme.mp3) plays.\n"))

        val after = context.assertRoundTrip(Invocation(DeleteResource, DeleteResource.Input("files/music/theme.mp3"))) {
            context.filesSnapshot().filterKeys { !it.startsWith(".kubriko") } to context.blockTypes!!["Ball"]
        }
        assertEquals(listOf(BlockDefinition.Log("hi")), (after.second as ActorTypeDefinition).events.single().blocks)
        assertEquals("", SceneSettingsState.read(context.scenes.actors("level")!!.single().state).music)
        assertTrue("The theme plays." in context.screenplay.read("README")!!)
        assertFalse(context.resource("files/music/theme.mp3").exists())
    }

    @Test
    fun deletingASpriteLeavesThePagesShowingItACopy() = runTest {
        val ball = ActorTypeDefinition(name = "Ball", sprite = "drawable/ball.png")
        val context = context(ball, sprites = listOf("drawable/ball.png"))
        CreatePage.apply(context, CreatePage.Input("levels/cave", "# The cave\n\n![The ball](../../$resources/drawable/ball.png)\n\nA [ball](../../$resources/drawable/ball.png) rolls in.\n"))
        val after = context.assertRoundTrip(Invocation(DeleteResource, DeleteResource.Input("drawable/ball.png"))) {
            context.filesSnapshot().filterKeys { !it.startsWith(".kubriko") } to context.blockTypes!!["Ball"]
        }
        assertNull((after.second as ActorTypeDefinition).sprite)
        assertEquals(png.toList(), after.first["screenplay/levels/cave-1.png"])
        assertEquals("# The cave\n\n![The ball](cave-1.png)\n\nA ball rolls in.\n", context.screenplay.read("levels/cave"))
    }

    @Test
    fun deletingFindsTheFilesOnDisk() = runTest {
        val context = context(sprites = listOf("drawable/ball.png"))
        context.resource("drawable/ball.png").delete()
        assertEquals(Rejection(LookupProblem.RESOURCE_MISSING, "drawable/ball.png"), DeleteResource.apply(context, DeleteResource.Input("drawable/ball.png", keepsUsages = true)).failure)
        assertNotNull(Invocation(DeleteResource, DeleteResource.Input("drawable/gone.png")).validate(context))
    }

    private fun sceneWithMusic(music: String) = """[{"typeId":"SceneSettings","state":${kotlinx.serialization.json.JsonPrimitive(SceneSettingsState.withMusic("{}", music))}}]"""
}
