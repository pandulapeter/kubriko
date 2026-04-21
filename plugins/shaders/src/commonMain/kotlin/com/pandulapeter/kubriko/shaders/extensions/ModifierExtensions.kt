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

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shaders.Shader

fun <T : Shader.State> Modifier.shader(
    shader: Shader<T>,
    gameTime: State<Long>,
) = this then graphicsLayer {
    @Suppress("UNUSED_EXPRESSION") gameTime.value  // Invalidates the Canvas, causing a refresh on every frame
    clip = true
    renderEffect = createRenderEffect(shader, size)
}

internal expect fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): RenderEffect?

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
}