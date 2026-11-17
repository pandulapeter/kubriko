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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.TextFont
import org.jetbrains.compose.resources.stringResource

/**
 * What a text is drawn in, as its font menu offers it: a generic family, or a font file of the game.
 */
internal sealed interface FontChoice {

    data class Family(val font: TextFont) : FontChoice

    /**
     * The font file at [path] (from `composeResources`).
     */
    data class File(val path: String) : FontChoice

    companion object {

        /**
         * What a text can be drawn in: the generic families, then the game's [fonts].
         */
        fun choicesOf(fonts: List<String>): List<FontChoice> = TextFont.entries.map(::Family) + fonts.map(::File)

        /**
         * What [text] is drawn in.
         */
        fun of(text: ScreenTextDefinition): FontChoice = text.fontFile?.let(::File) ?: Family(text.font)
    }
}

/**
 * This text drawn in [choice]: a family alone, or a font file, which keeps the family the text had for while the game
 * has no such file.
 */
internal fun ScreenTextDefinition.withFont(choice: FontChoice) = when (choice) {
    is FontChoice.Family -> copy(font = choice.font, fontFile = null)
    is FontChoice.File -> copy(fontFile = choice.path)
}

@Composable
internal fun FontChoice.title(): String = when (this) {
    is FontChoice.Family -> stringResource(font.title)
    is FontChoice.File -> ResourceKind.nameOf(path)
}
