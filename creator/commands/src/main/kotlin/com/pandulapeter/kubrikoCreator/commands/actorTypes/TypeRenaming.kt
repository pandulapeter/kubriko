/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext

/**
 * Renames the Actor type or Manager type [oldName] to [newName] with the entries of every Scene whose type ID is its
 * name (an Actor type's Actors, or the values Scenes give a Manager type), and the values of Actor type reference
 * Properties the Scenes keep naming it, putting the type's name back when the Scenes can't be written.
 */
@OptIn(EditorMutation::class)
internal suspend fun CommandContext.renameTypeWithItsSceneEntries(oldName: String, newName: String) {
    requireBlockTypes.rename(oldName, newName)
    try {
        val references = actorTypeReferenceProperties()
        withContext(ioDispatcher) { scenes.renameTypeId(oldName, newName, references) }
    } catch (exception: Exception) {
        // Exact, since validate refused a taken name: only references this rename made point to the new one.
        requireBlockTypes.rename(newName, oldName)
        throw exception
    }
}

/**
 * The Actor type reference Properties each type keeps in Scenes (an Actor type with its inherited ones), by the type's
 * name, for the types that have any.
 */
private fun CommandContext.actorTypeReferenceProperties(): Map<String, Set<String>> {
    val types = requireBlockTypes.types.value
    val family = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>())
    return types.associate { type ->
        val properties = if (type is ActorTypeDefinition) family.propertiesOf(type.name) else type.properties
        type.name to properties.filterIsInstance<PropertyDefinition.ActorType>().mapTo(HashSet()) { it.name }
    }.filterValues { it.isNotEmpty() }
}
