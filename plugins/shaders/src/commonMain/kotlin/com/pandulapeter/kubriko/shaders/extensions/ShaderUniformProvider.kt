/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.shaders.Shader

/**
 * A provider for setting uniform values on a [Shader].
 *
 * Implementations are platform-specific and handle the actual binding of values to the SKSL program.
 */
interface ShaderUniformProvider {
    /**
     * Sets an integer uniform value.
     *
     * @param name The name of the uniform in the SKSL code.
     * @param value The value to set.
     */
    fun uniform(name: String, value: Int)

    /**
     * Sets a float uniform value.
     *
     * @param name The name of the uniform in the SKSL code.
     * @param value The value to set.
     */
    fun uniform(name: String, value: Float)

    /**
     * Sets a float2 uniform value.
     *
     * @param name The name of the uniform in the SKSL code.
     * @param value1 The first component of the vector.
     * @param value2 The second component of the vector.
     */
    fun uniform(name: String, value1: Float, value2: Float)

    /**
     * Sets a texture uniform value, exposed to the SKSL code as a child shader
     * (declared there as `uniform shader name;` and sampled with `name.eval(coordinates)`,
     * where the coordinates are in the bitmap's pixel space).
     *
     * The platform shader object is cached by the [ImageBitmap]'s identity, so re-applying the
     * same instance every frame is cheap; pass a new bitmap only when the contents change.
     *
     * @param name The name of the uniform in the SKSL code.
     * @param value The bitmap to sample.
     */
    fun uniform(name: String, value: ImageBitmap)
}
