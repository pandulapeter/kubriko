/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FamilyPropertyCommandsTest {
    private val health = PropertyDefinition.Number("health")
    private val parent = ActorTypeDefinition(name = "Parent", properties = listOf(health))
    private val child = ActorTypeDefinition(
        name = "Child",
        parent = "Parent",
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(BlockDefinition.SetNumber("health", 3f), BlockDefinition.Kotlin("health += 1f"), BlockDefinition.Log("Health: {health}")),
            ),
        ),
    )
    private val grandChild = ActorTypeDefinition(name = "GrandChild", parent = "Child", properties = listOf(PropertyDefinition.Number("health", 5f)))
    private val actors = listOf(
        SceneActorEntry("Parent", """{"health":1.0}""", "p"),
        SceneActorEntry("Child", """{"health":2.0}""", "c"),
        SceneActorEntry("GrandChild", """{"health":3.0}""", "g"),
    )
    private var isSceneLocked = false

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg definitions: BlockTypeDefinition = arrayOf(parent, child, grandChild), actors: List<SceneActorEntry> = this@FamilyPropertyCommandsTest.actors) = commandContext(
        *definitions,
        sceneWriteText = { file, text -> if (isSceneLocked) throw IOException("Locked.") else file.writeTextAtomically(text) },
    ).apply {
        scenes.write("main", SceneDocument.format(actors))
    }

    private fun CommandContext.type(name: String) = blockTypes!!.actorType(name)!!

    private fun CommandContext.states() = SceneDocument.parse(scenes.read("main")!!).map { it.state }

    private fun rename(actorType: String, newName: String, initialValue: Float = 0f) = Invocation(EditProperty, EditProperty.Input(actorType, "health", PropertyDefinition.Number(newName, initialValue)))

    @Test
    fun renamingOnTheDeclarerRenamesTheFamily() = runTest {
        val context = context()
        val renamed = context.assertRoundTrip(rename("Parent", "hp")) { listOf(context.type("Parent"), context.type("Child"), context.type("GrandChild")) to context.states() }
        assertEquals(listOf(PropertyDefinition.Number("hp")), renamed.first[0].properties)
        assertEquals(listOf(BlockDefinition.SetNumber("hp", 3f), BlockDefinition.Kotlin("hp += 1f"), BlockDefinition.Log("Health: {hp}")), renamed.first[1].events[0].blocks)
        assertEquals(listOf(PropertyDefinition.Number("hp", 5f)), renamed.first[2].properties)
        assertEquals(listOf("""{"hp":1.0}""", """{"hp":2.0}""", """{"hp":3.0}"""), renamed.second)
    }

    @Test
    fun aFamilyRenameWhoseSceneCantBeWrittenChangesNoMember() = runTest {
        val context = context()
        val scene = context.scenes.fileOf("main").readText()
        isSceneLocked = true
        val executor = CommandExecutor(context, testScope())
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Locked.")), executor.execute(rename("Parent", "hp")))
        assertEquals(listOf(parent, child, grandChild), listOf(context.type("Parent"), context.type("Child"), context.type("GrandChild")))
        assertEquals(scene, context.scenes.fileOf("main").readText())
    }

    @Test
    fun renamingAnInheritedPropertyIsRefused() = runTest {
        val context = context()
        assertEquals(Rejection(CommandProblem.PROPERTY_INHERITED, "health", "Parent"), EditProperty.validate(context, rename("GrandChild", "hp").input))
        assertNull(EditProperty.validate(context, rename("GrandChild", "health", initialValue = 8f).input))
    }

    @Test
    fun aNewNameTakenInTheFamilyIsRefused() = runTest {
        val context = context(parent, child.copy(properties = listOf(PropertyDefinition.Text("score"))), grandChild)
        assertEquals(Rejection(NameProblem.PROPERTY_TAKEN), EditProperty.validate(context, rename("Parent", "score").input))
    }

    @Test
    fun addingAPropertyADescendantHasOfAnotherKindIsRefused() = runTest {
        val context = context(parent, child.copy(properties = listOf(PropertyDefinition.Number("name"))), grandChild)
        assertEquals(
            Rejection(CommandProblem.PROPERTY_KIND_IN_DESCENDANT, "name", "Child"),
            AddProperty.validate(context, AddProperty.Input("Parent", PropertyDefinition.Text("name"))),
        )
    }

    @Test
    fun addingCleansTheFamilysScenes() = runTest {
        val context = context(actors = listOf(SceneActorEntry("Child", """{"speed":"fast"}""", "c")))
        val original = context.scenes.read("main")
        val executor = CommandExecutor(context, testScope())
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(AddProperty, AddProperty.Input("Parent", PropertyDefinition.Number("speed")))))
        assertEquals(listOf("{}"), context.states())
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(original, context.scenes.read("main"))
        assertEquals(parent, context.type("Parent"))
    }
}
