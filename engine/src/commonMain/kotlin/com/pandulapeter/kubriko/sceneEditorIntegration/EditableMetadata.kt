package com.pandulapeter.kubriko.sceneEditorIntegration

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.traits.Editable
import kotlin.reflect.KClass

/**
 * Defines the deserialization logic for [Editable] Actors. Should be registered when instantiating [Kubriko].
 * Use the static [invoke] function for a simplified way to create instances.
 *
 * @param type - The unique [String] that defines the type of the Actor.
 * @param deserializeState - This lambda will be invoked to restore the [Editable.State] of and [Editable] from a [String].
 * The serialization logic is defined in the [Editable] implementation.
 * @param type - The [KClass] of the [Editable] this metadata refers to.
 */
// TODO: Extract into a plugin
data class EditableMetadata<T : Editable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Editable.State<T>,
    val type: KClass<T>,
) {

    companion object {
        /**
         * Simplified way to instantiate [EditableMetadata].
         *
         * @param type - The unique [String] that defines the type of the Actor.
         * @param deserializeState - This lambda will be invoked to restore the [Editable.State] of and [Editable] from a [String].
         * The serialization logic is defined in the [Editable] implementation.
         */
        inline operator fun <reified T : Editable<T>> invoke(
            typeId: String,
            noinline deserializeState: (String) -> Editable.State<T>,
        ): EditableMetadata<T> = EditableMetadata(
            typeId = typeId,
            deserializeState = deserializeState,
            type = T::class,
        )
    }
}