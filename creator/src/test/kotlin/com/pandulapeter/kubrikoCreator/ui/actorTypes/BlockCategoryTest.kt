/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockCategoryTest {

    @Test
    fun everyBlockIsInExactlyOneCategory() {
        val kinds = BlockCategory.entries.flatMap { it.kinds }
        assertEquals(kinds.distinct(), kinds)
        assertEquals(BlockKind.entries.toSet(), kinds.toSet())
    }

    @Test
    fun aManagerTypesCameraTabOffersEveryCameraBlockButFollowing() {
        assertEquals(true, BlockCategory.CAMERA in BlockCategory.availableIn(EventType.GAME_STARTED, BlockTypeKind.MANAGER))
        assertEquals(
            listOf(BlockKind.MOVE_CAMERA, BlockKind.SHAKE_CAMERA, BlockKind.ZOOM_CAMERA),
            BlockCategory.CAMERA.kinds.filter { it.isAvailableIn(EventType.GAME_STARTED, BlockTypeKind.MANAGER) },
        )
        assertEquals(BlockCategory.CAMERA.kinds, BlockCategory.CAMERA.kinds.filter { it.isAvailableIn(EventType.EVERY_FRAME, BlockTypeKind.ACTOR) })
    }

    @Test
    fun theSizeAndScaleBlocksAreAnActorsOnly() {
        val sizeKinds = listOf(BlockKind.SET_SIZE, BlockKind.SET_SCALE, BlockKind.CHANGE_SCALE)
        assertEquals(sizeKinds, BlockCategory.ACTORS.kinds.filter { it in sizeKinds })
        assertEquals(sizeKinds, sizeKinds.filter { it.isAvailableIn(EventType.EVERY_FRAME, BlockTypeKind.ACTOR) })
        assertEquals(emptyList(), sizeKinds.filter { it.isAvailableIn(EventType.GAME_STARTED, BlockTypeKind.MANAGER) })
    }
}
