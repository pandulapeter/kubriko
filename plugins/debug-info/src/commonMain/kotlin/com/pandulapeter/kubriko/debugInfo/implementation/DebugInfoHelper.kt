package com.pandulapeter.kubriko.debugInfo.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class DebugInfoHelper(kubriko: Kubriko) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val debugInfoMetadata = kubriko.get<MetadataManager>().let { metadataManager ->
        kubriko.get<ActorManager>().let { actorManager ->
            combine(
                metadataManager.fps,
                actorManager.allActors,
                actorManager.visibleActorsWithinViewport,
                metadataManager.runtimeInMilliseconds,
            ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds ->
                DebugInfoMetadata(
                    fps = fps,
                    totalActorCount = allActors.count(),
                    visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
                    playTimeInSeconds = runtimeInMilliseconds / 1000,
                )
            }.stateIn(this, SharingStarted.Eagerly, DebugInfoMetadata())
        }
    }
}