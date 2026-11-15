/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.FULL_ZOOM
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.floatLiteral

/**
 * The code of the camera Blocks, through `GeneratedScenes`, which keeps what the camera does ([CameraCode]).
 */
internal object CameraBlockCode {

    val FOLLOW_WITH_CAMERA = BlockCode(listOf("$SCENES_FIELD.${CameraCode.FOLLOW_FUNCTION}(this)"), needs = setOf(BlockNeed.SCENES))

    fun moveCamera(block: BlockDefinition.MoveCamera, scope: BlockScope) = BlockCode(
        lines = listOf("$SCENES_FIELD.${CameraCode.MOVE_FUNCTION}(${NumberValueCode.float(block.x, scope)}, ${NumberValueCode.float(block.y, scope)})"),
        needs = setOf(BlockNeed.SCENES),
    )

    fun shakeCamera(block: BlockDefinition.ShakeCamera, scope: BlockScope) = BlockCode(
        lines = listOf("$SCENES_FIELD.${CameraCode.SHAKE_FUNCTION}(${NumberValueCode.float(block.strength, scope)}, ${NumberValueCode.float(block.durationInMilliseconds, scope)})"),
        needs = setOf(BlockNeed.SCENES),
    )

    /**
     * The zoom is in percent in the Block, and a scale factor in the viewport.
     */
    fun zoomCamera(block: BlockDefinition.ZoomCamera, scope: BlockScope) = BlockCode(
        lines = listOf(
            "$SCENES_FIELD.${CameraCode.ZOOM_FUNCTION}(" + when (val zoom = block.zoom) {
                is NumberValue.Fixed -> floatLiteral(zoom.value / FULL_ZOOM)
                else -> "${NumberValueCode.float(zoom, scope)} / ${floatLiteral(FULL_ZOOM)}"
            } + ")",
        ),
        needs = setOf(BlockNeed.SCENES),
    )
}
