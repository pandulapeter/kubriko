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
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
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
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PropertyCommandsTest {
    private val score = PropertyDefinition.Number("score", 5f)
    private val title = PropertyDefinition.Text("title")
    private val player = ActorTypeDefinition(
        name = "Player",
        properties = listOf(score, title),
        events = listOf(
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", 2f), BlockDefinition.Log("Score: {score}, {scoreboard}"))),
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.If(ConditionDefinition.Number("score", value = 3f)))),
        ),
    )

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg actors: SceneActorEntry) = commandContext(player).apply {
        scenes.write("main", SceneDocument.format(actors.toList()))
        scenes.takeChanges()
    }

    private fun CommandContext.player() = blockTypes!!["Player"]!!

    private fun CommandContext.states() = SceneDocument.parse(scenes.read("main")!!).map { it.state }

    @Test
    fun onlyChangesThatKeepAPropertysNameAreUndoneTogether() {
        val typed = EditProperty.mergeKey(EditProperty.Input("Player", "score", PropertyDefinition.Number("score", 1f)))
        assertEquals(typed, EditProperty.mergeKey(EditProperty.Input("Player", "score", PropertyDefinition.Number("score", 12f))))
        assertEquals(null, EditProperty.mergeKey(EditProperty.Input("Player", "score", PropertyDefinition.Number("points", 5f))))
        assertEquals(false, typed == EditProperty.mergeKey(EditProperty.Input("Player", "lives", PropertyDefinition.Number("lives", 1f))))
    }

    @Test
    fun propertiesAreAddedEditedAndRemovedAndEachIsUndone() = runTest {
        val context = context()
        val lives = PropertyDefinition.Number("lives", 3f)
        assertEquals(listOf(score, lives, title), context.assertRoundTrip(Invocation(AddProperty, AddProperty.Input("Player", lives, index = 1))) { context.player().properties })
        assertEquals(listOf(score, PropertyDefinition.Number("lives", 5f), title), context.assertRoundTrip(Invocation(EditProperty, EditProperty.Input("Player", "lives", PropertyDefinition.Number("lives", 5f)))) { context.player().properties })
        assertEquals(listOf(score, title), context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Player", "lives"))) { context.player().properties })
    }

    @Test
    fun renamingMovesTheBlocksLogMessagesAndSceneValuesAlong() = runTest {
        val context = context(
            SceneActorEntry("Player", """{"score":12.0,"title":"Hi"}"""),
            SceneActorEntry("Other", """{"score":7.0}"""),
        )
        val renamed = context.assertRoundTrip(Invocation(EditProperty, EditProperty.Input("Player", "score", PropertyDefinition.Number("points", 5f)))) { context.player() to context.states() }
        assertEquals(listOf(PropertyDefinition.Number("points", 5f), title), renamed.first.properties)
        assertEquals(listOf(BlockDefinition.ChangeNumber("points", 2f), BlockDefinition.Log("Score: {points}, {scoreboard}")), renamed.first.events[0].blocks)
        assertEquals(listOf(BlockDefinition.If(ConditionDefinition.Number("points", value = 3f))), renamed.first.events[1].blocks)
        assertEquals(listOf("""{"points":12.0,"title":"Hi"}""", """{"score":7.0}"""), renamed.second)
        assertEquals(setOf(SceneChange.Written("main", isNew = false)), context.scenes.takeChanges().toSet())
    }

    @Test
    fun removingKeepsSceneValuesSoUndoingBringsThemBackButAddingDropsThoseOfAnotherKind() = runTest {
        val context = context(SceneActorEntry("Player", """{"score":12.0}"""))
        Invocation(RemoveProperty, RemoveProperty.Input("Player", "score")).apply(context)
        assertEquals(listOf("""{"score":12.0}"""), context.states())
        assertEquals(listOf(BlockDefinition.Log("Score: score, {scoreboard}")), context.player().events[0].blocks)
        Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Text("score"))).apply(context)
        assertEquals(listOf("{}"), context.states())
    }

    @Test
    fun addingKeepsSceneValuesOfTheRightKind() = runTest {
        val context = context(SceneActorEntry("Player", """{"lives":2,"isShielded":true,"name":"Ann"}"""))
        Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Number("lives"))).apply(context)
        Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Toggle("isShielded"))).apply(context)
        Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Text("name"))).apply(context)
        assertEquals(listOf("""{"lives":2,"isShielded":true,"name":"Ann"}"""), context.states())
        assertEquals(emptyList(), context.scenes.takeChanges())
    }

    @Test
    fun propertyChangesAreValidated() = runTest {
        val context = context()
        assertEquals(Rejection(NameProblem.PROPERTY_TAKEN), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Toggle("tiTLE"))))
        assertEquals(Rejection(NameProblem.PROPERTY_CHARACTERS), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Toggle("is shielded"))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Number("lives", Float.NaN))))
        assertEquals(Rejection(CommandProblem.PROPERTY_UNKNOWN, "Player", "lives"), RemoveProperty.validate(context, RemoveProperty.Input("Player", "lives")))
        assertEquals(Rejection(CommandProblem.PROPERTY_KIND_CHANGED), EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Text("score"))))
        assertEquals(Rejection(NameProblem.PROPERTY_TAKEN), EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Number("title"))))
        assertNull(EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Number("score", 1f))))
    }

    @Test
    fun aRenameIsAStepOfItsOwnAndTypingAValueIsOne() = runTest {
        val context = context(SceneActorEntry("Player", """{"score":7.0}""", "p"))
        val executor = CommandExecutor(context, testScope())
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "score", score.copy(name = "speed")))))
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "speed", score.copy(name = "pace")))))
        listOf(8f, 9f).forEach { value ->
            assertIs<CommandOutcome.Success>(executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "pace", score.copy(name = "pace", initialValue = value)))))
        }
        assertEquals(3, executor.history.value.undoable.size)
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(listOf(score.copy(name = "pace"), title), context.player().properties)
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(listOf(score.copy(name = "speed"), title), context.player().properties)
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(player, context.player())
        assertEquals(listOf("""{"score":7.0}"""), context.states())
        repeat(3) { assertIs<CommandOutcome.Success>(executor.step(isUndo = false)) }
        assertEquals(listOf(score.copy(name = "pace", initialValue = 9f), title), context.player().properties)
    }

    @Test
    fun editingAnotherPropertyIsAStepOfItsOwn() = runTest {
        val context = context()
        val executor = CommandExecutor(context, testScope())
        executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "score", score.copy(initialValue = 1f))))
        executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "title", title.copy(initialValue = "Hi"))))
        executor.execute(Invocation(EditProperty, EditProperty.Input("Player", "score", score.copy(initialValue = 2f))))
        assertEquals(3, executor.history.value.undoable.size)
        executor.step(isUndo = true)
        assertEquals(listOf(score.copy(initialValue = 1f), title.copy(initialValue = "Hi")), context.player().properties)
        executor.step(isUndo = true)
        executor.step(isUndo = true)
        assertEquals(player, context.player())
    }

    private var isSceneLocked = false

    @OptIn(EditorMutation::class)
    private fun TestScope.lockableContext(vararg definitions: BlockTypeDefinition, actors: List<SceneActorEntry>) = commandContext(
        *definitions,
        sceneWriteText = { file, text -> if (isSceneLocked) throw IOException("Locked.") else file.writeTextAtomically(text) },
    ).apply {
        scenes.write("main", actors)
        isSceneLocked = true
    }

    @Test
    fun anAddedPropertyWhoseSceneCantBeWrittenIsNotAdded() = runTest {
        val context = lockableContext(player, actors = listOf(SceneActorEntry("Player", """{"lives":"many"}""")))
        val scene = context.scenes.read("main")
        val executor = CommandExecutor(context, testScope())
        assertEquals(
            CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Locked.")),
            executor.execute(Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Number("lives")))),
        )
        assertEquals(player, context.player())
        assertEquals(scene, context.scenes.fileOf("main").readText())
        assertEquals(0, executor.history.value.undoable.size)
    }

    @Test
    fun anEditedPropertyWhoseSceneCantBeWrittenIsNotRenamed() = runTest {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
        val brick = ActorTypeDefinition(
            name = "Brick",
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", 10f, manager = "Game")))),
        )
        val context = lockableContext(brick, game, actors = listOf(SceneActorEntry("Game", """{"score":1.0}""")))
        val executor = CommandExecutor(context, testScope())
        assertEquals(
            CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Locked.")),
            executor.execute(Invocation(EditProperty, EditProperty.Input("Game", "score", PropertyDefinition.Number("points")))),
        )
        assertEquals(game, context.blockTypes!!["Game"])
        assertEquals(brick, context.blockTypes["Brick"])
        assertEquals(0, executor.history.value.undoable.size)
    }

    @Test
    fun undoingAnAddBringsBackTheValuesItDropped() = runTest {
        val context = context(SceneActorEntry("Player", """{"title":"full"}""", "p"))
        val original = context.scenes.read("main")
        val executor = CommandExecutor(context, testScope())
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(RemoveProperty, RemoveProperty.Input("Player", "title"))))
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Number("title")))))
        val added = context.scenes.read("main")
        assertEquals(listOf("{}"), context.states())
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(original, context.scenes.fileOf("main").readText())
        assertEquals(player, context.player())
        assertIs<CommandOutcome.Success>(executor.step(isUndo = false))
        assertIs<CommandOutcome.Success>(executor.step(isUndo = false))
        assertEquals(added, context.scenes.fileOf("main").readText())
        assertEquals(listOf(score, PropertyDefinition.Number("title")), context.player().properties)
    }

    @Test
    fun addingAPropertyThatDropsSceneValuesRoundTrips() = runTest {
        val context = context(SceneActorEntry("Player", """{"lives":"many"}""", "p"))
        val added = context.assertRoundTrip(Invocation(AddProperty, AddProperty.Input("Player", PropertyDefinition.Number("lives")))) { context.player() to context.states() }
        assertEquals(listOf("{}"), added.second)
    }
}
