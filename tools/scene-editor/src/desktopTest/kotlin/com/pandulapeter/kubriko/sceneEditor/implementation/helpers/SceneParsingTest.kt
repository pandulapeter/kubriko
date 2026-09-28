/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SceneParsingTest {

    @Test
    fun validContentReturnsItsItems() {
        assertEquals(listOf("a", "b"), deserializeSceneOrNull("[{},{}]") { listOf("a", "b") })
    }

    @Test
    fun emptyArrayIsAnEmptyScene() {
        assertEquals(emptyList(), deserializeSceneOrNull("[]") { emptyList<String>() })
        assertEquals(emptyList(), deserializeSceneOrNull(" [ ]\n") { emptyList<String>() })
    }

    @Test
    fun unreadableContentIsRefused() {
        assertNull(deserializeSceneOrNull("") { emptyList<String>() })
        assertNull(deserializeSceneOrNull("not json") { emptyList<String>() })
    }

    @Test
    fun throwingDeserializerIsRefused() {
        assertNull(deserializeSceneOrNull<String>("[{}]") { throw IllegalArgumentException() })
    }

    @Test
    fun cancellationIsRethrown() {
        assertFailsWith<CancellationException> {
            deserializeSceneOrNull<String>("[{}]") { throw CancellationException() }
        }
    }
}
