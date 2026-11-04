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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.ProposedFileChange
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectUpgradeCommandsTest {
    private val root = testDirectory()
    private val game = File(root, "src/commonMain/kotlin/com/example/game/Game.kt")
    private val entryPoint = File(root, "src/commonMain/kotlin/com/example/game/GameEntryPoint.kt")

    private fun oldProject() {
        File(root, ".kubriko").mkdirs()
        File(root, ".kubriko/project.json").writeText("""{ "packageName": "com.example.game" }""")
        File(root, "gradle.properties").writeText(
            "org.gradle.caching=true\norg.gradle.configuration-cache=true\n" +
                "game.name=Game\ngame.versionName=1.0.0\ngame.versionNumber=1\ngame.applicationId=com.example.game\ngame.vendor=\ngame.description=\n" +
                "game.firstScene=main\ngame.aspectRatio=dynamic\ngame.viewportWidth=1920\ngame.viewportHeight=1080\ngame.frameRate=60\n",
        )
        File(root, "build.gradle.kts").writeText("// Reads game.name and game.firstScene into commonMain.\n")
        game.parentFile.mkdirs()
        game.writeText("package com.example.game\n\nimport a.B\n\nval kubriko = Kubriko.newInstance(\n    SceneLoader(scenePath = \"files/scenes/main.json\"),\n)\n")
        File(root, "src/desktopMain/kotlin/com/example/game/Main.kt").apply { parentFile.mkdirs() }.writeText("// Sets KubrikoWiring.isLoggingEnabled and KubrikoWiring.firstSceneOverride.\n")
        entryPoint.writeText("package com.example.game\n\nclass GameEntryPoint : CreatorEntryPoint {\n\n    override fun createSerializationManager() = GameSerialization.createSerializationManager()\n}\n")
    }

    @Test
    fun theUpgradeIsShownBeforeItIsMade() = runTest {
        oldProject()
        val context = commandContext(rootDirectory = root)
        val upgrade = assertIs<CommandOutcome.Success>(GetProjectUpgrade.read(context, NoInput)).result as JsonObject
        assertEquals("true", upgrade.getValue("isNeeded").jsonPrimitive.content)
        val changes = upgrade.getValue("changes") as JsonArray
        assertEquals(listOf("src/commonMain/kotlin/com/example/game/Game.kt", "src/commonMain/kotlin/com/example/game/GameEntryPoint.kt"), changes.map { it.jsonObject.getValue("path").jsonPrimitive.content })
        assertContains(changes[0].jsonObject.getValue("after").jsonPrimitive.content, "    *KubrikoWiring.createManagers(firstScene = GameSettings.FIRST_SCENE).toTypedArray(),\n)")
        assertEquals("""{"name":"game","usesBlockBuiltActorTypes":true,"actorTypes":[],"managerTypes":[],"scenes":[],"needsUpgrade":true}""", assertIs<CommandOutcome.Success>(GetProjectOverview.read(context, NoInput)).result.toString())
    }

    @Test
    fun aFileThatIsntTextIsShownByItsPathOnly() = runTest {
        oldProject()
        File(root, "src/wasmJsMain/kotlin/com/example/game/Main.kt").apply { parentFile.mkdirs() }.writeText("package com.example.game\n")
        val upgrade = assertIs<CommandOutcome.Success>(GetProjectUpgrade.read(commandContext(rootDirectory = root), NoInput)).result as JsonObject
        val font = (upgrade.getValue("changes") as JsonArray).map { it.jsonObject }.single { it.getValue("path").jsonPrimitive.content.endsWith(".ttf") }
        assertEquals(setOf("path", "added"), font.keys)
        assertEquals("src/wasmJsMain/composeResources/font/public_sans_regular.ttf", font.getValue("path").jsonPrimitive.content)
    }

    @Test
    fun upgradingChangesTheFilesAndRecordsTheVersion() = runTest {
        oldProject()
        var upgradedCount = 0
        val context = commandContext(rootDirectory = root, onProjectUpgraded = { upgradedCount++ }, projectUpgrade = { ProjectUpgrader(root).check() })
        assertNull(Invocation(UpgradeProject, NoInput).validate(context))
        assertTrue(Invocation(UpgradeProject, NoInput).apply(context).let { it.inverse == null && it.isRewritingFiles })
        assertContains(game.readText(), "import com.example.game.generated.KubrikoWiring\n")
        assertContains(entryPoint.readText(), "    override fun createCustomManagers() = KubrikoWiring.createManagers(isInSceneEditor = true)\n")
        assertContains(File(root, ".kubriko/project.json").readText(), "\"templateVersion\": ${ProjectUpgrader.CURRENT_VERSION}")
        assertEquals(1, upgradedCount)
        assertEquals(Rejection(CommandProblem.UPGRADE_NOT_NEEDED), Invocation(UpgradeProject, NoInput).validate(context))
    }

    @Test
    fun filesChangedTooMuchCannotBeUpgraded() = runTest {
        oldProject()
        game.writeText("package com.example.game\n")
        entryPoint.delete()
        assertEquals(
            Rejection(CommandProblem.UPGRADE_BY_HAND, "src/commonMain/kotlin/com/example/game/Game.kt, src/commonMain/kotlin/com/example/game/GameEntryPoint.kt"),
            Invocation(UpgradeProject, NoInput).validate(commandContext(rootDirectory = root, projectUpgrade = { ProjectUpgrader(root).check() })),
        )
    }

    @Test
    fun validatingTrustsTheLastCheckInsteadOfReadingTheFiles() = runTest {
        oldProject()
        val context = commandContext(rootDirectory = root, projectUpgrade = { TemplateUpgrade.UpToDate })
        assertEquals(Rejection(CommandProblem.UPGRADE_NOT_NEEDED), Invocation(UpgradeProject, NoInput).validate(context))
    }

    @Test
    fun applyingChecksAgainWhenTheLastCheckIsStale() = runTest {
        val stale = TemplateUpgrade.Needed(changes = listOf(ProposedFileChange("Game.kt", before = "", after = "")), byHand = emptyList())
        val context = commandContext(rootDirectory = root, projectUpgrade = { stale })
        assertNull(Invocation(UpgradeProject, NoInput).validate(context))
        assertEquals(Applied.failed(CommandProblem.UPGRADE_NOT_NEEDED), Invocation(UpgradeProject, NoInput).apply(context))
        assertEquals(emptyList(), root.listFiles().orEmpty().toList())
    }
}
