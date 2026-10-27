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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * When an Event's Blocks run.
 */
@Serializable
internal enum class EventType {

    /**
     * Once, when the Actor is added to the Scene.
     */
    @SerialName("addedToScene")
    ADDED_TO_SCENE,

    /**
     * On every frame, before the Actor moves.
     */
    @SerialName("everyFrame")
    EVERY_FRAME,

    /**
     * When the player presses the Event's key.
     */
    @SerialName("keyPressed")
    KEY_PRESSED,

    /**
     * When the Actor reaches an edge of the screen. It is kept inside the screen either way.
     */
    @SerialName("edgeTouched")
    EDGE_TOUCHED,
}
