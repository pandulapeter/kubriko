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

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SerializationManagerImpl<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(
    vararg serializableMetadata: MD,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : SerializationManager<MD, T>(isLoggingEnabled, instanceNameForLogging) {
    private val typeIdsToMetadata = serializableMetadata.associateBy { registration -> registration.typeId }.toImmutableMap()
    private val typeIdsToDeserializers = serializableMetadata.associate { registration -> registration.typeId to registration.deserializeState }.toImmutableMap()
    private val typeResolvers = serializableMetadata.associate { registration -> registration.type to registration.typeId }.toImmutableMap()
    override val registeredTypeIds = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun getTypeId(type: KClass<out @UnsafeVariance T>) = typeResolvers[type]

    override fun getMetadata(typeId: String) = typeIdsToMetadata[typeId]

    override fun serializeActors(
        actors: List<@UnsafeVariance T>,
    ) = json.encodeToString(
        actors.mapNotNull { actor ->
            getTypeId(actor::class)?.let { typeId ->
                ActorStateWrapper(
                    typeId = typeId,
                    serializedState = actor.save().serialize(),
                )
            }
        }
    )

    override fun deserializeActors(
        serializedStates: String,
    ) = try {
        json.decodeFromString<List<ActorStateWrapper>>(serializedStates).mapNotNull { wrapper ->
            typeIdsToDeserializers[wrapper.typeId]?.invoke(wrapper.serializedState)?.restore()
        }
    } catch (_: SerializationException) {
        emptyList()
    }

    @kotlinx.serialization.Serializable
    private data class ActorStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}