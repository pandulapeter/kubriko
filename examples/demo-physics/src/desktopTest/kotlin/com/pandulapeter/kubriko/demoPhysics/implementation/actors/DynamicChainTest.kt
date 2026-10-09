/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicChainTest {

    @Test
    fun saveReturnsTheStateTheChainWasRestoredFrom() = listOf(20, 21).forEach { linkCount ->
        val saved = DynamicChain.State(
            linkCount = linkCount,
            initialCenterOffset = SceneOffset(100f.sceneUnit, (-50f).sceneUnit),
        ).restore().save()
        assertEquals(linkCount, saved.linkCount)
        assertEquals(100f, saved.initialCenterOffset.x.raw, 0.01f)
        assertEquals(-50f, saved.initialCenterOffset.y.raw, 0.01f)
    }
}
