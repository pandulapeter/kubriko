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
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.ImmutableSet
import kotlin.reflect.KClass

/**
 * Manager responsible for serializing and deserializing [Actor]s that implement the [Serializable] interface.
 *
 * It uses [SerializableMetadata] to know how to handle different types of actors.
 *
 * @param MD The type of metadata used by this manager.
 * @param T The base type of serializable actors managed by this manager.
 */
sealed class SerializationManager<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SerializationManager",
) {
    /**
     * The set of all registered type IDs.
     */
    abstract val registeredTypeIds: ImmutableSet<String>

    /**
     * Returns the type ID for the given actor type, or null if it's not registered.
     */
    abstract fun getTypeId(type: KClass<out @UnsafeVariance T>): String?

    /**
     * Returns the metadata for the given type ID, or null if it's not registered.
     */
    abstract fun getMetadata(typeId: String): MD?

    /**
     * Serializes a list of actors into a string representation.
     */
    abstract fun serializeActors(actors: List<@UnsafeVariance T>): String

    /**
     * Deserializes a string representation into a list of actors.
     */
    abstract fun deserializeActors(serializedStates: String): List<T>

    companion object {

        /**
         * Creates a new [SerializationManager] instance.
         *
         * For a simpler way to create an instance when using the default [SerializableMetadata],
         * see [SerializableMetadata.newSerializationManagerInstance].
         *
         * @param serializableMetadata The metadata for all types of actors that can be serialized.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun <MD : SerializableMetadata<out T>, T : Serializable<out T>> newInstance(
            vararg serializableMetadata: MD,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): SerializationManager<MD, T> = SerializationManagerImpl(
            serializableMetadata = serializableMetadata,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}