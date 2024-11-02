package com.pandulapeter.kubriko.sceneSerializer

import com.pandulapeter.kubriko.sceneSerializer.implementation.SceneSerializerImpl
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableMetadata
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
interface SceneSerializer {

    val registeredTypeIds: Set<String>

    fun getTypeId(type: KClass<out Editable<*>>): String?

    fun getType(typeId: String): KClass<out Editable<*>>?

    suspend fun serializeActors(actors: List<Editable<*>>): String

    suspend fun deserializeActors(serializedStates: String): List<Editable<*>>

    companion object {

        fun newInstance(
            vararg editableMetadata: EditableMetadata<out Editable<*>>,
        ): SceneSerializer = SceneSerializerImpl(
            editableMetadata = editableMetadata,
        )
    }
}