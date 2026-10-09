/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.gamepadInput.implementation.GamepadEventHandler
import com.pandulapeter.kubriko.gamepadInput.implementation.RawGamepadState
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Focus loss (which releases every held input) is not covered: a headless instance is never shown, so it stays
 * focused and there is no public way to drive `StateManager.isFocused` from a test.
 */
class GamepadInputManagerTest {

    @Test
    fun stickInsideTheDeadZoneReadsZero() = withGamepads { kubriko, manager, handler, _ ->
        handler.connect(0).apply {
            leftStickX = 0.1f
            leftStickY = -0.1f
        }
        kubriko.tick()

        assertEquals(0f, manager.gamepads[0].leftStickX)
        assertEquals(0f, manager.gamepads[0].leftStickY)
    }

    @Test
    fun stickOutsideTheDeadZoneIsRescaled() = withGamepads { kubriko, manager, handler, _ ->
        listOf(0.6f to 0f, 1f to 0f, 0.3f to -0.4f, 0.8f to 0.8f).forEach { (x, y) ->
            handler.connect(0).apply {
                rightStickX = x
                rightStickY = y
            }
            kubriko.tick()

            val magnitude = hypot(x, y)
            val scale = ((magnitude - DEAD_ZONE) / (1f - DEAD_ZONE)).coerceAtMost(1f) / magnitude
            assertEquals(x * scale, manager.gamepads[0].rightStickX)
            assertEquals(y * scale, manager.gamepads[0].rightStickY)
        }
    }

    @Test
    fun triggerAboveTheThresholdPressesItsButton() = withGamepads { kubriko, manager, handler, actor ->
        val gamepad = handler.connect(0)
        kubriko.tick()
        actor.events.clear()

        gamepad.leftTrigger = TRIGGER_THRESHOLD + 0.1f
        kubriko.tick()
        assertTrue(manager.isButtonPressed(0, GamepadButton.LEFT_TRIGGER))

        gamepad.leftTrigger = TRIGGER_THRESHOLD
        kubriko.tick()
        assertFalse(manager.isButtonPressed(0, GamepadButton.LEFT_TRIGGER))

        assertEquals(listOf("pressed 0 LEFT_TRIGGER", "released 0 LEFT_TRIGGER"), actor.events)
    }

    @Test
    fun buttonChangesAreReportedOnceEach() = withGamepads { kubriko, _, handler, actor ->
        val gamepad = handler.connect(0)
        kubriko.tick()
        actor.events.clear()

        gamepad.setButton(GamepadButton.SOUTH, true)
        kubriko.tick(count = 2)
        gamepad.setButton(GamepadButton.EAST, true)
        kubriko.tick(count = 2)
        gamepad.setButton(GamepadButton.SOUTH, false)
        kubriko.tick(count = 2)

        assertEquals(listOf("pressed 0 SOUTH", "pressed 0 EAST", "released 0 SOUTH"), actor.events)
    }

    @Test
    fun connectionChangesAreReportedAndCounted() = withGamepads { kubriko, manager, handler, actor ->
        handler.connect(0)
        handler.connect(2)
        kubriko.tick(count = 2)
        assertEquals(2, manager.connectedGamepadCount.value)

        handler.gamepads[0].reset()
        kubriko.tick(count = 2)
        assertEquals(1, manager.connectedGamepadCount.value)

        assertEquals(listOf("connected 0", "connected 2", "disconnected 0"), actor.events)
    }

    @Test
    fun disconnectingAPadReleasesItsHeldButtons() = withGamepads { kubriko, manager, handler, actor ->
        handler.connect(0).setButton(GamepadButton.SOUTH, true)
        kubriko.tick()
        actor.events.clear()

        handler.gamepads[0].reset()
        kubriko.tick()

        assertFalse(manager.isButtonPressed(0, GamepadButton.SOUTH))
        assertEquals(listOf("released 0 SOUTH", "disconnected 0"), actor.events)
    }

    private fun withGamepads(block: (ManualKubriko, GamepadInputManagerImpl, FakeGamepadEventHandler, RecordingActor) -> Unit) {
        val handler = FakeGamepadEventHandler()
        val manager = GamepadInputManagerImpl(
            deadZone = DEAD_ZONE,
            triggerThreshold = TRIGGER_THRESHOLD,
            isLoggingEnabled = false,
            instanceNameForLogging = null,
            initialGamepadEventHandler = handler,
        )
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            manager,
        )
        try {
            val actor = RecordingActor()
            kubriko.actorManager.add(actor)
            handler.connect(PROBE_SLOT)
            kubriko.tickUntil { actor.hasReceivedState }
            handler.gamepads[PROBE_SLOT].reset()
            kubriko.tick()
            actor.events.clear()
            block(kubriko, manager, handler, actor)
        } finally {
            kubriko.dispose()
        }
    }

    private class FakeGamepadEventHandler : GamepadEventHandler {
        lateinit var gamepads: Array<RawGamepadState>

        fun connect(index: Int) = gamepads[index].also { it.isConnected = true }

        @Composable
        override fun isValid() = true

        override fun startListening(gamepads: Array<RawGamepadState>) {
            this.gamepads = gamepads
        }

        override fun stopListening() = Unit

        override fun poll() = Unit
    }

    private class RecordingActor : GamepadInputAware {
        var hasReceivedState = false
        val events = mutableListOf<String>()

        override fun handleGamepadState(gamepad: GamepadState) {
            hasReceivedState = true
        }

        override fun onGamepadButtonPressed(gamepad: GamepadState, button: GamepadButton) {
            events.add("pressed ${gamepad.index} $button")
        }

        override fun onGamepadButtonReleased(gamepad: GamepadState, button: GamepadButton) {
            events.add("released ${gamepad.index} $button")
        }

        override fun onGamepadConnected(gamepad: GamepadState) {
            events.add("connected ${gamepad.index}")
        }

        override fun onGamepadDisconnected(gamepad: GamepadState) {
            events.add("disconnected ${gamepad.index}")
        }
    }

    private companion object {
        const val DEAD_ZONE = 0.2f
        const val TRIGGER_THRESHOLD = 0.5f
        const val PROBE_SLOT = 3
    }
}
