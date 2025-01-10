package com.pandulapeter.kubriko.serialization

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.serialization.SerializableMetadata.Companion.invoke
import kotlin.reflect.KClass

// TODO: Revisit documentation, rename class if needed.
/**
 * Defines the deserialization logic for [Serializable] [Actor]s. Should be registered when instantiating [Kubriko].
 * Use the static [invoke] function for a simplified way to create instances.
 *
 * @param type - The unique [String] that defines the type of the Actor.
 * @param deserializeState - This lambda will be invoked to restore the [Serializable.State] of an [Serializable] from a [String].
 * The serialization logic is defined in the [Serializable] implementation.
 * @param type - The [KClass] of the [Serializable] this metadata refers to.
 */
open class SerializableMetadata<T : Serializable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Serializable.State<T>,
    val type: KClass<T>,
) {

    companion object {
        /**
         * TODO: Documentation
         */
        fun newSerializationManagerInstance(
            vararg serializableMetadata: SerializableMetadata<*>,
            isLoggingEnabled: Boolean = false,
        ) = SerializationManager.newInstance<SerializableMetadata<*>, Serializable<*>>(
            serializableMetadata = serializableMetadata,
            isLoggingEnabled = isLoggingEnabled,
        )

        /**
         * Simplified way to instantiate [SerializableMetadata].
         *
         * @param typeId - The unique [String] that defines the type of the Actor.
         * @param deserializeState - This lambda will be invoked to restore the [Serializable.State] of an [Serializable] from a [String].
         * The serialization logic is defined in the [Serializable] implementation.
         */
        inline operator fun <reified T : Serializable<T>> invoke(
            typeId: String,
            noinline deserializeState: (String) -> Serializable.State<T>,
        ): SerializableMetadata<T> = SerializableMetadata(
            typeId = typeId,
            deserializeState = deserializeState,
            type = T::class,
        )
    }
}