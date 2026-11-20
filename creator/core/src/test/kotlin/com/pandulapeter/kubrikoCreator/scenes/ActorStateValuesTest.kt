/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorStateValuesTest {
    private val state = """{"speed":2.5,"body":{"position":{"x":1.0,"y":2.0}},"name":"left","isActive":true}"""

    @Test
    fun theValuesBesidesTheBodyAreReadInOrderAndChangedInPlace() {
        assertEquals(listOf("speed", "name", "isActive"), ActorStateEdits.otherValuesOf(state).map { it.first })
        assertEquals(
            """{"speed":4,"body":{"position":{"x":1.0,"y":2.0}},"name":"left","isActive":true}""",
            ActorStateEdits.withValue(state, "speed", JsonPrimitive(4)),
        )
        assertEquals(emptyList(), ActorStateEdits.otherValuesOf("[]"))
        assertNull(ActorStateEdits.withValue("[]", "speed", JsonPrimitive(4)))
    }
}
