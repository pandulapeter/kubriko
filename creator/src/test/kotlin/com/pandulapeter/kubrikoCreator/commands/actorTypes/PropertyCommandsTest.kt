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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_property_kind_changed
import kubriko.creator.generated.resources.error_command_property_unknown
import kubriko.creator.generated.resources.error_property_name_invalid
import kubriko.creator.generated.resources.error_property_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PropertyCommandsTest {
    private val score = PropertyDefinition.Number("score", 5f)
    private val title = PropertyDefinition.Text("title")
    private val player = ActorTypeDefinition(
        name = "Player",
        properties = listOf(score, title),
        events = listOf(
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", 2f), BlockDefinition.Log("Score: {score}, {scoreboard}"))),
        ),
    )
    private val changedScenes = mutableListOf<String>()

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg actors: SceneActorEntry) = commandContext(player, onScenesChanged = { changedScenes += it }).apply {
        scenes.write("main", SceneDocument.format(actors.toList()))
    }

    private fun CommandContext.player() = actorTypes!!["Player"]!!

    private fun CommandContext.states() = SceneDocument.parse(scenes.read("main")!!).map { it.state }

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
        assertEquals(listOf(BlockDefinition.ChangeNumber("points", 2f), BlockDefinition.Log("Score: {points}, {scoreboard}")), renamed.first.events.single().blocks)
        assertEquals(listOf("""{"points":12.0,"title":"Hi"}""", """{"score":7.0}"""), renamed.second)
        assertEquals(listOf("main"), changedScenes.distinct())
    }

    @Test
    fun removingKeepsSceneValuesSoUndoingBringsThemBackButAddingDropsThoseOfAnotherKind() = runTest {
        val context = context(SceneActorEntry("Player", """{"score":12.0}"""))
        Invocation(RemoveProperty, RemoveProperty.Input("Player", "score")).apply(context)
        assertEquals(listOf("""{"score":12.0}"""), context.states())
        assertEquals(listOf(BlockDefinition.ChangeNumber("score", 2f), BlockDefinition.Log("Score: {score}, {scoreboard}")), context.player().events.single().blocks)
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
        assertEquals(emptyList(), changedScenes)
    }

    @Test
    fun propertyChangesAreValidated() = runTest {
        val context = context()
        assertEquals(Rejection(Res.string.error_property_name_taken), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Toggle("tiTLE"))))
        assertEquals(Rejection(Res.string.error_property_name_invalid), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Toggle("is shielded"))))
        assertEquals(Rejection(Res.string.error_command_number_invalid), AddProperty.validate(context, AddProperty.Input("Player", PropertyDefinition.Number("lives", Float.NaN))))
        assertEquals(Rejection(Res.string.error_command_property_unknown, "Player", "lives"), RemoveProperty.validate(context, RemoveProperty.Input("Player", "lives")))
        assertEquals(Rejection(Res.string.error_command_property_kind_changed), EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Text("score"))))
        assertEquals(Rejection(Res.string.error_property_name_taken), EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Number("title"))))
        assertNull(EditProperty.validate(context, EditProperty.Input("Player", "score", PropertyDefinition.Number("score", 1f))))
        assertEquals(EditProperty.mergeKey(EditProperty.Input("Player", "score", score)), EditProperty.mergeKey(EditProperty.Input("Player", "points", score)))
    }
}
