package com.pandulapeter.kubriko.engine.actor

import com.pandulapeter.kubriko.engine.actor.traits.Editable
import kotlin.reflect.KClass

data class EditableMetadata<T: Actor>(
    val typeId: String,
    val deserializeState: (String) -> Editable.State<T>,
    val type: KClass<T>,
)

inline  fun <reified T: Actor> EditableMetadata(
    typeId: String,
    noinline deserializeState: (String) -> Editable.State<T>,
) = EditableMetadata(
    typeId = typeId,
    deserializeState = deserializeState,
    type = T::class,
)