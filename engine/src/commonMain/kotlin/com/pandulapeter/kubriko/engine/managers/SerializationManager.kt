package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import kotlin.reflect.KClass

interface SerializationManager {

    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String

    suspend fun serializeInstanceStates(instanceStates: List<AvailableInEditor.State<*>>): String

    suspend fun deserializeInstanceStates(serializedStates: String): List<AvailableInEditor.State<*>>
}