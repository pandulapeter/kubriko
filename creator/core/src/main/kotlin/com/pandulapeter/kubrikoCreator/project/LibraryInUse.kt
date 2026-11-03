/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import java.io.File

/**
 * A Kubriko library the game's build depends on, as [KubrikoBuildFiles] decides from its sources, and why.
 *
 * @param uses What the Block-built types and the Scenes do that needs it.
 * @param handWrittenFiles The Kotlin files outside the generated code that use it, by their path from the project's
 *   folder.
 * @param isUsedByGeneratedCode Whether the code Kubriko Creator generates uses it.
 */
public data class LibraryInUse(
    val artifact: KubrikoArtifact,
    val uses: List<PluginUse> = emptyList(),
    val handWrittenFiles: List<String> = emptyList(),
    val isUsedByGeneratedCode: Boolean = false,
) {
    /**
     * Whether every game uses it, whatever it does.
     */
    val isAlwaysUsed: Boolean get() = artifact in KubrikoArtifact.ALWAYS_USED

    public companion object {
        private const val SOURCE_DIRECTORY = "src"
        private const val GENERATED_DIRECTORY = "generated"

        /**
         * Every library the project in [rootDirectory] uses, in [KubrikoArtifact]'s order: those its sources name, as its
         * build does, with [definitions] and the [scenes] saying why. Reads every Kotlin file and Scene, so it is called
         * off the main thread.
         */
        public fun read(rootDirectory: File, definitions: List<BlockTypeDefinition>, scenes: SceneStore): List<LibraryInUse> {
            val sources = ProjectTree.walk(File(rootDirectory, SOURCE_DIRECTORY))
                .filter { it.isFile && it.extension == "kt" }
                .associate { it.relativeTo(rootDirectory).invariantSeparatorsPath to it.readText() }
            val sceneMusic = scenes.names().sorted().mapNotNull { scene -> scenes.read(scene)?.let(SceneSettingsState::musicOf)?.let { scene to it } }.toMap()
            return of(sources, PluginUse.of(definitions, sceneMusic))
        }

        /**
         * The libraries the Kotlin [sources] (their texts by their paths) name, with the [uses] of each.
         */
        internal fun of(sources: Map<String, String>, uses: List<PluginUse>): List<LibraryInUse> {
            val namedBy = sources.mapValues { (_, text) -> KubrikoArtifact.namedIn(text) }
            val used = KubrikoArtifact.ALWAYS_USED + namedBy.values.flatten()
            val (generated, handWritten) = namedBy.entries.partition { (path, _) -> GENERATED_DIRECTORY in path.split('/').dropLast(1) }
            return KubrikoArtifact.entries.filter { it in used }.map { artifact ->
                LibraryInUse(
                    artifact = artifact,
                    uses = uses.filter { it.artifact == artifact },
                    handWrittenFiles = handWritten.filter { artifact in it.value }.map { it.key }.sorted(),
                    isUsedByGeneratedCode = generated.any { artifact in it.value },
                )
            }
        }
    }
}
