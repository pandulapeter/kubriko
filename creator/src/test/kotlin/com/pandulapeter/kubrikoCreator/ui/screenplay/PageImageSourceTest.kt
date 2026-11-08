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

import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddPageImage
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageImageSourceTest {
    private val directory = testDirectory()

    private fun clipboardHolding(flavor: DataFlavor, data: Any) = Clipboard("test").apply {
        setContents(
            object : Transferable {
                override fun getTransferDataFlavors() = arrayOf(flavor)

                override fun isDataFlavorSupported(candidate: DataFlavor) = candidate == flavor

                override fun getTransferData(candidate: DataFlavor) = data
            },
            null,
        )
    }

    @Test
    fun `copied image files are pasted, copied text isn't, and a copied image is saved as a PNG first`() {
        val image = File(directory, "Cave map.png").apply { ImageIO.write(BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB), "png", this) }
        val notes = File(directory, "notes.txt").apply { writeText("x") }
        assertEquals(listOf(PageImageSource.ImageFile(image, "Cave map")), imagesOnClipboard(clipboardHolding(DataFlavor.javaFileListFlavor, listOf(image, notes))))
        assertEquals(emptyList(), imagesOnClipboard(Clipboard("test").apply { setContents(StringSelection("text"), null) }))
        val pasted = imagesOnClipboard(clipboardHolding(DataFlavor.imageFlavor, BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB))).single()
        assertEquals("", pasted.description)
        assertEquals(6 to 3, ImageIO.read(pasted.file).let { it.width to it.height })
        assertEquals(emptyList(), imagesOnClipboard(Clipboard("empty")))
    }

    @Test
    fun `an image goes to the page shown as add_page_image, and a document takes none`() {
        val dispatched = mutableListOf<Invocation<*>>()
        assertNull(pageImageAdderOf(WorkspaceSelection.Document("docs/a.md"), dispatched::add))
        val add = pageImageAdderOf(WorkspaceSelection.Page("levels/cave"), dispatched::add)!!
        add(PageImageSource.ImageFile(File("/tmp/bat.png")), "Moves")
        add(PageImageSource.Sprite("drawable/bat.png"), null)
        assertEquals(
            listOf(
                AddPageImage.Input("levels/cave", "bat", file = File("/tmp/bat.png").path, section = "Moves"),
                AddPageImage.Input("levels/cave", "bat", sprite = "drawable/bat.png"),
            ),
            dispatched.map { it.input },
        )
    }
}
