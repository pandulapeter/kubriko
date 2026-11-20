/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.actorTypes.SpriteChoices
import com.pandulapeter.kubrikoCreator.ui.components.DragPreviewChip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_file
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SidebarDragPreviewTest {
    private val ship = File("ship.png")
    private val sheet = SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 2, framesPerRow = 2)
    private val sprites = SpriteChoices(SpriteFiles(mapOf("drawable/ship.png" to ship)), mapOf("ship" to sheet))

    @Test
    fun aSpriteOrAnActorTypeLooksAsItIsDrawnAndAnythingElseAsItsIcon() {
        val tinted = ActorTypeDefinition("Ship", sprite = "drawable/ship.png", color = 0xFF112233, tintsSprite = true)
        val ball = ActorTypeDefinition("Ball", shape = ActorShape.RECTANGLE, color = 0xFF445566)
        assertEquals(DraggedLook.Sprite(ship, sprites.sheetOf("drawable/ship.png")), draggedLookOf(dragged("drawable/ship.png", FolderGroup.SPRITES), emptyList(), sprites))
        assertEquals(DraggedLook.Sprite(ship, sprites.sheetOf("drawable/ship.png"), Color(0xFF112233)), draggedLookOf(dragged("Ship", FolderGroup.ACTOR_TYPES), listOf(tinted, ball), sprites))
        assertEquals(DraggedLook.Shape(ActorShape.RECTANGLE, Color(0xFF445566)), draggedLookOf(dragged("Ball", FolderGroup.ACTOR_TYPES), listOf(tinted, ball), sprites))
        assertNull(draggedLookOf(dragged("Ball", FolderGroup.ACTOR_TYPES).copy(isFolder = true), listOf(ball), sprites))
        assertNull(draggedLookOf(dragged("level", FolderGroup.SCENES), listOf(ball), sprites))
        assertNull(draggedLookOf(dragged("drawable/gone.png", FolderGroup.SPRITES), emptyList(), sprites))
        val gone = tinted.copy(name = "Ghost", sprite = "drawable/gone.png", shape = ActorShape.CIRCLE)
        assertEquals(DraggedLook.Shape(ActorShape.CIRCLE, Color(0xFF112233)), draggedLookOf(dragged("Ghost", FolderGroup.ACTOR_TYPES), listOf(gone), sprites))
    }

    @Test
    fun theChipShowsTheSpriteInPlaceOfTheIcon() = uiTest(width = 240, height = 120) {
        val file = File(testDirectory(), "ship.png")
        ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB).apply { for (x in 0 until 4) for (y in 0 until 4) setRGB(x, y, 0xFFCC2244.toInt()) }, "png", file)
        showing {
            DragPreviewChip(
                pointer = { Offset.Zero },
                icon = Res.drawable.ic_file,
                tint = Color.White,
                text = "ship",
                thumbnail = { DraggedLookThumbnail(DraggedLook.Sprite(file, sheet = null)) },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onRoot().captureToImage().toPixelMap()[THUMBNAIL_X, THUMBNAIL_Y] == Color(0xFFCC2244) }
        saveSnapshot("sidebar-drag-preview-sprite")
    }

    private fun dragged(name: String, group: FolderGroup) = SidebarDragState.Dragged(name, group, folder = "")

    private companion object {
        const val THUMBNAIL_X = 40
        const val THUMBNAIL_Y = 35
    }
}
