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
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.usagesOf
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetSceneValuesTest {
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("lives", initialValue = 3f), PropertyDefinition.Text("title"), PropertyDefinition.Toggle("isHard")),
    )
    private val wall = SceneActorEntry("Wall", """{"body":{"position":{"x":1.0,"y":2.0}}}""", "wall")

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg actors: SceneActorEntry) = commandContext(game, ActorTypeDefinition("Wall")).apply {
        scenes.write("level", SceneDocument.format(actors.toList()))
        scenes.takeChanges()
    }

    private fun CommandContext.level() = checkNotNull(scenes.read("level")?.let(SceneDocument::parse))

    private fun set(vararg values: ActorValueChanges.PropertyValue, cleared: List<String> = emptyList()) =
        Invocation(SetSceneValues, SetSceneValues.Input("level", "Game", values.toList(), cleared))

    @Test
    fun aSceneGivesValuesThroughAnEntryOfTheManagerTypeThatGoesOnceItGivesNone() = runTest {
        val context = context(wall)
        val afterSetting = context.assertRoundTrip(set(ActorValueChanges.PropertyValue("lives", number = 5f), ActorValueChanges.PropertyValue("isHard", toggle = true))) { context.level() }
        assertEquals(listOf("Game", "Wall"), afterSetting.map { it.typeId })
        assertEquals("""{"lives":5.0,"isHard":true}""", afterSetting[0].state)
        set(ActorValueChanges.PropertyValue("lives", number = 5f), ActorValueChanges.PropertyValue("isHard", toggle = true)).apply(context)
        val id = context.level()[0].id
        val afterChanging = context.assertRoundTrip(set(ActorValueChanges.PropertyValue("title", text = "Hard"), cleared = listOf("isHard"))) { context.level() }
        assertEquals(SceneActorEntry("Game", """{"lives":5.0,"title":"Hard"}""", id), afterChanging[0])
        assertEquals(listOf(wall), context.assertRoundTrip(set(cleared = listOf("lives", "title"))) { context.level() })
    }

    @Test
    fun valuesNeedTheManagersPropertiesOfTheirKindsEachNamedOnce() = runTest {
        val context = context(wall)
        assertEquals(LookupProblem.MANAGER_TYPE_MISSING, SetSceneValues.validate(context, SetSceneValues.Input("level", "Nowhere"))?.reason)
        assertEquals(LookupProblem.SCENE_MISSING, SetSceneValues.validate(context, SetSceneValues.Input("cave", "Game"))?.reason)
        assertEquals(Rejection(CommandProblem.PROPERTY_VALUE_INVALID, "lives"), set(ActorValueChanges.PropertyValue("lives", text = "five")).validate(context))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), set(ActorValueChanges.PropertyValue("lives", number = Float.NaN)).validate(context))
        assertEquals(Rejection(CommandProblem.SCENE_VALUE_NAMED_TWICE, "lives"), set(ActorValueChanges.PropertyValue("lives", number = 1f), cleared = listOf("lives")).validate(context))
        assertEquals(LookupProblem.ACTOR_PROPERTY_MISSING_SIMILAR, set(ActorValueChanges.PropertyValue("live", number = 1f)).validate(context)?.reason)
        assertEquals(null, set(ActorValueChanges.PropertyValue("lives", number = 1f)).validate(context))
        assertEquals(Rejection(CommandProblem.SCENE_VALUES_NOTHING_TO_CHANGE), set().validate(context))
    }

    @Test
    fun aValueOfAPropertyRemovedSinceCanStillBeCleared() = runTest {
        val context = context(SceneActorEntry("Game", """{"lives":5.0,"gone":1.0}""", "values"))
        assertEquals(null, set(cleared = listOf("gone")).validate(context))
        assertEquals(LookupProblem.ACTOR_PROPERTY_MISSING, set(cleared = listOf("other")).validate(context)?.reason)
        set(cleared = listOf("gone")).apply(context)
        assertEquals(listOf(SceneActorEntry("Game", """{"lives":5.0}""", "values")), context.level())
    }

    @Test
    fun theValuesFollowTheManagerTypeAndItsPropertiesWhenRenamedAndGoWithItWhenDeleted() = runTest {
        val context = context(wall, SceneActorEntry("Game", """{"lives":5.0}""", "values"))
        assertEquals(
            listOf(Usage(UsagePlace.InModel(RelationPlace.Scene("level")), RelationKind.GIVES_VALUES_TO, RelationSubject.Type("Game"), UsageEffect.SCENE_VALUES_REMOVED)),
            context.usagesOf(RelationSubject.Type("Game")),
        )
        EditProperty.apply(context, EditProperty.Input("Game", "lives", PropertyDefinition.Number("hearts", initialValue = 3f)))
        assertEquals("""{"hearts":5.0}""", context.level()[1].state)
        RenameManagerType.apply(context, RenameManagerType.Input("Game", "Match"))
        assertEquals("Match", context.level()[1].typeId)
        assertEquals(listOf(wall), context.assertRoundTrip(Invocation(DeleteManagerType, DeleteManagerType.Input("Match"))) { context.level() })
    }
}
