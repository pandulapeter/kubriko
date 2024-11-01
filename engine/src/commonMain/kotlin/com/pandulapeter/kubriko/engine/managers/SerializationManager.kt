package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.traits.Editable
import kotlin.reflect.KClass

interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String

    suspend fun serializeInstanceStates(instanceStates: List<Editable.State<out Editable<*>>>): String

    suspend fun deserializeInstanceStates(serializedStates: String): List<Editable.State<out Editable<*>>>
}