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
 * A block-built Manager type, as stored in `.kubriko/managers/<name>.json`: game-wide state and rules, like the score
 * or the number of lives, which Kubriko Creator generates a `Manager` subclass from. The game has one of each.
 */
@Serializable
@Description("A block-built Manager type: game-wide values (like the score or the lives left) that every Actor type's Blocks can read and change, the Events it reacts to, and the texts it shows on the screen.")
internal data class ManagerTypeDefinition(
    @Description("The name of the Manager type, and of its generated Kotlin class: an uppercase letter, then letters and numbers. Actor types and Manager types can't share a name.")
    @SerialName("name") override val name: String,
    @Description("The game-wide values it keeps, like a score, which Blocks of any type can change.")
    @SerialName("properties") override val properties: List<PropertyDefinition> = emptyList(),
    @Description("The Events it reacts to, in order. Each runs its Blocks in order.")
    @SerialName("events") override val events: List<EventDefinition> = emptyList(),
    @Description("The texts it shows on top of the game, like the score, in order.")
    @SerialName("texts") val texts: List<ScreenTextDefinition> = emptyList(),
) : BlockTypeDefinition {
    override val kind get() = BlockTypeKind.MANAGER

    override fun withName(name: String) = copy(name = name)

    override fun withProperties(properties: List<PropertyDefinition>) = copy(properties = properties)

    override fun withEvents(events: List<EventDefinition>) = copy(events = events)
}
