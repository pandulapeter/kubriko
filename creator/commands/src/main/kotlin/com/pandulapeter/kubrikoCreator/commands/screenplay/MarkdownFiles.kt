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
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * The name of the page at [path], a path from the project's root, or null if no page is there.
 */
internal fun pageOf(path: String): String? = PageNaming.nameOf(path)

/**
 * The text of the page or document at [path], a path from the project's root, or null if there is none or it isn't
 * UTF-8 text, which writing it back would change.
 */
internal fun CommandContext.readMarkdown(path: String): String? = documents.read(path)

/**
 * Writes the page or document at [path], a path from the project's root, through the store it belongs to.
 */
@EditorMutation
internal fun CommandContext.writeMarkdown(path: String, text: String) {
    val page = pageOf(path)
    if (page != null) screenplay.write(page, text) else documents.write(path, text)
}
