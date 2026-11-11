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

/**
 * A value each Actor of a block-built Actor type keeps, like its score or a name: a property of the generated class,
 * saved with the Actor in the Scene and editable per Actor in the Scene editor. Blocks change it while the game runs.
 */
@Serializable
@Description("A value each Actor of the type keeps (like a score), saved in the Scene and editable per Actor there. The type property says what kind of value it is.")
public sealed interface PropertyDefinition {

    /**
     * Also the name of the Kotlin property, and its key in the Scene.
     */
    public val name: String

    public fun withName(name: String): PropertyDefinition

    @Serializable
    @SerialName("number")
    @Description("A number.")
    public data class Number(
        @Description(NAME_DESCRIPTION)
        @SerialName("name") override val name: String,
        @Description(INITIAL_VALUE_DESCRIPTION)
        @SerialName("initialValue") val initialValue: Float = 0f,
    ) : PropertyDefinition {
        override fun withName(name: String): Number = copy(name = name)
    }

    @Serializable
    @SerialName("text")
    @Description("A piece of text.")
    public data class Text(
        @Description(NAME_DESCRIPTION)
        @SerialName("name") override val name: String,
        @Description(INITIAL_VALUE_DESCRIPTION)
        @SerialName("initialValue") val initialValue: String = "",
    ) : PropertyDefinition {
        override fun withName(name: String): Text = copy(name = name)
    }

    @Serializable
    @SerialName("toggle")
    @Description("Either on (true) or off (false).")
    public data class Toggle(
        @Description(NAME_DESCRIPTION)
        @SerialName("name") override val name: String,
        @Description(INITIAL_VALUE_DESCRIPTION)
        @SerialName("initialValue") val initialValue: Boolean = false,
    ) : PropertyDefinition {
        override fun withName(name: String): Toggle = copy(name = name)
    }
}

private const val NAME_DESCRIPTION = "Its name, also in Kotlin: a lowercase letter, then letters and numbers."
private const val INITIAL_VALUE_DESCRIPTION = "What each Actor starts with, unless its Scene says otherwise."
