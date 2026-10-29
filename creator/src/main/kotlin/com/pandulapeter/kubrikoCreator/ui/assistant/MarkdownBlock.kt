/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

/**
 * One block of a Markdown text, as the assistant panel draws it.
 */
internal sealed interface MarkdownBlock {

    data class Paragraph(
        val text: String,
    ) : MarkdownBlock

    data class Heading(
        val level: Int,
        val text: String,
    ) : MarkdownBlock

    /**
     * @param number The item's number in an ordered list, or null for a bullet.
     * @param depth How deeply the item is nested, from 0.
     */
    data class ListItem(
        val text: String,
        val number: String? = null,
        val depth: Int = 0,
    ) : MarkdownBlock

    data class Quote(
        val text: String,
    ) : MarkdownBlock

    data class Code(
        val text: String,
    ) : MarkdownBlock

    data object Rule : MarkdownBlock
}
