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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectLayoutTest {
    private val root = testDirectory()

    @Test
    fun theTypesAndNewScenesBelongToTheModulesTheirFoldersAreIn() {
        listOf("build.gradle.kts", "game/build.gradle.kts").forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
        File(root, "game/${SceneStore.DEFAULT_SCENE_DIRECTORY}/levels").mkdirs()
        assertEquals(
            ProjectLayout(sceneFolders = emptySet(), typeModule = "game", sceneModule = "", modules = modulesOf("" to false, "game" to false)),
            ProjectLayout.of(root, emptyMap(), generatedPath = "game/src/commonMain/kotlin/com/game/generated"),
        )
        val scene = File(root, "game/${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json").apply { writeText("[]") }
        assertEquals(
            ProjectLayout(sceneFolders = setOf("levels"), typeModule = "game", sceneModule = "game", modules = modulesOf("" to false, "game" to false)),
            ProjectLayout.of(root, mapOf(ResourceGroup.SCENES to listOf(ProjectResource(ResourceGroup.SCENES, scene, scene.relativeTo(root).invariantSeparatorsPath, module = "game"))), "game/src"),
            "The Scenes folder is the one the scan found Scenes in",
        )
    }

    @Test
    fun aProjectWithoutBlockBuiltTypesHasNoTypeModule() {
        File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/levels").mkdirs()
        assertEquals(ProjectLayout(sceneFolders = setOf("levels")), ProjectLayout.of(root, emptyMap(), generatedPath = null))
    }

    @Test
    fun theScenesFolderTheGameLoadsFromIsTheOneNewScenesGoToEvenBeforeItHasAny() {
        listOf("build.gradle.kts", "game/build.gradle.kts", "tools/build.gradle.kts").forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
        val tools = File(root, "tools/${SceneStore.DEFAULT_SCENE_DIRECTORY}/sandbox.json").apply { parentFile.mkdirs(); writeText("[]") }
        val layout = ProjectLayout.of(
            rootDirectory = root,
            resources = mapOf(ResourceGroup.SCENES to listOf(ProjectResource(ResourceGroup.SCENES, tools, tools.relativeTo(root).invariantSeparatorsPath, module = "tools"))),
            generatedPath = "game/src/commonMain/kotlin/com/game/generated",
            sceneHome = File(root, "game/${SceneStore.DEFAULT_SCENE_DIRECTORY}"),
        )
        assertEquals("game", layout.sceneModule)
        assertEquals(emptySet(), layout.sceneFolders)
    }

    private fun modulesOf(vararg modules: Pair<String, Boolean>) = modules.associate { (path, usesKubriko) -> path to ProjectModule(usesKubriko) }
}
