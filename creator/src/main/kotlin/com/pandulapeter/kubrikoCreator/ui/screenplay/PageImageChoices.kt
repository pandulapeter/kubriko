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

import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * What the page editor's Image menu and pasting offer.
 *
 * @param sprites The sprites the page can show, by their paths from `composeResources`.
 * @param onStart Called as an image is about to be chosen, while the chunk typed into still is, so that the image goes
 *   into its section and what was typed is saved before the image changes the page.
 * @param onChoose Puts the chosen image on the page.
 */
internal class PageImageChoices(
    val sprites: List<String>,
    val onStart: () -> Unit,
    val onChoose: (PageImageSource) -> Unit,
) {
    /**
     * Puts [pasted] on the page.
     *
     * @return Whether the paste was images, so that it isn't pasted as text too.
     */
    fun paste(pasted: PastedImages): Boolean = when (pasted) {
        is PastedImages.Files -> pasted.files.isNotEmpty().also { isImages ->
            if (isImages) {
                onStart()
                pasted.files.forEach(onChoose)
            }
        }
        is PastedImages.Image -> true.also {
            onStart()
            onChoose(PageImageSource.Pasted(pasted))
        }
    }
}

/**
 * Asks the user for an image file, returning null if they cancel.
 */
internal fun pickImageFile(title: String): File? = JFileChooser().run {
    dialogTitle = title
    fileFilter = FileNameExtensionFilter(title, *PageImages.EXTENSIONS.toTypedArray())
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile else null
}
