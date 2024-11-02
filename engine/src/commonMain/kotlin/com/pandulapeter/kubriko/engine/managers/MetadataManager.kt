package com.pandulapeter.kubriko.engine.managers

import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface MetadataManager {
    val fps: StateFlow<Float>
    val runtimeInMilliseconds: StateFlow<Long>
}