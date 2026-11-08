/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(EditorMutation::class)
class MentionFollowingTest {
    private val reports = mutableListOf<ConsoleEntry>()

    private fun TestScope.context(): CommandContext = commandContext(
        ActorTypeDefinition(name = "Player"),
        ActorTypeDefinition(name = "Bat", folder = "enemies"),
        ManagerTypeDefinition(name = "Score"),
        report = { reports += it },
    ).apply {
        scenes.write("main", "[]")
        screenplay.refresh()
    }

    private fun CommandContext.page(name: String, text: String) = File(project.rootDirectory, "screenplay/$name.md").apply { parentFile.mkdirs() }.writeText(text).also { screenplay.refresh() }

    private fun CommandContext.files() = project.rootDirectory.walkTopDown().filter { it.isFile && it.extension == "md" }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    @Test
    fun `renaming an Actor type follows its mentions, and undo puts every byte back`() = runTest {
        val context = context()
        context.page("README", "The [hero](../.kubriko/actors/Player.json#look) and a [stale one](../.kubriko/actors/Hero.json).\r\n")
        context.page("levels/cave", "![Hero](../../.kubriko/actors/./Player.json \"Hero\")\n")
        val after = context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Player", "Hero"))) { context.files() }
        assertEquals(
            mapOf(
                "screenplay/README.md" to "The [hero](../.kubriko/actors/Hero.json#look) and a [stale one](../.kubriko/actors/Hero.json).\r\n",
                "screenplay/levels/cave.md" to "![Hero](../../.kubriko/actors/Hero.json \"Hero\")\n",
            ),
            after,
        )
    }

    @Test
    fun `the AI hears which pages followed a change`() = runTest {
        val context = context()
        context.page("README", "[Score](../.kubriko/managers/Score.json)")
        context.page("hero", "Nothing to follow.")
        val applied = RenameManagerType.apply(context, RenameManagerType.Input("Score", "Points"))
        assertEquals(listOf("screenplay/README.md"), applied.details!!.getValue(MENTIONS_FOLLOWED_IN).jsonArray.map { it.jsonPrimitive.content })
        assertEquals("[Score](../.kubriko/managers/Points.json)", context.files().getValue("screenplay/README.md"))
    }

    @Test
    fun `a change no page mentions leaves the pages and the undo step alone`() = runTest {
        val context = context()
        context.page("README", "[Bat](../.kubriko/actors/enemies/Bat.json)")
        val applied = RenameActorType.apply(context, RenameActorType.Input("Player", "Hero"))
        assertNull(applied.details)
        assertEquals(RenameActorType, applied.inverse!!.command)
    }

