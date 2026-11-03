/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.template.TargetHost
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import com.pandulapeter.kubrikoCreator.testing.testAndroidVersions
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TargetCommandsTest {
    private val parentDirectory = createTempDirectory().toFile()
    private val root = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory))
    private val sdk = File(parentDirectory, "sdk").apply { File(this, "platforms").mkdirs() }
    private val targets = GameTargets(
        root,
        testAndroidVersions,
        object : TargetHost {
            override fun androidSdk(rootDirectory: File) = sdk

            override val canBuildIos = true
        },
    )

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    @Test
    fun theTargetsAreReadForTheAi() = runTest {
        val result = assertIs<CommandOutcome.Success>(GetProjectSettings.read(commandContext(rootDirectory = root, targets = targets), NoInput)).result
        assertEquals(
            """[{"target":"desktop","isOn":true,"note":"${TargetProblem.DESKTOP_ALWAYS_ON.english}"},{"target":"web","isOn":true},""" +
                """{"target":"android","isOn":false},{"target":"ios","isOn":false}]""",
            result.jsonObject.getValue("targets").toString(),
        )
    }

    @Test
    fun aChangeIsShownAsTheLinesItChangesAndTheFilesItCreates() = runTest {
        val result = assertIs<CommandOutcome.Success>(GetTargetChange.read(commandContext(rootDirectory = root, targets = targets), android(isOn = true))).result.jsonObject
        assertEquals(
            listOf("androidApp/build.gradle.kts", "androidApp/src/main/AndroidManifest.xml", "local.properties", "src/androidMain/kotlin/com/example/mygame/MainActivity.kt"),
            result.getValue("created").jsonArray.map { it.jsonPrimitive.content },
        )
        val settings = result.getValue("changes").jsonArray.single { it.jsonObject.getValue("path").jsonPrimitive.content == "settings.gradle.kts" }
        assertEquals("…\n  }\n  \n+ include(\":androidApp\")\n+ ", settings.jsonObject.getValue("diff").jsonPrimitive.content)
        assertEquals(newBuild(), File(root, "build.gradle.kts").readText())
    }

    @Test
    fun turningATargetOnIsUndoneByTurningItOff() = runTest {
        val context = commandContext(rootDirectory = root, targets = targets)
        context.assertRoundTrip(Invocation(SetTarget, android(isOn = true))) {
            listOf("build.gradle.kts", "settings.gradle.kts", "gradle/libs.versions.toml").map { File(root, it).readText() }
        }
        assertContains(File(root, "androidApp/build.gradle.kts").readText(), "alias(libs.plugins.android.application)")
    }

    @Test
    fun whatCantBeChangedIsRejected() = runTest {
        assertEquals(Rejection(TargetProblem.DESKTOP_ALWAYS_ON), SetTarget.validate(commandContext(rootDirectory = root, targets = targets), desktopOff))
        assertEquals(Rejection(TargetProblem.CHANGED_BY_HAND, "android"), SetTarget.validate(commandContext(rootDirectory = root), android(isOn = true)))
        val applied = SetTarget.apply(commandContext(rootDirectory = root, targets = targets), GetTargetChange.Input(GameTarget.WEB, isOn = true))
        assertEquals(Rejection(TargetProblem.ALREADY_ON, "web"), applied.failure)
    }

    @Test
    fun theXcodeProjectFollowsTheApplicationId() = runTest {
        val context = commandContext(rootDirectory = root, targets = targets)
        SetTarget.apply(context, GetTargetChange.Input(GameTarget.IOS, isOn = true))
        val refused = SetProjectSettings.apply(context, SetProjectSettings.Input(applicationId = "com.example.my_game"))
        assertEquals(Rejection(TargetProblem.APPLICATION_ID_NOT_FOR_IOS, "com.example.my_game"), refused.failure)
        assertNull(SetProjectSettings.apply(context, SetProjectSettings.Input(applicationId = "com.example.other")).failure)
        assertEquals("PRODUCT_BUNDLE_IDENTIFIER=com.example.other\n", File(root, "iosApp/Configuration/Config.xcconfig").readText())
    }

    private fun newBuild() = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = File(parentDirectory, "other")))
        .resolve("build.gradle.kts").readText()

    private fun android(isOn: Boolean) = GetTargetChange.Input(GameTarget.ANDROID, isOn)

    private val desktopOff = GetTargetChange.Input(GameTarget.DESKTOP, isOn = false)
}
