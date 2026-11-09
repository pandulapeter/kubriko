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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.screenplay.DocumentNaming

/**
 * How long a document Kubriko Creator writes can be: more than a page, since a changelog or a guide is the user's own.
 */
internal const val DOCUMENT_MAXIMUM_LENGTH = 200_000

/**
 * Why there is no document at [path], or null if there is one.
 */
internal fun CommandContext.validateDocument(path: String): Rejection? = if (documents.isKnown(path)) null else Rejection(CommandProblem.DOCUMENT_MISSING, path)

/**
 * Why no new document can go to [path] (or [moved] be moved there), or null if one can.
 */
internal fun CommandContext.validateNewDocumentPath(path: String, moved: String? = null): Rejection? =
    DocumentNaming.validate(path)?.let(::Rejection) ?: validateFreeDocumentPath(path, moved)

/**
 * Why [path] is taken by another document than [moved], or null if it is free: all that undoing a delete or a move
 * checks, since the user may have named the file in a way Kubriko Creator wouldn't.
 */
internal fun CommandContext.validateFreeDocumentPath(path: String, moved: String? = null): Rejection? =
    if (documents.paths().any { it.equals(path, ignoreCase = true) && it != moved }) Rejection(CommandProblem.DOCUMENT_TAKEN, path) else null

internal fun validateDocumentText(text: String): Rejection? =
    if (text.length > DOCUMENT_MAXIMUM_LENGTH) Rejection(CommandProblem.DOCUMENT_TOO_LONG, DOCUMENT_MAXIMUM_LENGTH.toString()) else null

internal const val DOCUMENT_PATH_DESCRIPTION = "The document's path from the project's root, with .md, like README.md or docs/combat.md."
