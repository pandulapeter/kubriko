package com.pandulapeter.kubriko.serialization

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.ImmutableSet
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
sealed class SerializationManager<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

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
        ): SerializationManager<MD, T> = SerializationManagerImpl(
            serializableMetadata = serializableMetadata,
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}