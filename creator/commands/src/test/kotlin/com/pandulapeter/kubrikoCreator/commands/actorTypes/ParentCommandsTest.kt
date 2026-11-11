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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentCommandsTest {
    private val enemy = ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Number("hp")))
    private val bat = ActorTypeDefinition(name = "Bat", parent = "Enemy")
    private val slime = ActorTypeDefinition(name = "Slime", properties = listOf(PropertyDefinition.Text("hp")))

    @Test
    fun aParentIsSetAndUnsetButNeverMakesACycle() = runTest {
        val context = commandContext(enemy, bat, slime)
        assertEquals(Rejection(CommandProblem.PARENT_CYCLE, "Bat", "Enemy"), SetParent.validate(context, SetParent.Input("Enemy", "Bat")))
        assertEquals(Rejection(CommandProblem.PARENT_CYCLE, "Enemy", "Enemy"), SetParent.validate(context, SetParent.Input("Enemy", "Enemy")))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Ghost"), SetParent.validate(context, SetParent.Input("Bat", "Ghost")))
        assertEquals(Rejection(CommandProblem.PARENT_PROPERTY_KIND, "hp", "Enemy"), SetParent.validate(context, SetParent.Input("Slime", "Enemy")))
        assertEquals(null, context.assertRoundTrip(Invocation(SetParent, SetParent.Input("Bat", null))) { context.blockTypes!!.actorType("Bat")!!.parent })
    }

    @Test
    fun aParentWithChildrenIsNotDeletedAndRenamingItTakesItsChildrenAlong() = runTest {
        val context = commandContext(enemy, bat)
        assertEquals(Rejection(CommandProblem.ACTOR_TYPE_HAS_CHILDREN, "Enemy", "Bat"), DeleteActorType.validate(context, DeleteActorType.Input("Enemy")))
        context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Enemy", "Monster"))) { context.blockTypes!!.actorType("Bat")!!.parent }
        assertEquals("Monster", context.blockTypes!!.actorType("Bat")!!.parent)
    }

    @Test
    fun aChildOnlyGivesAnInheritedPropertyAnotherInitialValue() = runTest {
        val context = commandContext(enemy, bat)
        assertEquals(null, AddProperty.validate(context, AddProperty.Input("Bat", PropertyDefinition.Number("hp", initialValue = 5f))))
        assertEquals(Rejection(CommandProblem.PARENT_PROPERTY_KIND, "hp", "Enemy"), AddProperty.validate(context, AddProperty.Input("Bat", PropertyDefinition.Text("hp"))))
    }

    @Test
    fun aParentClashingWithADescendantIsRefused() = runTest {
        val other = ActorTypeDefinition(name = "Other", properties = listOf(PropertyDefinition.Number("health")))
        val child2 = ActorTypeDefinition(name = "Child2")
        val grandChild = ActorTypeDefinition(name = "GrandChild", parent = "Child2", properties = listOf(PropertyDefinition.Text("health")))
        val context = commandContext(other, child2, grandChild)
        assertEquals(Rejection(CommandProblem.PARENT_PROPERTY_KIND, "health", "Other"), SetParent.validate(context, SetParent.Input("Child2", "Other")))
    }

    @Test
    fun namesEndingLikeABaseClassAreReserved() {
        assertEquals(NameProblem.TYPE_BASE_SUFFIX, BlockTypeNaming.validate("EnemyBase", emptyList()))
        assertEquals(null, BlockTypeNaming.validate("Baseball", emptyList()))
    }
}
