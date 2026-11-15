/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceConditionCodeTest {
    private val sheep = BlockScope("Sheep", properties = listOf(PropertyDefinition.ActorType("predator", "Dog")), actorTypeNames = listOf("Sheep", "Dog"))

    private fun line(condition: ConditionDefinition, scope: BlockScope = sheep) = BlockCodeGenerator.blockCode(BlockDefinition.If(condition), EventType.EVERY_FRAME, scope).lines.first()

    @Test
    fun theDistanceToATypeByItsNameOrAPropertysIsMeasuredThroughTheRegistry() {
        assertEquals(
            "if (GeneratedActorTypes.distanceToNearestOf(actorManager.allActors.value, \"Dog\", this) < 150f) {",
            line(ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog"), Comparison.LESS, NumberValue.Fixed(150f))),
        )
        assertEquals(
            "if (GeneratedActorTypes.distanceToNearestOf(actorManager.allActors.value, predator, this) >= 50f) {",
            line(ConditionDefinition.Distance(ActorTypeValue.Property("predator"), Comparison.GREATER_OR_EQUAL, NumberValue.Fixed(50f))),
        )
    }

    @Test
    fun aTypeThatDoesntExistOrAManagerTypeNeverHolds() {
        assertEquals("if (false) {", line(ConditionDefinition.Distance(ActorTypeValue.Fixed("Wolf"))))
        assertEquals("if (false) {", line(ConditionDefinition.Distance(ActorTypeValue.Property("missing"))))
        assertEquals("if (false) {", line(ConditionDefinition.Distance(ActorTypeValue.Fixed("Dog")), BlockScope("Game", BlockTypeKind.MANAGER, actorTypeNames = listOf("Dog"))))
    }
}
