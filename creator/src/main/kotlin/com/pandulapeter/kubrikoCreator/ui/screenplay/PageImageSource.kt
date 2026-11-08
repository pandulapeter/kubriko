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
     * An image copied to the clipboard, read off the main thread when it is put on the page.
     */
    data class Pasted(
        val image: PastedImages.Image,
    ) : PageImageSource

    /**
     * A sprite, shown where it is: its path from `composeResources` (`drawable/ball.png`).
     */
    data class Sprite(
        val path: String,
    ) : PageImageSource
}

/**
 * What pasting [clipboard] (the system's by default) puts on a page, read without blocking: the image files copied in a
 * file manager; a copied image itself, which [PastedImages.Image] reads later off the main thread; or nothing when it holds
 * text, which is pasted as text, since many apps offer the text they copy as an image too.
 */
internal fun pastedImagesOf(clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard): PastedImages = runCatching {
    when {
        clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor) -> PastedImages.Files(
            imageFilesAmong((clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>()).map { PageImageSource.ImageFile(it) },
        )
        clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) -> PastedImages.Files(emptyList())
        clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) -> PastedImages.Image(clipboard)
        else -> PastedImages.Files(emptyList())
    }
}.getOrDefault(PastedImages.Files(emptyList()))

/**
 * What a paste puts on a page.
 */
internal sealed interface PastedImages {

    /**
     * Image files, none when the paste is text.
     */
    data class Files(
        val files: List<PageImageSource.ImageFile>,
    ) : PastedImages

    /**
     * A copied image on [clipboard], which [read] saves to a temporary PNG file first.
     */
    class Image(
        private val clipboard: Clipboard,
    ) : PastedImages {
        /**
         * The image, saved as a PNG, or null if it is gone or can't be read. Blocks, so it runs off the main thread.
         */
        fun read(): PageImageSource.ImageFile? = runCatching {
            (clipboard.getData(DataFlavor.imageFlavor) as? java.awt.Image)?.let(::savedAsPng)?.let { PageImageSource.ImageFile(it, description = "") }
        }.getOrNull()
    }
}

/**
 * The image files among [files], dropped onto a page.
 */
internal fun imageFilesAmong(files: List<File>): List<File> = files.filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }

private fun savedAsPng(image: java.awt.Image): File? {
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
