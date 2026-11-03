/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.testing.testAndroidVersions
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameTargetsTest {
    private val parentDirectory = createTempDirectory().toFile()
    private val directory = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory))
    private val sdk = File(parentDirectory, "sdk").apply { File(this, "platforms").mkdirs() }
    private var host = FakeHost(sdk = sdk, canBuildIos = true)
    private val targets get() = GameTargets(directory, testAndroidVersions, host)
    private val buildFile = File(directory, "build.gradle.kts")
    private val newBuild = buildFile.readText()
    private val newSettings = file("settings.gradle.kts").readText()
    private val newCatalog = file("gradle/libs.versions.toml").readText()

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    @Test
    fun aNewProjectIsBuiltForTheDesktopAndTheWeb() {
        assertEquals(
            listOf(
                TargetState(GameTarget.DESKTOP, isOn = true, problem = TargetProblem.DESKTOP_ALWAYS_ON),
                TargetState(GameTarget.WEB, isOn = true),
                TargetState(GameTarget.ANDROID, isOn = false),
                TargetState(GameTarget.IOS, isOn = false),
            ),
            targets.read(),
        )
    }

    @Test
    fun aProjectThatIsntOptedInHasNoTargetsToChange() {
        file(".kubriko/project.json").delete()
        assertEquals(null, targets.read())
        assertEquals(TargetChange.Refused(TargetProblem.CHANGED_BY_HAND, listOf("web")), targets.change(GameTarget.WEB, isOn = false))
    }

    @Test
    fun whatStandsInTheWayIsRead() {
        host = FakeHost(sdk = null, canBuildIos = false)
        assertEquals(listOf(null, TargetProblem.ANDROID_SDK_MISSING, TargetProblem.XCODE_MISSING), targets.read()!!.drop(1).map { it.problem })
    }

    @Test
    fun turningTheWebOffRemovesItsTargetAndThePageTitleAndOnBringsThemBack() {
        val off = turn(GameTarget.WEB, isOn = false)
        assertEquals(listOf("build.gradle.kts"), off.changes.map { it.path })
        val build = buildFile.readText()
        listOf("wasmJs", "ExperimentalWasmDsl", "wasmJsProcessResources").forEach { assertFalse(it in build, it) }
        assertFalse(targets.read()!!.single { it.target == GameTarget.WEB }.isOn)
        assertTrue(file("src/wasmJsMain/resources/index.html").isFile)
        assertTrue(off.isReversible)
        turn(GameTarget.WEB, isOn = true)
        assertEquals(newBuild, buildFile.readText())
        assertTrue(buildFile.readText().contains("    jvm(\"desktop\")\n    @OptIn(ExperimentalWasmDsl::class)\n    wasmJs {\n        browser()\n        binaries.executable()\n    }\n"))
    }

    @Test
    fun turningTheWebOnCreatesItsMissingFiles() {
        turn(GameTarget.WEB, isOn = false)
        file("src/wasmJsMain").deleteRecursively()
        val on = targets.change(GameTarget.WEB, isOn = true)
        assertIs<TargetChange.Proposed>(on)
        assertEquals(
            listOf("build.gradle.kts" to false, "src/wasmJsMain/kotlin/com/example/mygame/Main.kt" to true, "src/wasmJsMain/resources/index.html" to true),
            on.changes.map { it.path to it.isNew },
        )
        assertTrue(targets.apply(on))
        assertContains(file("src/wasmJsMain/resources/index.html").readText(), "src=\"my-game.js\"")
    }

    @Test
    fun turningAndroidOnAddsTheLibraryTargetAndThePackagingModule() {
        val on = turn(GameTarget.ANDROID, isOn = true)
        assertEquals(
            listOf(
                "androidApp/build.gradle.kts" to true,
                "androidApp/src/main/AndroidManifest.xml" to true,
                "build.gradle.kts" to false,
                "gradle/libs.versions.toml" to false,
                "local.properties" to true,
                "settings.gradle.kts" to false,
                "src/androidMain/kotlin/com/example/mygame/MainActivity.kt" to true,
            ),
            on.changes.map { it.path to it.isNew },
        )
        val build = buildFile.readText()
        assertContains(build, "plugins {\n    alias(libs.plugins.android.application) apply false\n    alias(libs.plugins.android.kotlinMultiplatformLibrary)\n")
        assertContains(
            build,
            "    jvm(\"desktop\")\n    android {\n        namespace = \"com.example.mygame\"\n        compileSdk = libs.versions.android.compileSdk.get().toInt()\n" +
                "        minSdk = libs.versions.android.minSdk.get().toInt()\n        androidResources.enable = true\n    }\n",
        )
        assertContains(build, "        }\n        androidMain.dependencies {\n            implementation(libs.androidx.activity.compose)\n        }\n")
        val catalog = file("gradle/libs.versions.toml").readText()
        assertContains(catalog, "[versions]\nandroid-compileSdk = \"37\"\nandroid-gradlePlugin = \"9.5.0\"\nandroid-minSdk = \"29\"\nandroidx-activity = \"1.13.0\"\ncompose-multiplatform")
        assertContains(catalog, "[plugins]\nandroid-application = { id = \"com.android.application\", version.ref = \"android-gradlePlugin\" }\n")
        assertContains(file("settings.gradle.kts").readText(), "}\n\ninclude(\":androidApp\")\n")
        assertEquals("sdk.dir=${sdk.path.replace("\\", "\\\\")}\n", file("local.properties").readText())
        assertContains(file("src/androidMain/kotlin/com/example/mygame/MainActivity.kt").readText(), "package com.example.mygame\n")
        assertTrue(targets.read()!!.single { it.target == GameTarget.ANDROID }.isOn)
    }

    @Test
    fun turningAndroidOffBringsTheBuildFilesBackAndKeepsItsFiles() {
        turn(GameTarget.ANDROID, isOn = true)
        file("androidApp/build.gradle.kts").appendText("// Mine.\n")
        turn(GameTarget.ANDROID, isOn = false)
        assertEquals(newBuild, buildFile.readText())
        assertEquals(newSettings, file("settings.gradle.kts").readText())
        assertEquals(newCatalog, file("gradle/libs.versions.toml").readText())
        val on = turn(GameTarget.ANDROID, isOn = true)
        assertFalse(on.changes.any { it.path == "androidApp/build.gradle.kts" || it.path == "local.properties" })
        assertContains(file("androidApp/build.gradle.kts").readText(), "// Mine.")
    }

    @Test
    fun aLocalPropertiesNamingTheSdkIsKept() {
        file("local.properties").writeText("sdk.dir=${sdk.path.replace("\\", "\\\\").replace(":", "\\:")}\n")
        val on = turn(GameTarget.ANDROID, isOn = true)
        assertFalse(on.changes.any { it.path == "local.properties" })
        assertTrue(on.isReversible)
    }

    @Test
    fun aLocalPropertiesNamingAFolderThatIsntAnSdkIsFixedAndReported() {
        file("local.properties").writeText("# Mine.\nsdk.dir=/nowhere\n")
        turn(GameTarget.ANDROID, isOn = true)
        assertEquals("# Mine.\nsdk.dir=${sdk.path.replace("\\", "\\\\")}\n", file("local.properties").readText())
        file("local.properties").writeText("sdk.dir=/nowhere\n")
        val android = targets.read()!!.single { it.target == GameTarget.ANDROID }
        assertEquals(TargetProblem.ANDROID_SDK_DIRECTORY_INVALID, android.problem)
        assertEquals(listOf("/nowhere"), android.arguments)
    }

    @Test
    fun turningOffWhatTheUserAddedToIsntUndoneByTurningItBack() {
        turn(GameTarget.ANDROID, isOn = true)
        buildFile.writeText(buildFile.readText().replace("implementation(libs.androidx.activity.compose)\n", "implementation(libs.androidx.activity.compose)\n            implementation(\"com.example:mine:1\")\n"))
        assertFalse(turn(GameTarget.ANDROID, isOn = false).isReversible)
    }

    @Test
    fun catalogEntriesSomethingElseUsesOrThatWereThereBeforeStay() {
        val catalog = file("gradle/libs.versions.toml")
        catalog.writeText(
            catalog.readText()
                .replace("[versions]\n", "[versions]\nandroid-minSdk = \"24\"\n")
                .replace("[libraries]\n", "[libraries]\nandroidx-activity-ktx = { group = \"androidx.activity\", name = \"activity-ktx\", version.ref = \"androidx-activity\" }\n"),
        )
        val before = catalog.readText()
        turn(GameTarget.ANDROID, isOn = true)
        assertContains(catalog.readText(), "android-minSdk = \"24\"\n")
        turn(GameTarget.ANDROID, isOn = false)
        assertContains(catalog.readText(), "androidx-activity = \"1.13.0\"\n")
        assertContains(catalog.readText(), "android-minSdk = \"24\"\n")
        assertFalse("android-gradlePlugin" in catalog.readText())
        assertEquals(before, catalog.readText().replace("androidx-activity = \"1.13.0\"\n", ""))
    }

    @Test
    fun turningIosOnAgainGivesTheXcodeProjectTheApplicationId() {
        turn(GameTarget.IOS, isOn = true)
        turn(GameTarget.IOS, isOn = false)
        file("gradle.properties").writeText(file("gradle.properties").readText().replace("game.applicationId=com.example.mygame", "game.applicationId=com.example.other"))
        turn(GameTarget.IOS, isOn = true)
        assertEquals("PRODUCT_BUNDLE_IDENTIFIER=com.example.other\n", file("iosApp/Configuration/Config.xcconfig").readText())
    }

    @Test
    fun aWriteThatFailsPutsBackWhatWasWrittenBeforeIt() {
        val change = assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true))
        file("src/androidMain").writeText("A file where a folder goes.")
        assertFailsWith<IOException> { targets.apply(change) }
        assertEquals(newBuild, buildFile.readText())
        assertEquals(newCatalog, file("gradle/libs.versions.toml").readText())
        assertFalse(file("androidApp/build.gradle.kts").exists())
        assertFalse(file("local.properties").exists())
    }

    @Test
    fun theIdNamesExactlyTheChange() {
        val first = assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true))
        assertEquals(first.id, assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true)).id)
        file("settings.gradle.kts").appendText("// Changed.\n")
        assertNotEquals(first.id, assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true)).id)
    }

    @Test
    fun turningIosOnAddsItsTargetsAndTheXcodeProject() {
        val on = turn(GameTarget.IOS, isOn = true)
        assertEquals(
            listOf(
                ".gitignore",
                "build.gradle.kts",
                "iosApp/Configuration/Config.xcconfig",
                "iosApp/iosApp.xcodeproj/project.pbxproj",
                "iosApp/iosApp/ContentView.swift",
                "iosApp/iosApp/Info.plist",
                "iosApp/iosApp/iOSApp.swift",
                "src/iosMain/kotlin/com/example/mygame/MainViewController.kt",
            ),
            on.changes.map { it.path },
        )
        assertContains(buildFile.readText(), "    jvm(\"desktop\")\n    listOf(\n        iosArm64(),\n        iosSimulatorArm64(),\n    ).forEach { iosTarget ->\n")
        assertContains(buildFile.readText(), "\n\nval writeXcodeGameInfo by tasks.registering {\n")
        assertEquals("PRODUCT_BUNDLE_IDENTIFIER=com.example.mygame\n", file("iosApp/Configuration/Config.xcconfig").readText())
        assertContains(file(".gitignore").readText(), "\nxcuserdata/\n")
        turn(GameTarget.IOS, isOn = false)
        assertEquals(newBuild, buildFile.readText())
        assertTrue(file("iosApp/iosApp.xcodeproj/project.pbxproj").isFile)
    }

    @Test
    fun everyTargetCanBeOnAtOnce() {
        turn(GameTarget.ANDROID, isOn = true)
        turn(GameTarget.IOS, isOn = true)
        turn(GameTarget.WEB, isOn = false)
        assertEquals(listOf(true, false, true, true), targets.read()!!.map { it.isOn })
        turn(GameTarget.ANDROID, isOn = false)
        turn(GameTarget.IOS, isOn = false)
        turn(GameTarget.WEB, isOn = true)
        assertEquals(newBuild, buildFile.readText())
    }

    @Test
    fun anAndroidTargetWithoutAnSdkCantBuild() {
        turn(GameTarget.ANDROID, isOn = true)
        host = FakeHost(sdk = null, canBuildIos = false)
        assertEquals(TargetProblem.ANDROID_SDK_MISSING_WHILE_ON, targets.read()!!.single { it.target == GameTarget.ANDROID }.problem)
        turn(GameTarget.ANDROID, isOn = false)
    }

    @Test
    fun whatCantBeDoneIsRefused() {
        assertEquals(TargetChange.Refused(TargetProblem.DESKTOP_ALWAYS_ON), targets.change(GameTarget.DESKTOP, isOn = false))
        assertEquals(TargetChange.Refused(TargetProblem.ALREADY_ON, listOf("web")), targets.change(GameTarget.WEB, isOn = true))
        assertEquals(TargetChange.Refused(TargetProblem.ALREADY_OFF, listOf("ios")), targets.change(GameTarget.IOS, isOn = false))
        host = FakeHost(sdk = null, canBuildIos = false)
        assertEquals(TargetChange.Refused(TargetProblem.ANDROID_SDK_MISSING), targets.change(GameTarget.ANDROID, isOn = true))
        assertEquals(TargetChange.Refused(TargetProblem.XCODE_MISSING), targets.change(GameTarget.IOS, isOn = true))
    }

    @Test
    fun anApplicationIdWithAnUnderscoreCantGoToIos() {
        file("gradle.properties").writeText(file("gradle.properties").readText().replace("game.applicationId=com.example.mygame", "game.applicationId=com.example.my_game"))
        assertEquals(TargetChange.Refused(TargetProblem.APPLICATION_ID_NOT_FOR_IOS, listOf("com.example.my_game")), targets.change(GameTarget.IOS, isOn = true))
    }

    @Test
    fun aBuildFileChangedByHandIsLeftToTheUser() {
        buildFile.writeText(newBuild.replace("    jvm(\"desktop\")\n", "    jvm(\"desktop\") { }\n"))
        assertEquals(TargetChange.Refused(TargetProblem.CHANGED_BY_HAND, listOf("android")), targets.change(GameTarget.ANDROID, isOn = true))
        buildFile.writeText(newBuild.replace("filter { it.replace(\"@GAME_NAME@\", title.get()) }", "filter { it }"))
        assertEquals(TargetChange.Refused(TargetProblem.CHANGED_BY_HAND, listOf("web")), targets.change(GameTarget.WEB, isOn = false))
    }

    @Test
    fun aProjectThatNeedsAnUpgradeIsUpgradedFirst() {
        file(".kubriko/project.json").writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "templateVersion": 9 }""")
        buildFile.writeText(newBuild.substringBefore("val gameFirstScene"))
        assertEquals(TargetChange.Refused(TargetProblem.TEMPLATE_OUTDATED), targets.change(GameTarget.ANDROID, isOn = true))
    }

    @Test
    fun aChangeIsMadeOnlyIfNoFileChangedSince() {
        val on = assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true))
        file("settings.gradle.kts").appendText("// Changed.\n")
        assertFalse(targets.apply(on))
        assertEquals(newBuild, buildFile.readText())
        assertFalse(file("androidApp").exists())
    }

    @Test
    fun windowsLineBreaksAreKept() {
        buildFile.writeText(newBuild.replace("\n", "\r\n"))
        turn(GameTarget.IOS, isOn = true)
        assertFalse(buildFile.readText().replace("\r\n", "").contains('\n'))
    }

    private fun turn(target: GameTarget, isOn: Boolean): TargetChange.Proposed {
        val change = assertIs<TargetChange.Proposed>(targets.change(target, isOn))
        assertTrue(targets.apply(change))
        return change
    }

    private fun file(path: String) = File(directory, path)

    private class FakeHost(private val sdk: File?, override val canBuildIos: Boolean) : TargetHost {
        override fun androidSdk(rootDirectory: File) = sdk
    }
}
