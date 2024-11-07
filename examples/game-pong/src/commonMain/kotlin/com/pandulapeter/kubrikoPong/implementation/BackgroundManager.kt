package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.collection.FractalShader

internal class BackgroundManager : Manager() {

    private lateinit var shaderManager: ShaderManager
    private lateinit var metadataManager: MetadataManager

    override fun onInitialize(kubriko: Kubriko) {
        shaderManager = kubriko.require()
        metadataManager = kubriko.require()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        shaderManager.add(
            FractalShader((metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
        )
    }
}