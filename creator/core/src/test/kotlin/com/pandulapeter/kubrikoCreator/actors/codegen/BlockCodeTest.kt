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
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.ACTOR_MANAGER
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.AUDIO
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.BOUNCE_OFF
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.KEYBOARD_INPUT_MANAGER
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.MOVEMENT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.NUMBER_TEXT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.SCENES
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.STATE_MANAGER
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockNeed.VIEWPORT_MANAGER
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class BlockCodeTest {
    private val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
    private val actorScope = BlockScope(
        typeName = "Ball",
        properties = listOf(PropertyDefinition.Number("speed")),
        actorTypeNames = listOf("Ball"),
        managerTypes = listOf(game),
    )

    private data class Expected(
        val needs: Set<BlockNeed> = emptySet(),
        val imports: Set<String> = emptySet(),
        val managers: Set<String> = emptySet(),
    )

    @Test
    fun everyBlockStatesWhatItsCodeNeeds() {
        val sceneOffset = "com.pandulapeter.kubriko.types.SceneOffset"
        val absoluteValue = "kotlin.math.absoluteValue"
        val defaults = mapOf(
            BlockKind.SET_VELOCITY to Expected(setOf(MOVEMENT), setOf(sceneOffset)),
            BlockKind.ADD_TO_VELOCITY to Expected(setOf(MOVEMENT), setOf(sceneOffset)),
            BlockKind.STOP to Expected(setOf(MOVEMENT), setOf(sceneOffset)),
            BlockKind.RUN_PARENT_EVENT to Expected(),
            BlockKind.JUMP_TO to Expected(setOf(MOVEMENT), setOf(sceneOffset)),
            BlockKind.BOUNCE_OFF_EDGE to Expected(setOf(MOVEMENT), setOf(absoluteValue, sceneOffset)),
            BlockKind.REMOVE_SELF to Expected(setOf(ACTOR_MANAGER)),
            BlockKind.BOUNCE_OFF_OTHER to Expected(setOf(MOVEMENT, BOUNCE_OFF)),
            BlockKind.REMOVE_OTHER to Expected(setOf(ACTOR_MANAGER)),
            BlockKind.SET_NUMBER to Expected(),
            BlockKind.CHANGE_NUMBER to Expected(),
            BlockKind.SET_TEXT to Expected(),
            BlockKind.SET_TOGGLE to Expected(),
            BlockKind.ADD_ACTOR to Expected(),
            BlockKind.MOVE_TOWARDS_NEAREST to Expected(),
            BlockKind.START_TIMER to Expected(),
            BlockKind.STOP_TIMER to Expected(),
            BlockKind.GO_TO_SCENE to Expected(),
            BlockKind.RESTART_SCENE to Expected(setOf(SCENES)),
            BlockKind.PAUSE_GAME to Expected(setOf(STATE_MANAGER)),
            BlockKind.PLAY_SOUND to Expected(),
            BlockKind.PLAY_MUSIC to Expected(),
            BlockKind.STOP_MUSIC to Expected(setOf(AUDIO)),
            BlockKind.IF to Expected(imports = setOf("kotlin.random.Random")),
            BlockKind.ELSE to Expected(),
            BlockKind.REPEAT to Expected(),
            BlockKind.END to Expected(),
            BlockKind.LOG to Expected(),
            BlockKind.KOTLIN to Expected(),
        )
        assertEquals(BlockKind.entries.toSet(), defaults.keys)
        defaults.forEach { (kind, expected) ->
            assertEquals(expected, codeOf(kind.createDefault(), kind.onlyIn ?: EventType.EVERY_FRAME), kind.name)
        }
        val variants = listOf(
            BlockDefinition.BounceOffEdge to Expected(),
            BlockDefinition.BounceOffOther to Expected(),
            BlockDefinition.RemoveOther to Expected(),
            BlockDefinition.PlaySound("hit.wav") to Expected(setOf(AUDIO)),
            BlockDefinition.PlayMusic("level.mp3") to Expected(setOf(AUDIO)),
            BlockDefinition.GoToScene("Level") to Expected(setOf(SCENES)),
            BlockDefinition.AddActor("Ball") to Expected(setOf(ACTOR_MANAGER), setOf(sceneOffset)),
            BlockDefinition.AddActor("Missing") to Expected(),
            BlockDefinition.MoveTowardsNearest("Ball") to Expected(setOf(MOVEMENT, ACTOR_MANAGER), setOf("com.pandulapeter.kubriko.helpers.extensions.normalized", sceneOffset)),
            BlockDefinition.MoveTowardsNearest("Missing") to Expected(),
            BlockDefinition.SetNumber("speed", 1f) to Expected(),
            BlockDefinition.SetNumber("score", 1f, manager = "Game") to Expected(managers = setOf("Game")),
            BlockDefinition.If(ConditionDefinition.Number("speed")) to Expected(),
            BlockDefinition.If(ConditionDefinition.Number("score", manager = "Game")) to Expected(managers = setOf("Game")),
            BlockDefinition.If(ConditionDefinition.Text("missing")) to Expected(),
            BlockDefinition.If(ConditionDefinition.Toggle("missing")) to Expected(),
            BlockDefinition.If(ConditionDefinition.KeyHeld()) to Expected(setOf(KEYBOARD_INPUT_MANAGER), setOf("androidx.compose.ui.input.key.Key")),
            BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice("NoSuchKey"))) to Expected(),
            BlockDefinition.If(ConditionDefinition.Velocity()) to Expected(setOf(MOVEMENT)),
            BlockDefinition.If(ConditionDefinition.Chance()) to Expected(imports = setOf("kotlin.random.Random")),
            BlockDefinition.If(ConditionDefinition.Paused()) to Expected(setOf(STATE_MANAGER)),
            BlockDefinition.Log("{speed}") to Expected(setOf(NUMBER_TEXT)),
            BlockDefinition.Log("{Game.score}") to Expected(setOf(NUMBER_TEXT), managers = setOf("Game")),
            BlockDefinition.Kotlin("velocity = SceneOffset.Zero") to Expected(setOf(MOVEMENT, ACTOR_MANAGER, VIEWPORT_MANAGER), setOf(sceneOffset)),
        )
        variants.forEach { (block, expected) -> assertEquals(expected, codeOf(block, EventType.EVERY_FRAME), block.toString()) }
    }

    @Test
    fun aKotlinBlockNamingAbsoluteValueImportsItInEveryEvent() {
        val ball = ActorTypeDefinition(
            name = "Ball",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("velocity = SceneOffset(velocity.x.raw.absoluteValue.sceneUnit, velocity.y)")))),
        )
        assertContains(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), "import kotlin.math.absoluteValue\n")
    }

    @Test
    fun anImportOnlyKotlinBlockGeneratesNoStatements() {
        val code = BlockCodeGenerator.blockCode(BlockDefinition.Kotlin("import kotlin.math.sin"), EventType.EVERY_FRAME, actorScope)
        assertEquals(emptyList(), code.lines)
        assertEquals(setOf("kotlin.math.sin"), code.imports)
    }

    @Test
    fun aManagersActorOnlyBlocksNeedNothing() {
        val managerScope = BlockScope("Game", BlockTypeKind.MANAGER, game.properties, listOf("Ball"), listOf(game))
        listOf(BlockDefinition.RemoveOther, BlockDefinition.RemoveSelf, BlockDefinition.MoveTowardsNearest("Ball")).forEach { block ->
            assertEquals(BlockCode(listOf("// This Block only works in an Actor type.")), BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, managerScope), block.toString())
        }
    }

    private fun codeOf(block: BlockDefinition, eventType: EventType) = BlockCodeGenerator.blockCode(block, eventType, actorScope).let { Expected(it.needs, it.imports, it.managers) }
}
