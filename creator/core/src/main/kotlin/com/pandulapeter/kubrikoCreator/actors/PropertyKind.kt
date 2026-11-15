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

/**
 * The kinds of Property, in the order the editor offers them, with the Kotlin type each one becomes.
 *
 * @param defaultName What a new Property of this kind is called, unless the Actor type has one by that name already.
 * @param savedType The type a saved state keeps it as, when it isn't [kotlinType]: a Compose `Color` is saved through
 *   Kubriko's serializer for it.
 */
public enum class PropertyKind(
    internal val kotlinType: String,
    public val defaultName: String,
    public val create: (name: String) -> PropertyDefinition,
    internal val savedType: String = kotlinType,
) {
    NUMBER("Float", "value", { PropertyDefinition.Number(it) }),
    TEXT("String", "label", { PropertyDefinition.Text(it) }),
    TOGGLE("Boolean", "isOn", { PropertyDefinition.Toggle(it) }),
    COLOR("Color", "tint", { PropertyDefinition.Color(it) }, savedType = "SerializableColor"),
    ACTOR_TYPE("String", "actorType", { PropertyDefinition.ActorType(it) });

    public companion object {

        public fun of(property: PropertyDefinition): PropertyKind = when (property) {
            is PropertyDefinition.Number -> NUMBER
            is PropertyDefinition.Text -> TEXT
            is PropertyDefinition.Toggle -> TOGGLE
            is PropertyDefinition.Color -> COLOR
            is PropertyDefinition.ActorType -> ACTOR_TYPE
        }
    }
}
