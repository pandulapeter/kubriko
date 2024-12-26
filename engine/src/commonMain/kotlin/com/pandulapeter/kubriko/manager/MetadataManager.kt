package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.implementation.manager.MetadataManagerImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class MetadataManager : Manager() {

    abstract val fps: StateFlow<Float>
    abstract val totalRuntimeInMilliseconds: StateFlow<Long>
    abstract val activeRuntimeInMilliseconds: StateFlow<Long>

    companion object {
        fun newInstance(): MetadataManager = MetadataManagerImpl()
    }
}