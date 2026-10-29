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
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_has_children
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_parent_cycle
import kubriko.creator.generated.resources.error_command_parent_property_kind
import kubriko.creator.generated.resources.error_type_name_base_suffix
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentCommandsTest {
    private val enemy = ActorTypeDefinition(name = "Enemy", properties = listOf(PropertyDefinition.Number("hp")))
    private val bat = ActorTypeDefinition(name = "Bat", parent = "Enemy")
    private val slime = ActorTypeDefinition(name = "Slime", properties = listOf(PropertyDefinition.Text("hp")))

    @Test
    fun aParentIsSetAndUnsetButNeverMakesACycle() = runTest {
        val context = commandContext(enemy, bat, slime)
        assertEquals(Rejection(Res.string.error_command_parent_cycle, "Bat", "Enemy"), SetParent.validate(context, SetParent.Input("Enemy", "Bat")))
        assertEquals(Rejection(Res.string.error_command_parent_cycle, "Enemy", "Enemy"), SetParent.validate(context, SetParent.Input("Enemy", "Enemy")))
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Ghost"), SetParent.validate(context, SetParent.Input("Bat", "Ghost")))
        assertEquals(Rejection(Res.string.error_command_parent_property_kind, "hp", "Enemy"), SetParent.validate(context, SetParent.Input("Slime", "Enemy")))
        assertEquals(null, context.assertRoundTrip(Invocation(SetParent, SetParent.Input("Bat", null))) { context.blockTypes!!.actorType("Bat")!!.parent })
    }

    @Test
    fun aParentWithChildrenIsNotDeletedAndRenamingItTakesItsChildrenAlong() = runTest {
        val context = commandContext(enemy, bat)
        assertEquals(Rejection(Res.string.error_command_actor_type_has_children, "Enemy", "Bat"), DeleteActorType.validate(context, DeleteActorType.Input("Enemy")))
        context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Enemy", "Monster"))) { context.blockTypes!!.actorType("Bat")!!.parent }
        assertEquals("Monster", context.blockTypes!!.actorType("Bat")!!.parent)
    }

    @Test
    fun aChildOnlyGivesAnInheritedPropertyAnotherInitialValue() = runTest {
        val context = commandContext(enemy, bat)
        assertEquals(null, AddProperty.validate(context, AddProperty.Input("Bat", PropertyDefinition.Number("hp", initialValue = 5f))))
        assertEquals(Rejection(Res.string.error_command_parent_property_kind, "hp", "Enemy"), AddProperty.validate(context, AddProperty.Input("Bat", PropertyDefinition.Text("hp"))))
    }

    @Test
    fun namesEndingLikeABaseClassAreReserved() {
        assertEquals(Res.string.error_type_name_base_suffix, BlockTypeNaming.validate("EnemyBase", emptyList()))
        assertEquals(null, BlockTypeNaming.validate("Baseball", emptyList()))
    }
}
