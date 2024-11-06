package com.pandulapeter.kubriko.debugMenu.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class DebugMenuController(kubriko: Kubriko) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val debugMenuMetadata = kubriko.require<MetadataManager>().let { metadataManager ->
        kubriko.require<ActorManager>().let { actorManager ->
            combine(
                metadataManager.fps,
                actorManager.allActors,
                actorManager.visibleActorsWithinViewport,
                metadataManager.runtimeInMilliseconds,
            ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds ->
                DebugMenuMetadata(
                    fps = fps,
                    totalActorCount = allActors.count(),
                    visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
                    playTimeInSeconds = runtimeInMilliseconds / 1000,
                )
            }.stateIn(this, SharingStarted.Eagerly, DebugMenuMetadata())
        }
    }
}