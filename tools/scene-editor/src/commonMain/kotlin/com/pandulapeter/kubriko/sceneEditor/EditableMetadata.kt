package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata.Companion.invoke
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.SerializableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.reflect.KClass

// TODO: Revisit documentation, rename class if needed.
/**
 * Defines the deserialization logic for [Editable] [Actor]s. Should be registered when instantiating [Kubriko].
 * Use the static [invoke] function for a simplified way to create instances.
 *
 * @param type - The unique [String] that defines the type of the Actor.
 * @param deserializeState - This lambda will be invoked to restore the [Serializable.State] of an [Editable] from a [String].
 * The serialization logic is defined in the [Editable] implementation.
 * @param instantiate - TODO
 * @param type - The [KClass] of the [Editable] this metadata refers to.
 */
class EditableMetadata<T : Editable<T>>(
    typeId: String,
    deserializeState: (String) -> Serializable.State<T>,
    val instantiate: (SceneOffset) -> Serializable.State<T>,
    type: KClass<T>,
) : SerializableMetadata<T>(
    typeId = typeId,
    deserializeState = deserializeState,
    type = type,
) {

    companion object {
        /**
         * TODO: Documentation
         */
        fun newSerializationManagerInstance(
            vararg editableMetadata: EditableMetadata<*>,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ) = SerializationManager.newInstance<EditableMetadata<*>, Editable<*>>(
            serializableMetadata = editableMetadata,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )

        /**
         * Simplified way to instantiate [EditableMetadata].
         *
         * @param typeId - The unique [String] that defines the type of the Actor.
         * @param deserializeState - This lambda will be invoked to restore the [Serializable.State] of an [Editable] from a [String].
         * The serialization logic is defined in the [Editable] implementation.
         * @param instantiate - TODO
         */
        inline operator fun <reified T : Editable<T>> invoke(
            typeId: String,
            noinline deserializeState: (String) -> Serializable.State<T>,
            noinline instantiate: (SceneOffset) -> Serializable.State<T>,
        ): EditableMetadata<T> = EditableMetadata(
            typeId = typeId,
            deserializeState = deserializeState,
            instantiate = instantiate,
            type = T::class,
        )
    }
}