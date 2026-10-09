/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class BoxBodySerializerTest {

    @Test
    fun missingPivotDefaultsToTheCenterOfTheDecodedSize() {
        val body = BoxBody(
            initialPosition = SceneOffset(3f.sceneUnit, 4f.sceneUnit),
            initialSize = SceneSize(10f.sceneUnit, 20f.sceneUnit),
            initialPivot = SceneOffset.Zero,
        )
        val encoded = Json.encodeToJsonElement(BoxBodySerializer, body).jsonObject
        val withoutPivot = JsonObject(encoded - "pivot")

        val decoded = Json.decodeFromJsonElement(BoxBodySerializer, withoutPivot)

        assertEquals(SceneOffset(5f.sceneUnit, 10f.sceneUnit), decoded.pivot)
    }
}
