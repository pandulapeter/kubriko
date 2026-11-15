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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.FULL_ZOOM
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.floatLiteral

/**
 * The code of the camera Blocks, through `GeneratedScenes`, which keeps what the camera does ([CameraCode]).
 */
internal object CameraBlockCode {

    val FOLLOW_WITH_CAMERA = BlockCode(listOf("$SCENES_FIELD.${CameraCode.FOLLOW_FUNCTION}(this)"), needs = setOf(BlockNeed.SCENES, BlockNeed.CAMERA))

    fun moveCamera(block: BlockDefinition.MoveCamera, scope: BlockScope) =
        cameraCode(CameraCode.MOVE_FUNCTION, "${NumberValueCode.float(block.x, scope)}, ${NumberValueCode.float(block.y, scope)}", scope)

    fun shakeCamera(block: BlockDefinition.ShakeCamera, scope: BlockScope) =
        cameraCode(CameraCode.SHAKE_FUNCTION, "${NumberValueCode.float(block.strength, scope)}, ${NumberValueCode.float(block.durationInMilliseconds, scope)}", scope)

    /**
     * The zoom is in percent in the Block, and a scale factor in the viewport.
     */
    fun zoomCamera(block: BlockDefinition.ZoomCamera, scope: BlockScope) = cameraCode(
        function = CameraCode.ZOOM_FUNCTION,
        arguments = when (val zoom = block.zoom) {
            is NumberValue.Fixed -> floatLiteral(zoom.value / FULL_ZOOM)
            else -> "${NumberValueCode.float(zoom, scope)} / ${floatLiteral(FULL_ZOOM)}"
        },
        scope = scope,
    )

    /**
     * A call of `GeneratedScenes`' camera [function], which an Actor makes as itself, so that only the Actors of the
     * Scene shown move the camera, and a Manager type as the game.
     */
    private fun cameraCode(function: String, arguments: String, scope: BlockScope) = if (scope.kind == BlockTypeKind.ACTOR) {
        BlockCode(listOf("$SCENES_FIELD.$function($arguments, this)"), needs = setOf(BlockNeed.SCENES, BlockNeed.CAMERA))
    } else {
        BlockCode(listOf("$SCENES_FIELD.$function($arguments)"), needs = setOf(BlockNeed.SCENES))
    }
}
