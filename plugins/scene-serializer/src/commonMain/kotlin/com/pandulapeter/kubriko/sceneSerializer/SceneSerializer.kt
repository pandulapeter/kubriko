package com.pandulapeter.kubriko.sceneSerializer

import com.pandulapeter.kubriko.sceneSerializer.implementation.SceneSerializerImpl
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableMetadata
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
interface SceneSerializer {

    // TODO: Rename this
    val typeIdsForEditor: Set<String>

    fun resolveTypeId(type: KClass<*>): String?

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