/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_actor_type_name_invalid
import kubriko.creator.generated.resources.error_actor_type_name_reserved
import kubriko.creator.generated.resources.error_actor_type_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorTypeNamingTest {

    @Test
    fun namesAreKotlinClassNames() {
        listOf("Ball", "Enemy2", "PlayerShip").forEach { assertNull(ActorTypeNaming.validate(it, emptyList()), it) }
        listOf("", "ball", "2Ball", "Player Ship", "Player-Ship", "Ballé", "Player_1").forEach {
            assertEquals(Res.string.error_actor_type_name_invalid, ActorTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun namesTheGeneratedCodeAlreadyUsesAreReserved() {
        listOf("GeneratedActorTypes", "State", "Color").forEach {
            assertEquals(Res.string.error_actor_type_name_reserved, ActorTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun namesMustBeUniqueIgnoringCase() {
        assertEquals(Res.string.error_actor_type_name_taken, ActorTypeNaming.validate("Ball", listOf("Ball")))
        assertEquals(Res.string.error_actor_type_name_taken, ActorTypeNaming.validate("BALL", listOf("Ball")))
        assertNull(ActorTypeNaming.validate("Balls", listOf("Ball")))
    }
}
