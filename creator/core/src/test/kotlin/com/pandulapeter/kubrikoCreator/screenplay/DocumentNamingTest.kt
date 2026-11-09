/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.actors.NameProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentNamingTest {

    @Test
    fun documentsCreatorMakesAreMarkdownOutsideTheScreenplayAndTheBuildFolders() {
        listOf("README.md", "docs/combat.md", "editor/docs/v1.2 notes.md", "CHANGELOG.md", "NOTES.MD").forEach { assertTrue(DocumentNaming.isValid(it), it) }
        listOf(
            "notes.txt",
            ".md",
            "docs/.hidden.md",
            ".github/notes.md",
            "screenplay/hero.md",
            "build/notes.md",
            "app/build/notes.md",
            "node_modules/notes.md",
            "docs//notes.md",
            "/notes.md",
            "../notes.md",
            "docs /notes.md",
            "docs./notes.md",
            "a\\b.md",
            "a".repeat(200) + ".md",
        ).forEach { assertFalse(DocumentNaming.isValid(it), it) }
    }

    @Test
    fun aPathIsRefusedForTheRuleItBreaks() {
        mapOf(
            "notes.txt" to NameProblem.DOCUMENT_NOT_MARKDOWN,
            "screenplay/hero.md" to NameProblem.DOCUMENT_IN_SCREENPLAY,
            "app/build/notes.md" to NameProblem.DOCUMENT_IN_LEFT_OUT_FOLDER,
            "docs/.hidden.md" to NameProblem.DOCUMENT_FIRST_CHARACTER,
            "docs/a!b.md" to NameProblem.DOCUMENT_CHARACTERS,
            "docs./notes.md" to NameProblem.DOCUMENT_END,
            "docs//notes.md" to NameProblem.PATH_PART_EMPTY,
            "jegyzetek/ötletek.md" to NameProblem.NAME_LETTER_BEYOND_A_TO_Z,
        ).forEach { (path, problem) -> assertEquals(problem, DocumentNaming.validate(path), path) }
        assertNull(DocumentNaming.validate("a".repeat(DocumentNaming.MAXIMUM_LENGTH) + ".md"))
    }

    @Test
    fun aNameInAFolderBecomesAPath() {
        assertEquals("docs/combat.md", DocumentNaming.pathOf("docs", "combat"))
        assertEquals("README.md", DocumentNaming.pathOf("", "README.md"))
        assertEquals("docs", DocumentNaming.folderOf("docs/combat.md"))
        assertEquals("", DocumentNaming.folderOf("README.md"))
        assertEquals("combat", DocumentNaming.nameOf("docs/combat.md"))
        assertEquals("Notes", DocumentNaming.nameOf("Notes.MD"))
    }

    @Test
    fun aDocumentBecomesAPageOfItsOwnNameAsAPageCanHoldIt() {
        listOf("docs/combat.md" to "combat", "v1.2 notes.md" to "v1-2 notes", "_draft.md" to "draft", "....md" to "document").forEach { (path, page) ->
            assertEquals(page, DocumentNaming.pageNameOf(path))
            assertTrue(PageNaming.isValid(page), page)
        }
    }
}
