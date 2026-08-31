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

/**
 * The digital inputs of a gamepad, following the layout every supported platform maps its controllers to.
 *
 * The four face buttons are named by their position rather than by a letter, because the letters printed on them
 * differ between vendors: what Xbox calls A is in the [SOUTH] position, while on a Nintendo layout the same
 * position is labelled B.
 */
enum class GamepadButton {

    /**
     * The bottom face button (A on Xbox, Cross on PlayStation, B on Nintendo).
     */
    SOUTH,

    /**
     * The right face button (B on Xbox, Circle on PlayStation, A on Nintendo).
     */
    EAST,

    /**
     * The left face button (X on Xbox, Square on PlayStation, Y on Nintendo).
     */
    WEST,

    /**
     * The top face button (Y on Xbox, Triangle on PlayStation, X on Nintendo).
     */
    NORTH,

    /**
     * The upper left shoulder button (L1 / LB).
     */
    LEFT_SHOULDER,

    /**
     * The upper right shoulder button (R1 / RB).
     */
    RIGHT_SHOULDER,

    /**
     * The left trigger (L2 / LT) treated as a digital button.
     * Pressed while [GamepadState.leftTrigger] is above the trigger threshold of the [GamepadInputManager].
     */
    LEFT_TRIGGER,

    /**
     * The right trigger (R2 / RT) treated as a digital button.
     * Pressed while [GamepadState.rightTrigger] is above the trigger threshold of the [GamepadInputManager].
     */
    RIGHT_TRIGGER,

    /**
     * The left stick pressed down (L3).
     */
    LEFT_STICK,

    /**
     * The right stick pressed down (R3).
     */
    RIGHT_STICK,

    /**
     * The up direction of the directional pad.
     */
    DPAD_UP,

    /**
     * The down direction of the directional pad.
     */
    DPAD_DOWN,

    /**
     * The left direction of the directional pad.
     */
    DPAD_LEFT,

    /**
     * The right direction of the directional pad.
     */
    DPAD_RIGHT,

    /**
     * The primary menu button (Start / Options / Plus).
     */
    START,

    /**
     * The secondary menu button (Back / Select / Share / Minus).
     */
    SELECT,

    /**
     * The vendor button in the middle of the gamepad (Xbox / PlayStation / Home).
     * Many platforms reserve this button for the system and never report it.
     */
    GUIDE;

    internal val bitMask = 1 shl ordinal
}
