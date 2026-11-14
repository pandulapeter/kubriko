/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The names only generated Actor classes use are free in Manager types, while the ones both use stay reserved in both.
 */
class ActorOnlyPropertyNamesTest {

    @Test
    fun aManagerTypeTakesTheNamesOnlyActorTypesReserve() = runTest {
        val context = commandContext(ActorTypeDefinition(name = "Ball"), ManagerTypeDefinition(name = "Game"))
        fun rejection(type: String, name: String) = AddProperty.validate(context, AddProperty.Input(type, PropertyDefinition.Number(name)))
        listOf("opacity", "look", "velocityFromKeys", "scaleTweenFrom", "com").forEach { name ->
            assertNull(rejection("Game", name), name)
            assertEquals(Rejection(NameProblem.PROPERTY_RESERVED), rejection("Ball", name), name)
        }
        assertEquals(Rejection(NameProblem.PROPERTY_RESERVED), rejection("Game", "velocity"))
    }
}
