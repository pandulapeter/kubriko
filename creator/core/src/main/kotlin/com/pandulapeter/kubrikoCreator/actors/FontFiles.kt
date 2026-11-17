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

/**
 * The fonts a text can be drawn in: the font files right in a `font` folder of a `composeResources` folder (not a
 * qualified one), whose names make a resource accessor, by their path from `composeResources` (`font/pixel.ttf`).
 *
 * @param files The font files by their path.
 */
public data class FontFiles(
    val files: Map<String, File> = emptyMap(),
) {
    public val paths: List<String> get() = files.keys.sorted()

    public companion object {

        public fun of(resources: Map<ResourceGroup, List<ProjectResource>>): FontFiles = FontFiles(ResourceAccessors.filesOf(resources[ResourceGroup.FONTS].orEmpty(), ::accessorOf))

        /**
         * The name of the Compose resource accessor of the font at [path] (`pixel-1` becomes `pixel_1`), or null if
         * [path] isn't a font file's.
         */
        public fun accessorOf(path: String): String? = ResourceAccessors.accessorOf(path, ResourceKind.FONT_FOLDER, ResourceKind.FONT.extensions)
    }
}
