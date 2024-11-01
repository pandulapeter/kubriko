package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.actor.Actor
import com.pandulapeter.kubriko.engine.actor.traits.Editable
import kotlin.reflect.KClass

interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String

    suspend fun serializeInstanceStates(instanceStates: List<Editable.State<out Actor>>): String

    suspend fun deserializeInstanceStates(serializedStates: String): List<Editable.State<out Actor>>
}