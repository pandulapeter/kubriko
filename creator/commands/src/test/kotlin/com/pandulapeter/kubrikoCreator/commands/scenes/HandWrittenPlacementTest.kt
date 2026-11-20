/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.HandWrittenActorTypes
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HandWrittenPlacementTest {
    private val ball = ActorTypeDefinition(name = "Ball", width = 30f, height = 40f)
    private val loaded = object : HandWrittenActorTypes {
        override val typeIds = setOf("Ball", "Spawner", "Broken", SceneSettingsCodeGenerator.SETTINGS_NAME)

        override fun newState(typeId: String, x: Float, y: Float) = when (typeId) {
            "Spawner" -> """{"body":{"position":{"x":$x,"y":$y}},"rate":2.0}"""
            else -> null
        }
    }

    @OptIn(EditorMutation::class)
    private fun TestScope.context(handWritten: HandWrittenActorTypes = loaded) = commandContext(ball, handWrittenActorTypes = handWritten).apply {
        scenes.write("main", "[]")
        scenes.takeChanges()
    }

    @Test
    fun anActorOfATypeWrittenByHandIsPlacedAsItsClassCreatesIt() = runTest {
        val context = context()
        val input = PlaceActor.Input("main", "Spawner", 3f, 4f, actorId = "spawner")
        assertNull(PlaceActor.validate(context, input))
        val actors = context.assertRoundTrip(Invocation(PlaceActor, input)) { context.scenes.read("main")?.let(SceneDocument::parse) }!!
        assertEquals("""{"body":{"position":{"x":3.0,"y":4.0}},"rate":2.0}""", actors.single().state)
        assertEquals(3f to 4f, SceneDocument.positionOf(actors.single().state))
    }

    @Test
    fun blockBuiltTypesAreStillPlacedAtTheirSizeAndOthersThatCantBePlacedAreRefused() = runTest {
        val context = context()
        PlaceActors.apply(context, PlaceActors.Input("main", listOf(PlaceActors.Placement("Ball", 1f, 2f, "ball"))))
        assertEquals(SceneDocument.blockBuiltState(1f, 2f, 30f, 40f), context.scenes.read("main")?.let(SceneDocument::parse)?.single()?.state)
        assertNotNull(PlaceActor.validate(context, PlaceActor.Input("main", SceneSettingsCodeGenerator.SETTINGS_NAME, 0f, 0f)))
        assertNotNull(PlaceActor.validate(context(HandWrittenActorTypes.None), PlaceActor.Input("main", "Spawner", 0f, 0f)))
        assertEquals(Rejection(CommandProblem.SCENE_ACTOR_NOT_CREATED, "Broken"), PlaceActor.apply(context, PlaceActor.Input("main", "Broken", 0f, 0f)).failure)
        assertEquals("[]", context.scenes.read("main")?.let(SceneDocument::parse)?.drop(1)?.let(SceneDocument::format))
    }

    @Test
    fun redoingAPlacementBringsBackTheStateItWasPlacedWithEvenOnceTheClassesAreGone() = runTest {
        var handWritten: HandWrittenActorTypes = loaded
        val context = commandContext(ball, handWrittenActorTypes = object : HandWrittenActorTypes {
            override val typeIds get() = handWritten.typeIds

            override fun newState(typeId: String, x: Float, y: Float) = handWritten.newState(typeId, x, y)
        }).apply {
            @OptIn(EditorMutation::class)
            scenes.write("main", "[]")
        }
        val executor = CommandExecutor(context, testScope())
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(PlaceActor, PlaceActor.Input("main", "Spawner", 3f, 4f, actorId = "spawner"))))
        val placed = context.scenes.read("main")
        executor.undo()
        handWritten = HandWrittenActorTypes.None
        executor.redo()
        assertEquals(placed, context.scenes.read("main"))
    }
}
