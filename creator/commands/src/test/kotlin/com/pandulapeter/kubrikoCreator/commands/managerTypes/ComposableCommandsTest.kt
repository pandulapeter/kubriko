/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.GeneratedCodeLocation
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerComposables
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.blockTypesController
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposableCommandsTest {
    private val hud = ManagerTypeDefinition(name = "Hud")
    private val location = GeneratedCodeLocation("src/commonMain/kotlin", "com.example.game")
    private val path = "src/commonMain/kotlin/com/example/game/ui/HudUi.kt"

    private fun TestScope.context(location: GeneratedCodeLocation? = this@ComposableCommandsTest.location) =
        commandContext(blockTypes = blockTypesController(repository = FakeBlockTypeRepository(listOf(hud), codeLocation = location)))

    private fun CommandContext.hud() = blockTypes!!.managerType("Hud")

    private fun CommandContext.file() = File(project.rootDirectory, path)

    @Test
    fun aManagerTypeShowsAComposableNamedInFullOrNone() = runTest {
        val context = context()
        val set = context.assertRoundTrip(Invocation(SetComposable, SetComposable.Input("Hud", "com.example.game.ui.HudUi"))) { context.hud()!!.composable }
        assertEquals("com.example.game.ui.HudUi", set)
        assertNull(context.assertRoundTrip(Invocation(SetComposable, SetComposable.Input("Hud", ""))) { context.hud()!!.composable })
        assertEquals(Rejection(CommandProblem.COMPOSABLE_NAME_INVALID, "HudUi"), Invocation(SetComposable, SetComposable.Input("Hud", "HudUi")).validate(context))
    }

    @Test
    fun aNewComposableIsMadeInTheUiPackageAndShownAndItsUndoRemovesOnlyAnUnchangedFile() = runTest {
        val context = context()
        val made = context.assertRoundTrip(Invocation(CreateComposable, CreateComposable.Input("Hud"))) { context.hud()!!.composable to context.file().takeIf { it.isFile }?.readText() }
        assertEquals("com.example.game.ui.HudUi" to ManagerComposables.textOf(location, "Hud", "HudUi"), made)
        val undo = assertNotNull(CreateComposable.apply(context, CreateComposable.Input("Hud", "HudScreen")).inverse)
        val changed = File(context.project.rootDirectory, "src/commonMain/kotlin/com/example/game/ui/HudScreen.kt").apply { appendText("// Mine\n") }
        undo.apply(context)
        assertEquals("com.example.game.ui.HudUi", context.hud()!!.composable)
        assertTrue(changed.readText().endsWith("// Mine\n"), "A Composable changed since it was made is left to the user.")
    }

    @Test
    fun aComposableIsNeverMadeOverAFileOrWithoutWhereTheCodeIs() = runTest {
        val context = context()
        context.file().apply { parentFile.mkdirs() }.writeText("// Taken\n")
        assertEquals(Rejection(CommandProblem.COMPOSABLE_FILE_TAKEN, path), CreateComposable.apply(context, CreateComposable.Input("Hud")).failure)
        assertEquals("// Taken\n", context.file().readText())
        assertNull(context.hud()!!.composable)
        assertEquals(Rejection(CommandProblem.COMPOSABLE_NAME_INVALID, "Hud Ui"), Invocation(CreateComposable, CreateComposable.Input("Hud", "Hud Ui")).validate(context))
        assertEquals(Rejection(CommandProblem.NO_ACTOR_TYPES), Invocation(CreateComposable, CreateComposable.Input("Hud")).validate(context(location = null)))
        assertFalse(File(context(location = null).project.rootDirectory, path).exists())
    }
}
