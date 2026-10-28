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
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_upgrade_by_hand
import kubriko.creator.generated.resources.error_command_upgrade_not_needed
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProjectUpgradeCommandsTest {
    private val root = createTempDirectory().toFile()
    private val game = File(root, "src/commonMain/kotlin/com/example/game/Game.kt")
    private val entryPoint = File(root, "src/commonMain/kotlin/com/example/game/GameEntryPoint.kt")

    private fun oldProject() {
        File(root, ".kubriko").mkdirs()
        File(root, ".kubriko/project.json").writeText("""{ "packageName": "com.example.game" }""")
        game.parentFile.mkdirs()
        game.writeText("package com.example.game\n\nimport a.B\n\nval kubriko = Kubriko.newInstance(\n    SceneLoader(scenePath = \"files/scenes/main.json\"),\n)\n")
        File(root, "src/desktopMain/kotlin/com/example/game/Main.kt").apply { parentFile.mkdirs() }.writeText("// Sets KubrikoWiring.isLoggingEnabled.\n")
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
        assertContains(changes[0].jsonObject.getValue("after").jsonPrimitive.content, "    *KubrikoWiring.createManagers(firstScene = \"main\").toTypedArray(),\n)")
        assertEquals("""{"name":"game","usesBlockBuiltActorTypes":true,"actorTypes":[],"managerTypes":[],"scenes":[],"needsUpgrade":true}""", assertIs<CommandOutcome.Success>(GetProjectOverview.read(context, NoInput)).result.toString())
    }

    @Test
    fun upgradingChangesTheFilesAndRecordsTheVersion() = runTest {
        oldProject()
        var upgradedCount = 0
        val context = commandContext(rootDirectory = root, onProjectUpgraded = { upgradedCount++ })
        assertNull(Invocation(UpgradeProject, NoInput).validate(context))
        assertNull(Invocation(UpgradeProject, NoInput).apply(context).inverse)
        assertContains(game.readText(), "import com.example.game.generated.KubrikoWiring\n")
        assertContains(entryPoint.readText(), "    override fun createCustomManagers() = KubrikoWiring.createManagers(isInSceneEditor = true)\n")
        assertContains(File(root, ".kubriko/project.json").readText(), "\"templateVersion\": 5")
        assertEquals(1, upgradedCount)
        assertEquals(Rejection(Res.string.error_command_upgrade_not_needed), Invocation(UpgradeProject, NoInput).validate(context))
    }

    @Test
    fun filesChangedTooMuchCannotBeUpgraded() = runTest {
        oldProject()
        game.writeText("package com.example.game\n")
        entryPoint.delete()
        assertEquals(
            Rejection(Res.string.error_command_upgrade_by_hand, "src/commonMain/kotlin/com/example/game/Game.kt, src/commonMain/kotlin/com/example/game/GameEntryPoint.kt"),
            Invocation(UpgradeProject, NoInput).validate(commandContext(rootDirectory = root)),
        )
    }
}
