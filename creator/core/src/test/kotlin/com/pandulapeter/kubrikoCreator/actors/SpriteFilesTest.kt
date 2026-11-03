/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpriteFilesTest {

    private fun resource(path: String) = ProjectResource(ResourceGroup.SPRITES, File(path), path)

    @Test
    fun onlyBitmapsRightInADrawableFolderAreSprites() {
        val files = SpriteFiles.of(
            mapOf(
                ResourceGroup.SPRITES to listOf(
                    resource("src/commonMain/composeResources/drawable/ball.png"),
                    resource("src/commonMain/composeResources/drawable/paddle-2.webp"),
                    resource("src/commonMain/composeResources/drawable/logo.svg"),
                    resource("src/commonMain/composeResources/drawable-xxhdpi/ball.png"),
                    resource("src/commonMain/composeResources/drawable/icons/star.png"),
                    resource("src/commonMain/composeResources/files/brick.png"),
                    resource("src/desktopMain/composeResources/drawable/ball.jpg"),
                ),
            ),
        )
        assertEquals(listOf("drawable/ball.png", "drawable/paddle-2.webp"), files.paths)
        assertEquals(setOf("ball", "paddle_2"), files.filesByAccessor.keys)
        assertEquals(SpriteFiles(), SpriteFiles.of(emptyMap()))
    }

    @Test
    fun accessorsAreNamedAsComposeNamesThem() {
        assertEquals("ball", SpriteFiles.accessorOf("drawable/ball.png"))
        assertEquals("big_ball", SpriteFiles.accessorOf("drawable/big-ball.PNG"))
        assertEquals("_1up", SpriteFiles.accessorOf("drawable/1up.png"))
        assertNull(SpriteFiles.accessorOf("drawable/my ball.png"))
        assertNull(SpriteFiles.accessorOf("drawable/ball"))
        assertNull(SpriteFiles.accessorOf("drawable/ball.xml"))
        assertNull(SpriteFiles.accessorOf("files/ball.png"))
    }

    @Test
    fun theSizeOfAnImageIsReadFromItsFile() {
        val directory = createTempDirectory().toFile()
        val image = File(directory, "ball.png").apply { ImageIO.write(BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB), "png", this) }
        assertEquals(24 to 16, SpriteFiles.sizeOf(image))
        assertNull(SpriteFiles.sizeOf(File(directory, "missing.png")))
        assertNull(SpriteFiles.sizeOf(File(directory, "text.png").apply { writeText("not an image") }))
        directory.deleteRecursively()
    }
}
