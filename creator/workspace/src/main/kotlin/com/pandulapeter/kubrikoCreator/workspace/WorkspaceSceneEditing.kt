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
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesController
import com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorTypeLook
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneClassesBuild
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneSources
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The Scene editor's builds, run by the workspace's task runner after the type changes are written.
 */
internal class WorkspaceSceneClasses(
    private val tasks: CreatorTaskRunner,
    private val incrementalCompilation: IncrementalCompilationPolicy,
    override val classpathFile: File,
    private val blockTypes: BlockTypesController?,
) : SceneClassesBuild {
    override suspend fun runTask(taskName: String, properties: Map<String, String>) = tasks.run(taskName, properties)

    override fun requestCleanBuild() = incrementalCompilation.requestCleanBuild()

    override suspend fun warmUpCompiler() = tasks.warmUpCompiler()

    override suspend fun awaitWrites() {
        blockTypes?.awaitWrites()
    }
}

/**
 * What the open Scene is made of, read from the workspace's Scene store, resource scan, and block types.
 */
internal class WorkspaceSceneSources(
    private val scenes: SceneStore,
    private val resources: StateFlow<Map<ResourceGroup, List<ProjectResource>>>,
    private val blockTypes: BlockTypesController?,
) : SceneSources {
    override fun readScene(scene: ProjectResource) = scenes.actors(SceneStore.nameOf(scene.file)).orEmpty()

    override fun readSceneProblem(scene: ProjectResource) = scenes.problemOf(SceneStore.nameOf(scene.file))

    override fun spriteFiles() = SpriteFiles.of(resources.value).filesByAccessor

    override fun actorTypeLooks() = ActorTypeLook.of(blockTypes?.actorTypes?.value.orEmpty(), SpriteFiles.of(resources.value).files)
}
