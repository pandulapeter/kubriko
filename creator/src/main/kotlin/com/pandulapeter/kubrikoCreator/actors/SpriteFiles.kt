/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import javax.imageio.ImageIO

/**
 * The images an Actor type can be drawn as: the bitmaps right in a `drawable` folder of a `composeResources` folder
 * (not a qualified one, whose size would change with the screen's density), whose names make a resource accessor, by
 * their path from `composeResources` (`drawable/ball.png`).
 *
 * @param files The image files by their path.
 */
internal data class SpriteFiles(
    val files: Map<String, File> = emptyMap(),
) {
    val paths get() = files.keys.sorted()

    /**
     * The files by the name of their resource accessor, which is how the game finds them.
     */
    val filesByAccessor get() = files.mapKeys { (path, _) -> accessorOf(path)!! }

    companion object {

        fun of(resources: Map<ResourceGroup, List<ProjectResource>>) = SpriteFiles(
            files = resources[ResourceGroup.SPRITES].orEmpty()
                .mapNotNull { resource -> pathOf(resource)?.let { it to resource.file } }
                .distinctBy { (path, _) -> accessorOf(path) }
                .toMap(),
        )

        /**
         * The name of the Compose resource accessor of the image at [path], as its Gradle plugin makes it (`ball-1`
         * becomes `ball_1`), or null if [path] isn't a sprite file's.
         */
        fun accessorOf(path: String): String? {
            val segments = path.split('/')
            if (segments.size != 2 || segments[0] != DRAWABLE) return null
            val name = segments[1].substringBeforeLast('.', "")
            if (segments[1].substringAfterLast('.', "").lowercase() !in EXTENSIONS) return null
            return name.replace('-', '_').let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }.takeIf(accessorPattern::matches)
        }

        /**
         * The width and height of the image in [file] in pixels, or null if it can't be read.
         */
        fun sizeOf(file: File): Pair<Int, Int>? = try {
            ImageIO.createImageInputStream(file)?.use { input ->
                ImageIO.getImageReaders(input).asSequence().firstOrNull()?.let { reader ->
                    try {
                        reader.input = input
                        reader.getWidth(0) to reader.getHeight(0)
                    } finally {
                        reader.dispose()
                    }
                }
            }
        } catch (_: Exception) {
            null
        }

        private fun pathOf(resource: ProjectResource) = resource.relativePath.substringAfter("$COMPOSE_RESOURCES/", "").takeIf { accessorOf(it) != null }

        private const val COMPOSE_RESOURCES = "composeResources"
        private const val DRAWABLE = "drawable"
        private val EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
        private val accessorPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