    @Test
    fun `moving types into and between folders follows their mentions`() = runTest {
        val context = context()
        context.page("README", "[Bat](../.kubriko/actors/enemies/Bat.json) [Player](../.kubriko/actors/Player.json)")
        context.blockTypes!!.createFolder(BlockTypeKind.ACTOR, "heroes")
        val moved = context.assertRoundTrip(Invocation(MoveToFolder, MoveToFolder.Input("Player", "heroes"))) { context.files() }
        assertEquals("[Bat](../.kubriko/actors/enemies/Bat.json) [Player](../.kubriko/actors/heroes/Player.json)", moved.getValue("screenplay/README.md"))
        val renamed = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.ACTOR_TYPES, "enemies", "foes"))) { context.files() }
        assertEquals("[Bat](../.kubriko/actors/foes/Bat.json) [Player](../.kubriko/actors/heroes/Player.json)", renamed.getValue("screenplay/README.md"))
    }

    @Test
    fun `renaming a Scene or its folder follows its mentions`() = runTest {
        val context = context()
        val scene = context.projectPathOf(context.scenes.fileOf("main"))
        context.page("README", "[Main](${PageLinks.destinationOf("screenplay/README.md", scene)})")
        val renamed = context.assertRoundTrip(Invocation(RenameScene, RenameScene.Input("main", "levels/first"))) { context.files() }
        val first = context.projectPathOf(context.scenes.fileOf("levels/first"))
        assertEquals("[Main](${PageLinks.destinationOf("screenplay/README.md", first)})", renamed.getValue("screenplay/README.md"))
        RenameScene.apply(context, RenameScene.Input("main", "levels/first"))
        val folder = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.SCENES, "levels", "worlds"))) { context.files() }
        val world = context.projectPathOf(context.scenes.fileOf("worlds/first"))
        assertEquals("[Main](${PageLinks.destinationOf("screenplay/README.md", world)})", folder.getValue("screenplay/README.md"))
    }

    @Test
    fun `a moved page keeps its own links working and the links to it follow`() = runTest {
        val context = context()
        context.page("README", "Meet the [hero](hero.md#moves).")
        context.page("hero", "Back to [the game](README.md), the [code](../.kubriko/actors/Player.json), and [the web](https://example.com).")
        val moved = context.assertRoundTrip(Invocation(MovePage, MovePage.Input("hero", "cast/hero"))) { context.files() }
        assertEquals(
            mapOf(
                "screenplay/README.md" to "Meet the [hero](cast/hero.md#moves).",
                "screenplay/cast/hero.md" to "Back to [the game](../README.md), the [code](../../.kubriko/actors/Player.json), and [the web](https://example.com).",
            ),
            moved,
        )
        val folder = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.PAGES, "cast", "people"))) { context.files() }
        assertEquals("Meet the [hero](people/hero.md#moves).", folder.getValue("screenplay/README.md"))
        assertEquals(moved.getValue("screenplay/cast/hero.md"), folder.getValue("screenplay/people/hero.md"))
    }

    @Test
    fun `documents moved by a command keep their own links working, and pages follow them`() = runTest {
        val context = context()
        File(context.project.rootDirectory, "docs/combat.md").apply { parentFile.mkdirs() }.writeText("[Bat](../.kubriko/actors/enemies/Bat.json)")
        context.documents.refresh(listOf("docs/combat.md"))
        context.page("README", "See [combat](../docs/combat.md).")
        val moved = context.assertRoundTrip(Invocation(MoveDocument, MoveDocument.Input("docs/combat.md", "notes/old/combat.md"))) { context.files() }
        assertEquals("See [combat](../notes/old/combat.md).", moved.getValue("screenplay/README.md"))
        assertEquals("[Bat](../../.kubriko/actors/enemies/Bat.json)", moved.getValue("notes/old/combat.md"))
        val added = context.assertRoundTrip(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("notes/old/combat.md", "combat"))) { context.files() }
        assertEquals("See [combat](combat.md).", added.getValue("screenplay/README.md"))
        assertEquals("[Bat](../.kubriko/actors/enemies/Bat.json)", added.getValue("screenplay/combat.md"))
    }

    @Test
    fun `a mention written escaped or encoded is followed too`() = runTest {
        val context = context()
        File(context.project.rootDirectory, "docs/my notes (old).md").apply { parentFile.mkdirs() }.writeText("Notes.")
        context.documents.refresh(listOf("docs/my notes (old).md"))
        context.page("README", "[a](../docs/my%20notes%20%28old%29.md) [b](<../docs/my notes (old).md>)")
        MoveDocument.apply(context, MoveDocument.Input("docs/my notes (old).md", "docs/new.md"))
        assertEquals("[a](../docs/new.md) [b](<../docs/new.md>)", context.files().getValue("screenplay/README.md"))
    }

    @Test
    fun `a document that isn't UTF-8 text moves as it is`() = runTest {
        val context = context()
        val bytes = "[Bat](../.kubriko/actors/enemies/Bat.json) caf\u00e9".toByteArray(Charsets.ISO_8859_1)
        File(context.project.rootDirectory, "docs/latin.md").apply { parentFile.mkdirs() }.writeBytes(bytes)
        context.documents.refresh(listOf("docs/latin.md"))
        MoveDocument.apply(context, MoveDocument.Input("docs/latin.md", "notes/old/latin.md"))
        assertContentEquals(bytes, File(context.project.rootDirectory, "notes/old/latin.md").readBytes())
    }

    @Test
    fun `a page that can't be written leaves every link as it was and says so`() = runTest {
        val context = context()
        context.page("a", "[Player](../.kubriko/actors/Player.json)")
        context.page("locked/b", "[Player](../../.kubriko/actors/Player.json)")
        val locked = File(context.project.rootDirectory, "screenplay/locked")
        assumeTrue(locked.setWritable(false))
        try {
            assumeTrue(runCatching { File(locked, "probe").createNewFile() }.getOrDefault(false).not())
            val before = context.files()
            val applied = RenameActorType.apply(context, RenameActorType.Input("Player", "Hero"))
            assertEquals(before, context.files())
            assertEquals(RenameActorType, applied.inverse!!.command)
            assertEquals(CommandProblem.MENTIONS_NOT_FOLLOWED, reports.filterIsInstance<ConsoleEntry.Status>().single().text)
            assertEquals("Hero", context.blockTypes!!.actorType("Hero")?.name)
        } finally {
            locked.setWritable(true)
        }
    }
}
