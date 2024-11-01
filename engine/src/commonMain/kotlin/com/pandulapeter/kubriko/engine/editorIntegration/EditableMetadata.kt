package com.pandulapeter.kubriko.engine.editorIntegration

import com.pandulapeter.kubriko.engine.traits.Editable
import kotlin.reflect.KClass

data class EditableMetadata<T: Editable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Editable.State<T>,
    val type: KClass<T>,
)

inline  fun <reified T: Editable<T>> EditableMetadata(
    typeId: String,
    noinline deserializeState: (String) -> Editable.State<T>,
) : EditableMetadata<T> = EditableMetadata(
    typeId = typeId,
    deserializeState = deserializeState,
    type = T::class,
)