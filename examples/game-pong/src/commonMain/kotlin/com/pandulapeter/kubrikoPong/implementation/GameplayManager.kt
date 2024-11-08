package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.collection.CloudShader
import com.pandulapeter.kubrikoPong.implementation.actors.Ball

internal class GameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        actorManager.add(Ball())
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = actorManager.add(
        CloudShader(
            time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f,
            canvasIndex = 1,
        )
    )
}