package com.pandulapeter.kubriko.engine.actor

import com.pandulapeter.kubriko.engine.actor.traits.AvailableInEditor
import kotlin.reflect.KClass

data class ActorRegistrationForEditor<T: Actor>(
    val typeId: String,
    val deserializeState: (String) -> AvailableInEditor.State<T>,
    val type: KClass<T>,
)

inline  fun <reified T: Actor> ActorRegistrationForEditor(
    typeId: String,
    noinline deserializeState: (String) -> AvailableInEditor.State<T>,
) = ActorRegistrationForEditor(
    typeId = typeId,
    deserializeState = deserializeState,
    type = T::class,
)