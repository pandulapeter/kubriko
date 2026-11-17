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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FontFilesTest {

    private fun resource(path: String) = ProjectResource(ResourceGroup.FONTS, File(path), path)

    @Test
    fun onlyFontsRightInAFontFolderAreFontsAndTheFirstOfAnAccessorWins() {
        val files = FontFiles.of(
            mapOf(
                ResourceGroup.FONTS to listOf(
                    resource("src/commonMain/composeResources/font/pixel.ttf"),
                    resource("src/commonMain/composeResources/font/title-2.otf"),
                    resource("src/commonMain/composeResources/font-ja/pixel.ttf"),
                    resource("src/commonMain/composeResources/font/titles/big.ttf"),
                    resource("src/commonMain/composeResources/files/other.ttf"),
                    resource("src/desktopMain/composeResources/font/pixel.otf"),
                ),
            ),
        )
        assertEquals(listOf("font/pixel.ttf", "font/title-2.otf"), files.paths)
    }

    @Test
    fun accessorsAreNamedAsComposeResourcesNamesThem() {
        assertEquals("title_2", FontFiles.accessorOf("font/title-2.otf"))
        assertEquals("_8bit", FontFiles.accessorOf("font/8bit.ttf"))
        assertNull(FontFiles.accessorOf("drawable/pixel.ttf"))
        assertNull(FontFiles.accessorOf("font/pixel.png"))
        assertNull(FontFiles.accessorOf("font/pixel font.ttf"))
    }
}
