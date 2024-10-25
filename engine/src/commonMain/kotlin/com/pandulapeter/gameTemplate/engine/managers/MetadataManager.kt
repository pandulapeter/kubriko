package com.pandulapeter.gameTemplate.engine.managers

import kotlinx.coroutines.flow.StateFlow

interface MetadataManager {
    val fps: StateFlow<Float>
    val visibleGameObjectCount: StateFlow<Int>
    val totalGameObjectCount: StateFlow<Int>
    val runtimeInMilliseconds: StateFlow<Long>
}