package com.pandulapeter.kubriko.debugInfo.implementation

internal data class DebugInfoMetadata(
    val fps: Float = 0f,
    val totalActorCount: Int = 0,
    val visibleActorWithinViewportCount: Int = 0,
    val playTimeInSeconds: Long = 0,
)