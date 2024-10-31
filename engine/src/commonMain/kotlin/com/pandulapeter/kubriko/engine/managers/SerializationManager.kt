package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.actor.Actor
import com.pandulapeter.kubriko.engine.actor.traits.AvailableInEditor
import kotlin.reflect.KClass

interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String

    suspend fun serializeInstanceStates(instanceStates: List<AvailableInEditor.State<out Actor>>): String

    suspend fun deserializeInstanceStates(serializedStates: String): List<AvailableInEditor.State<out Actor>>
}