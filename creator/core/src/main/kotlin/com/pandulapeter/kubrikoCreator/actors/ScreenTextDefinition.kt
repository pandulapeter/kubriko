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
 * A text a Manager type shows on top of the game, drawn by the generated Manager's `Composable`.
 *
 * @param color The text's color as `0xAARRGGBB`, stored as `"#RRGGBB"` ([HexColorSerializer]).
 * @param shownWhen What has to be true for the text to show, or null to show it always. Only conditions on Properties
 *   ([isShownWhenAllowed]), which are Compose state, so the text appears and goes the moment they change.
 */
@Serializable
@Description("A text shown on top of the game, in a corner, along an edge, or in the middle of the screen.")
public data class ScreenTextDefinition(
    @Description("The text. {name} is replaced by the value of the Manager's Property called name, and {Other.name} by a Property of the Manager type Other, as in \"Score: {score}\".")
    @SerialName("text") val text: String = "",
    @Description("Where on the screen it is shown.")
    @SerialName("position") val position: ScreenPosition = ScreenPosition.TOP_LEFT,
    @Description("The size of its letters.")
    @SerialName("size") val size: Float = 24f,
    @Description("Its color, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
    @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long = 0xFF202020,
    @Description("Its letters' family.")
    @SerialName("font") val font: TextFont = TextFont.DEFAULT,
    @Description("Whether its letters are bold.")
    @SerialName("isBold") val isBold: Boolean = false,
    @Description("Whether its letters are italic.")
    @SerialName("isItalic") val isItalic: Boolean = false,
    @Description("Shows it only while this number, text, or toggle condition holds (its number fixed or a Property); left out to show it always.")
    @SerialName("shownWhen") val shownWhen: ConditionDefinition? = null,
) {
    public companion object {
        private const val DEFAULT_PROPERTY_NAME = "score"

        /**
         * Whether a text can be shown only while [condition] holds: a condition on a Property, whose number (if it
         * compares one) is fixed or another Property's, since those are what the text's `Composable` follows. A random
         * number would change at every recomposition, and an edge isn't Compose state.
         */
        public fun isShownWhenAllowed(condition: ConditionDefinition): Boolean = ConditionKind.of(condition).isAvailableForTexts &&
            condition.numberValues.all { it is NumberValue.Fixed || it is NumberValue.Property }

        /**
         * The condition a text starts with when it is made to show only while one holds: on the first toggle Property of
         * [properties], or else the first number or text one, so that it names a Property from the start, or a toggle
         * condition with no Property chosen when there is none, which the health checks point out.
         */
        public fun defaultConditionFor(properties: List<PropertyDefinition>): ConditionDefinition {
            val property = properties.firstOrNull { it is PropertyDefinition.Toggle }
                ?: properties.firstOrNull { it is PropertyDefinition.Number }
                ?: properties.firstOrNull { it is PropertyDefinition.Text }
            return when (property) {
                is PropertyDefinition.Toggle -> ConditionDefinition.Toggle(property.name)
                is PropertyDefinition.Number -> ConditionDefinition.Number(property.name)
                is PropertyDefinition.Text -> ConditionDefinition.Text(property.name)
                is PropertyDefinition.Color, null -> ConditionDefinition.Toggle()
            }
        }

        /** A new text showing the Manager's first Property ([properties]), or `{score}` when it has none yet. */
        public fun defaultFor(properties: List<PropertyDefinition>): ScreenTextDefinition = ScreenTextDefinition(text = "{${properties.firstOrNull()?.name ?: DEFAULT_PROPERTY_NAME}}")
    }
}
