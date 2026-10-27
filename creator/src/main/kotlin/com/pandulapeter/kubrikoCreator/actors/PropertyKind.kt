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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.property_kind_number
import kubriko.creator.generated.resources.property_kind_text
import kubriko.creator.generated.resources.property_kind_toggle
import org.jetbrains.compose.resources.StringResource

/**
 * The kinds of Property, in the order the editor offers them, with the Kotlin type each one becomes.
 *
 * @param defaultName What a new Property of this kind is called, unless the Actor type has one by that name already.
 */
internal enum class PropertyKind(
    val title: StringResource,
    val kotlinType: String,
    val defaultName: String,
    val create: (name: String) -> PropertyDefinition,
) {
    NUMBER(Res.string.property_kind_number, "Float", "value", { PropertyDefinition.Number(it) }),
    TEXT(Res.string.property_kind_text, "String", "label", { PropertyDefinition.Text(it) }),
    TOGGLE(Res.string.property_kind_toggle, "Boolean", "isOn", { PropertyDefinition.Toggle(it) });

    companion object {

        fun of(property: PropertyDefinition) = when (property) {
            is PropertyDefinition.Number -> NUMBER
            is PropertyDefinition.Text -> TEXT
            is PropertyDefinition.Toggle -> TOGGLE
        }
    }
}
