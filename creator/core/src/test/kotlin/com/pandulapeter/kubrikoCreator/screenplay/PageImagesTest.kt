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

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PageImagesTest {
    private fun png(width: Int, height: Int) = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", it) }.toByteArray()

    @Test
    fun `an image no wider than the limit is kept byte for byte`() {
        val bytes = png(40, 20)
        val encoded = assertNotNull(PageImages.encoded(bytes, "PNG"))
        assertContentEquals(bytes, encoded.bytes)
        assertEquals("png", encoded.extension)
    }

    @Test
    fun `a wider image is scaled down to the limit, keeping its shape`() {
        val encoded = assertNotNull(PageImages.encoded(png(3200, 800), "png"))
        val image = ImageIO.read(encoded.bytes.inputStream())
        assertEquals(PageImages.MAXIMUM_WIDTH to 400, image.width to image.height)
    }

    @Test
    fun `an image of a kind a page can't show becomes a PNG, and what isn't an image is refused`() {
        assertEquals("png", PageImages.encoded(png(10, 10), "tiff")?.extension)
        assertNull(PageImages.encoded("not an image".toByteArray(), "png"))
        val webP = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)
        assertEquals("webp", PageImages.encoded(webP, "webp")?.extension)
        assertNull(PageImages.encoded(webP, "png"))
    }

    @Test
    fun `the next image is named after the page with the first free number`() {
        val taken = setOf("screenplay/levels/cave-1.png", "screenplay/levels/cave-2.png")
        assertEquals("screenplay/levels/cave-3.png", PageImages.nextPathOf("levels/cave", "png") { it in taken })
        assertEquals("screenplay/README-1.jpg", PageImages.nextPathOf("README", "jpg") { false })
    }

    @Test
    fun `a line goes at the end of the page, before Planned, or at the end of a section`() {
        val page = "# Hero\r\n\r\nA knight.\r\n\r\n## Moves\r\n\r\nJumps.\r\n\r\n## Planned\r\n\r\n- Wall jump\r\n"
        assertEquals(
            "# Hero\r\n\r\nA knight.\r\n\r\n## Moves\r\n\r\nJumps.\r\n\r\n![](hero-1.png)\r\n\r\n## Planned\r\n\r\n- Wall jump\r\n",
            PageImages.withLine(page, "![](hero-1.png)"),
        )
        assertEquals("# Hero\n\nA knight.\n\n![](a.png)\n", PageImages.withLine("# Hero\n\nA knight.", "![](a.png)"))
        assertEquals("![](a.png)\n", PageImages.withLine("", "![](a.png)"))
        assertEquals("## Moves\n\nJumps.\n\n![](a.png)\n\n## Other\n", PageImages.withLine("## Moves\n\nJumps.\n## Other\n", "![](a.png)", section = "moves"))
        assertNull(PageImages.withLine(page, "x", section = "Enemies"))
    }
}
