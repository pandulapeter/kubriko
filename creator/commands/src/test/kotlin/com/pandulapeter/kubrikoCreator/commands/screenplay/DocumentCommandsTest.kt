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
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DocumentCommandsTest {
    private val combat = "# Combat\r\n\r\nSwords.\r\n\r\n## Moves\r\n\r\nSlash.\r\n"

    private fun CommandContext.files() = project.rootDirectory.walkTopDown().filter { it.isFile }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    private fun CommandContext.write(path: String, text: String) {
        File(project.rootDirectory, path).apply { parentFile.mkdirs() }.writeText(text)
        documents.refresh((documents.paths() + path).sorted())
    }

    @Test
    fun documentsAreCreatedEditedMovedAndDeletedWithUndo() = runTest {
        val context = commandContext()
        assertEquals(mapOf("docs/combat.md" to combat), context.assertRoundTrip(Invocation(CreateDocument, CreateDocument.Input("docs/combat.md", combat))) { context.files() })
        val edited = context.assertRoundTrip(Invocation(EditDocument, EditDocument.Input("docs/combat.md", "## Moves\n\nSlash and parry.", section = "Moves"))) { context.files() }
        assertEquals(combat.replace("Slash.", "Slash and parry."), edited.getValue("docs/combat.md"))
        assertEquals(setOf("editor/combat.md"), context.assertRoundTrip(Invocation(MoveDocument, MoveDocument.Input("docs/combat.md", "editor/combat.md"))) { context.files().keys })
        assertEquals(emptyMap(), context.assertRoundTrip(Invocation(DeleteDocument, DeleteDocument.Input("editor/combat.md"))) { context.files() })
    }

    @Test
    fun aDeletedDocumentComesBackByteForByte() = runTest {
        val context = commandContext()
        val bytes = "# Notes\r\n\r\nÁrvíztűrő\r\n".toByteArray()
        File(context.project.rootDirectory, "Notes (old).MD").writeBytes(bytes)
        context.documents.refresh(listOf("Notes (old).MD"))
        val inverse = DeleteDocument.apply(context, DeleteDocument.Input("Notes (old).MD")).inverse!!.copy(isRestoring = true)
        assertNull(inverse.validate(context))
        inverse.apply(context)
        assertContentEquals(bytes, File(context.project.rootDirectory, "Notes (old).MD").readBytes())
    }

    @Test
    fun aDocumentThatIsntTextIsLeftAlone() = runTest {
        val context = commandContext()
        File(context.project.rootDirectory, "notes.md").writeBytes(byteArrayOf(0x23, 0xC3.toByte(), 0x28))
        context.documents.refresh(listOf("notes.md"))
        assertEquals(Rejection(CommandProblem.DOCUMENT_UNREADABLE, "notes.md"), DeleteDocument.apply(context, DeleteDocument.Input("notes.md")).failure)
        assertEquals(Rejection(CommandProblem.DOCUMENT_UNREADABLE, "notes.md"), EditDocument.apply(context, EditDocument.Input("notes.md", "# Notes")).failure)
        assertEquals(3, File(context.project.rootDirectory, "notes.md").length())
    }

    @Test
    fun documentChangesAreValidated() = runTest {
        val context = commandContext()
        context.write("README.md", "# Game\n")
        assertEquals(Rejection(CommandProblem.DOCUMENT_TAKEN, "readme.md"), CreateDocument.validate(context, CreateDocument.Input("readme.md", "")))
        assertEquals(Rejection(CommandProblem.DOCUMENT_PATH_INVALID, "screenplay/hero.md"), CreateDocument.validate(context, CreateDocument.Input("screenplay/hero.md", "")))
        assertEquals(Rejection(CommandProblem.DOCUMENT_PATH_INVALID, "build/notes.md"), CreateDocument.validate(context, CreateDocument.Input("build/notes.md", "")))
        assertEquals(
            Rejection(CommandProblem.DOCUMENT_TOO_LONG, DOCUMENT_MAXIMUM_LENGTH.toString()),
            CreateDocument.validate(context, CreateDocument.Input("long.md", "x".repeat(DOCUMENT_MAXIMUM_LENGTH + 1))),
        )
        assertEquals(Rejection(CommandProblem.DOCUMENT_MISSING, "docs/notes.md"), EditDocument.validate(context, EditDocument.Input("docs/notes.md", "")))
        assertEquals(Rejection(CommandProblem.PAGE_SECTION_MISSING, "README.md", "Moves"), EditDocument.apply(context, EditDocument.Input("README.md", "## Moves", section = "Moves")).failure)
        assertEquals(Rejection(CommandProblem.DOCUMENT_TAKEN, "README.md"), MoveDocument.validate(context, MoveDocument.Input("README.md", "README.md")))
        assertEquals(Rejection(CommandProblem.DOCUMENT_PATH_INVALID, "screenplay/README.md"), MoveDocument.validate(context, MoveDocument.Input("README.md", "screenplay/README.md")))
        assertNull(MoveDocument.validate(context, MoveDocument.Input("README.md", "readme.md")))
        assertEquals(setOf("readme.md"), context.assertRoundTrip(Invocation(MoveDocument, MoveDocument.Input("README.md", "readme.md"))) { context.files().keys })
    }

    @Test
    fun aFileMadeOutsideMeanwhileIsntWrittenOver() = runTest {
        val context = commandContext()
        File(context.project.rootDirectory, "docs/combat.md").apply { parentFile.mkdirs() }.writeText("Mine")
        assertNull(CreateDocument.validate(context, CreateDocument.Input("docs/combat.md", combat)))
        assertEquals(Rejection(CommandProblem.DOCUMENT_TAKEN, "docs/combat.md"), CreateDocument.apply(context, CreateDocument.Input("docs/combat.md", combat)).failure)
        context.write("notes.md", "Notes")
        assertEquals(Rejection(CommandProblem.DOCUMENT_TAKEN, "docs/combat.md"), MoveDocument.apply(context, MoveDocument.Input("notes.md", "docs/combat.md")).failure)
        assertEquals(mapOf("docs/combat.md" to "Mine", "notes.md" to "Notes"), context.files())
    }

    @Test
    fun aDocumentIsAddedToTheScreenplayAndUndoMovesItBack() = runTest {
        val context = commandContext()
        context.write("docs/combat.md", combat)
        context.screenplay.refresh()
        val moved = context.assertRoundTrip(Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("docs/combat.md", "fights/combat"))) {
            context.files() to context.screenplay.names()
        }
        assertEquals(mapOf("screenplay/fights/combat.md" to combat) to listOf("fights/combat"), moved)
        assertEquals(emptyList(), context.documents.paths())
        context.write("docs/combat.md", combat)
        CreatePage.apply(context, CreatePage.Input("hero", "# Hero\n"))
        assertEquals(Rejection(CommandProblem.PAGE_TAKEN, "hero"), AddDocumentToScreenplay.validate(context, AddDocumentToScreenplay.Input("docs/combat.md", "hero")))
        assertEquals(Rejection(CommandProblem.PAGE_NAME_INVALID, "hero.md"), AddDocumentToScreenplay.validate(context, AddDocumentToScreenplay.Input("docs/combat.md", "hero.md")))
    }

    @Test
    fun undoingACreationIsDroppedOnceTheDocumentChangedOutside() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context = context, scope = testScope())
        executor.execute(Invocation(CreateDocument, CreateDocument.Input("notes.md", "# Notes\n")))
        File(context.project.rootDirectory, "notes.md").writeText("# Notes, mine\n")
        executor.undo()
        assertEquals("# Notes, mine\n", File(context.project.rootDirectory, "notes.md").readText())
    }

    @Test
    fun typingIntoADocumentIsOneStepToUndo() = runTest {
        val context = commandContext()
        context.write("README.md", combat)
        val executor = CommandExecutor(context = context, scope = testScope())
        executor.execute(Invocation(EditDocument, EditDocument.Input("README.md", "# Combat\n")))
        executor.execute(Invocation(EditDocument, EditDocument.Input("README.md", "# Combat, again\n")))
        executor.undo()
        assertEquals(combat, context.files().getValue("README.md"))
    }

    @Test
    fun documentsAreListedAndReadWhole() = runTest {
        val context = commandContext()
        context.write("README.md", "# Game\n")
        context.write("docs/combat.md", combat)
        val list = assertIs<CommandOutcome.Success>(ReadDocuments.read(context, ReadDocuments.Input())).result.jsonArray
        assertEquals(listOf("README.md", "docs/combat.md"), list.map { it.jsonPrimitive.content })
        val read = assertIs<CommandOutcome.Success>(ReadDocuments.read(context, ReadDocuments.Input(listOf("docs/combat.md")))).result.jsonObject
        assertEquals(combat, read.getValue("docs/combat.md").jsonPrimitive.content)
        assertEquals(Rejection(CommandProblem.DOCUMENT_MISSING, "notes.md"), assertIs<CommandOutcome.Rejected>(ReadDocuments.read(context, ReadDocuments.Input(listOf("notes.md")))).rejection)
    }

    @Test
    fun aDocumentIsItsOwnTarget() = runTest {
        val context = commandContext()
        assertEquals(CommandTarget.Document("notes.md"), CreateDocument.target(context, CreateDocument.Input("notes.md", "")))
    }

    @Test
    fun aDocumentTooLongForAPageOrNotTextStaysADocument() = runTest {
        val context = commandContext()
        context.write("CHANGELOG.md", "x".repeat(PAGE_MAXIMUM_LENGTH + 1))
        File(context.project.rootDirectory, "binary.md").writeBytes(byteArrayOf(0x23, 0xC3.toByte(), 0x28))
        context.documents.refresh(listOf("CHANGELOG.md", "binary.md"))
        assertEquals(Rejection(CommandProblem.PAGE_TOO_LONG, PAGE_MAXIMUM_LENGTH.toString()), AddDocumentToScreenplay.apply(context, AddDocumentToScreenplay.Input("CHANGELOG.md", "changelog")).failure)
        assertEquals(Rejection(CommandProblem.DOCUMENT_UNREADABLE, "binary.md"), AddDocumentToScreenplay.apply(context, AddDocumentToScreenplay.Input("binary.md", "binary")).failure)
        assertEquals(setOf("CHANGELOG.md", "binary.md"), context.files().keys)
    }

    @Test
    fun aDocumentLongerThanCreatorWouldWriteCanStillBeEditedButNotGrown() = runTest {
        val context = commandContext()
        val long = "x".repeat(DOCUMENT_MAXIMUM_LENGTH + 10)
        context.write("CHANGELOG.md", long)
        assertNull(EditDocument.apply(context, EditDocument.Input("CHANGELOG.md", long.dropLast(1))).failure)
        assertEquals(
            Rejection(CommandProblem.DOCUMENT_TOO_LONG, DOCUMENT_MAXIMUM_LENGTH.toString()),
            EditDocument.apply(context, EditDocument.Input("CHANGELOG.md", long + "y")).failure,
        )
        assertEquals(long.dropLast(1), context.files().getValue("CHANGELOG.md"))
    }

    @Test
    fun aDocumentWithAnUppercaseExtensionIsMovedAndRenamed() = runTest {
        val context = commandContext()
        context.write("NOTES.MD", "# Notes")
        assertEquals(setOf("docs/NOTES.MD"), context.assertRoundTrip(Invocation(MoveDocument, MoveDocument.Input("NOTES.MD", "docs/NOTES.MD"))) { context.files().keys })
    }

    @Test
    fun anotherFileDifferingOnlyInCaseTakesThePath() = runTest {
        val context = commandContext()
        context.documents.refresh(listOf("a.md", "A.md"))
        assertEquals(Rejection(CommandProblem.DOCUMENT_TAKEN, "A.md"), MoveDocument.validate(context, MoveDocument.Input("a.md", "A.md")))
        context.documents.refresh(listOf("a.md"))
        assertNull(MoveDocument.validate(context, MoveDocument.Input("a.md", "A.md")))
    }
}
