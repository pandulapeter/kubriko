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

package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.AspectRatio
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.GameSettingsProblem
import com.pandulapeter.kubrikoCreator.project.GameSettingsReading
import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
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

    private fun projectReadingItsSettings() {
        File(root, "build.gradle.kts").writeText("val gameName = providers.gradleProperty(\"game.name\")\nval gameFirstScene = providers.gradleProperty(\"game.firstScene\")\n")
        properties.writeText(GameSettings().writtenTo(identity.writtenTo("org.gradle.caching=true\n")))
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
            """{"identity":{"name":"My Game","versionName":"1.0.0","versionNumber":1,"applicationId":"com.example.mygame","vendor":"","description":""},""" +
                """"game":null,"libraries":[{"library":"engine","usedBy":"every game"},{"library":"tool-creator-api","usedBy":"every game"}]}""",
            result.toString(),
        )
    }

    @Test
    fun theGamesSettingsAndLibrariesAreReadForTheAi() = runTest {
        projectReadingItsSettings()
        properties.writeText(properties.readText().replace("game.aspectRatio=dynamic", "game.aspectRatio=fitVertical").replace("game.frameRate=60", "game.frameRate=fast"))
        File(root, "src/commonMain/kotlin/Game.kt").apply { parentFile.mkdirs() }.writeText("import com.pandulapeter.kubriko.physics.PhysicsManager\n")
        File(root, "src/commonMain/kotlin/generated/Ball.kt").apply { parentFile.mkdirs() }.writeText("import com.pandulapeter.kubriko.collision.Collidable\n")
        val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Ball")))
        val result = assertIs<CommandOutcome.Success>(GetProjectSettings.read(commandContext(ball, rootDirectory = root), NoInput)).result.jsonObject
        assertEquals(
            """{"firstScene":"main","aspectRatio":"fitVertical","viewportHeight":1080.0,"frameRate":60,"unreadable":["frameRate"]}""",
            result.getValue("game").toString(),
        )
        assertEquals(
            """[{"library":"engine","usedBy":"every game"},{"library":"plugin-collision","usedBy":["Ball: collision with Ball"]},""" +
                """{"library":"plugin-physics","files":["src/commonMain/kotlin/Game.kt"]},{"library":"tool-creator-api","usedBy":"every game"}]""",
            result.getValue("libraries").toString(),
        )
    }

    @Test
    fun theGamesSettingsChangeWithTheIdentityInOneStep() = runTest {
        projectReadingItsSettings()
        val context = commandContext(rootDirectory = root).apply { scenes.write("levels/1-1", "[]") }
        val input = SetProjectSettings.Input(name = "Brick Breaker", firstScene = "levels/1-1", aspectRatio = AspectRatio.FIXED, viewportWidth = 1280f, frameRate = 0)
        val after = context.assertRoundTrip(Invocation(SetProjectSettings, input)) { properties.readText() }
        assertEquals(identity.copy(name = "Brick Breaker"), GameIdentity.of(after))
        assertEquals(GameSettingsReading(GameSettings("levels/1-1", AspectRatio.FIXED, 1280f, 1080f, GameSettings.EVERY_DISPLAY_FRAME)), GameSettings.of(after))
        assertContains(after, "game.viewportHeight=1080\n")
    }

    @Test
    fun choosingAnAspectRatioWritesTheUnreadableSizeItUses() = runTest {
        projectReadingItsSettings()
        properties.writeText(properties.readText().replace("game.viewportWidth=1920", "game.viewportWidth=wide").replace("game.viewportHeight=1080", "game.viewportHeight=tall"))
        val context = commandContext(rootDirectory = root)
        assertEquals(emptyList(), GameSettings.read(root)?.invalidKeys)
        CommandExecutor(context, testScope()).execute(Invocation(SetProjectSettings, SetProjectSettings.Input(aspectRatio = AspectRatio.FIT_HORIZONTAL)))
        val after = properties.readText()
        assertContains(after, "game.viewportWidth=1920\n")
        assertContains(after, "game.viewportHeight=tall\n")
        assertEquals(emptyList(), GameSettings.read(root)?.invalidKeys)
    }

    @Test
    fun invalidGameSettingsAreRejected() = runTest {
        projectReadingItsSettings()
        val context = commandContext(rootDirectory = root)
        val before = properties.readText()
        assertEquals(Rejection(CommandProblem.SCENE_MISSING, "ghost"), Invocation(SetProjectSettings, SetProjectSettings.Input(firstScene = "ghost")).validate(context))
        val executor = CommandExecutor(context, testScope())
        assertEquals(
            CommandOutcome.Rejected(Rejection(GameSettingsProblem.FRAME_RATE_INVALID, "1000")),
            executor.execute(Invocation(SetProjectSettings, SetProjectSettings.Input(name = "Kept out", frameRate = -1))),
        )
        assertEquals(
            CommandOutcome.Rejected(Rejection(GameSettingsProblem.VIEWPORT_SIZE_INVALID, "100000")),
            executor.execute(Invocation(SetProjectSettings, SetProjectSettings.Input(viewportHeight = 0f))),
        )
        assertEquals(before, properties.readText())
    }

    @Test
    fun aProjectWhoseBuildReadsOnlyItsIdentitySaysToUpgradeForTheGamesSettings() = runTest {
        projectReadingItsIdentity()
        val context = commandContext(rootDirectory = root)
        val outcome = CommandExecutor(context, testScope()).execute(Invocation(SetProjectSettings, SetProjectSettings.Input(name = "Game", frameRate = 30)))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_SETTINGS_UNAVAILABLE)), outcome)
        assertEquals(identity.writtenTo("org.gradle.caching=true\n"), properties.readText())
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
        val result = assertIs<CommandOutcome.Success>(GetProjectSettings.read(context, NoInput)).result.jsonObject
        assertEquals(JsonNull, result.getValue("identity"))
        assertEquals(JsonNull, result.getValue("game"))
    }

    @Test
    fun typingIntoOneFieldIsOneStep() {
        assertEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "A")), SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "AB")))
        assertNotEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(name = "A")), SetProjectSettings.mergeKey(SetProjectSettings.Input(vendor = "A")))
        assertEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(viewportWidth = 1f)), SetProjectSettings.mergeKey(SetProjectSettings.Input(viewportWidth = 12f)))
        assertNotEquals(SetProjectSettings.mergeKey(SetProjectSettings.Input(viewportWidth = 1f)), SetProjectSettings.mergeKey(SetProjectSettings.Input(viewportHeight = 1f)))
    }
}
