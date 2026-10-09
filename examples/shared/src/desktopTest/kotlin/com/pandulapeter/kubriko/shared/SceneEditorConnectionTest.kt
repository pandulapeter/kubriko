/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneEditorConnectionTest {

    private val connection = SceneEditorConnection()

    @Test
    fun theEditorStartsClosed() {
        assertFalse(connection.isVisible.value)
    }

    @Test
    fun toggleOpensAndClosesTheEditor() {
        connection.toggle()
        assertTrue(connection.isVisible.value)
        connection.toggle()
        assertFalse(connection.isVisible.value)
    }

    @Test
    fun closeClosesAnOpenEditorAndKeepsAClosedOneClosed() {
        connection.toggle()
        connection.close()
        assertFalse(connection.isVisible.value)
        connection.close()
        assertFalse(connection.isVisible.value)
    }
}
