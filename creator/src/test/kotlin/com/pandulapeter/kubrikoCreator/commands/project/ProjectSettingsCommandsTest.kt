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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ProjectSettingsCommandsTest {
    private val root = createTempDirectory().toFile()
    private val properties = File(root, "gradle.properties")
    private val identity = GameIdentity("My Game", "1.0.0", 1, "com.example.mygame", "", "")

    private fun projectReadingItsIdentity() {
        File(root, "build.gradle.kts").writeText("val gameName = providers.gradleProperty(\"game.name\")\n")
        properties.writeText(identity.writtenTo("org.gradle.caching=true\n"))
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun settingsAreReadForTheAi() = runTest {
        projectReadingItsIdentity()
        val result = assertIs<CommandOutcome.Success>(GetProjectSettings.read(commandContext(rootDirectory = root), NoInput)).result
        assertEquals(
            """{"name":"My Game","versionName":"1.0.0","versionNumber":1,"applicationId":"com.example.mygame","vendor":"","description":""}""",
            result.toString(),
        )
    }

    @Test
    fun changingSettingsWritesOnlyThoseAndIsUndone() = runTest {
        projectReadingItsIdentity()
        val context = commandContext(rootDirectory = root)
        val input = SetProjectSettings.Input(name = "Brick Breaker", versionNumber = 2)
        val after = context.assertRoundTrip(Invocation(SetProjectSettings, input)) { properties.readText() }
        assertEquals(identity.copy(name = "Brick Breaker", versionNumber = 2), GameIdentity.of(after))
        assertEquals("org.gradle.caching=true\n", after.lines().first() + "\n")
    }

    @Test
    fun invalidOrEmptyChangesAreRejected() = runTest {
        projectReadingItsIdentity()
        val context = commandContext(rootDirectory = root)
        assertEquals(Rejection(CommandProblem.PROJECT_SETTINGS_EMPTY), Invocation(SetProjectSettings, SetProjectSettings.Input()).validate(context))
        val invalid = Invocation(SetProjectSettings, SetProjectSettings.Input(versionName = "1.0.0.0"))
        assertNull(invalid.validate(context))
        assertEquals(CommandOutcome.Rejected(Rejection(IdentityProblem.VERSION_NAME_INVALID, "1.0.0.0")), CommandExecutor(context, testScope()).execute(invalid))
        assertEquals(identity.writtenTo("org.gradle.caching=true\n"), properties.readText())
    }

    @Test
    fun aProjectWhoseBuildDoesNotReadSettingsSaysToUpgrade() = runTest {
        val context = commandContext(rootDirectory = root)
        val rejection = Rejection(CommandProblem.PROJECT_SETTINGS_UNAVAILABLE)
        val invocation = Invocation(SetProjectSettings, SetProjectSettings.Input(name = "Game"))
        assertNull(invocation.validate(context), "validating reads no files")
        assertEquals(CommandOutcome.Rejected(rejection), CommandExecutor(context, testScope()).execute(invocation))
        assertEquals(CommandOutcome.Rejected(rejection), GetProjectSettings.read(context, NoInput))
    }

    @Test
    fun typingIntoOneFieldIsOneStep() {
        assertEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "A")), SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "AB")))
        assertNotEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "A")), SetProjectSettings.mergeKey(SetProjectSettings.Input(vendor = "A")))
    }
}
