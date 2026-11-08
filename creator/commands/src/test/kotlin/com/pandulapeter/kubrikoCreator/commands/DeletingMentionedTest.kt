/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.project.CheckChange
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreateDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.context.screenplayTexts
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeletingMentionedTest {
    private val brick = ActorTypeDefinition(name = "Brick")

    private fun CommandContext.files() = project.rootDirectory.walkTopDown().filter { it.isFile }.associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    private fun CommandContext.text(path: String) = File(project.rootDirectory, path).readText()

    @Test
    fun deletingAnActorTypeTurnsItsMentionsIntoPlainTextAndIsUndoneByteForByte() = runTest {
        val context = commandContext(brick)
        CreatePage.apply(context, CreatePage.Input("README", "# Breakout\n\nThe ball breaks each [brick](../.kubriko/actors/Brick.json), and [bricks](../.kubriko/actors/Brick.json) fall.\n"))
        CreateDocument.apply(context, CreateDocument.Input("notes.md", "See the [Brick](.kubriko/actors/Brick.json) and [the game](screenplay/README.md).\n"))
        assertEquals(
            listOf(
                Usage(UsagePlace.Markdown("screenplay/README.md"), RelationKind.MENTIONS, RelationSubject.Type("Brick"), UsageEffect.MENTION_TO_TEXT, count = 2),
            ),
            context.usagesOf(RelationSubject.Type("Brick")),
        )
        val after = context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.files() }
        assertEquals("# Breakout\n\nThe ball breaks each brick, and bricks fall.\n", after.getValue("screenplay/README.md"))
        assertEquals("See the [Brick](.kubriko/actors/Brick.json) and [the game](screenplay/README.md).\n", after.getValue("notes.md"), "A document is the user's, so it is left as it is.")
    }

    @Test
    fun deletingASceneTurnsItsMentionsIntoPlainText() = runTest {
        val context = commandContext()
        File(context.project.rootDirectory, "src/commonMain/composeResources/files/scenes/cave.json").apply { parentFile.mkdirs() }.writeText("[]")
        context.scenes.refresh()
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nIt ends in the [cave](../src/commonMain/composeResources/files/scenes/cave.json).\n"))
        context.assertRoundTrip(Invocation(DeleteScene, DeleteScene.Input("cave"))) { context.files() }
        DeleteScene.apply(context, DeleteScene.Input("cave"))
        assertEquals("# Game\n\nIt ends in the cave.\n", context.text("screenplay/README.md"))
    }

    @Test
    fun deletingAPageTurnsTheLinksToItIntoPlainTextAndIsUndoneByteForByte() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("hero", "# The hero\n\nA knight.\n"))
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nStarring the [hero](hero.md) and [the hero again](hero.md#moves).\n"))
        val after = context.assertRoundTrip(Invocation(DeletePage, DeletePage.Input("hero"))) { context.files() }
        assertEquals("# Game\n\nStarring the hero and the hero again.\n", after.getValue("screenplay/README.md"))
        assertEquals(null, after["screenplay/hero.md"])
    }

    @Test
    fun whatIsRewrittenIsTheTextAsItIsNowNotAsItWasLastRead() = runTest {
        val context = commandContext(brick)
        CreatePage.apply(context, CreatePage.Input("README", "# Breakout\n\nThe ball breaks each [brick](../.kubriko/actors/Brick.json).\n"))
        context.screenplayTexts()
        File(context.project.rootDirectory, "screenplay/README.md").appendText("\nA line typed in another editor a moment ago.\n")
        DeleteActorType.apply(context, DeleteActorType.Input("Brick"))
        assertEquals("# Breakout\n\nThe ball breaks each brick.\n\nA line typed in another editor a moment ago.\n", context.text("screenplay/README.md"))
    }

    @Test
    fun anAiDeletingAPageHearsWhichPagesMentionedItAndCheckChangeListsThem() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context, testScope())
        CreatePage.apply(context, CreatePage.Input("hero", "# The hero\n\nA knight.\n"))
        CreatePage.apply(context, CreatePage.Input("README", "# Game\n\nStarring the [hero](hero.md).\n"))
        val check = CheckChange.read(context, CheckChange.Input("delete_page", buildJsonObject { put("page", "hero") }))
        assertContains(assertIs<CommandOutcome.Success>(check).result.jsonPrimitive.content, "screenplay/README.md mentions Page hero: the mention becomes plain text.")
        val result = assertIs<CommandOutcome.Success>(executor.call("delete_page", buildJsonObject { put("page", "hero") })).result.jsonObject
        assertEquals(listOf("screenplay/README.md"), result.getValue("mentionedIn").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("screenplay/README.md mentions Page hero: the mention becomes plain text."), result.getValue("usages").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun aPageThatStoppedMentioningItSinceItWasLastReadIsNeitherChangedNorReported() = runTest {
        val context = commandContext(brick)
        CreatePage.apply(context, CreatePage.Input("README", "# Breakout\n\nThe ball breaks each [brick](../.kubriko/actors/Brick.json).\n"))
        context.screenplayTexts()
        File(context.project.rootDirectory, "screenplay/README.md").writeText("# Breakout\n\nThe ball breaks everything.\n")
        val applied = DeleteActorType.apply(context, DeleteActorType.Input("Brick"))
        assertEquals(null, applied.details)
        assertEquals("# Breakout\n\nThe ball breaks everything.\n", context.text("screenplay/README.md"))
    }
}
