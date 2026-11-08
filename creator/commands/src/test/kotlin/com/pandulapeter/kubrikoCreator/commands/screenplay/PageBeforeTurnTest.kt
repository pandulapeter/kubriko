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

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PageBeforeTurnTest {
    private val hero = "# The hero\n\nA knight who jumps.\n"
    private val conversation = AiStepOrigin(conversationId = "conversation")

    private fun TestScope.executor() = CommandExecutor(commandContext(), testScope())

    private suspend fun CommandExecutor.byAi(name: String, origin: AiStepOrigin = conversation, vararg arguments: Pair<String, String>) =
        assertIs<CommandOutcome.Success>(call(name, buildJsonObject { arguments.forEach { (key, value) -> put(key, value) } }, origin))

    private fun CommandExecutor.before(since: Int, page: String) = history.value.pageBeforeTurn("conversation", since, page)

    @Test
    fun aPageTheTurnEditedWasAsItWasBeforeItsFirstEdit() = runTest {
        val executor = executor()
        executor.execute(Invocation(CreatePage, CreatePage.Input("hero", hero)))
        val since = executor.changeLog.revision
        executor.byAi("edit_page", arguments = arrayOf("page" to "hero", "text" to "# The hero\n\nA knight who rolls.\n"))
        executor.byAi("edit_page", arguments = arrayOf("page" to "hero", "text" to "# The hero\n\nA knight who swims.\n"))
        assertEquals(hero, executor.before(since, "hero"))
        assertNull(executor.history.value.pageBeforeTurn("conversation", executor.changeLog.revision, "hero"))
    }

    @Test
    fun onlyTheChangesNewerThanAnyoneElsesCountAsUndoingStopsAtTheirs() = runTest {
        val executor = executor()
        executor.execute(Invocation(CreatePage, CreatePage.Input("hero", hero)))
        val since = executor.changeLog.revision
        val rolls = "# The hero\n\nA knight who rolls.\n"
        executor.byAi("edit_page", arguments = arrayOf("page" to "hero", "text" to rolls))
        executor.byAi("edit_page", AiStepOrigin(conversationId = "another"), "page" to "hero", "text" to "# The hero\n\nA knight.\n")
        executor.byAi("edit_page", arguments = arrayOf("page" to "hero", "text" to "# The hero\n\nA knight who swims.\n"))
        assertEquals("# The hero\n\nA knight.\n", executor.before(since, "hero"))
        executor.execute(Invocation(EditPage, EditPage.Input("hero", "# The hero\n\nA knight who swims far.\n")))
        assertNull(executor.before(since, "hero"))
    }

    @Test
    fun aPageTheTurnMadeFromADocumentWasEmpty() = runTest {
        val executor = executor()
        executor.execute(Invocation(CreateDocument, CreateDocument.Input("design.md", hero)))
        val since = executor.changeLog.revision
        executor.byAi("add_document_to_screenplay", arguments = arrayOf("document" to "design.md", "page" to "hero"))
        assertEquals("", executor.before(since, "hero"))
    }

    @Test
    fun aPageTheTurnCreatedWasEmptyAndAnUntouchedOneIsntNamed() = runTest {
        val executor = executor()
        executor.execute(Invocation(CreatePage, CreatePage.Input("villain", "# The villain\n\nA dragon.\n")))
        val since = executor.changeLog.revision
        executor.byAi("create_page", arguments = arrayOf("page" to "hero", "text" to hero))
        executor.byAi("edit_page", arguments = arrayOf("page" to "hero", "text" to "# The hero\n\nA knight who rolls.\n"))
        assertEquals("", executor.before(since, "hero"))
        assertNull(executor.before(since, "villain"))
    }

    @Test
    fun aMovedPageIsFollowedBackAndSoAreTheLinksThatFollowedIt() = runTest {
        val executor = executor()
        val linking = "# The game\n\nIt has a [hero](hero.md).\n"
        executor.execute(Invocation(CreatePage, CreatePage.Input("hero", hero)))
        executor.execute(Invocation(CreatePage, CreatePage.Input("README", linking)))
        val since = executor.changeLog.revision
        executor.byAi("move_page", arguments = arrayOf("page" to "hero", "newPage" to "people/hero"))
        executor.byAi("edit_page", arguments = arrayOf("page" to "people/hero", "text" to "# The hero\n\nA knight who rolls.\n"))
        assertEquals(hero, executor.before(since, "people/hero"))
        assertEquals(linking, executor.before(since, "README"))
    }
}
