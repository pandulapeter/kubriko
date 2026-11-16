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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Which collision mask a round Actor gets: a circle while it is sure to stay round, an oval otherwise.
 */
class CollisionMaskCodeTest {
    private val collision = EventDefinition(EventType.COLLISION, otherActorType = "Ball")

    private fun generate(width: Float = 40f, height: Float = 40f, hasResizableMasks: Boolean = true, vararg blocks: BlockDefinition): String {
        val definition = ActorTypeDefinition(
            name = "Ball",
            shape = ActorShape.CIRCLE,
            width = width,
            height = height,
            events = listOf(collision, EventDefinition(EventType.EVERY_FRAME, blocks = blocks.toList())),
        )
        return ActorTypeCodeGenerator.generateActorType(
            definition = definition,
            packageName = "com.example.game",
            families = GeneratedFamilies(listOf(definition), emptyList(), emptySet(), ActorTypeFamily(listOf(definition)), hasResizableMasks = hasResizableMasks),
        )
    }

    @Test
    fun aRoundTypeChoosesACircleUnlessAnActorIsPlacedStretched() {
        val code = generate(blocks = arrayOf(BlockDefinition.SetScale()))
        assertContains(code, "    override val collisionMask: CollisionMask = if (body.size.width * body.scale.horizontal == body.size.height * body.scale.vertical) {\n        CircleCollisionMask(\n")
        assertContains(code, "    } else {\n        com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask(\n            initialPosition = bodyCenter,\n            initialSize = body.size * body.scale,\n            initialRotation = body.rotation,\n        )\n    }\n")
        assertContains(code, "            is CircleCollisionMask -> mask.radius = body.size.width * body.scale.horizontal / 2\n")
        assertContains(code, "            is com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask -> {\n                mask.rotation = body.rotation\n                mask.size = body.size * body.scale\n            }\n")
        assertContains(code, "import com.pandulapeter.kubriko.collision.mask.CollisionMask\n")
    }

    @Test
    fun aTypeThatIsntSquareOrThatTheSizeBlockOrKotlinResizesIsAnOvalFromTheStart() {
        listOf(
            generate(width = 60f, height = 30f),
            generate(blocks = arrayOf(BlockDefinition.SetSize())),
            generate(blocks = arrayOf(BlockDefinition.Kotlin("body.size = body.size * 2f"))),
        ).forEach { code ->
            assertContains(code, "    override val collisionMask = com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask(\n")
            assertFalse("CircleCollisionMask" in code, code)
        }
        assertFalse("mask.size" in generate(width = 60f, height = 30f), "An oval that never changes size isn't resized")
    }

    @Test
    fun anOlderKubrikoKeepsEveryRoundActorACircleUnlessTheSizeBlockNeedsAnOval() {
        val older = generate(width = 60f, height = 30f, hasResizableMasks = false)
        assertContains(older, "    override val collisionMask = CircleCollisionMask(\n")
        assertFalse("EllipseCollisionMask" in older, older)
        assertContains(generate(hasResizableMasks = false, blocks = arrayOf(BlockDefinition.SetSize())), "    override val collisionMask = com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask(\n")
    }

    @Test
    fun onlyTheFamilyMembersThatTheSizeBlockStretchesAreOvalsFromTheStart() {
        val parent = ActorTypeDefinition(name = "Ball", shape = ActorShape.CIRCLE, events = listOf(collision))
        val balloon = ActorTypeDefinition(name = "Balloon", parent = "Ball", shape = ActorShape.CIRCLE, events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetSize()))))
        val marble = ActorTypeDefinition(name = "Marble", parent = "Ball", shape = ActorShape.CIRCLE)
        val types = listOf(parent, balloon, marble)
        val families = GeneratedFamilies(types, emptyList(), emptySet(), ActorTypeFamily(types))
        fun code(definition: ActorTypeDefinition) = ActorTypeCodeGenerator.generateActorType(definition, "com.example.game", types.map { it.name }, actorTypes = types, families = families)
        assertContains(code(balloon), "    collisionMask = com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask(initialPosition")
        assertContains(code(marble), "    collisionMask = if (state.body.size.width * state.body.scale.horizontal == state.body.size.height * state.body.scale.vertical) CircleCollisionMask(")
        assertFalse("import com.pandulapeter.kubriko.collision.mask.CollisionMask\n" in code(marble), "A final class names no CollisionMask")
    }

    @Test
    fun aFamilysFinalClassPicksItsOwnMaskAsItIsCreated() {
        val parent = ActorTypeDefinition(name = "Ball", shape = ActorShape.CIRCLE, events = listOf(collision))
        val child = ActorTypeDefinition(name = "Egg", parent = "Ball", shape = ActorShape.CIRCLE, width = 30f, height = 50f)
        val types = listOf(parent, child)
        val families = GeneratedFamilies(types, emptyList(), emptySet(), ActorTypeFamily(types))
        val parentCode = ActorTypeCodeGenerator.generateActorType(parent, "com.example.game", types.map { it.name }, actorTypes = types, families = families)
        val childCode = ActorTypeCodeGenerator.generateActorType(child, "com.example.game", types.map { it.name }, actorTypes = types, families = families)
        assertContains(parentCode, "    final override val collisionMask: CollisionMask,\n")
        assertContains(parentCode, "    collisionMask = if (state.body.size.width * state.body.scale.horizontal == state.body.size.height * state.body.scale.vertical) CircleCollisionMask(")
        assertContains(childCode, "    collisionMask = com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask(initialPosition = state.body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }, ")
    }

    @Test
    fun theMaskKindsFollowTheShapeTheSquarenessAndTheBlocks() {
        assertEquals(setOf(MaskKind.BOX), CollisionMaskCode.kindsOf(MaskBody(ActorShape.RECTANGLE, isSquare = false, isResizedBySizeBlock = true), hasOvalMasks = true))
        assertEquals(setOf(MaskKind.CIRCLE), CollisionMaskCode.kindsOf(MaskBody(ActorShape.CIRCLE, isSquare = false, isResizedByKotlin = true), hasOvalMasks = false))
        assertEquals(setOf(MaskKind.ELLIPSE), CollisionMaskCode.kindsOf(MaskBody(ActorShape.CIRCLE, isSquare = true, isResizedBySizeBlock = true), hasOvalMasks = false))
        assertEquals(setOf(MaskKind.CIRCLE, MaskKind.ELLIPSE), CollisionMaskCode.kindsOf(MaskBody(ActorShape.CIRCLE, isSquare = true), hasOvalMasks = true))
        assertEquals(setOf(MaskKind.ELLIPSE), CollisionMaskCode.kindsOf(MaskBody(ActorShape.CIRCLE, isSquare = true, isResizedByKotlin = true), hasOvalMasks = true))
    }

    @Test
    fun anOlderKubrikoKeepsARoundActorWithScaleBlocksACircle() {
        val older = generate(hasResizableMasks = false, blocks = arrayOf(BlockDefinition.SetScale(), BlockDefinition.ChangeScale()))
        assertContains(older, "    override val collisionMask = CircleCollisionMask(\n")
        assertFalse("EllipseCollisionMask" in older, older)
    }
}
