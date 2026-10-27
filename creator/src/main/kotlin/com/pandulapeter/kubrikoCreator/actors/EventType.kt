/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.commands.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * When an Event's Blocks run.
 */
@Serializable
@Description("When an Event happens.")
internal enum class EventType {

    @Description("Once, when the Actor is added to the Scene.")
    @SerialName("addedToScene")
    ADDED_TO_SCENE,

    @Description("On every frame, before the Actor moves.")
    @SerialName("everyFrame")
    EVERY_FRAME,

    @Description("When the player presses the Event's key.")
    @SerialName("keyPressed")
    KEY_PRESSED,

    @Description("When the Actor reaches an edge of the screen. It is kept inside the screen either way.")
    @SerialName("edgeTouched")
    EDGE_TOUCHED,

    @Description("On every frame the Actor overlaps an Actor of the Event's other Actor type, once per overlapping Actor.")
    @SerialName("collision")
    COLLISION,

    @Description("When the Event's Timer, started by a startTimer Block, finishes (each time, if it repeats).")
    @SerialName("timerFinished")
    TIMER_FINISHED,
}
