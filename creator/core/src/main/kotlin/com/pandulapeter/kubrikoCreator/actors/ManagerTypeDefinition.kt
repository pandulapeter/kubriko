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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A block-built Manager type, as stored in `.kubriko/managers/<name>.json`: game-wide state and rules, like the score
 * or the number of lives, which Kubriko Creator generates a `Manager` subclass from. The game has one of each.
 */
@Serializable
@Description("A block-built Manager type: game-wide values (like the score or the lives left) that every Actor type's Blocks can read and change, the Events it reacts to, and the texts it shows on the screen.")
public data class ManagerTypeDefinition(
    @Description("The name of the Manager type, and of its generated Kotlin class: an uppercase letter, then letters and numbers. Actor types and Manager types can't share a name.")
    @SerialName("name") override val name: String,
    @Description("What the Manager type is for in the game, in one short line (\"the ball the player bounces off the paddle\"), or none.")
    @SerialName("description") override val description: String? = null,
    @Description("The game-wide values it keeps, like a score, which Blocks of any type can change.")
    @SerialName("properties") override val properties: List<PropertyDefinition> = emptyList(),
    @Description("The Events it reacts to, in order. Each runs its Blocks in order.")
    @SerialName("events") override val events: List<EventDefinition> = emptyList(),
    @Description("The texts it shows on top of the game, like the score, in order.")
    @SerialName("texts") val texts: List<ScreenTextDefinition> = emptyList(),
    @Description("Compose code drawn on top of the game after the texts, for UI the texts can't express. See set_kotlin_ui for what is in scope.")
    @SerialName("kotlinUi") val kotlinUi: String? = null,
    @Transient override val folder: String = "",
) : BlockTypeDefinition {
    override val kind: BlockTypeKind get() = BlockTypeKind.MANAGER

    override fun withName(name: String): ManagerTypeDefinition = copy(name = name)

    override fun withDescription(description: String?): ManagerTypeDefinition = copy(description = description)

    override fun withFolder(folder: String): ManagerTypeDefinition = copy(folder = folder)

    override fun withProperties(properties: List<PropertyDefinition>): ManagerTypeDefinition = copy(properties = properties)

    override fun withEvents(events: List<EventDefinition>): ManagerTypeDefinition = copy(events = events)
}
