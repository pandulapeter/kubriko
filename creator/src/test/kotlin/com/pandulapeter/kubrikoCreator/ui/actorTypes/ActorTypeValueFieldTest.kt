/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ActorTypeValueFieldTest {
    private val scope = BlockScope("Spawner", BlockTypeKind.ACTOR, listOf(PropertyDefinition.ActorType("spawned")), actorTypeNames = listOf("Spawner", "Enemy"))

    @Test
    fun aBlocksActorTypeIsChosenByNameOrTakenFromAProperty() = uiTest {
        var value by mutableStateOf<ActorTypeValue>(ActorTypeValue.Fixed(""))
        showing { ActorTypeValueField(value, scope) { value = it } }
        onNodeWithText("Choose an Actor type").performClick()
        onNodeWithText("Enemy").performClick()
        waitForIdle()
        assertEquals(ActorTypeValue.Fixed("Enemy"), value)
        onNodeWithContentDescription("Kind of Actor type").performClick()
        onNodeWithText("An Actor type reference Property's type").performClick()
        waitForIdle()
        assertEquals(ActorTypeValue.Property(), value)
        onNodeWithText("Choose a Property").performClick()
        onNodeWithText("spawned").performClick()
        waitForIdle()
        assertEquals(ActorTypeValue.Property("spawned"), value)
        saveSnapshot("actor-type-value-property")
    }
}
