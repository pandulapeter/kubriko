package com.pandulapeter.kubriko.sprites.helpers

import com.pandulapeter.kubriko.sprites.SpriteResource
import com.pandulapeter.kubriko.sprites.SpriteResource.Rotation
import org.jetbrains.compose.resources.DrawableResource

fun DrawableResource.toSpriteResource(rotation: Rotation = Rotation.NONE) = SpriteResource(this, rotation)