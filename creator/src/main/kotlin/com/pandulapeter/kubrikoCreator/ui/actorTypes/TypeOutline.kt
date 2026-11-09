/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.timerNames

/**
 * What a type's scope and family depend on: everything but its Events and Blocks, apart from the Timers they use.
 */
internal data class TypeOutline(
    val kind: BlockTypeKind,
    val name: String,
    val parent: String?,
    val properties: List<PropertyDefinition>,
    val timerNames: List<String>,
) {
    /**
     * A definition of the type holding only its outline: no Events, and so no Blocks that could go stale while it is kept.
     */
    fun toDefinition(): BlockTypeDefinition = when (kind) {
        BlockTypeKind.ACTOR -> ActorTypeDefinition(name = name, parent = parent, properties = properties)
        BlockTypeKind.MANAGER -> ManagerTypeDefinition(name = name, properties = properties)
    }

    companion object {
        fun of(definition: BlockTypeDefinition) = TypeOutline(
            kind = definition.kind,
            name = definition.name,
            parent = (definition as? ActorTypeDefinition)?.parent,
            properties = definition.properties,
            timerNames = definition.timerNames,
        )
    }
}
