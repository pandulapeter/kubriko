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
 * How [MarkdownText] draws and follows the links of a text that knows what they point at, such as a page's mentions.
 */
public interface MarkdownLinks {

    /**
     * The chip a link to [destination] is drawn as, or null for an ordinary link.
     */
    public fun chipOf(destination: String): MarkdownChip?

    /**
     * Follows a click on an ordinary link to [destination], instead of opening it in the browser right away.
     */
    public fun onLinkClick(destination: String)
}

/**
 * A link drawn as a chip in [accent], showing its label.
 *
 * @param isMissing Whether what it points at is gone, which the chip shows struck through.
 * @param onClick What a click on it does, or null if it can't be clicked.
 */
public data class MarkdownChip(
    val accent: Accent,
    val isMissing: Boolean = false,
    val onClick: (() -> Unit)? = null,
)
