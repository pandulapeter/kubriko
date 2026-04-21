/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata.Companion.invoke
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.SerializableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.reflect.KClass

/**
 * Defines the deserialization and instantiation logic for [Editable] [Actor]s.
 *
 * This class extends [SerializableMetadata] to include an `instantiate` function,
 * which allows the Scene Editor to create new actor instances at a specific location.
 *
 * @param typeId A unique string that identifies the actor type.
 * @param deserializeState A function that restores a [Serializable.State] from a string.
 * @param instantiate A function that creates a new [Serializable.State] for an actor at the given [SceneOffset].
 * @param type The class of the [Editable] actor this metadata refers to.
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
         * Creates a new [SerializationManager] instance configured for [Editable] actors.
         *
         * @param editableMetadata The metadata for all types of actors that can be edited.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
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
         * A simplified way to instantiate [EditableMetadata] using reified type parameters.
         *
         * @param typeId A unique string that identifies the actor type.
         * @param deserializeState A function that restores a [Serializable.State] from a string.
         * @param instantiate A function that creates a new [Serializable.State] for an actor at the given [SceneOffset].
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
