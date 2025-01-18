/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.ImmutableSet
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
sealed class SerializationManager<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SerializationManager",
) {
    abstract val registeredTypeIds: ImmutableSet<String>

    abstract fun getTypeId(type: KClass<out @UnsafeVariance T>): String?

    abstract fun getMetadata(typeId: String): MD?

    abstract fun serializeActors(actors: List<@UnsafeVariance T>): String

    abstract fun deserializeActors(serializedStates: String): List<T>

    companion object {

        /**
         * TODO: Documentation. Mention the shortcut in Metadata to this generic mess
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