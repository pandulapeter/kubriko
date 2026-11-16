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

import com.pandulapeter.kubrikoCreator.actors.AnimationNaming
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles

/**
 * The statements of the Blocks that change an Actor's size and scale, through functions of its class, so the Blocks stay
 * one line each. The collision mask follows both at the end of the update ([ActorClassCode.appendCollisionMaskUpdate]).
 * The names start with an underscore, which no Property's can, so no Property needs reserving for them; the classes they
 * build are named in full, from [AnimationNaming.TYPES_PACKAGE], since an Actor type may be named after one. A size or a
 * scale that isn't a finite number (a Property divided by zero) is left out, as the camera's positions are.
 */
internal object SizeCode {
    private const val SIZE_FUNCTION = "_setSize"
    private const val SCALE_FUNCTION = "_setScale"
    private const val CHANGE_SCALE_FUNCTION = "_changeScale"
    private const val PIVOT_FRACTION_X = "_pivotFractionX"
    private const val PIVOT_FRACTION_Y = "_pivotFractionY"
    private const val TYPES = AnimationNaming.TYPES_PACKAGE
    private const val RESIZABLE_BOX_MASKS_SINCE = "0.8.1"

    /**
     * Whether a project on [kubrikoVersion] (null when its build doesn't say) has box collision masks whose size changes in
     * place, which Kubriko has had since [RESIZABLE_BOX_MASKS_SINCE]; one that can't be told is taken to, since Creator
     * raises the projects it opens to its own Kubriko.
     */
    fun hasResizableBoxMasks(kubrikoVersion: String?) =
        kubrikoVersion == null || KubrikoBuildFiles.compareVersions(kubrikoVersion, RESIZABLE_BOX_MASKS_SINCE) >= 0

    fun setSize(block: BlockDefinition.SetSize, scope: BlockScope) = BlockCode(
        lines = listOf("$SIZE_FUNCTION(${NumberValueCode.float(block.width, scope)}, ${NumberValueCode.float(block.height, scope)})"),
        needs = setOf(BlockNeed.SIZE),
    )

    fun setScale(block: BlockDefinition.SetScale, scope: BlockScope) = BlockCode(
        lines = listOf("$SCALE_FUNCTION(${NumberValueCode.float(block.scale, scope)})"),
        needs = setOf(BlockNeed.SCALE),
    )

    fun changeScale(block: BlockDefinition.ChangeScale, scope: BlockScope) = BlockCode(
        lines = listOf("$CHANGE_SCALE_FUNCTION(${NumberValueCode.float(block.by, scope)})"),
        needs = setOf(BlockNeed.SCALE),
    )

    /**
     * The function the Set the size Block calls, which keeps the pivot at the same fraction of the size, so the Actor
     * grows around its middle or its sprite's origin; the fraction is remembered from the last size that had one, for an
     * Actor that grows again from nothing.
     */
    fun StringBuilder.appendSizeFunction(visibility: String) {
        appendLine(
            """
            |
            |    private var $PIVOT_FRACTION_X = 0.5f
            |    private var $PIVOT_FRACTION_Y = 0.5f
            |
            |    $visibility fun $SIZE_FUNCTION(width: Float, height: Float) {
            |        if (!width.isFinite() || !height.isFinite()) return
            |        val size = body.size
            |        if (size.width.raw > 0f) $PIVOT_FRACTION_X = body.pivot.x.raw / size.width.raw
            |        if (size.height.raw > 0f) $PIVOT_FRACTION_Y = body.pivot.y.raw / size.height.raw
            |        val newWidth = width.coerceAtLeast(0f)
            |        val newHeight = height.coerceAtLeast(0f)
            |        body.size = $TYPES.SceneSize(newWidth.sceneUnit, newHeight.sceneUnit)
            |        body.pivot = $TYPES.SceneOffset((newWidth * $PIVOT_FRACTION_X).sceneUnit, (newHeight * $PIVOT_FRACTION_Y).sceneUnit)
            |    }
            """.trimMargin(),
        )
    }

    /**
     * The functions the Set the scale and Change the scale Blocks call: setting the scale makes it the same across and
     * down, as Animate does, while changing it adds to each, so an Actor placed wider than tall stays so.
     */
    fun StringBuilder.appendScaleFunctions(visibility: String) {
        appendLine(
            """
            |
            |    $visibility fun $SCALE_FUNCTION(scale: Float) {
            |        if (!scale.isFinite()) return
            |        val kept = scale.coerceAtLeast(0f)
            |        body.scale = $TYPES.Scale(kept, kept)
            |    }
            |
            |    $visibility fun $CHANGE_SCALE_FUNCTION(by: Float) {
            |        if (!by.isFinite()) return
            |        body.scale = $TYPES.Scale((body.scale.horizontal + by).coerceAtLeast(0f), (body.scale.vertical + by).coerceAtLeast(0f))
            |    }
            """.trimMargin(),
        )
    }
}
