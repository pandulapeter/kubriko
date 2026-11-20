/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneLayerFilterTest {

    @Test
    fun aHiddenOrLockedLayerCantBePicked() {
        val filter = SceneLayerFilter().withHidden(1, isHidden = true).withLocked(null, isLocked = true)
        assertFalse(filter.isPickable(1))
        assertFalse(filter.isPickable(null))
        assertTrue(filter.isPickable(0))
        assertEquals(SceneLayerFilter(), filter.withHidden(1, isHidden = false).withLocked(null, isLocked = false))
    }
}
