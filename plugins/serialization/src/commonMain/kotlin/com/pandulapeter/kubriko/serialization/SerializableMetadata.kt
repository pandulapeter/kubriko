/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization

import com.pandulapeter.kubriko.actor.Actor
import kotlin.reflect.KClass

/**
 * Defines the deserialization logic for [Serializable] [Actor]s.
 *
 * Instances of this class should be registered when instantiating the [SerializationManager].
 *
 * @param typeId A unique string that identifies the actor type.
 * @param deserializeState A function that restores a [Serializable.State] from a string.
 * @param type The class of the [Serializable] actor this metadata refers to.
 */
open class SerializableMetadata<T : Serializable<T>>(
    val typeId: String,
    val deserializeState: (String) -> Serializable.State<T>,
    val type: KClass<T>,
) {

    companion object {
        /**
         * Creates a new [SerializationManager] instance using the default [SerializableMetadata] and [Serializable] types.
         *
         * This is a convenience shortcut for projects that don't need custom metadata or actor base types.
         *
         * @param serializableMetadata The metadata for all types of actors that can be serialized.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newSerializationManagerInstance(
            vararg serializableMetadata: SerializableMetadata<*>,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ) = SerializationManager.newInstance(
            serializableMetadata = serializableMetadata,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )

        /**
         * A simplified way to instantiate [SerializableMetadata] using reified type parameters.
         *
         * @param typeId A unique string that identifies the actor type.
         * @param deserializeState A function that restores a [Serializable.State] from a string.
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
