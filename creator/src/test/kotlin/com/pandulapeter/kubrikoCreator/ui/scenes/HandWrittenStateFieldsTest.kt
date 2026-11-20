/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HandWrittenStateFieldsTest {

    @Test
    fun eachValueIsEditedAsItsKindAndWholeNumbersStayWhole() {
        assertEquals(StateValueKind.Toggle(true), StateValueKind.of(JsonPrimitive(true)))
        assertEquals(StateValueKind.Text("left"), StateValueKind.of(JsonPrimitive("left")))
        val whole = StateValueKind.of(JsonPrimitive(3)) as StateValueKind.Number
        assertEquals(JsonPrimitive(4L), whole.encode(3.6f))
        assertEquals(JsonPrimitive(2.5f), (StateValueKind.of(JsonPrimitive(1.5f)) as StateValueKind.Number).encode(2.5f))
        assertEquals(StateValueKind.Raw("18446744069414584320"), StateValueKind.of(JsonPrimitive(18446744069414584320uL.toString().toBigInteger())))
        assertEquals(StateValueKind.Raw("[1,2]"), StateValueKind.of(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2)))))
        assertEquals(StateValueKind.Raw("null"), StateValueKind.of(JsonNull))
    }

    @Test
    fun aValueFirstShownAsJsonStaysJsonWhateverItBecomes() {
        assertEquals(StateValueKind.Raw("1"), StateValueKind.of(JsonPrimitive(1), shownAs = StateValueKind.of(JsonNull)))
        assertEquals(StateValueKind.Number(1f, isWhole = true), StateValueKind.of(JsonPrimitive(1), shownAs = StateValueKind.Number(0f, isWhole = true)))
    }

    @Test
    fun theFieldsShowTheValuesBesidesTheBodyAndChangeThem() = uiTest(width = 400, height = 400) {
        val changes = mutableListOf<Pair<String, JsonElement>>()
        showing {
            Column {
                HandWrittenStateFields(
                    state = """{"body":{"position":{"x":1.0,"y":2.0}},"rate":2.5,"isActive":false,"label":"left","path":[1,2]}""",
                    onValueChange = { key, value, isTyped -> if (!isTyped) changes += key to value },
                )
            }
        }
        onNodeWithText("What its class keeps").assertExists()
        onNodeWithText("body").assertDoesNotExist()
        saveSnapshot("hand-written-state-fields")
        onNodeWithText("isActive").performClick()
        waitForIdle()
        assertEquals(listOf<Pair<String, JsonElement>>("isActive" to JsonPrimitive(true)), changes)
    }
}
