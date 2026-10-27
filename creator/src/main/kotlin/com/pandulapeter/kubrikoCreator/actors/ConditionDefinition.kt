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
 * What an if Block checks before it runs the Blocks that follow it.
 */
@Serializable
@Description("What an if Block checks. The type property says which condition it is.")
internal sealed interface ConditionDefinition {

    @Serializable
    @SerialName("number")
    @Description("Compares a number Property of the type, or of a Manager type, with a value.")
    data class Number(
        @Description("The name of one of the number Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description("How the Property is compared with the value.")
        @SerialName("comparison") val comparison: Comparison = Comparison.GREATER,
        @Description("The value.")
        @SerialName("value") val value: Float = 0f,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : ConditionDefinition

    @Serializable
    @SerialName("text")
    @Description("Checks whether a text Property of the type, or of a Manager type, is a given text.")
    data class Text(
        @Description("The name of one of the text Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description("The text it is compared with.")
        @SerialName("value") val value: String = "",
        @Description("True to check that it is the text, false to check that it isn't.")
        @SerialName("isEqual") val isEqual: Boolean = true,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : ConditionDefinition

    @Serializable
    @SerialName("toggle")
    @Description("Checks whether a toggle Property of the type, or of a Manager type, is on or off.")
    data class Toggle(
        @Description("The name of one of the toggle Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description("True to check that it is on, false to check that it is off.")
        @SerialName("isOn") val isOn: Boolean = true,
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : ConditionDefinition

    @Serializable
    @SerialName("keyHeld")
    @Description("Checks whether a key is being held down.")
    data class KeyHeld(
        @Description("The key.")
        @SerialName("key") val key: KeyChoice = KeyChoice.SPACE,
        @Description("True to check that it is held, false to check that it isn't.")
        @SerialName("isHeld") val isHeld: Boolean = true,
    ) : ConditionDefinition

    @Serializable
    @SerialName("velocity")
    @Description("Compares the velocity of the Actor along one axis, in scene units per second, with a value. Only Actor types have it.")
    data class Velocity(
        @Description("Which part of the velocity: horizontal (positive to the right) or vertical (positive downwards).")
        @SerialName("axis") val axis: Axis = Axis.HORIZONTAL,
        @Description("How the velocity is compared with the value.")
        @SerialName("comparison") val comparison: Comparison = Comparison.GREATER,
        @Description("The value.")
        @SerialName("value") val value: Float = 0f,
    ) : ConditionDefinition

    @Serializable
    @SerialName("chance")
    @Description("True by chance, each time it is checked.")
    data class Chance(
        @Description("How likely it is to be true, in percent, from 0 to 100.")
        @SerialName("percent") val percent: Float = 50f,
    ) : ConditionDefinition
}
