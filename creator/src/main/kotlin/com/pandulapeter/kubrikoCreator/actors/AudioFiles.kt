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

/**
 * The sound and music files the generated code can play: those in the `files` folder of a `composeResources` folder,
 * by their path from `composeResources` (`files/sounds/hit.wav`), which is how the game reads them.
 */
internal data class AudioFiles(
    val sounds: List<String> = emptyList(),
    val music: List<String> = emptyList(),
) {
    companion object {

        fun of(resources: Map<ResourceGroup, List<ProjectResource>>) = AudioFiles(
            sounds = pathsOf(resources[ResourceGroup.SOUNDS].orEmpty()),
            music = pathsOf(resources[ResourceGroup.MUSIC].orEmpty()),
        )

        /**
         * The path of [resource] from its `composeResources` folder, or null if the game can't read it by its path.
         */
        fun pathOf(resource: ProjectResource) = resource.relativePath.substringAfter("$COMPOSE_RESOURCES/", "").takeIf { it.startsWith("$FILES/") }

        private fun pathsOf(resources: List<ProjectResource>) = resources.mapNotNull(::pathOf).distinct().sorted()

        private const val COMPOSE_RESOURCES = "composeResources"
        private const val FILES = "files"
    }
}
