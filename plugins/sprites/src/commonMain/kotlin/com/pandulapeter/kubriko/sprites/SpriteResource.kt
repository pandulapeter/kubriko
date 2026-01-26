package com.pandulapeter.kubriko.sprites

import org.jetbrains.compose.resources.DrawableResource

data class SpriteResource(
    val drawableResource: DrawableResource,
    val rotation: Rotation = Rotation.NONE,
) {
    enum class Rotation {
        NONE, DEGREES_90, DEGREES_180, DEGREES_270
    }
}