/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultDeltaTest {
    private val padding = "x".repeat(400)

    @Test
    fun theSameResultIsUnchangedUnlessItIsShorterThanSayingSo() {
        val result = Json.parseToJsonElement("""{"a":"$padding"}""")
        assertEquals(JsonPrimitive(ResultDelta.UNCHANGED), ResultDelta.of(result, result))
        val small = Json.parseToJsonElement("""{"a":1}""")
        assertEquals(small, ResultDelta.of(small, small))
    }

    @Test
    fun anObjectReturnsWhatChangedAndItsListsTheirItems() {
        val previous = Json.parseToJsonElement("""{"name":"Ball","padding":"$padding","events":["0 a","1 b","2 c"],"parent":"Shape"}""")
        val current = Json.parseToJsonElement("""{"name":"Ball","padding":"$padding","events":["0 a","1 B"],"appearance":"circle"}""")
        val delta = ResultDelta.of(previous, current).toString()
        assertEquals(
            """{"sinceYourLastRead":"Only what changed since you last read it in this conversation. Pass full: true for the whole result.","changed":{"events[1]":"1 B","appearance":"circle"},"removed":["events[2]","parent"]}""",
            delta,
        )
    }

    @Test
    fun aTextReturnsTheLinesThatChanged() {
        val delta = ResultDelta.of(JsonPrimitive("$padding\nBall\nBrick"), JsonPrimitive("$padding\nBall\nPaddle")) as JsonPrimitive
        assertEquals(listOf("- Brick", "+ Paddle"), delta.content.lines().drop(1))
    }

    @Test
    fun aDeltaNoShorterThanTheResultIsTheResult() {
        val current = Json.parseToJsonElement("""{"a":2}""")
        assertEquals(current, ResultDelta.of(Json.parseToJsonElement("""{"a":1}"""), current))
        assertEquals(JsonPrimitive(3), ResultDelta.of(JsonPrimitive(2), JsonPrimitive(3)))
    }
}
