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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(EditorMutation::class)
class PageImageCommandsTest {

    private fun TestScope.context(): CommandContext = commandContext().apply {
        file("screenplay/levels/cave.md", "# Cave\n\n![Map](cave-1.png) ![Bats](cave-2.gif) ![Other](other-1.png)\n")
        file("screenplay/levels/cave-1.png", "map")
        file("screenplay/levels/cave-2.gif", "bats")
        file("screenplay/levels/cave-3.txt", "not an image")
        file("screenplay/levels/other-1.png", "other")
        file("screenplay/README.md", "Start in the [cave](levels/cave.md), ![map](levels/cave-1.png).\n")
        screenplay.refresh()
    }

    private fun CommandContext.file(path: String, text: String) = File(project.rootDirectory, path).apply { parentFile.mkdirs() }.writeText(text)

    private fun CommandContext.files() = File(project.rootDirectory, "screenplay").walkTopDown().filter { it.isFile }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    @Test
    fun `a moved page takes its own images along, renamed after it, and every link follows`() = runTest {
        val context = context()
        val after = context.assertRoundTrip(Invocation(MovePage, MovePage.Input("levels/cave", "areas/den"))) { context.files() }
        assertEquals(
            mapOf(
                "screenplay/areas/den.md" to "# Cave\n\n![Map](den-1.png) ![Bats](den-2.gif) ![Other](../levels/other-1.png)\n",
                "screenplay/areas/den-1.png" to "map",
                "screenplay/areas/den-2.gif" to "bats",
                "screenplay/levels/cave-3.txt" to "not an image",
                "screenplay/levels/other-1.png" to "other",
                "screenplay/README.md" to "Start in the [cave](areas/den.md), ![map](areas/den-1.png).\n",
            ),
            after,
        )
    }

    @Test
    fun `an image whose new name is taken stays, and its link still finds it`() = runTest {
        val context = context()
        context.file("screenplay/den-1.png", "someone else's")
        MovePage.apply(context, MovePage.Input("levels/cave", "den"))
        assertEquals("# Cave\n\n![Map](levels/cave-1.png) ![Bats](den-2.gif) ![Other](levels/other-1.png)\n", context.files().getValue("screenplay/den.md"))
        assertEquals("map", context.files().getValue("screenplay/levels/cave-1.png"))
    }

    @Test
    fun `a renamed folder of pages takes every file in it along`() = runTest {
        val context = context()
        val after = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.PAGES, "levels", "areas"))) { context.files() }
        assertEquals(setOf("screenplay/README.md", "screenplay/areas/cave.md", "screenplay/areas/cave-1.png", "screenplay/areas/cave-2.gif", "screenplay/areas/cave-3.txt", "screenplay/areas/other-1.png"), after.keys)
        assertEquals("Start in the [cave](areas/cave.md), ![map](areas/cave-1.png).\n", after.getValue("screenplay/README.md"))
    }

    @Test
    fun `deleting a page deletes its own images no other page shows only when asked, and undo brings back every byte`() = runTest {
        val keeping = context()
        assertEquals(keeping.files() - "screenplay/levels/cave.md", keeping.assertRoundTrip(Invocation(DeletePage, DeletePage.Input("levels/cave"))) { keeping.files() })
        val deleting = context()
        val after = deleting.assertRoundTrip(Invocation(DeletePage, DeletePage.Input("levels/cave", deleteImages = true))) { deleting.files() }
        assertEquals(setOf("screenplay/README.md", "screenplay/levels/cave-1.png", "screenplay/levels/cave-3.txt", "screenplay/levels/other-1.png"), after.keys)
        val applied = DeletePage.apply(context(), DeletePage.Input("levels/cave", deleteImages = true))
        assertEquals(listOf("screenplay/levels/cave-2.gif"), applied.details!!.getValue("deletedImages").jsonArray.map { it.jsonPrimitive.content })
    }
}
