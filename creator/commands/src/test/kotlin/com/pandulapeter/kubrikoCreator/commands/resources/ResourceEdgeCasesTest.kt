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
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
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

/**
 * What the sprite, sound, and music commands do with files whose names break today's rules, files of other modules,
 * images the game would mix up with a sprite, and undo after references that were there before.
 */
class ResourceEdgeCasesTest {
    private val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 1)
    private val wav = "RIFF\u0000\u0000\u0000\u0000WAVE".toByteArray(Charsets.ISO_8859_1)

    /**
     * A project whose last scan found [paths] (from the project's root), written to disk.
     */
    private fun TestScope.scanned(vararg paths: String, types: List<ActorTypeDefinition> = emptyList(), report: (ConsoleEntry) -> Unit = {}): CommandContext {
        val root = testDirectory()
        val resources = paths.map { path ->
            val file = File(root, path).apply { parentFile.mkdirs(); writeBytes(if (path.endsWith(".wav")) wav else png) }
            val group = when (path.substringAfterLast('.')) {
                "wav" -> ResourceGroup.SOUNDS
                "mp3" -> ResourceGroup.MUSIC
                else -> ResourceGroup.SPRITES
            }
            ProjectResource(group, file, path)
        }.groupBy { it.group }
        return commandContext(*types.toTypedArray(), rootDirectory = root, resources = resources, report = report)
    }

    private fun CommandContext.file(path: String) = File(project.rootDirectory, path)

    @Test
    fun aFileWhoseNameMakesNoAccessorCanBeRenamedAndDeleted() = runTest {
        val context = scanned("src/commonMain/composeResources/drawable/my ball.png", "src/commonMain/composeResources/drawable/ball-1.png", "src/commonMain/composeResources/drawable/ball_1.png")
        assertNull(Invocation(RenameResource, RenameResource.Input("drawable/my ball.png", "drawable/my-ball.png")).validate(context))
        assertNull(Invocation(DeleteResource, DeleteResource.Input("drawable/ball_1.png")).validate(context))
        assertNull(Invocation(RenameResource, RenameResource.Input("drawable/ball-1.png", "drawable/ball-one.png")).validate(context))
    }

    @Test
    fun undoingARenameBringsBackANameTodaysRulesRefuse() = runTest {
        val context = scanned("src/commonMain/composeResources/files/sounds/Explosion 1.wav")
        val applied = RenameResource.apply(context, RenameResource.Input("files/sounds/Explosion 1.wav", "files/sounds/explosion-1.wav"))
        val undo = assertNotNull(applied.inverse).copy(isRestoring = true)
        assertNull(undo.validate(context))
        assertNull(undo.apply(context).failure)
        assertTrue(context.file("src/commonMain/composeResources/files/sounds/Explosion 1.wav").isFile)
    }

    @Test
    fun renamingToTheSameNameIsRefused() = runTest {
        val context = scanned("src/commonMain/composeResources/drawable/ball.png")
        assertEquals(Rejection(CommandProblem.RESOURCE_UNCHANGED, "drawable/ball.png"), Invocation(RenameResource, RenameResource.Input("drawable/ball.png", "drawable/ball.png")).validate(context))
    }

    @Test
    fun anImageTheGameWouldMixUpWithASpriteTakesItsName() = runTest {
        val context = scanned("src/commonMain/composeResources/drawable/icon.svg")
        val source = File(testDirectory(), "icon.png").apply { writeBytes(png) }
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/icon.png"), Invocation(AddResource, AddResource.Input(source.path, "drawable/icon.png")).validate(context))
        context.file("src/commonMain/composeResources/drawable/ball.xml").writeText("<vector/>")
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/ball.png"), AddResource.apply(context, AddResource.Input(source.path, "drawable/ball.png")).failure)
        assertFalse(context.file("src/commonMain/composeResources/drawable/ball.png").exists())
        assertEquals("drawable/icon-2.png", context.newResourcePathsOf(listOf("icon.png")).single())
    }

    @Test
    fun anotherModulesFilesAreNeverTheGames() = runTest {
        val context = scanned("src/commonMain/composeResources/drawable/icon.png", "editor/src/commonMain/composeResources/drawable/icon.png", "editor/src/commonMain/composeResources/drawable/logo.png")
        assertNotNull(Invocation(DeleteResource, DeleteResource.Input("drawable/logo.png")).validate(context))
        assertNull(DeleteResource.apply(context, DeleteResource.Input("drawable/icon.png")).failure)
        assertFalse(context.file("src/commonMain/composeResources/drawable/icon.png").exists())
        assertTrue(context.file("editor/src/commonMain/composeResources/drawable/icon.png").isFile)
    }

    @Test
    fun undoingARenamePutsBackOnlyWhatItChanged() = runTest {
        val hero = ActorTypeDefinition(name = "Hero", sprite = "drawable/hero.png")
        val ball = ActorTypeDefinition(name = "Ball", sprite = "drawable/ball.png")
        val context = scanned("src/commonMain/composeResources/drawable/ball.png", types = listOf(hero, ball))
        val applied = RenameResource.apply(context, RenameResource.Input("drawable/ball.png", "drawable/hero.png"))
        assertEquals("drawable/hero.png", (context.blockTypes!!["Ball"] as ActorTypeDefinition).sprite)
        assertNull(assertNotNull(applied.inverse).copy(isRestoring = true).apply(context).failure)
        assertEquals(listOf(hero, ball).sortedBy { it.name }, context.blockTypes!!.types.value)
    }

    @Test
    fun renamingSaysWhichKotlinBlocksStillNameTheFile() = runTest {
        val reported = ArrayList<ConsoleEntry>()
        val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("hi"), BlockDefinition.Kotlin("val image = Res.drawable.ball")))))
        val context = scanned("src/commonMain/composeResources/drawable/ball.png", types = listOf(ball), report = { reported += it })
        RenameResource.apply(context, RenameResource.Input("drawable/ball.png", "drawable/orb.png"))
        assertEquals(
            listOf(listOf("Ball#0.1", "drawable/ball.png", "drawable/orb.png")),
            reported.filterIsInstance<ConsoleEntry.Status>().filter { it.text == CommandProblem.RESOURCE_NAMED_IN_CODE }.map { it.arguments },
        )
    }

    @Test
    fun withoutAFullScanTheSpritesAndAudioKnownAreChecked() = runTest {
        val context = commandContext(spriteFiles = SpriteFiles(mapOf("drawable/ball.png" to File("ball.png"))), audioFiles = AudioFiles(sounds = listOf("files/hit.wav")))
        assertEquals(listOf("drawable/ball.png", "files/hit.wav"), context.knownResourcePaths())
    }
}
