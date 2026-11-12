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
public data class AudioFiles(
    val sounds: List<String> = emptyList(),
    val music: List<String> = emptyList(),
) {
    public companion object {

        public fun of(resources: Map<ResourceGroup, List<ProjectResource>>): AudioFiles = AudioFiles(
            sounds = pathsOf(resources[ResourceGroup.SOUNDS].orEmpty()),
            music = pathsOf(resources[ResourceGroup.MUSIC].orEmpty()),
        )

        /**
         * The sounds and music among [paths] (from `composeResources`), told apart by [ResourceKind.of].
         */
        public fun ofPaths(paths: Collection<String>): AudioFiles = AudioFiles(
            sounds = paths.filter { ResourceKind.of(it) == ResourceKind.SOUND },
            music = paths.filter { ResourceKind.of(it) == ResourceKind.MUSIC },
        )

        /**
         * The audio files of [resources] once the project was scanned, or null before: a project always has a build
         * file, so no resources at all means none were scanned yet, rather than that every audio file is gone.
         */
        public fun ofScanned(resources: Map<ResourceGroup, List<ProjectResource>>): AudioFiles? = resources.takeIf { it.isNotEmpty() }?.let(::of)

        /**
         * The path of [resource] from its `composeResources` folder, or null if the game can't read it by its path.
         */
        internal fun pathOf(resource: ProjectResource) = ResourceKind.pathFromComposeResources(resource.relativePath)?.takeIf { it.startsWith("$FILES/") }

        private fun pathsOf(resources: List<ProjectResource>) = resources.mapNotNull(::pathOf).distinct().sorted()

        private const val FILES = "files"
    }
}
