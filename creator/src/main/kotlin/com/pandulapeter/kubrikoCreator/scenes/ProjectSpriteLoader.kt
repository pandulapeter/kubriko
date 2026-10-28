/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import java.io.File

/**
 * Loads a project's sprites from its files, for the Scene editor in Kubriko Creator's own window, where Compose
 * resources are read through Creator's class loader, which can't see the project's.
 *
 * @param filesByAccessor The project's sprite files by the name of their resource accessor.
 */
internal class ProjectSpriteLoader(filesByAccessor: Map<String, File>) {

    /**
     * Compose resources are equal when their IDs are, so these stand for the project's own resources of those names.
     */
    @OptIn(InternalResourceApi::class)
    private val files = filesByAccessor.mapKeys { (accessor, _) -> DrawableResource("drawable:$accessor", emptySet()) }

    fun fileOf(resource: DrawableResource) = files[resource]

    suspend fun load(resource: SpriteResource): ImageBitmap? {
        val file = fileOf(resource.drawableResource) ?: return null
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        return decode(bytes, resource.rotation)
    }

    private fun decode(bytes: ByteArray, rotation: SpriteResource.Rotation): ImageBitmap {
        val image = Image.makeFromEncoded(bytes)
        if (rotation == SpriteResource.Rotation.NONE) return image.toComposeImageBitmap()
        val isSideways = rotation == SpriteResource.Rotation.DEGREES_90 || rotation == SpriteResource.Rotation.DEGREES_270
        val width = if (isSideways) image.height else image.width
        val height = if (isSideways) image.width else image.height
        return Surface.makeRasterN32Premul(width, height).use { surface ->
            surface.canvas.translate(width / 2f, height / 2f)
            surface.canvas.rotate(
                when (rotation) {
                    SpriteResource.Rotation.DEGREES_90 -> 90f
                    SpriteResource.Rotation.DEGREES_180 -> 180f
                    else -> 270f
                },
            )
            surface.canvas.drawImage(image, -image.width / 2f, -image.height / 2f)
            surface.makeImageSnapshot().toComposeImageBitmap()
        }
    }
}
