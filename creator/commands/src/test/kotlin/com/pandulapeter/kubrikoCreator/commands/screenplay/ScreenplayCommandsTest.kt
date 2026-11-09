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

import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ScreenplayCommandsTest {
    private val hero = "# The hero\n\nA knight who jumps.\n\n## Moves\n\nRuns.\n\n## Planned\n\n- Wall jump\n"

    private fun CommandContext.pages() = File(project.rootDirectory, "screenplay").walkTopDown().filter { it.isFile }.associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    @Test
    fun pagesAreCreatedEditedMovedAndDeletedWithUndo() = runTest {
        val context = commandContext()
        assertEquals(mapOf("screenplay/hero.md" to hero), context.assertRoundTrip(Invocation(CreatePage, CreatePage.Input("hero", hero))) { context.pages() })
        val edited = context.assertRoundTrip(Invocation(EditPage, EditPage.Input("hero", "## Moves\n\nRuns and rolls.", section = "Moves"))) { context.pages() }
        assertEquals(hero.replace("Runs.", "Runs and rolls."), edited.getValue("screenplay/hero.md"))
        assertEquals(setOf("screenplay/people/hero.md"), context.assertRoundTrip(Invocation(MovePage, MovePage.Input("hero", "people/hero"))) { context.pages() }.keys)
        assertEquals(emptyMap(), context.assertRoundTrip(Invocation(DeletePage, DeletePage.Input("people/hero"))) { context.pages() })
    }

    @Test
    fun pageChangesAreValidated() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("hero", hero))
        assertEquals(Rejection(CommandProblem.PAGE_TAKEN, "Hero"), CreatePage.validate(context, CreatePage.Input("Hero", "")))
        assertEquals(Rejection(NameProblem.FOLDER_CHARACTERS), CreatePage.validate(context, CreatePage.Input("hero.md", "")))
        assertEquals(Rejection(CommandProblem.PAGE_TOO_LONG, PAGE_MAXIMUM_LENGTH.toString()), CreatePage.validate(context, CreatePage.Input("long", "x".repeat(PAGE_MAXIMUM_LENGTH + 1))))
        assertEquals(Rejection(CommandProblem.PAGE_MISSING, "villain"), EditPage.validate(context, EditPage.Input("villain", "")))
        assertEquals(Rejection(CommandProblem.PAGE_SECTION_MISSING, "hero", "Enemies"), EditPage.apply(context, EditPage.Input("hero", "## Enemies", section = "Enemies")).failure)
        assertNull(MovePage.validate(context, MovePage.Input("hero", "HERO")))
        assertEquals(Rejection(CommandProblem.PAGE_TAKEN, "hero"), MovePage.validate(context, MovePage.Input("hero", "hero")))
    }

    @Test
    fun aPageRenamedOnlyInCaseTakesTheNewName() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("hero", hero))
        assertEquals(setOf("screenplay/Hero.md"), context.assertRoundTrip(Invocation(MovePage, MovePage.Input("hero", "Hero"))) { context.pages().keys })
    }

    @Test
    fun aPageMadeOutsideMeanwhileIsntWrittenOver() = runTest {
        val context = commandContext()
        context.screenplay.refresh()
        File(context.project.rootDirectory, "screenplay/hero.md").apply { parentFile.mkdirs() }.writeText("Mine")
        assertNull(CreatePage.validate(context, CreatePage.Input("hero", hero)))
        assertEquals(Rejection(CommandProblem.PAGE_TAKEN, "hero"), CreatePage.apply(context, CreatePage.Input("hero", hero)).failure)
        assertEquals("Mine", context.pages().getValue("screenplay/hero.md"))
    }

    @Test
    fun pagesAreListedWithTheirSummariesAndReadWhole() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("hero", hero))
        CreatePage.apply(context, CreatePage.Input("README", "# The game\n\nA platformer.\n"))
        val list = assertIs<CommandOutcome.Success>(ListPages.read(context, NoInput)).result.jsonArray
        assertEquals(listOf("README", "hero"), list.map { it.jsonObject.getValue("page").jsonPrimitive.content })
        assertEquals("A knight who jumps.", list[1].jsonObject.getValue("summary").jsonPrimitive.content)
        val pages = assertIs<CommandOutcome.Success>(ReadPages.read(context, ReadPages.Input(listOf("hero")))).result.jsonObject
        assertEquals(hero, pages.getValue("hero").jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals(null, pages.getValue("hero").jsonObject["images"])
        assertIs<CommandOutcome.Rejected>(ReadPages.read(context, ReadPages.Input(listOf("villain"))))
    }

    @Test
    fun typingIntoAPageIsOneStepToUndo() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("hero", hero))
        val executor = CommandExecutor(context = context, scope = testScope())
        executor.execute(Invocation(EditPage, EditPage.Input("hero", "# The hero\n")))
        executor.execute(Invocation(EditPage, EditPage.Input("hero", "# The hero, again\n")))
        executor.undo()
        assertEquals(hero, context.pages().getValue("screenplay/hero.md"))
    }

    @Test
    fun pageFoldersAreCreatedRenamedWithTheirPagesAndDeletedWithUndo() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("levels/cave", hero))
        CreatePage.apply(context, CreatePage.Input("levels/deep/pit", hero))
        context.assertRoundTrip(Invocation(CreateFolder, CreateFolder.Input(FolderGroup.PAGES, "people"))) { context.screenplay.scanFolders() }
        val renamed = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.PAGES, "levels", "worlds"))) { context.screenplay.scanNames() to context.screenplay.scanFolders() }
        assertEquals(listOf("worlds/cave", "worlds/deep/pit") to setOf("people", "worlds", "worlds/deep"), renamed)
        assertEquals(Rejection(CommandProblem.FOLDER_NOT_EMPTY, "worlds"), DeleteFolder.validate(context, DeleteFolder.Input(FolderGroup.PAGES, "worlds")))
        assertEquals(Rejection(NameProblem.PAGE_NAME_TOO_LONG, "120"), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.PAGES, "a".repeat(121))))
        val left = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.PAGES, "people"))) { context.screenplay.scanFolders() }
        assertEquals(setOf("worlds", "worlds/deep"), left)
    }

    @Test
    fun aFolderIsntRenamedSoThatAPageInItsNameGrowsPastTheLimit() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("a/" + "p".repeat(110), hero))
        assertEquals(Rejection(NameProblem.PAGE_NAME_TOO_LONG, "120"), RenameFolder.validate(context, RenameFolder.Input(FolderGroup.PAGES, "a", "b".repeat(15))))
        assertNull(RenameFolder.validate(context, RenameFolder.Input(FolderGroup.PAGES, "a", "b".repeat(9))))
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.PAGES, "c/" + "f".repeat(110)))
        assertEquals(Rejection(NameProblem.PAGE_NAME_TOO_LONG, "120"), RenameFolder.validate(context, RenameFolder.Input(FolderGroup.PAGES, "c", "d".repeat(15))))
    }

    @Test
    fun aPageNamedOrSizedAsOnlyTheUserCanMakeOneComesBackOnUndo() = runTest {
        val context = commandContext()
        val long = "# Long\n\n" + "x".repeat(PAGE_MAXIMUM_LENGTH)
        File(context.project.rootDirectory, "screenplay/Notes (old).md").apply { parentFile.mkdirs() }.writeText(long)
        context.screenplay.refresh()
        val edited = context.assertRoundTrip(Invocation(EditPage, EditPage.Input("Notes (old)", "# Short\n"))) { context.pages() }
        assertEquals("# Short\n", edited.getValue("screenplay/Notes (old).md"))
        EditPage.apply(context, EditPage.Input("Notes (old)", long))
        val moved = context.assertRoundTrip(Invocation(MovePage, MovePage.Input("Notes (old)", "notes"))) { context.pages().keys }
        assertEquals(setOf("screenplay/notes.md"), moved)
        MovePage.apply(context, MovePage.Input("notes", "Notes (old)"))
        val deleted = context.assertRoundTrip(Invocation(DeletePage, DeletePage.Input("Notes (old)"))) { context.pages() }
        assertEquals(emptyMap(), deleted)
    }
}
