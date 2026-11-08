/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

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

    /**
     * An image on a line of its own: [alt] describes it, [destination] is where it is, as written.
     */
    data class Image(
        val alt: String,
        val destination: String,
    ) : MarkdownBlock

    /**
     * @param rows The rows under the [header], each with as many cells as the header (missing ones empty, extra ones dropped).
     */
    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : MarkdownBlock
}
