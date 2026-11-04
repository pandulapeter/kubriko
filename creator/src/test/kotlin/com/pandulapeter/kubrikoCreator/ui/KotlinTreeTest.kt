/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinTreeTest {
    private fun kotlin(path: String, kotlinPackage: String) = ProjectResource(ResourceGroup.KOTLIN, File(path), path, kotlinPackage)

    private val starter = listOf(
        kotlin("src/commonMain/kotlin/com/game/Game.kt", "com.game"),
        kotlin("src/commonMain/kotlin/com/game/GameEntryPoint.kt", "com.game"),
        kotlin("src/commonMain/kotlin/com/game/ui/feature1/Menu.kt", "com.game.ui.feature1"),
        kotlin("src/commonMain/kotlin/com/game/ui/feature1/state/MenuState.kt", "com.game.ui.feature1.state"),
        kotlin("src/desktopMain/kotlin/com/game/Main.kt", "com.game"),
        kotlin("src/wasmJsMain/kotlin/com/game/Main.kt", "com.game"),
        kotlin("src/iosMain/kotlin/com/game/MainViewController.kt", "com.game"),
        kotlin("src/androidMain/kotlin/com/game/MainActivity.kt", "com.game"),
        kotlin("src/androidMain/kotlin/com/game/android/Permissions.kt", "com.game.android"),
    )

    /**
     * Each row as one line: its depth as indentation, then what it shows.
     */
    private fun List<KotlinTreeRow>.outline() = map { row ->
        "  ".repeat(row.depth) + when (row) {
            is KotlinTreeRow.Container -> "[" + (row.platform?.name ?: row.sourceSet) + "]"
            is KotlinTreeRow.Package -> row.name + "/"
            is KotlinTreeRow.File -> row.resource.name + (row.detail?.let { " ($it)" }.orEmpty())
        }
    }

    @Test
    fun theRootPackageIsTheLongestOneEveryFileShares() {
        assertEquals("com.game", rootPackageOf(starter))
        assertEquals("", rootPackageOf(starter + kotlin("src/commonMain/kotlin/Loose.kt", "")))
        assertEquals("com.game.ui.feature1", rootPackageOf(starter.filter { "ui" in it.relativePath }))
        assertEquals("", rootPackageOf(emptyList()))
        assertEquals("com", rootPackageOf(listOf(kotlin("a/src/main/kotlin/A.kt", "com.gamer"), kotlin("b/src/main/kotlin/B.kt", "com.game"))))
    }

    @Test
    fun platformFilesAreInContainersAndPackagesBelowTheRootAreFoldersMergedWhileTheyHoldNothingElse() {
        assertEquals(
            listOf(
                "[DESKTOP]",
                "  Main.kt",
                "[WEB]",
                "  Main.kt",
                "[ANDROID]",
                "  android/",
                "    Permissions.kt",
                "  MainActivity.kt",
                "[IOS]",
                "  MainViewController.kt",
                "ui.feature1/",
                "  state/",
                "    MenuState.kt",
                "  Menu.kt",
                "Game.kt",
                "GameEntryPoint.kt",
            ),
            kotlinTreeRows(starter, rootPackageOf(starter), toggled = emptySet(), isEverythingOpen = true).outline(),
        )
    }

    @Test
    fun aFolderStopsMergingAtThePackageThatHoldsSomethingElse() {
        val resources = listOf(
            kotlin("src/commonMain/kotlin/game/Game.kt", "game"),
            kotlin("src/commonMain/kotlin/game/ui/Theme.kt", "game.ui"),
            kotlin("src/commonMain/kotlin/game/ui/feature1/state/MenuState.kt", "game.ui.feature1.state"),
        )
        assertEquals(
            listOf("ui/", "  feature1.state/", "    MenuState.kt", "  Theme.kt", "Game.kt"),
            kotlinTreeRows(resources, rootPackageOf(resources), toggled = emptySet(), isEverythingOpen = true).outline(),
        )
        assertEquals("shared/ui.feature1.state", kotlinTreeRows(resources, "game", toggled = emptySet(), isEverythingOpen = true)[1].key)
    }

    @Test
    fun containersStartClosedAndFoldersOpenAndToggledOnesTheOtherWayUnlessEverythingIsOpen() {
        assertEquals(
            listOf("[DESKTOP]", "[WEB]", "[ANDROID]", "[IOS]", "ui.feature1/", "  state/", "    MenuState.kt", "  Menu.kt", "Game.kt", "GameEntryPoint.kt"),
            kotlinTreeRows(starter, "com.game", toggled = emptySet()).outline(),
        )
        val toggled = setOf("container:ANDROID", "shared/ui.feature1")
        val rows = kotlinTreeRows(starter, "com.game", toggled)
        assertEquals(
            listOf("[DESKTOP]", "[WEB]", "[ANDROID]", "  android/", "    Permissions.kt", "  MainActivity.kt", "[IOS]", "ui.feature1/", "Game.kt", "GameEntryPoint.kt"),
            rows.outline(),
        )
        assertEquals(
            mapOf(
                "container:DESKTOP" to false,
                "container:WEB" to false,
                "container:ANDROID" to true,
                "container:ANDROID/android" to true,
                "container:IOS" to false,
                "shared/ui.feature1" to false,
            ),
            rows.mapNotNull { row ->
                when (row) {
                    is KotlinTreeRow.Container -> row.key to row.isExpanded
                    is KotlinTreeRow.Package -> row.key to row.isExpanded
                    is KotlinTreeRow.File -> null
                }
            }.toMap(),
        )
        assertEquals(kotlinTreeRows(starter, "com.game", toggled = emptySet(), isEverythingOpen = true), kotlinTreeRows(starter, "com.game", toggled, isEverythingOpen = true))
    }

    @Test
    fun sourceSetsOfNoPlatformGetContainersNamedAfterThemAfterThePlatformsAndSharedOnesStayOnTop() {
        val resources = listOf(
            kotlin("src/main/kotlin/game/Game.kt", "game"),
            kotlin("src/commonTest/kotlin/game/GameTest.kt", "game"),
            kotlin("src/appleMain/kotlin/game/Apple.kt", "game"),
            kotlin("src/jvmMain/kotlin/game/Jvm.kt", "game"),
            kotlin("scripts/src/Tool.kt", "game.tools"),
        )
        assertEquals(
            listOf("[DESKTOP]", "  Jvm.kt", "[appleMain]", "  Apple.kt", "[commonTest]", "  GameTest.kt", "tools/", "  Tool.kt", "Game.kt"),
            kotlinTreeRows(resources, rootPackageOf(resources), toggled = emptySet(), isEverythingOpen = true).outline(),
        )
    }

    @Test
    fun filesOfTheSameNameInOneFolderAreToldApartByTheirSourceSets() {
        val resources = listOf(
            kotlin("src/iosMain/kotlin/game/Main.kt", "game"),
            kotlin("src/iosArm64Main/kotlin/game/Main.kt", "game"),
            kotlin("src/iosMain/kotlin/game/Other.kt", "game"),
        )
        assertEquals(
            listOf("[IOS]", "  Main.kt (iosArm64Main)", "  Main.kt (iosMain)", "  Other.kt"),
            kotlinTreeRows(resources, "game", toggled = emptySet(), isEverythingOpen = true).outline(),
        )
    }

    @Test
    fun aFileWhosePackageIsOutsideTheRootIsShownByItsWholePackage() {
        val resources = listOf(kotlin("src/commonMain/kotlin/other/Thing.kt", "other.place"))
        assertEquals(listOf("other.place/", "  Thing.kt"), kotlinTreeRows(resources, "com.game", toggled = emptySet(), isEverythingOpen = true).outline())
    }

    @Test
    fun sourceSetsAreMatchedToThePlatformsTheyBelongTo() {
        assertEquals(SourcePlatform.ANDROID, sourcePlatformOf("androidMain"))
        assertEquals(SourcePlatform.DESKTOP, sourcePlatformOf("desktopMain"))
        assertEquals(SourcePlatform.DESKTOP, sourcePlatformOf("jvmMain"))
        assertEquals(SourcePlatform.IOS, sourcePlatformOf("iosSimulatorArm64Main"))
        assertEquals(SourcePlatform.WEB, sourcePlatformOf("wasmJsMain"))
        assertEquals(SourcePlatform.WEB, sourcePlatformOf("jsMain"))
        assertEquals(SourcePlatform.WEB, sourcePlatformOf("webMain"))
        assertNull(sourcePlatformOf("desktopTest"))
        assertNull(sourcePlatformOf("nativeMain"))
    }
}
