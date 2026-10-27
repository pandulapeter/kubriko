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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HexColorSerializerTest {
    private val json = Json

    @Test
    fun opaqueColorsAreWrittenAsSixDigitHex() {
        assertEquals("\"#6060AA\"", json.encodeToString(HexColorSerializer, 0xFF6060AA))
    }

    @Test
    fun translucentColorsKeepTheirAlpha() {
        assertEquals("\"#806060AA\"", json.encodeToString(HexColorSerializer, 0x806060AA))
        assertEquals(0x806060AA, json.decodeFromString(HexColorSerializer, "\"#806060AA\""))
    }

    @Test
    fun hexColorsAreReadBackAsOpaque() {
        assertEquals(0xFF6060AA, json.decodeFromString(HexColorSerializer, "\"#6060aa\""))
    }

    @Test
    fun theNumbersEarlierVersionsWroteStillLoad() {
        assertEquals(0xFF6060AA, json.decodeFromString(HexColorSerializer, "4284506282"))
    }

    @Test
    fun anythingElseIsRejected() {
        listOf("\"red\"", "\"#6060A\"", "\"6060AAFF\"").forEach {
            assertFailsWith<SerializationException>(it) { json.decodeFromString(HexColorSerializer, it) }
        }
    }
}
