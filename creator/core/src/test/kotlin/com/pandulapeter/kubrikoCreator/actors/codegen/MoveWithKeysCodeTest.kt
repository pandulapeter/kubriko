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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SteeringKeys
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MoveWithKeysCodeTest {

    private val scope = BlockScope("Hero", properties = listOf(PropertyDefinition.Number("speed")))

    private fun lines(block: BlockDefinition, eventType: EventType = EventType.EVERY_FRAME) = BlockCodeGenerator.blockCode(block, eventType, scope).lines

    private fun generate(vararg events: EventDefinition) = ActorTypeCodeGenerator.generateActorType(
        ActorTypeDefinition(name = "Hero", properties = listOf(PropertyDefinition.Number("speed")), events = events.toList()),
        "com.example.game",
    )

    @Test
    fun theCallChecksOnlyTheBlocksOwnKeysAndPassesTheSpeedAsAnArgument() {
        assertEquals(
            listOf(
                "velocity = velocityFromKeys(",
                "    speed = 200f,",
                "    isLeftHeld = keyboardInputManager.isKeyPressed(Key.DirectionLeft),",
                "    isRightHeld = keyboardInputManager.isKeyPressed(Key.DirectionRight),",
                "    isUpHeld = keyboardInputManager.isKeyPressed(Key.DirectionUp),",
                "    isDownHeld = keyboardInputManager.isKeyPressed(Key.DirectionDown),",
                "    movesDiagonally = true,",
                ")",
            ),
            lines(BlockDefinition.MoveWithKeys()),
        )
        val wasd = lines(BlockDefinition.MoveWithKeys(SteeringKeys.WASD, movesDiagonally = false, speed = NumberValue.Property("speed")))
        assertEquals("    speed = speed,", wasd[1])
        assertEquals("    isLeftHeld = keyboardInputManager.isKeyPressed(Key.A),", wasd[2])
        assertEquals("    movesDiagonally = false,", wasd[6])
        assertEquals(
            "    isUpHeld = keyboardInputManager.isKeyPressed(Key.DirectionUp) || keyboardInputManager.isKeyPressed(Key.W),",
            lines(BlockDefinition.MoveWithKeys(SteeringKeys.ARROWS_AND_WASD))[4],
        )
        assertEquals(listOf("// Move with keys only works in an every frame Event."), lines(BlockDefinition.MoveWithKeys(), EventType.KEY_PRESSED))
    }

    @Test
    fun theFunctionCancelsOppositeKeysKeepsDiagonalsAsFastAndIsDeclaredOnlyWhenUsed() {
        assertFalse("velocityFromKeys" in generate(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop))))
        val code = generate(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.MoveWithKeys())))
        assertContains(
            code,
            "    private fun velocityFromKeys(speed: Float, isLeftHeld: Boolean, isRightHeld: Boolean, isUpHeld: Boolean, isDownHeld: Boolean, movesDiagonally: Boolean): SceneOffset {\n" +
                "        val x = (if (isRightHeld) 1f else 0f) - (if (isLeftHeld) 1f else 0f)\n" +
                "        val y = if (x != 0f && !movesDiagonally) 0f else (if (isDownHeld) 1f else 0f) - (if (isUpHeld) 1f else 0f)\n" +
                "        val step = if (x != 0f && y != 0f) speed * 0.70710677f else speed\n" +
                "        return SceneOffset((x * step).sceneUnit, (y * step).sceneUnit)\n" +
                "    }\n",
        )
        listOf(
            "import androidx.compose.ui.input.key.Key\n",
            "import com.pandulapeter.kubriko.helpers.extensions.sceneUnit\n",
            "import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager\n",
            "    private lateinit var keyboardInputManager: KeyboardInputManager\n",
        ).forEach { assertContains(code, it) }
        assertFalse("KeyboardInputAware" in code, code)
    }
}
