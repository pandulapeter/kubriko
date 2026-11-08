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
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Where an image put on a page in the page editor comes from.
 */
internal sealed interface PageImageSource {

    /**
     * An image file, copied next to the page, described as [description] (its name, or nothing for a pasted image).
     */
    data class ImageFile(
        val file: File,
        val description: String = file.nameWithoutExtension,
    ) : PageImageSource

    /**
     * A sprite, shown where it is: its path from `composeResources` (`drawable/ball.png`).
     */
    data class Sprite(
        val path: String,
    ) : PageImageSource
}

/**
 * The images on [clipboard] (the system's by default): the image files copied in a file manager, or a copied image itself,
 * saved to a temporary PNG file first; none when it holds text, which is pasted as text.
 */
internal fun imagesOnClipboard(clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard): List<PageImageSource.ImageFile> = runCatching {
    when {
        clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor) -> imageFilesAmong((clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>())
            .map { PageImageSource.ImageFile(it) }
        clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) -> emptyList()
        clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) -> listOfNotNull((clipboard.getData(DataFlavor.imageFlavor) as? Image)?.let(::savedAsPng)?.let { PageImageSource.ImageFile(it, description = "") })
        else -> emptyList()
    }
}.getOrDefault(emptyList())

/**
 * The image files among [files], dropped onto a page.
 */
internal fun imageFilesAmong(files: List<File>): List<File> = files.filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }

private fun savedAsPng(image: Image): File? {
    val width = image.getWidth(null)
    val height = image.getHeight(null)
    if (width <= 0 || height <= 0) return null
    val buffered = image as? BufferedImage ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { copy ->
        copy.createGraphics().apply {
            drawImage(image, 0, 0, null)
            dispose()
        }
    }
    return File.createTempFile("kubriko-creator-pasted-", ".png").apply {
        deleteOnExit()
        ImageIO.write(buffered, "png", this)
    }
}
