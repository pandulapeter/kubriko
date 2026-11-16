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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The code of the Set the size, Set the scale, and Change the scale Blocks, and of the functions they call.
 */
class SizeCodeTest {
    private val scope = BlockScope("Balloon", properties = listOf(PropertyDefinition.Number("air")))

    private fun lines(block: BlockDefinition) = BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, scope).lines

    private fun generate(vararg events: EventDefinition) = ActorTypeCodeGenerator.generateActorType(
        ActorTypeDefinition(name = "Balloon", shape = ActorShape.RECTANGLE, properties = listOf(PropertyDefinition.Number("air")), events = events.toList()),
        "com.example.game",
    )

    @Test
    fun eachBlockIsOneCallOfItsFunction() {
        assertEquals(listOf("_setSize(120f, air)"), lines(BlockDefinition.SetSize(NumberValue.Fixed(120f), NumberValue.Property("air"))))
        assertEquals(listOf("_setScale(air)"), lines(BlockDefinition.SetScale(NumberValue.Property("air"))))
        assertEquals(listOf("_setScale(body.scale.horizontal + -0.5f)"), lines(BlockDefinition.ChangeScale(NumberValue.Fixed(-0.5f))))
    }

    @Test
    fun theSizeKeepsThePivotAtTheSameFractionOfItAndLeavesOutWhatIsntANumber() {
        val code = generate(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.SetSize())))
        assertContains(code, "    private fun _setSize(width: Float, height: Float) {\n        if (!width.isFinite() || !height.isFinite()) return\n")
        assertContains(code, "        if (size.width.raw > 0f) _pivotFractionX = body.pivot.x.raw / size.width.raw\n")
        assertContains(code, "        body.pivot = com.pandulapeter.kubriko.types.SceneOffset((newWidth * _pivotFractionX).sceneUnit, (newHeight * _pivotFractionY).sceneUnit)\n")
        assertContains(code, "import com.pandulapeter.kubriko.helpers.extensions.sceneUnit\n")
        assertFalse("_setScale" in code, code)
    }

    @Test
    fun theScaleIsTheSameAcrossAndDownAndNeverNegative() {
        val code = generate(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeScale())))
        assertContains(code, "        if (!scale.isFinite()) return\n        val kept = scale.coerceAtLeast(0f)\n        body.scale = com.pandulapeter.kubriko.types.Scale(kept, kept)\n")
        assertFalse("_setSize" in code, code)
    }

    @Test
    fun aCollidableActorResizedOnlyWhenAddedStillUpdatesItsMask() {
        val code = generate(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.SetScale())),
            EventDefinition(EventType.COLLISION, otherActorType = "Balloon"),
        )
        assertContains(code, "Dynamic")
        assertContains(code, "        collisionMask.rotation = body.rotation\n        collisionMask.size = body.size * body.scale\n")
    }
}
