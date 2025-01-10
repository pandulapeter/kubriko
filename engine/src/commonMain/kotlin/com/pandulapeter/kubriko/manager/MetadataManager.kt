package com.pandulapeter.kubriko.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class MetadataManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "MetadataManager",
) {
    abstract val fps: StateFlow<Float>
    abstract val totalRuntimeInMilliseconds: StateFlow<Long>
    abstract val activeRuntimeInMilliseconds: StateFlow<Long>

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): MetadataManager = MetadataManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}