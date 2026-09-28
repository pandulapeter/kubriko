/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals

class SizeExtensionsTest {

    @Test
    fun sizeIsDividedByScaleOnly() {
        assertEquals(SceneSize(50f.sceneUnit, 25f.sceneUnit), Size(100f, 50f).toSceneSize(Scale(2f, 2f)))
    }

    @Test
    @Suppress("DEPRECATION")
    fun deprecatedOverloadIgnoresViewportSize() {
        assertEquals(
            SceneSize(50f.sceneUnit, 25f.sceneUnit),
            Size(100f, 50f).toSceneSize(viewportSize = Size(800f, 600f), viewportScaleFactor = Scale(2f, 2f)),
        )
    }

    @Test
    fun nonUniformScale() {
        assertEquals(SceneSize(30f.sceneUnit, 90f.sceneUnit), Size(90f, 90f).toSceneSize(Scale(3f, 1f)))
    }
}
