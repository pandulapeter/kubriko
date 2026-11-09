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

import com.pandulapeter.kubrikoCreator.actors.NameFlaw
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.project.ResourceScanner

/**
 * How the project's documents are named: by their paths from the project's root, `.md` included (`docs/combat.md`). The
 * user names theirs as they like; what Kubriko Creator creates, or moves one to, follows [isValid].
 */
public object DocumentNaming {

    private const val MAXIMUM_LENGTH = 200

    /**
     * Why Kubriko Creator may not create a document at [path], with the arguments of the message, or null if it may: a
     * Markdown file (`.md` in any case) outside the screenplay and outside every folder the project walk leaves out, each
     * part starting with a letter or a digit, so it is never hidden, and ending in neither a space nor a dot, which
     * Windows drops.
     */
    public fun validate(path: String): Pair<NameProblem, List<String>>? = if (path.length > MAXIMUM_LENGTH) {
        NameProblem.DOCUMENT_PATH_TOO_LONG to listOf(MAXIMUM_LENGTH.toString())
    } else {
        (kindProblemOf(path) ?: partsProblemOf(path))?.let { it to emptyList() }
    }

    private fun partsProblemOf(path: String) = path.split('/').let { parts ->
        parts.firstNotNullOfOrNull(::partProblemOf) ?: if (parts.dropLast(1).any(ProjectTree::isLeftOut)) NameProblem.DOCUMENT_IN_LEFT_OUT_FOLDER else null
    }

    /**
     * Why the file at [path] isn't a document wherever its folders are (see [ResourceScanner.isDocument]), or null if it is one.
     */
    public fun kindProblemOf(path: String): NameProblem? = when {
        ResourceScanner.isDocument(path) -> null
        path.startsWith(PageNaming.FOLDER + "/") -> NameProblem.DOCUMENT_IN_SCREENPLAY
        else -> NameProblem.DOCUMENT_NOT_MARKDOWN
    }

    private fun partProblemOf(part: String) = when (NameFlaw.of(part, NameFlaw::isLetterOrDigitAToZ) { NameFlaw.isLetterOrDigitAToZ(it) || it in " ._-" }) {
        NameFlaw.EMPTY -> NameProblem.PATH_PART_EMPTY
        NameFlaw.LETTER_BEYOND_A_TO_Z -> NameProblem.NAME_LETTER_BEYOND_A_TO_Z
        NameFlaw.FIRST_CHARACTER -> NameProblem.DOCUMENT_FIRST_CHARACTER
        NameFlaw.CHARACTER -> NameProblem.DOCUMENT_CHARACTERS
        null -> if (part.endsWith(' ') || part.endsWith('.')) NameProblem.DOCUMENT_END else null
    }

    /**
     * The path of a document called [name] in [folder] (an empty string for the project's root), with `.md` added unless
     * [name] already ends with it.
     */
    public fun pathOf(folder: String, name: String): String {
        val fileName = if (name.endsWith(".${PageNaming.EXTENSION}", ignoreCase = true)) name else "$name.${PageNaming.EXTENSION}"
        return if (folder.isEmpty()) fileName else "$folder/$fileName"
    }

    /**
     * The folder the document at [path] is in, or an empty string for one at the project's root.
     */
    public fun folderOf(path: String): String = path.substringBeforeLast('/', missingDelimiterValue = "")

    /**
     * The name of the page the document at [path] becomes in the screenplay: its own name, at the screenplay's top, with
     * what a page's name can't hold ([PageNaming]) turned into dashes.
     */
    public fun pageNameOf(path: String): String = nameOf(path)
        .replace(Regex("[^A-Za-z0-9 _-]"), "-")
        .trim(' ', '-', '_')
        .ifEmpty { "document" }
        .take(PAGE_NAME_LENGTH)
        .trimEnd()

    private const val PAGE_NAME_LENGTH = 100

    /**
     * The name of the document at [path], without its folders and `.md`.
     */
    public fun nameOf(path: String): String = path.substringAfterLast('/').let { it.substring(0, it.length - PageNaming.EXTENSION.length - 1) }
}
