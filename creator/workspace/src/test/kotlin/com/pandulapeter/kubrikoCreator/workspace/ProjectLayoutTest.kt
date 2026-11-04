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
            ProjectLayout(sceneFolders = emptySet(), typeModule = "game", sceneModule = ""),
            ProjectLayout.of(root, emptyMap(), generatedPath = "game/src/commonMain/kotlin/com/game/generated"),
        )
        val scene = File(root, "game/${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json").apply { writeText("[]") }
        assertEquals(
            ProjectLayout(sceneFolders = setOf("levels"), typeModule = "game", sceneModule = "game"),
            ProjectLayout.of(root, mapOf(ResourceGroup.SCENES to listOf(ProjectResource(ResourceGroup.SCENES, scene, scene.relativeTo(root).invariantSeparatorsPath, module = "game"))), "game/src"),
            "The Scenes folder is the one the scan found Scenes in",
        )
    }

    @Test
    fun aProjectWithoutBlockBuiltTypesHasNoTypeModule() {
        File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/levels").mkdirs()
        assertEquals(ProjectLayout(sceneFolders = setOf("levels")), ProjectLayout.of(root, emptyMap(), generatedPath = null))
    }
}
