/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput.implementation

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import com.pandulapeter.kubriko.gamepadInput.GamepadActivationNode
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadFocusNavigationHostState
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import com.pandulapeter.kubriko.gamepadInput.GamepadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamepadFocusNavigatorTest {

    private val gamepads = List(MAX_GAMEPAD_COUNT) { GamepadState(index = it) }
    private val gamepadInputManager = GamepadInputManager.newInstance()
    private val navigator = GamepadFocusNavigator(gamepads, gamepadInputManager)
    private val focusManager = RecordingFocusManager()
    private val inputModeManager = RecordingInputModeManager()
    private var backCount = 0
    private var activationCount = 0

    init {
        gamepadInputManager.onFocusNavigationHostAttached(
            GamepadFocusNavigationHostState(
                focusManager = focusManager,
                inputModeManager = inputModeManager,
                onBack = { backCount++ },
                hasOnBack = { true },
            ),
        )
        gamepads[0].isConnected = true
    }

    @Test
    fun pressingADirectionStepsImmediately() {
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_RIGHT)

        navigator.updateFocusNavigation(0f)

        assertEquals(listOf(FocusDirection.Right), focusManager.moves)
    }

    @Test
    fun heldDirectionWaitsBeforeRepeating() {
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_RIGHT)
        navigator.updateFocusNavigation(0f)

        holdFor(milliseconds = 100)

        assertEquals(1, focusManager.moves.size)
    }

    @Test
    fun heldDirectionKeepsRepeatingFasterThanItsInitialDelay() {
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_DOWN)
        navigator.updateFocusNavigation(0f)
        var timeUntilFirstRepeat = 0
        while (focusManager.moves.size == 1 && timeUntilFirstRepeat < 10_000) {
            navigator.updateFocusNavigation(FRAME_TIME.toFloat())
            timeUntilFirstRepeat += FRAME_TIME
        }

        holdFor(milliseconds = timeUntilFirstRepeat * 3)

        assertTrue(focusManager.moves.size > 5, "${focusManager.moves.size} steps")
        assertTrue(focusManager.moves.all { it == FocusDirection.Down })
    }

    @Test
    fun releasingAndPressingAgainStepsImmediately() {
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_LEFT)
        navigator.updateFocusNavigation(0f)
        release(GamepadButton.DPAD_LEFT)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        press(GamepadButton.DPAD_LEFT)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        assertEquals(listOf(FocusDirection.Left, FocusDirection.Left), focusManager.moves)
    }

    @Test
    fun slightlyLeaningStickDoesNotStep() {
        navigator.restFocusNavigation()
        gamepads[0].leftStickY = 0.2f

        navigator.updateFocusNavigation(0f)

        assertEquals(emptyList(), focusManager.moves)
    }

    @Test
    fun fullyLeaningStickSteps() {
        navigator.restFocusNavigation()
        gamepads[0].leftStickY = 1f

        navigator.updateFocusNavigation(0f)

        assertEquals(listOf(FocusDirection.Down), focusManager.moves)
    }

    @Test
    fun stickIsReducedToTheAxisItLeansOnTheMost() {
        navigator.restFocusNavigation()
        gamepads[0].leftStickX = -0.8f
        gamepads[0].leftStickY = 0.6f

        navigator.updateFocusNavigation(0f)

        assertEquals(listOf(FocusDirection.Left), focusManager.moves)
    }

    @Test
    fun failedStepEntersTheFocusedContainerOnlyWithoutAnActivationTarget() {
        focusManager.canMove = false
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(0f)
        assertEquals(listOf(FocusDirection.Up, FocusDirection.Enter), focusManager.moves)

        focusManager.moves.clear()
        gamepadInputManager.onActivationTargetFocused(GamepadActivationNode(gamepadInputManager) { activationCount++ })
        release(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(0f)
        press(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(0f)
        assertEquals(listOf(FocusDirection.Up), focusManager.moves)
    }

    @Test
    fun southActivatesTheFocusedTargetOncePerPress() {
        gamepadInputManager.onActivationTargetFocused(GamepadActivationNode(gamepadInputManager) { activationCount++ })
        navigator.restFocusNavigation()

        press(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(16f)
        navigator.updateFocusNavigation(16f)
        assertEquals(1, activationCount)

        release(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(16f)
        press(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(16f)
        assertEquals(2, activationCount)
        assertEquals(InputMode.Keyboard, inputModeManager.requestedInputMode)
    }

    @Test
    fun eastRunsTheHostsBackActionOncePerPress() {
        navigator.restFocusNavigation()

        press(GamepadButton.EAST)
        navigator.updateFocusNavigation(16f)
        navigator.updateFocusNavigation(16f)

        assertEquals(1, backCount)
    }

    @Test
    fun controlsHeldWhenNavigationRestsAreIgnoredUntilPressedAgain() {
        gamepadInputManager.onActivationTargetFocused(GamepadActivationNode(gamepadInputManager) { activationCount++ })
        press(GamepadButton.SOUTH)
        press(GamepadButton.EAST)
        press(GamepadButton.DPAD_LEFT)
        navigator.restFocusNavigation()

        navigator.updateFocusNavigation(16f)
        assertEquals(0, activationCount)
        assertEquals(0, backCount)
        assertEquals(emptyList(), focusManager.moves)

        release(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(16f)
        press(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(16f)
        assertEquals(1, activationCount)
    }

    @Test
    fun disconnectedGamepadsAreIgnored() {
        gamepads[1].pressedButtons = GamepadButton.SOUTH.bitMask or GamepadButton.DPAD_DOWN.bitMask
        navigator.restFocusNavigation()

        val hasInput = navigator.updateFocusNavigation(16f)

        assertEquals(false, hasInput)
        assertEquals(emptyList(), focusManager.moves)
        assertEquals(false, navigator.isAnyGamepadPressing(GamepadButton.SOUTH))
        assertEquals(false, navigator.isAnyFocusDirectionHeld())
    }

    @Test
    fun theInnermostHostReceivesTheStepsUntilItIsDetached() {
        val popupFocusManager = RecordingFocusManager()
        val popupHost = GamepadFocusNavigationHostState(
            focusManager = popupFocusManager,
            inputModeManager = RecordingInputModeManager(),
            onBack = {},
            hasOnBack = { true },
        )
        gamepadInputManager.onFocusNavigationHostAttached(popupHost)
        navigator.restFocusNavigation()
        press(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(0f)
        release(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        gamepadInputManager.onFocusNavigationHostDetached(popupHost)
        press(GamepadButton.DPAD_UP)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        assertEquals(listOf(FocusDirection.Up), popupFocusManager.moves)
        assertEquals(listOf(FocusDirection.Up), focusManager.moves)
    }

    @Test
    fun unfocusingAPopupTargetMakesTheOneBehindItCurrentAgain() {
        var activatedTarget = ""
        val menuTarget = GamepadActivationNode(gamepadInputManager) { activatedTarget = "menu" }
        val popupTarget = GamepadActivationNode(gamepadInputManager) { activatedTarget = "popup" }
        gamepadInputManager.onActivationTargetFocused(menuTarget)
        gamepadInputManager.onActivationTargetFocused(popupTarget)
        navigator.restFocusNavigation()
        press(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())
        assertEquals("popup", activatedTarget)
        release(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        gamepadInputManager.onActivationTargetUnfocused(popupTarget)
        press(GamepadButton.SOUTH)
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())

        assertEquals("menu", activatedTarget)
    }

    private fun holdFor(milliseconds: Int) = repeat(milliseconds / FRAME_TIME) {
        navigator.updateFocusNavigation(FRAME_TIME.toFloat())
    }

    private fun press(button: GamepadButton) {
        gamepads[0].pressedButtons = gamepads[0].pressedButtons or button.bitMask
    }

    private fun release(button: GamepadButton) {
        gamepads[0].pressedButtons = gamepads[0].pressedButtons and button.bitMask.inv()
    }

    private class RecordingFocusManager : FocusManager {
        val moves = mutableListOf<FocusDirection>()
        var canMove = true

        override fun clearFocus(force: Boolean) = Unit

        override fun moveFocus(focusDirection: FocusDirection): Boolean {
            moves.add(focusDirection)
            return canMove
        }
    }

    private class RecordingInputModeManager : InputModeManager {
        var requestedInputMode: InputMode? = null

        override val inputMode = InputMode.Touch

        override fun requestInputMode(inputMode: InputMode): Boolean {
            requestedInputMode = inputMode
            return true
        }
    }

    private companion object {
        const val FRAME_TIME = 16
    }
}
