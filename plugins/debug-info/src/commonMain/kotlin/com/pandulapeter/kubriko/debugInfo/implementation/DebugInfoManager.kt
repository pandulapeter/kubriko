package com.pandulapeter.kubriko.debugInfo.implementation

import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class DebugInfoManager(kubriko: Kubriko) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val debugInfoMetadata = combine(
        kubriko.metadataManager.fps,
        kubriko.actorManager.allActors,
        kubriko.actorManager.visibleActorsWithinViewport,
        kubriko.metadataManager.runtimeInMilliseconds,
    ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds ->
        DebugInfoMetadata(
            fps = fps,
            totalActorCount = allActors.count(),
            visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, DebugInfoMetadata())
}