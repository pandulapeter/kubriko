package com.pandulapeter.kubriko.engine.editorIntegration

import com.pandulapeter.kubriko.engine.traits.Editable
import kotlin.reflect.KClass

data class EditableActorMetadata<T : Editable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Editable.State<T>,
    val type: KClass<T>,
) {

    companion object {
        inline operator fun <reified T : Editable<T>> invoke(
            typeId: String,
            noinline deserializeState: (String) -> Editable.State<T>,
        ): EditableActorMetadata<T> = EditableActorMetadata(
            typeId = typeId,
            deserializeState = deserializeState,
            type = T::class,
        )
    }
}