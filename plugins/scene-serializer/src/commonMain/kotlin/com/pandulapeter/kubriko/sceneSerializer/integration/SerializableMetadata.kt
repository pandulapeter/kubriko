package com.pandulapeter.kubriko.sceneSerializer.integration

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.sceneSerializer.integration.SerializableMetadata.Companion.invoke
import kotlin.reflect.KClass

// TODO: Revisit documentation, rename class if needed.
/**
 * Defines the deserialization logic for [Editable] [Actor]s. Should be registered when instantiating [Kubriko].
 * Use the static [invoke] function for a simplified way to create instances.
 *
 * @param type - The unique [String] that defines the type of the Actor.
 * @param deserializeState - This lambda will be invoked to restore the [Editable.State] of and [Editable] from a [String].
 * The serialization logic is defined in the [Editable] implementation.
 * @param type - The [KClass] of the [Editable] this metadata refers to.
 */
open class SerializableMetadata<T : Serializable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Serializable.State<T>,
    val type: KClass<T>,
) {

    companion object {
        /**
         * Simplified way to instantiate [SerializableMetadata].
         *
         * @param typeId - The unique [String] that defines the type of the Actor.
         * @param deserializeState - This lambda will be invoked to restore the [Editable.State] of and [Editable] from a [String].
         * The serialization logic is defined in the [Editable] implementation.
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