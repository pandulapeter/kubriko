/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.project.ProjectModule
import com.pandulapeter.kubrikoCreator.project.ProjectModules
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import java.io.File

/**
 * Where in the project Kubriko Creator keeps what it makes, which the resource tree shows besides the files themselves.
 * Modules are named as [com.pandulapeter.kubrikoCreator.project.ProjectResource.module] names them.
 *
 * @param sceneFolders The folders of the Scenes folder new Scenes go to, empty ones too.
 * @param typeModule The module the block-built types' code is generated into, or null for a project without them.
 * @param sceneModule The module new Scenes and Scene folders go to.
 * @param modules Every module of the project ([ProjectModules]), by its folder.
 */
public data class ProjectLayout(
    val sceneFolders: Set<String> = emptySet(),
    val typeModule: String? = null,
    val sceneModule: String = "",
    val modules: Map<String, ProjectModule> = emptyMap(),
) {
    internal companion object {
        /**
         * Reads the layout of the project in [rootDirectory], whose last scan found [resources], from the files as they
         * are now, which blocks. It is read from that scan rather than [SceneStore]'s, so the two are shown together.
         *
         * @param sceneHome The Scenes folder the game loads its Scenes from, when known ([SceneStore]'s `homeDirectory`).
         * @param snapshot The files as the watcher found them for that scan, if it was made from them.
         */
        fun of(
            rootDirectory: File,
            resources: Map<ResourceGroup, List<ProjectResource>>,
            generatedPath: String?,
            sceneHome: File? = null,
            snapshot: ProjectSnapshot? = null,
        ): ProjectLayout {
            val sceneDirectory = SceneStore.newSceneDirectoryOf(rootDirectory, resources[ResourceGroup.SCENES].orEmpty(), sceneHome)
            return ProjectLayout(
                sceneFolders = SceneStore.foldersIn(sceneDirectory),
                typeModule = generatedPath?.let { ResourceScanner.moduleOf(rootDirectory, it) },
                sceneModule = ResourceScanner.moduleOf(rootDirectory, sceneDirectory.relativeTo(rootDirectory).invariantSeparatorsPath),
                modules = ProjectModules.find(rootDirectory, snapshot),
            )
        }
    }
}
