/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PageImageReadingTest {

    @Test
    fun `read_pages lists the images a page shows without sending them, and view_page_image shows one as a PNG`() = runTest {
        val context = commandContext()
        val root = context.project.rootDirectory
        File(root, "screenplay").mkdirs()
        File(root, "screenplay/hero.md").writeText("![A](hero-1.png) [not an image](hero-2.png) ![B](../art/wide.jpg) ![A again](hero-1.png)")
        File(root, "art").mkdirs()
        ImageIO.write(BufferedImage(10, 5, BufferedImage.TYPE_INT_ARGB), "png", File(root, "screenplay/hero-1.png"))
        ImageIO.write(BufferedImage(2000, 100, BufferedImage.TYPE_INT_RGB), "jpg", File(root, "art/wide.jpg"))
        context.screenplay.refresh()
        val page = assertIs<CommandOutcome.Success>(ReadPages.read(context, ReadPages.Input(listOf("hero")))).result.jsonObject.getValue("hero").jsonObject
        assertEquals(listOf("screenplay/hero-1.png", "art/wide.jpg"), page.getValue("images").jsonArray.map { it.jsonPrimitive.content })
        val small = assertIs<CommandOutcome.Success>(ViewPageImage.read(context, ViewPageImage.Input("screenplay/hero-1.png")))
        assertContentEquals(File(root, "screenplay/hero-1.png").readBytes(), small.image)
        val wide = ImageIO.read(assertIs<CommandOutcome.Success>(ViewPageImage.read(context, ViewPageImage.Input("art/wide.jpg"))).image!!.inputStream())
        assertEquals(1280 to 64, wide.width to wide.height)
    }

    @Test
    fun `only images inside the project are shown`() = runTest {
        val context = commandContext()
        File(context.project.rootDirectory, "notes.txt").writeText("x")
        listOf("../outside.png", "notes.txt", "missing.png", "/etc/passwd").forEach { path ->
            assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, path)), ViewPageImage.read(context, ViewPageImage.Input(path)), path)
        }
        File(context.project.rootDirectory, "a.webp").writeBytes("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_IMAGE_NOT_VIEWABLE, "a.webp")), ViewPageImage.read(context, ViewPageImage.Input("a.webp")))
    }
}
