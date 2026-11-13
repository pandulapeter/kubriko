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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberValueCodeTest {
    private val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Text("title")))
    private val scope = BlockScope(
        typeName = "Ball",
        properties = listOf(PropertyDefinition.Number("speed")),
        managerTypes = listOf(game),
    )

    @Test
    fun aFixedNumberIsALiteral() {
        assertEquals("1.5f", NumberValueCode.float(NumberValue.Fixed(1.5f), scope))
        assertEquals("-2f", NumberValueCode.float(NumberValue.Fixed(-2f), scope))
        assertEquals("3", NumberValueCode.whole(NumberValue.Fixed(3.7f), scope))
        assertEquals("0", NumberValueCode.whole(NumberValue.Fixed(-3f), scope))
    }

    @Test
    fun aRandomNumberIsDrawnFromTheWiringsRandomBetweenItsTwoInEitherOrder() {
        assertEquals("(KubrikoWiring.random.nextFloat() * 4f + 1f)", NumberValueCode.float(NumberValue.Random(1f, 5f), scope))
        assertEquals("(KubrikoWiring.random.nextFloat() * 100f - 50f)", NumberValueCode.float(NumberValue.Random(50f, -50f), scope))
        assertEquals("KubrikoWiring.random.nextInt(1, 6).toFloat()", NumberValueCode.float(NumberValue.Random(1f, 5f, isWhole = true), scope))
        assertEquals("KubrikoWiring.random.nextInt(2, 5)", NumberValueCode.whole(NumberValue.Random(1.5f, 4.2f), scope))
        assertEquals("KubrikoWiring.random.nextInt(0, 3)", NumberValueCode.whole(NumberValue.Random(-4f, 2f), scope))
    }

    @Test
    fun aRandomNumberWithNothingToChooseFromIsTheOneNumberItCanBe() {
        assertEquals("2f", NumberValueCode.float(NumberValue.Random(2f, 2f), scope))
        assertEquals("2f", NumberValueCode.float(NumberValue.Random(1.6f, 2.4f, isWhole = true), scope))
        assertEquals("1f", NumberValueCode.float(NumberValue.Random(1.2f, 1.4f, isWhole = true), scope))
        assertEquals("3", NumberValueCode.whole(NumberValue.Random(3f, 3f), scope))
        assertEquals("0", NumberValueCode.whole(NumberValue.Random(-5f, -1f), scope))
        assertEquals("3", NumberValueCode.whole(NumberValue.Random(2.4f, 3f), scope))
        assertEquals("KubrikoWiring.random.nextInt(0, 2147483647)", NumberValueCode.whole(NumberValue.Random(0f, 3e9f), scope))
        assertEquals("2147483646", NumberValueCode.whole(NumberValue.Random(3e9f, 4e9f), scope))
        assertEquals("KubrikoWiring.random.nextInt(-2147483648, 2147483647).toFloat()", NumberValueCode.float(NumberValue.Random(-3e9f, 3e9f, isWhole = true), scope))
        assertEquals("3f", NumberValueCode.float(NumberValue.Random(3f, 2.4f, isWhole = true), scope))
    }

    @Test
    fun aPropertyIsReadAsItIsWhenTheBlockRuns() {
        assertEquals("speed", NumberValueCode.float(NumberValue.Property("speed"), scope))
        assertEquals("gameManager.score", NumberValueCode.float(NumberValue.Property("score", manager = "Game"), scope))
        assertEquals("speed.toInt().coerceAtLeast(0)", NumberValueCode.whole(NumberValue.Property("speed"), scope))
        assertEquals("gameManager.score.sceneUnit", NumberValueCode.sceneUnit(NumberValue.Property("score", manager = "Game"), scope))
        assertEquals(setOf("Game"), NumberValueCode.managersReadBy(listOf(NumberValue.Property("score", manager = "Game"), NumberValue.Property("speed")), scope))
    }

    @Test
    fun aPropertyThatIsMissingOrNotANumberReadsAsZero() {
        listOf(NumberValue.Property(), NumberValue.Property("missing"), NumberValue.Property("title", manager = "Game"), NumberValue.Property("score", manager = "Missing")).forEach { value ->
            assertEquals("0f", NumberValueCode.float(value, scope), value.toString())
            assertEquals("0", NumberValueCode.whole(value, scope), value.toString())
            assertEquals(emptySet(), NumberValueCode.managersReadBy(listOf(value), scope), value.toString())
        }
    }

    @Test
    fun aManagerTypeReadsItsOwnPropertiesByTheirNames() {
        val managerScope = BlockScope("Game", BlockTypeKind.MANAGER, game.properties, managerTypes = listOf(game))
        assertEquals("score", NumberValueCode.float(NumberValue.Property("score", manager = "Game"), managerScope))
        assertEquals(emptySet(), NumberValueCode.managersReadBy(listOf(NumberValue.Property("score", manager = "Game")), managerScope))
    }

    @Test
    fun theBlocksUseTheirNumbersWhereverTheyTakeOne() {
        fun lines(block: BlockDefinition) = BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, scope.copy(isAnimated = true)).lines
        val random = NumberValue.Random(1f, 3f)
        assertEquals(
            listOf("velocity = SceneOffset((KubrikoWiring.random.nextFloat() * 2f + 1f).sceneUnit, gameManager.score.sceneUnit)"),
            lines(BlockDefinition.SetVelocity(random, NumberValue.Property("score", manager = "Game"))),
        )
        assertEquals(listOf("speed += (KubrikoWiring.random.nextFloat() * 2f + 1f)"), lines(BlockDefinition.ChangeNumber("speed", random)))
        assertEquals(listOf("repeat(speed.toInt().coerceAtLeast(0)) {"), lines(BlockDefinition.Repeat(NumberValue.Property("speed"))))
        assertEquals(listOf("setFrame(KubrikoWiring.random.nextInt(1, 4))"), lines(BlockDefinition.SetFrame(random)))
        assertEquals(
            listOf("timerOneRemainingInMilliseconds = (speed * 1000f + 0.5f).toLong().coerceAtLeast(1)", "timerOnePeriodInMilliseconds = timerOneRemainingInMilliseconds"),
            lines(BlockDefinition.StartTimer("timerOne", NumberValue.Property("speed"), isRepeating = true)),
        )
        assertEquals(listOf("timerOneRemainingInMilliseconds = 1500", "timerOnePeriodInMilliseconds = 0"), lines(BlockDefinition.StartTimer("timerOne", NumberValue.Fixed(1.5f))))
    }

    @Test
    fun anEdgeIsReadFromTheScenesWhenTheBlockRuns() {
        assertEquals("generatedScenes.rightEdge.raw", NumberValueCode.float(NumberValue.Edge(SceneEdge.RIGHT), scope))
        assertEquals("(generatedScenes.leftEdge.raw + 50f)", NumberValueCode.float(NumberValue.Edge(SceneEdge.LEFT, 50f), scope))
        assertEquals("(generatedScenes.bottomEdge.raw - 2.5f)", NumberValueCode.float(NumberValue.Edge(SceneEdge.BOTTOM, -2.5f), scope))
        assertEquals("generatedScenes.topEdge.raw.toInt().coerceAtLeast(1)", NumberValueCode.whole(NumberValue.Edge(SceneEdge.TOP), scope, minimum = 1))
        assertEquals(
            "((generatedScenes.topEdge.raw + 10f) + KubrikoWiring.random.nextFloat() * (generatedScenes.bottomEdge.raw - (generatedScenes.topEdge.raw + 10f)))",
            NumberValueCode.float(NumberValue.BetweenEdges(SceneEdge.TOP, 10f, SceneEdge.BOTTOM), scope),
        )
    }

    @Test
    fun aBlockReadingAnEdgeNeedsTheScenesInActorAndManagerTypes() {
        val value = NumberValue.BetweenEdges()
        assertEquals(setOf(BlockNeed.SCENES), NumberValueCode.needsOf(listOf(NumberValue.Fixed(1f), value)))
        assertEquals(emptySet(), NumberValueCode.needsOf(listOf(NumberValue.Fixed(1f), NumberValue.Random())))
        val managerScope = BlockScope("Game", BlockTypeKind.MANAGER, game.properties, managerTypes = listOf(game))
        listOf(scope, managerScope).forEach { blockScope ->
            val code = BlockCodeGenerator.blockCode(BlockDefinition.SetNumber("score", NumberValue.Edge(SceneEdge.TOP), manager = "Game"), EventType.EVERY_FRAME, blockScope)
            assertEquals(true, BlockNeed.SCENES in code.needs, blockScope.typeName)
        }
    }

    @Test
    fun anActorAddedFromThisOneStillGoesToTheEdgeItself() {
        val scope = BlockScope.of(ActorTypeDefinition(name = "Ball"), listOf(ActorTypeDefinition(name = "Ball")))
        fun line(block: BlockDefinition.AddActor) = BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, scope).lines.single()
        assertEquals(
            "actorManager.add(Ball.metadata.instantiate(SceneOffset((generatedScenes.rightEdge.raw + 50f).sceneUnit, body.position.y + 10f.sceneUnit)).restore())",
            line(BlockDefinition.AddActor("Ball", x = NumberValue.Edge(SceneEdge.RIGHT, 50f), y = NumberValue.Fixed(10f))),
        )
        assertEquals(
            "actorManager.add(Ball.metadata.instantiate(body.position + SceneOffset(5f.sceneUnit, 10f.sceneUnit)).restore())",
            line(BlockDefinition.AddActor("Ball", x = NumberValue.Fixed(5f), y = NumberValue.Fixed(10f))),
        )
        assertEquals(
            "actorManager.add(Ball.metadata.instantiate(SceneOffset(5f.sceneUnit, generatedScenes.topEdge.raw.sceneUnit)).restore())",
            line(BlockDefinition.AddActor("Ball", x = NumberValue.Fixed(5f), y = NumberValue.Edge(SceneEdge.TOP), isRelative = false)),
        )
    }
}
