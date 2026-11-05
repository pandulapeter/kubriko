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

import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.project.ResourceScanner

/**
 * How the project's documents are named: by their paths from the project's root, `.md` included (`docs/combat.md`). The
 * user names theirs as they like; what Kubriko Creator creates, or moves one to, follows [isValid].
 */
public object DocumentNaming {

    private val namePattern = Regex("[A-Za-z0-9][A-Za-z0-9 ._-]*")
    private const val MAXIMUM_LENGTH = 200

    /**
     * Whether Kubriko Creator may create a document at [path]: a Markdown file outside the screenplay and outside every
     * folder the project walk leaves out, each part starting with a letter or a digit, so it is never hidden, and ending
     * in neither a space nor a dot, which Windows drops.
     */
    public fun isValid(path: String): Boolean = path.length <= MAXIMUM_LENGTH &&
        path.endsWith(".${PageNaming.EXTENSION}") &&
        ResourceScanner.isDocument(path) &&
        path.split('/').let { parts ->
            parts.all { part -> namePattern.matches(part) && !part.endsWith(' ') && !part.endsWith('.') } &&
                parts.dropLast(1).none { ProjectTree.isLeftOut(it) } &&
                parts.last() != ".${PageNaming.EXTENSION}"
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
