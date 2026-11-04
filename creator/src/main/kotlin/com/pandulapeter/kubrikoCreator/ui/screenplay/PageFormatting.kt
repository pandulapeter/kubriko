/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The formatting the page editor's toolbar and shortcuts write, each as plain Markdown into what is being typed.
 */
internal object PageFormatting {

    /**
     * [value] with its selection wrapped in [marker] on both sides (`**` for bold), still selected, or the markers
     * inserted with the cursor between them when nothing is selected.
     */
    fun wrapped(value: TextFieldValue, marker: String): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val text = value.text.substring(0, start) + marker + value.text.substring(start, end) + marker + value.text.substring(end)
        return TextFieldValue(text, TextRange(start + marker.length, end + marker.length))
    }

    /**
     * [value] with its selection made the text of a link, the cursor where the address goes.
     */
    fun linked(value: TextFieldValue): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val label = value.text.substring(start, end)
        val text = value.text.substring(0, start) + "[" + label + "]()" + value.text.substring(end)
        return TextFieldValue(text, TextRange(start + label.length + 3))
    }

    /**
     * [value] with each line the selection touches starting with [prefix] (`## ` for a heading, `- ` for a list item),
     * or without it if every one of them already does.
     */
    fun withLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
        val text = value.text
        val firstLine = if (value.selection.min == 0) 0 else text.lastIndexOf('\n', value.selection.min - 1) + 1
        val lastLineEnd = text.indexOf('\n', value.selection.max).let { if (it < 0) text.length else it }
        val lines = text.substring(firstLine, lastLineEnd).split('\n')
        val isRemoving = lines.all { it.startsWith(prefix) }
        val changed = lines.joinToString("\n") { if (isRemoving) it.removePrefix(prefix) else prefix + it }
        val shift = changed.length - (lastLineEnd - firstLine)
        return TextFieldValue(text.substring(0, firstLine) + changed + text.substring(lastLineEnd), TextRange((value.selection.max + shift).coerceIn(firstLine, firstLine + changed.length)))
    }
}
