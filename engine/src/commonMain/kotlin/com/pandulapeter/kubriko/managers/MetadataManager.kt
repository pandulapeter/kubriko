package com.pandulapeter.kubriko.managers

import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface MetadataManager {
    val fps: StateFlow<Float>
    val runtimeInMilliseconds: StateFlow<Long>
}