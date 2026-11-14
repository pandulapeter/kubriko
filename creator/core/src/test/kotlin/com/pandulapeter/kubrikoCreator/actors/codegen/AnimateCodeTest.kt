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
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The code of the Animate Block, of what plays its animations, and of a box collision mask that follows the size.
 */
class AnimateCodeTest {

    private val scope = BlockScope("Flash", properties = listOf(PropertyDefinition.Number("grow")))

    private fun lines(block: BlockDefinition) = BlockCodeGenerator.blockCode(block, EventType.ADDED_TO_SCENE, scope).lines

    private fun generate(vararg blocks: BlockDefinition, shape: ActorShape = ActorShape.CIRCLE, isCollidable: Boolean = false) = ActorTypeCodeGenerator.generateActorType(
        ActorTypeDefinition(
            name = "Flash",
            shape = shape,
            properties = listOf(PropertyDefinition.Number("grow")),
            events = listOf(
                EventDefinition(EventType.ADDED_TO_SCENE, blocks = blocks.toList()),
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("grow"))),
            ),
        ),
        "com.example.game",
        collidableActorTypeNames = if (isCollidable) setOf("Flash") else emptySet(),
    )

    @Test
    fun eachValueStartsThroughItsOwnFunctionReadingItsNumbersAsArguments() {
        assertEquals(listOf("startScaleTween(from = 1f, to = 2f, seconds = 0.5f, easing = 2, removesActor = false)"), lines(BlockDefinition.Animate()))
        assertEquals(
            listOf("startScaleTween(from = grow, to = (KubrikoWiring.random.nextFloat() * 2f + 1f), seconds = 0f, easing = 0, removesActor = true)"),
            lines(BlockDefinition.Animate(AnimatedValue.Scale(NumberValue.Property("grow"), NumberValue.Random(1f, 3f)), NumberValue.Fixed(0f), AnimationEasing.LINEAR, removesActor = true)),
        )
        assertEquals(
            listOf("startOpacityTween(from = 100f / 100f, to = 0f / 100f, seconds = 1f, easing = 1, removesActor = false)"),
            lines(BlockDefinition.Animate(AnimatedValue.Opacity(), NumberValue.Fixed(1f), AnimationEasing.EASE_IN)),
        )
        assertEquals(
            listOf("startColorTween(from = Color(0xFFFFFFFF), to = Color(0xFF102030), seconds = 0.5f, easing = 3, removesActor = false)"),
            lines(BlockDefinition.Animate(AnimatedValue.Color(to = 0xFF102030), easing = AnimationEasing.EASE_IN_OUT)),
        )
    }

    @Test
    fun aPositionGoesByAnOffsetOrToAPlaceAndAnEdgeIsTheScenesEitherWay() {
        assertEquals(
            listOf("startPositionTween(to = body.position + SceneOffset(0f.sceneUnit, -100f.sceneUnit), seconds = 0.5f, easing = 2, removesActor = false)"),
            lines(BlockDefinition.Animate(AnimatedValue.Position())),
        )
        assertEquals(
            listOf("startPositionTween(to = SceneOffset(generatedScenes.edge(GeneratedScenes.LEFT).sceneUnit, body.position.y + 5f.sceneUnit), seconds = 0.5f, easing = 2, removesActor = false)"),
            lines(BlockDefinition.Animate(AnimatedValue.Position(NumberValue.Edge(SceneEdge.LEFT), NumberValue.Fixed(5f)))),
        )
        assertEquals(
            listOf("startPositionTween(to = SceneOffset(10f.sceneUnit, grow.sceneUnit), seconds = 0.5f, easing = 2, removesActor = false)"),
            lines(BlockDefinition.Animate(AnimatedValue.Position(NumberValue.Fixed(10f), NumberValue.Property("grow"), isRelative = false))),
        )
    }

    @Test
    fun onlyTheAnimatedValuesGetMembersAndTheyAdvanceBeforeTheEvents() {
        assertFalse("Tween" in generate(BlockDefinition.SetOpacity()))
        val code = generate(BlockDefinition.Animate(), BlockDefinition.Animate(AnimatedValue.Opacity(), removesActor = true))
        assertTrue("colorTween" !in code && "positionTween" !in code, code)
        assertContains(code, "    private var scaleTweenFrom: Float = 0f\n    private var scaleTweenTo: Float = 0f\n    private var scaleTweenElapsed = 0\n    private var scaleTweenDuration = 0\n")
        assertContains(code, "    private var opacityTweenRemovesActor = false\n")
        assertContains(
            code,
            "        if (isRemoved) return\n" +
                "        if (scaleTweenDuration > 0) {\n" +
                "            scaleTweenElapsed = (scaleTweenElapsed + deltaTimeInMilliseconds).coerceAtMost(scaleTweenDuration)\n" +
                "            val scale = scaleTweenFrom + (scaleTweenTo - scaleTweenFrom) * tweenProgress(scaleTweenElapsed, scaleTweenDuration, scaleTweenEasing)\n" +
                "            body.scale = com.pandulapeter.kubriko.types.Scale(scale, scale)\n" +
                "            if (scaleTweenElapsed == scaleTweenDuration) {\n" +
                "                scaleTweenDuration = 0\n" +
                "                if (scaleTweenRemovesActor) {\n" +
                "                    isRemoved = true\n" +
                "                    actorManager.remove(this)\n" +
                "                    return\n" +
                "                }\n" +
                "            }\n" +
                "        }\n",
        )
        assertTrue(code.indexOf("if (opacityTweenDuration > 0)") < code.indexOf("onEveryFrame(deltaTimeInMilliseconds)"), code)
        assertContains(code, "            opacity = opacityTweenFrom + (opacityTweenTo - opacityTweenFrom) * tweenProgress(opacityTweenElapsed, opacityTweenDuration, opacityTweenEasing)\n")
        assertFalse("import com.pandulapeter.kubriko.types.Scale\n" in code, code)
    }

    @Test
    fun startingSetsTheStartAtOnceAndTakesAtLeastAMillisecond() {
        val code = generate(BlockDefinition.Animate(), BlockDefinition.Animate(AnimatedValue.Opacity()), BlockDefinition.Animate(AnimatedValue.Color()))
        assertContains(
            code,
            "    private fun startScaleTween(from: Float, to: Float, seconds: Float, easing: Int, removesActor: Boolean) {\n" +
                "        scaleTweenFrom = from\n" +
                "        scaleTweenTo = to\n" +
                "        scaleTweenElapsed = 0\n" +
                "        scaleTweenDuration = (seconds * 1000f + 0.5f).toInt().coerceIn(1, Int.MAX_VALUE / 2)\n" +
                "        scaleTweenEasing = easing\n" +
                "        scaleTweenRemovesActor = removesActor\n" +
                "        body.scale = com.pandulapeter.kubriko.types.Scale(scaleTweenFrom, scaleTweenFrom)\n" +
                "    }\n",
        )
        assertContains(code, "        opacityTweenFrom = from.coerceIn(0f, 1f)\n        opacityTweenTo = to.coerceIn(0f, 1f)\n")
        assertContains(code, "        opacity = opacityTweenFrom\n    }\n")
        assertContains(code, "        color = colorTweenFrom.copy(alpha = color.alpha)\n    }\n")
        assertContains(code, "                alpha = color.alpha,\n")
        assertEquals(1, Regex("fun tweenProgress").findAll(code).count())
    }

    @Test
    fun aPositionMovesByWhatItsEasingAddedSinceTheFrameBeforeSoItAddsUpWithTheVelocity() {
        val code = generate(BlockDefinition.Animate(AnimatedValue.Position()))
        assertContains(
            code,
            "            val progressBefore = tweenProgress(positionTweenElapsed, positionTweenDuration, positionTweenEasing)\n" +
                "            positionTweenElapsed = (positionTweenElapsed + deltaTimeInMilliseconds).coerceAtMost(positionTweenDuration)\n" +
                "            body.position += (positionTweenTo - positionTweenFrom) * (tweenProgress(positionTweenElapsed, positionTweenDuration, positionTweenEasing) - progressBefore)\n",
        )
        assertContains(code, "        positionTweenFrom = body.position\n        positionTweenTo = to\n")
        assertContains(code, "import com.pandulapeter.kubriko.types.SceneOffset\n")
    }

    @Test
    fun aDynamicBoxCollisionMaskIsBuiltAgainOnlyWhenTheSizeOrScaleChanged() {
        val code = generate(BlockDefinition.Animate(), shape = ActorShape.RECTANGLE, isCollidable = true)
        assertContains(code, "    override var collisionMask = BoxCollisionMask(\n")
        assertContains(code, "    private var collisionMaskSize = body.size * body.scale\n")
        assertContains(
            code,
            "        collisionMask.position = bodyCenter\n" +
                "        if (collisionMaskSize != body.size * body.scale) {\n" +
                "            collisionMaskSize = body.size * body.scale\n" +
                "            collisionMask = BoxCollisionMask(initialPosition = bodyCenter, initialSize = collisionMaskSize, initialRotation = body.rotation)\n" +
                "        } else {\n" +
                "            collisionMask.rotation = body.rotation\n" +
                "        }\n",
        )
        val circle = generate(BlockDefinition.Animate(), isCollidable = true)
        assertFalse("collisionMaskSize" in circle, circle)
        assertContains(circle, "    override val collisionMask = CircleCollisionMask(\n")
    }

    @Test
    fun aBoxThatNeverUpdatesKeepsItsMask() {
        val code = ActorTypeCodeGenerator.generateActorType(
            ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE),
            "com.example.game",
            collidableActorTypeNames = setOf("Wall"),
        )
        assertContains(code, "    override val collisionMask = BoxCollisionMask(\n")
        assertFalse("collisionMaskSize" in code, code)
    }
}
