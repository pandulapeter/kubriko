/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * The images a page holds as copies of its own: next to the page, named after it and numbered (`hero-1.png`), and no
 * wider than [MAXIMUM_WIDTH], since pages live in git.
 */
public object PageImages {

    /**
     * The widest a copied image is kept, in pixels; a wider one is scaled down.
     */
    public const val MAXIMUM_WIDTH: Int = 1600

    /**
     * The file extensions of the images a page can show.
     */
    public val EXTENSIONS: Set<String> = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

    /**
     * An image ready to be written: its bytes, and the extension they need.
     */
    public class Encoded(
        public val bytes: ByteArray,
        public val extension: String,
    )

    /**
     * [bytes], an image file whose name ended in [extension], as a page keeps it: unchanged while it is no wider than
     * [MAXIMUM_WIDTH] and of a kind the page can show, otherwise scaled down (a JPEG staying one, anything else becoming
     * a PNG); null if it isn't an image Java can read, or a WebP image, which is kept as it is.
     */
    public fun encoded(bytes: ByteArray, extension: String): Encoded? {
        val lowercase = extension.lowercase()
        val image = runCatching { ImageIO.read(bytes.inputStream()) }.getOrNull() ?: return Encoded(bytes, lowercase).takeIf { lowercase == "webp" && isWebP(bytes) }
        if (image.width <= MAXIMUM_WIDTH && lowercase in EXTENSIONS) return Encoded(bytes, lowercase)
        val isPhoto = lowercase == "jpg" || lowercase == "jpeg"
        return Encoded(scaledDown(image, MAXIMUM_WIDTH, asJpeg = isPhoto), if (isPhoto) "jpg" else "png")
    }

    /**
     * [image] no wider than [maximumWidth], keeping its shape, encoded as a JPEG when [asJpeg] (a photo, which a PNG would
     * make several times larger) and as a PNG otherwise.
     */
    public fun scaledDown(image: BufferedImage, maximumWidth: Int, asJpeg: Boolean = false): ByteArray {
        val scale = (maximumWidth.toFloat() / image.width).coerceAtMost(1f)
        val result = BufferedImage((image.width * scale).toInt().coerceAtLeast(1), (image.height * scale).toInt().coerceAtLeast(1), if (asJpeg) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB)
        result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(image, 0, 0, result.width, result.height, null)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(result, if (asJpeg) "jpg" else "png", it) }.toByteArray()
    }

    /**
     * Whether [bytes] start like a WebP file, which Java can't read, so it is kept as it is, however wide.
     */
    private fun isWebP(bytes: ByteArray) = bytes.size >= 12 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(bytes, 8, 4, Charsets.ISO_8859_1) == "WEBP"

    /**
     * The path from the project's root of the next image of the page [page] with [extension]: in the page's folder,
     * named after the page and the first number that [isTaken] (asked of each path, ignoring case on its own) says
     * is free.
     */
    public fun nextPathOf(page: String, extension: String, isTaken: (String) -> Boolean): String {
        val base = PageNaming.pathOf(page).removeSuffix(".${PageNaming.EXTENSION}")
        return generateSequence(1) { it + 1 }.map { "$base-$it.$extension" }.first { !isTaken(it) }
    }

    /**
     * The paths from the project's root of the page [page]'s own images in the project in [rootDirectory]: the image files
     * next to it named after it and numbered, as [nextPathOf] names them, in the order of their numbers. Reads the disk.
     */
    public fun ownImagesOf(rootDirectory: File, page: String): List<String> {
        val pagePath = PageNaming.pathOf(page)
        val base = page.substringAfterLast('/')
        val folder = File(rootDirectory, pagePath).parentFile ?: return emptyList()
        val pattern = Regex(Regex.escape(base) + "-(\\d+)\\.([A-Za-z]+)")
        return folder.listFiles().orEmpty()
            .mapNotNull { file -> pattern.matchEntire(file.name)?.takeIf { file.isFile && it.groupValues[2].lowercase() in EXTENSIONS }?.let { file to it.groupValues[1].toBigInteger() } }
            .sortedBy { it.second }
            .map { (file, _) -> pagePath.substringBeforeLast('/', "").let { if (it.isEmpty()) file.name else "$it/${file.name}" } }
    }

    /**
     * Where the image at [path], one of the page [page]'s own ([ownImagesOf]), goes when the page is renamed to [newPage]:
     * next to it, named after it, with the same number and extension.
     */
    public fun movedImagePathOf(path: String, page: String, newPage: String): String {
        val suffix = path.substringAfterLast('/').removePrefix(page.substringAfterLast('/'))
        return PageNaming.pathOf(newPage).removeSuffix(".${PageNaming.EXTENSION}") + suffix
    }

    /**
     * The page [page]'s own images ([ownImagesOf]) that none of [otherFiles] shows or links to: the project's other pages
     * and documents, their texts by their paths from the project's root.
     */
    public fun unusedImagesOf(rootDirectory: File, page: String, otherFiles: Map<String, String>): List<String> {
        val used = otherFiles.flatMapTo(HashSet()) { (path, text) -> PageLinks.linksOf(text).mapNotNull { PageLinks.resolve(path, it.destination) } }
        return ownImagesOf(rootDirectory, page).filter { it !in used }
    }

    /**
     * [text] with [line] added as a paragraph of its own: at the end of the section headed [section] (ignoring case),
     * or at the end of the page, before its Planned section if it has one, without [section]; null if there is no
     * such section. The line breaks of [text] are kept.
     */
    public fun withLine(text: String, line: String, section: String? = null): String? {
        val separator = if ("\r\n" in text) "\r\n" else "\n"
        val lines = text.lines().toMutableList()
        if (lines.lastOrNull()?.isEmpty() == true) lines.removeAt(lines.lastIndex)
        val sections = PageText.sectionsOf(text)
        val end = if (section != null) {
            sections.firstOrNull { it.heading.equals(section.trim(), ignoreCase = true) }?.lines?.last?.plus(1) ?: return null
        } else {
            sections.lastOrNull { it.level == 2 && it.heading.equals(PageText.PLANNED, ignoreCase = true) }?.lines?.first ?: lines.size
        }
        var insertAt = end.coerceAtMost(lines.size)
        while (insertAt > 0 && lines[insertAt - 1].isBlank()) insertAt--
        val after = lines.drop(end.coerceAtMost(lines.size)).dropWhile { it.isBlank() }
        val before = lines.take(insertAt)
        val joined = (if (before.isEmpty()) emptyList() else before + "") + line + (if (after.isEmpty()) emptyList() else listOf("") + after)
        return joined.joinToString(separator) + separator
    }
}
