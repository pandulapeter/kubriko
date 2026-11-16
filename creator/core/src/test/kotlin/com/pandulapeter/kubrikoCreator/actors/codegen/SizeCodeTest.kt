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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
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
import kotlin.test.assertTrue

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
        assertEquals(listOf("_changeScale(-0.5f)"), lines(BlockDefinition.ChangeScale(NumberValue.Fixed(-0.5f))))
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
    fun aSetScaleIsTheSameAcrossAndDownAndAChangeKeepsTheProportionsAndNeitherIsNegative() {
        val code = generate(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeScale())))
        assertContains(code, "        if (!scale.isFinite()) return\n        val kept = scale.coerceAtLeast(0f)\n        body.scale = com.pandulapeter.kubriko.types.Scale(kept, kept)\n")
        assertContains(
            code,
            "        body.scale = com.pandulapeter.kubriko.types.Scale((body.scale.horizontal + by).coerceAtLeast(0f), (body.scale.vertical + by).coerceAtLeast(0f))\n",
        )
        assertFalse("_setSize" in code, code)
    }

    @Test
    fun aCollidableActorWithAKotlinBlockFollowsWhateverSizeItGives() {
        val code = generate(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Kotlin("body.size = body.size * 2f"))),
            EventDefinition(EventType.COLLISION, otherActorType = "Balloon"),
        )
        assertContains(code, "Dynamic")
        assertContains(code, "        collisionMask.size = body.size * body.scale\n")
    }

    @Test
    fun anActorWhoseSizeNeverChangesLeavesItsBoxMaskAsItIs() {
        val code = generate(EventDefinition(EventType.COLLISION, otherActorType = "Balloon", blocks = listOf(BlockDefinition.Stop)))
        assertContains(code, "        collisionMask.rotation = body.rotation\n")
        assertFalse("collisionMask.size" in code, code)
    }

    @Test
    fun anOlderKubrikoFollowsTheBoxSizeOnlyForTheSizeAndScaleBlocks() {
        assertFalse(SizeCode.hasResizableMasks("0.8.0"))
        assertTrue(SizeCode.hasResizableMasks("0.8.1-creator"))
        assertTrue(SizeCode.hasResizableMasks("0.9.0"))
        assertTrue(SizeCode.hasResizableMasks(null))
        val collision = EventDefinition(EventType.COLLISION, otherActorType = "Balloon")
        fun olderGenerate(vararg blocks: BlockDefinition): String {
            val definition = ActorTypeDefinition(name = "Balloon", shape = ActorShape.RECTANGLE, events = listOf(collision, EventDefinition(EventType.EVERY_FRAME, blocks = blocks.toList())))
            return ActorTypeCodeGenerator.generateActorType(
                definition = definition,
                packageName = "com.example.game",
                families = GeneratedFamilies(listOf(definition), emptyList(), emptySet(), ActorTypeFamily(listOf(definition)), hasResizableMasks = false),
            )
        }
        assertFalse("collisionMask.size" in olderGenerate(BlockDefinition.Animate()), "An Animate Block of the scale doesn't need it")
        assertContains(olderGenerate(BlockDefinition.SetScale()), "        collisionMask.size = body.size * body.scale\n")
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
