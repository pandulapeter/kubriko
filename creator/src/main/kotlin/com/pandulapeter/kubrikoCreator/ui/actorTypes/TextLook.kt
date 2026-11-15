/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.TextFont
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.text_font_cursive
import kubriko.creator.generated.resources.text_font_default
import kubriko.creator.generated.resources.text_font_monospace
import kubriko.creator.generated.resources.text_font_sans_serif
import kubriko.creator.generated.resources.text_font_serif
import kubriko.creator.generated.resources.text_style_bold
import kubriko.creator.generated.resources.text_style_bold_italic
import kubriko.creator.generated.resources.text_style_italic
import kubriko.creator.generated.resources.text_style_regular
import org.jetbrains.compose.resources.StringResource

/**
 * The letters of a text the editor offers as one choice: bold, italic, both, or neither.
 */
internal enum class TextStyleChoice(
    val isBold: Boolean,
    val isItalic: Boolean,
    val title: StringResource,
) {
    REGULAR(isBold = false, isItalic = false, Res.string.text_style_regular),
    BOLD(isBold = true, isItalic = false, Res.string.text_style_bold),
    ITALIC(isBold = false, isItalic = true, Res.string.text_style_italic),
    BOLD_ITALIC(isBold = true, isItalic = true, Res.string.text_style_bold_italic),
    ;

    companion object {
        fun of(text: ScreenTextDefinition) = entries.first { it.isBold == text.isBold && it.isItalic == text.isItalic }
    }
}

internal val TextFont.title: StringResource
    get() = when (this) {
        TextFont.DEFAULT -> Res.string.text_font_default
        TextFont.SANS_SERIF -> Res.string.text_font_sans_serif
        TextFont.SERIF -> Res.string.text_font_serif
        TextFont.MONOSPACE -> Res.string.text_font_monospace
        TextFont.CURSIVE -> Res.string.text_font_cursive
    }
