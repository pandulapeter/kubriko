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
 * One action an Event performs. Each one becomes a few lines of ordinary Kotlin in the generated Actor type.
 *
 * Speeds and velocities are in scene units per second.
 */
@Serializable
internal sealed interface BlockDefinition {

    @Serializable
    @SerialName("setVelocity")
    data class SetVelocity(
        @SerialName("x") val x: Float = 0f,
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("addToVelocity")
    data class AddToVelocity(
        @SerialName("x") val x: Float = 0f,
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    @Serializable
    @SerialName("stop")
    data object Stop : BlockDefinition

    @Serializable
    @SerialName("jumpTo")
    data class JumpTo(
        @SerialName("x") val x: Float = 0f,
        @SerialName("y") val y: Float = 0f,
    ) : BlockDefinition

    /**
     * Only meaningful in an [EventType.EDGE_TOUCHED] Event: turns the velocity away from the touched edge.
     */
    @Serializable
    @SerialName("bounceOffEdge")
    data object BounceOffEdge : BlockDefinition

    @Serializable
    @SerialName("removeSelf")
    data object RemoveSelf : BlockDefinition

    /**
     * Only meaningful in an [EventType.COLLISION] Event: turns the velocity away from the other Actor, along the axis
     * it was hit on.
     */
    @Serializable
    @SerialName("bounceOffOther")
    data object BounceOffOther : BlockDefinition

    /**
     * Only meaningful in an [EventType.COLLISION] Event: removes the Actor this one collided with.
     */
    @Serializable
    @SerialName("removeOther")
    data object RemoveOther : BlockDefinition

    /**
     * Kotlin statements inserted verbatim (in a scope of their own), for anything the other Blocks can't express.
     */
    @Serializable
    @SerialName("kotlin")
    data class Kotlin(
        @SerialName("code") val code: String = "",
    ) : BlockDefinition
}
