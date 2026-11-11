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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TokenBudgetTest {

    @Test
    fun theToolListStaysUnderItsBudget() {
        assertUnder(TOOL_LIST_BUDGET, "The tool list", CommandRegistry.editor.tools.joinToString("") { it.name + it.description + it.inputSchema })
    }

    @Test
    fun theToolsLoadedFromTheStartStayUnderTheirBudget() {
        val alwaysLoaded = CommandRegistry.editor.tools.filter { it.isAlwaysLoaded }
        assertUnder(ALWAYS_LOADED_BUDGET, "The tools loaded from the start", alwaysLoaded.joinToString("") { it.name + it.description + it.inputSchema })
    }

    @Test
    fun readingBreakoutStaysUnderItsBudgets() = runTest {
        val executor = breakout()
        assertUnder(DIGEST_BUDGET, "The digest", executor.read("get_project_digest", JsonObject(emptyMap())))
        assertUnder(ACTOR_TYPE_BUDGET, "read_actor_type of Ball", executor.read("read_actor_type", buildJsonObject { put("actorType", "Ball") }))
        assertUnder(ACTOR_TYPE_JSON_BUDGET, "read_actor_type of Ball as JSON", executor.read("read_actor_type", buildJsonObject { put("actorType", "Ball"); put("detail", "json") }))
        assertUnder(SCENE_BUDGET, "read_scene of level-1", executor.read("read_scene", buildJsonObject { put("scene", "level-1") }))
        assertUnder(SCENE_SUMMARY_BUDGET, "The summary of level-1", executor.read("read_scene", buildJsonObject { put("scene", "level-1"); put("summary", true) }))
        executor.call("create_page", buildJsonObject { put("page", "README"); put("text", "# Breakout\n\n" + "The ball breaks the bricks it hits. ".repeat(28)) })
        assertUnder(READ_PAGES_BUDGET, "read_pages of a page of 1,000 characters", executor.read("read_pages", buildJsonObject {}))
    }

    @Test
    fun thePageIndexStaysUnderItsBudgetHoweverManyPagesThereAre() {
        listOf(5, 30, 1_000).forEach { count ->
            val pages = (1..count).associate { "part-${it % 7}/page-$it" to "# Page $it\n\n" + "What this part of the game does, for the player. ".repeat(4) }
            assertUnder(PAGE_INDEX_BUDGET, "The index of $count pages", checkNotNull(PageIndexText.of(pages)))
        }
    }

    @OptIn(EditorMutation::class)
    private fun TestScope.breakout(): CommandExecutor {
        val context = commandContext(*BreakoutFixture.types.toTypedArray())
        context.scenes.write("level-1", BreakoutFixture.level)
        context.scenes.write("game-over", emptyList())
        context.scenes.descriptions.set("level-1", "the first level")
        return CommandExecutor(context, testScope())
    }

    private suspend fun CommandExecutor.read(name: String, arguments: JsonObject) = assertIs<CommandOutcome.Success>(call(name, arguments)).result

    private fun assertUnder(budget: Int, what: String, result: JsonElement) =
        assertUnder(budget, what, if (result is JsonPrimitive && result.isString) result.content else result.toString())

    private fun assertUnder(budget: Int, what: String, text: String) {
        val tokens = tokensOf(text)
        assertTrue(tokens <= budget, "$what grew to about $tokens tokens, over its budget of $budget.")
    }

    /**
     * About how many tokens [text] costs: Claude's tokenizer averages close to four characters a token on English and JSON.
     */
    private fun tokensOf(text: String) = text.length / CHARACTERS_PER_TOKEN

    /**
     * Each budget sits a little above what the read costs now, so that it can't creep up unnoticed: raise one only for a
     * reason, and lower it after a change that makes the read cheaper.
     */
    private companion object {
        const val CHARACTERS_PER_TOKEN = 4

        /**
         * The tools that take a Block list only the Blocks' names, so get_block_catalog describes them once.
         */
        const val TOOL_LIST_BUDGET = 23_200
        const val ALWAYS_LOADED_BUDGET = 4_100
        const val DIGEST_BUDGET = 300

        /**
         * A conversation starts with it, so a screenplay too large for it lists its folders instead ([PageIndexText]).
         */
        const val PAGE_INDEX_BUDGET = 1_030
        const val ACTOR_TYPE_BUDGET = 200
        const val ACTOR_TYPE_JSON_BUDGET = 300

        const val SCENE_BUDGET = 2_000
        const val SCENE_SUMMARY_BUDGET = 100

        /**
         * What read_pages adds to the page's own text, which the page's length limit (ScreenplayStyle) keeps in bounds.
         */
        const val READ_PAGES_BUDGET = 270
    }
}
