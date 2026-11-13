/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.foundation.ColorHex

/**
 * The parts of Actor types and Manager types written as short lines, as [BlockNotation] writes their Events and Blocks.
 */
internal object TypeNotation {

    fun appearanceOf(type: ActorTypeDefinition) = buildString {
        append(type.shape.name.lowercase())
        append(", ")
        append(BlockNotation.number(type.width))
        append('×')
        append(BlockNotation.number(type.height))
        append(", ")
        append(ColorHex.format(type.color))
        type.sprite?.let { append(", sprite ").append(it) }
        if (type.staysInsideScene) append(", stays inside the Scene")
    }

    fun of(property: PropertyDefinition) = when (property) {
        is PropertyDefinition.Number -> "${property.name}: number = ${BlockNotation.number(property.initialValue)}"
        is PropertyDefinition.Text -> "${property.name}: text = \"${property.initialValue}\""
        is PropertyDefinition.Toggle -> "${property.name}: toggle = ${if (property.initialValue) "on" else "off"}"
    }

    /**
     * The Event at [index] as [detail] asks: its title with the number of its Blocks, or with the Blocks themselves.
     */
    fun of(index: Int, event: EventDefinition, detail: ReadDetail) = "$index " + when (detail) {
        ReadDetail.OUTLINE -> BlockNotation.of(event) + " (" + event.blocks.size + if (event.blocks.size == 1) " Block)" else " Blocks)"
        ReadDetail.BLOCKS, ReadDetail.JSON -> BlockNotation.withBlocks(event)
    }

    fun of(index: Int, text: ScreenTextDefinition) =
        "$index \"${text.text}\" (${ScreenPosition.serializer().descriptor.getElementName(text.position.ordinal)}, ${BlockNotation.number(text.size)}, ${ColorHex.format(text.color)})"
}
