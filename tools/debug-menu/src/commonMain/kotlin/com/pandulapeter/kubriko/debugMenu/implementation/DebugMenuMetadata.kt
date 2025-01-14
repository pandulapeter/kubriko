package com.pandulapeter.kubriko.debugMenu.implementation

internal data class DebugMenuMetadata(
    val kubrikoInstanceName: String = "",
    val fps: Float = 0f,
    val totalActorCount: Int = 0,
    val visibleActorWithinViewportCount: Int = 0,
    val playTimeInSeconds: Long = 0,
    val isDebugOverlayEnabled: Boolean = false,
)