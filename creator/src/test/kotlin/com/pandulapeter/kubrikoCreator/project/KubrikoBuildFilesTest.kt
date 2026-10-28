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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KubrikoBuildFilesTest {
    private val root = createTempDirectory().toFile()
    private val catalog = File(root, KubrikoBuildFiles.CATALOG_FILE)
    private val build = File(root, KubrikoBuildFiles.BUILD_FILE)
    private val settings = File(root, KubrikoBuildFiles.SETTINGS_FILE)

    init {
        catalog.parentFile.mkdirs()
        catalog.writeText(CATALOG)
        build.writeText(BUILD)
        settings.writeText(SETTINGS)
        source("Game.kt", "import com.pandulapeter.kubriko.Kubriko\nimport com.pandulapeter.kubriko.collision.CollisionManager\nimport com.pandulapeter.kubriko.creator.CreatorEntryPoint\n")
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun source(name: String, text: String) = File(root, "src/commonMain/kotlin/com/example/game/$name").apply {
        parentFile.mkdirs()
        writeText(text)
    }

    private fun sync(version: String = "0.9.0", useMavenLocal: Boolean = false) = KubrikoBuildFiles(root, version, useMavenLocal).sync()

    @Test
    fun filesInTheTemplatesShapeNeedNothingWhenTheyMatchTheCode() {
        assertEquals(KubrikoBuildFileChanges(version = "0.9.0"), sync())
        assertEquals(CATALOG, catalog.readText())
        assertEquals(BUILD, build.readText())
        assertEquals(SETTINGS, settings.readText())
    }

    @Test
    fun aLibraryNothingUsesIsRemovedFromTheBuildAndTheCatalog() {
        source("Game.kt", "import com.pandulapeter.kubriko.Kubriko\nimport com.pandulapeter.kubriko.creator.CreatorEntryPoint\n")
        assertEquals(listOf("plugin-collision"), sync()?.removedArtifacts)
        assertFalse("collision" in build.readText())
        assertFalse("collision" in catalog.readText())
        assertContains(build.readText(), "            implementation(libs.compose.resources)\n            implementation(libs.kubriko.creatorApi)\n")
    }

    @Test
    fun aLibraryTheCodeStartsUsingIsAddedInOrder() {
        source("Ball.kt", "import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware\nval x = com.pandulapeter.kubriko.physics.RigidBody::class\n")
        assertEquals(listOf("plugin-keyboard-input", "plugin-physics"), sync()?.addedArtifacts)
        assertContains(
            build.readText(),
            "            implementation(libs.kubriko.engine)\n            implementation(libs.kubriko.keyboardInput)\n            implementation(libs.kubriko.physics)\n        }\n",
        )
        assertContains(
            catalog.readText(),
            "kubriko-engine = { group = \"io.github.pandulapeter.kubriko\", name = \"engine\", version.ref = \"kubriko\" }\n" +
                "kubriko-keyboardInput = { group = \"io.github.pandulapeter.kubriko\", name = \"plugin-keyboard-input\", version.ref = \"kubriko\" }\n" +
                "kubriko-physics = { group = \"io.github.pandulapeter.kubriko\", name = \"plugin-physics\", version.ref = \"kubriko\" }\n\n[plugins]",
        )
    }

    @Test
    fun librariesTheCreatorDoesNotKnowAndOtherReferencesStay() {
        catalog.writeText(CATALOG.replace("[plugins]", "kubriko-debugMenu = { group = \"io.github.pandulapeter.kubriko\", name = \"tool-debug-menu\", version.ref = \"kubriko\" }\n\n[plugins]"))
        build.writeText(BUILD.replace("        }\n    }\n}", "        }\n        getByName(\"desktopMain\").dependencies {\n            implementation(libs.kubriko.debugMenu)\n            runtimeOnly(libs.kubriko.collision)\n        }\n    }\n}"))
        source("Game.kt", "import com.pandulapeter.kubriko.Kubriko\nimport com.pandulapeter.kubriko.creator.CreatorEntryPoint\n")
        assertEquals(KubrikoBuildFileChanges(version = "0.9.0"), sync())
        assertContains(build.readText(), "implementation(libs.kubriko.debugMenu)")
        assertContains(build.readText(), "runtimeOnly(libs.kubriko.collision)")
        assertFalse("implementation(libs.kubriko.collision)" in build.readText())
        assertContains(catalog.readText(), "kubriko-collision = ")
        assertContains(catalog.readText(), "kubriko-debugMenu = ")
    }

    @Test
    fun anOlderKubrikoVersionIsRaisedToTheCreatorsOwn() {
        val changes = sync(version = "1.2.0")
        assertEquals("0.9.0", changes?.previousVersion)
        assertEquals("1.2.0", changes?.version)
        assertContains(catalog.readText(), "kubriko = \"1.2.0\"\n")
    }

    @Test
    fun aNewerKubrikoVersionIsKept() {
        val changes = sync(version = "0.8.5")
        assertNull(changes?.previousVersion)
        assertTrue(changes?.isNewerThanCreator == true)
        assertContains(catalog.readText(), "kubriko = \"0.9.0\"\n")
    }

    @Test
    fun aDevelopmentBuildAddsMavenLocalAndAReleaseRemovesIt() {
        sync(version = "0.9.0-creator", useMavenLocal = true)
        assertContains(catalog.readText(), "kubriko = \"0.9.0-creator\"\n")
        assertContains(settings.readText(), "    repositories {\n${KubrikoBuildFiles.MAVEN_LOCAL_REPOSITORY}        google()\n")
        sync(version = "0.9.0-creator", useMavenLocal = true)
        assertEquals(1, settings.readText().split("mavenLocal").size - 1)
        assertEquals("0.9.0-creator", sync(version = "0.9.0")?.previousVersion)
        assertEquals(SETTINGS, settings.readText())
    }

    @Test
    fun filesThatDoNotLookLikeTheTemplatesAreLeftAlone() {
        build.writeText(BUILD.replace("commonMain.dependencies {", "dependencies {"))
        source("Ball.kt", "import com.pandulapeter.kubriko.physics.RigidBody\n")
        assertNull(sync(version = "1.2.0"))
        assertContains(catalog.readText(), "kubriko = \"0.9.0\"\n")
        catalog.writeText(CATALOG.replace("kubriko = \"0.9.0\"\n", ""))
        assertNull(sync())
        catalog.delete()
        assertNull(sync())
    }

    @Test
    fun versionsAreComparedByTheirNumbers() {
        assertTrue(KubrikoBuildFiles.compareVersions("0.10.0", "0.9.1") > 0)
        assertTrue(KubrikoBuildFiles.compareVersions("0.8", "0.8.1") < 0)
        assertEquals(0, KubrikoBuildFiles.compareVersions("0.8.1-creator", "0.8.1"))
    }

    @Test
    fun theLibrariesAreFoundByThePackagesTheCodeNames() {
        assertEquals(
            setOf(KubrikoArtifact.ENGINE, KubrikoArtifact.CREATOR_API, KubrikoArtifact.SCENE_EDITOR_API, KubrikoArtifact.SPRITES),
            KubrikoArtifact.usedBy(sequenceOf("import com.pandulapeter.kubriko.sceneEditor.Editable\n// com.pandulapeter.kubriko.sprites.SpriteManager\nimport com.pandulapeter.kubriko.logger.Logger")),
        )
    }

    private companion object {
        val CATALOG = """
            |[versions]
            |compose-multiplatform = "1.12.1"
            |kotlin = "2.4.21"
            |kubriko = "0.9.0"
            |
            |[libraries]
            |compose-resources = { group = "org.jetbrains.compose.components", name = "components-resources", version.ref = "compose-multiplatform" }
            |kubriko-collision = { group = "io.github.pandulapeter.kubriko", name = "plugin-collision", version.ref = "kubriko" }
            |kubriko-creatorApi = { group = "io.github.pandulapeter.kubriko", name = "tool-creator-api", version.ref = "kubriko" }
            |kubriko-engine = { group = "io.github.pandulapeter.kubriko", name = "engine", version.ref = "kubriko" }
            |
            |[plugins]
            |compose = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
            |
        """.trimMargin()

        val BUILD = """
            |kotlin {
            |    sourceSets {
            |        commonMain.dependencies {
            |            implementation(libs.compose.resources)
            |            implementation(libs.kubriko.collision)
            |            implementation(libs.kubriko.creatorApi)
            |            implementation(libs.kubriko.engine)
            |        }
            |    }
            |}
            |
        """.trimMargin()

        val SETTINGS = """
            |rootProject.name = "game"
            |
            |dependencyResolutionManagement {
            |    repositories {
            |        google()
            |        mavenCentral()
            |    }
            |}
            |
        """.trimMargin()
    }
}
