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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneFramingTest {

    @Test
    fun theSceneSettingsBoundsAreShownWhenThereAreAny() {
        val bounds = listOf(
            SceneHitTest.Bounds(0, -400f, -300f, 400f, 300f, isBackground = true),
            SceneHitTest.Bounds(1, 1000f, 1000f, 1100f, 1100f),
        )
        assertEquals(SceneFraming(0f, 0f, 0.9f), SceneFraming.fit(bounds, 800f, 600f))
        // The narrower side decides.
        assertEquals(SceneFraming(0f, 0f, 0.45f), SceneFraming.fit(bounds, 400f, 600f))
    }

    @Test
    fun withoutSceneSettingsEveryActorIsShown() {
        val bounds = listOf(SceneHitTest.Bounds(0, 0f, 0f, 100f, 100f), SceneHitTest.Bounds(1, 100f, 100f, 200f, 200f))
        assertEquals(SceneFraming(100f, 100f, 4.5f), SceneFraming.fit(bounds, 1000f, 1000f))
        assertNull(SceneFraming.fit(emptyList(), 1000f, 1000f))
        assertNull(SceneFraming.fit(bounds, 0f, 1000f))
    }
}
