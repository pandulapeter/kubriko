package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.traits.Editable
import kotlin.reflect.KClass

interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String?

    suspend fun serializeActors(actors: List<Editable<*>>): String

    suspend fun deserializeActors(serializedStates: String): List<Editable<*>>
}