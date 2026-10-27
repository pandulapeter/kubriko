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
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.error_type_name_reserved
import kubriko.creator.generated.resources.error_type_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockTypeNamingTest {

    @Test
    fun namesAreKotlinClassNames() {
        listOf("Ball", "Enemy2", "PlayerShip").forEach { assertNull(BlockTypeNaming.validate(it, emptyList()), it) }
        listOf("", "ball", "2Ball", "Player Ship", "Player-Ship", "Ballé", "Player_1").forEach {
            assertEquals(Res.string.error_type_name_invalid, BlockTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun namesTheGeneratedCodeAlreadyUsesAreReserved() {
        listOf("GeneratedActorTypes", "State", "Color").forEach {
            assertEquals(Res.string.error_type_name_reserved, BlockTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun namesMustBeUniqueIgnoringCase() {
        assertEquals(Res.string.error_type_name_taken, BlockTypeNaming.validate("Ball", listOf("Ball")))
        assertEquals(Res.string.error_type_name_taken, BlockTypeNaming.validate("BALL", listOf("Ball")))
        assertNull(BlockTypeNaming.validate("Balls", listOf("Ball")))
    }
}
