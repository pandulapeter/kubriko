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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneSettingsStateTest {

    @Test
    fun colorsArePackedTheWayKubrikosSerializationSavesThem() {
        assertEquals(0xFF303048L, SceneSettingsState.argbOf(-58493709360037888L))
        assertEquals(-58493709360037888L, SceneSettingsState.packedColorOf(0xFF303048L))
    }

    @Test
    fun missingValuesAreTheGeneratedClassDefaults() {
        assertEquals(SceneSettingsState.Values(), SceneSettingsState.read("{}"))
        assertEquals(SceneSettingsState.Values(), SceneSettingsState.read("not json"))
    }

    @Test
    fun newSettingsGetABodyWithItsPivotInTheMiddle() {
        val state = SceneSettingsState.withChanges(backgroundColor = 0xFF102030L)
        assertEquals(SceneSettingsState.Values(backgroundColor = 0xFF102030L), SceneSettingsState.read(state))
        assertEquals("""{"x":640.0,"y":360.0}""", Json.parseToJsonElement(state).jsonObject.getValue("body").jsonObject.getValue("pivot").toString())
    }

    @Test
    fun changesKeepEverythingElse() {
        val before = """{"body":{"position":{"x":1.0,"y":2.0},"size":{"width":10.0,"height":20.0},"pivot":{"x":5.0,"y":10.0},"rotation":0.0},"cameraStart":{"x":3.0,"y":4.0},"other":true}"""
        val after = SceneSettingsState.withChanges(before, width = 30f, cameraY = 8f)
        assertEquals(SceneSettingsState.Values(x = 1f, y = 2f, width = 30f, height = 20f, cameraX = 3f, cameraY = 8f), SceneSettingsState.read(after))
        val body = Json.parseToJsonElement(after).jsonObject.getValue("body").jsonObject
        assertEquals("""{"x":15.0,"y":10.0}""", body.getValue("pivot").toString())
        assertEquals("0.0", body.getValue("rotation").toString())
        assertEquals("true", Json.parseToJsonElement(after).jsonObject.getValue("other").toString())
    }
}
