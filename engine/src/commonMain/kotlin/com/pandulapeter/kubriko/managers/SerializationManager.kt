package com.pandulapeter.kubriko.managers

import com.pandulapeter.kubriko.traits.Editable
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
// TODO: Extract into a plugin
interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String?

    suspend fun serializeActors(actors: List<Editable<*>>): String

    suspend fun deserializeActors(serializedStates: String): List<Editable<*>>
}