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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.ComicShader
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import com.pandulapeter.kubriko.shaders.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader

fun <T : Shader.State> Modifier.shader(
    shader: Shader<T>,
    gameTime: State<Long>,
) = if (shader is ContentShader<*>) this then Modifier.graphicsLayer {
    @Suppress("UNUSED_EXPRESSION") gameTime.value  // Invalidates the Canvas, causing a refresh on every frame
    clip = true
    val cache = shader.shaderCache
    val shaderState = shader.shaderState
    val dirtinessToken = shaderState.dirtinessToken
    renderEffect = if (cache.isUpToDate(shaderState, dirtinessToken, size)) {
        cache.cachedRenderEffect
    } else {
        createRenderEffect(shader, size).also {
            cache.cachedRenderEffect = it
            cache.markUpToDate(shaderState, dirtinessToken, size)
        }
    }
} else this then Modifier.drawWithContent {
    // A shader that doesn't read its layer replaces whatever the layer draws, so it is drawn as a fill of the layer's
    // bounds rather than as a render effect: an effect with nothing to read still renders into an offscreen target the
    // size of the layer on every frame (two on Android, which gives any node carrying one a layer of its own), where
    // a fill blends straight into the target already being drawn.
    @Suppress("UNUSED_EXPRESSION") gameTime.value  // Invalidates the draw, causing a refresh on every frame
    if (!drawGenerativeShader(shader)) clipRect { this@drawWithContent.drawContent() }
}

/**
 * Whether what [Shader.Cache] last built for [shaderState] at [size] still holds: the layer size is unchanged, and
 * either [dirtinessToken] says the uniforms are, or the state proves it by value equality.
 */
internal fun Shader.Cache.isUpToDate(shaderState: Shader.State, dirtinessToken: Int, size: Size) =
    size == cachedSize && if (dirtinessToken == Shader.State.DIRTINESS_UNKNOWN) {
        shaderState.hasValueEquality && shaderState == cachedState
    } else {
        dirtinessToken == cachedDirtinessToken
    }

internal fun Shader.Cache.markUpToDate(shaderState: Shader.State, dirtinessToken: Int, size: Size) {
    cachedDirtinessToken = dirtinessToken
    cachedSize = size
    cachedState = shaderState
}

// The built-in states are immutable data classes, so equal values prove equal uniforms — which none of
// them can express through the dirtiness token, whose default forces a rebuild on every invalidation.
// A custom state may compare by identity or hold mutable fields, so it keeps that fallback.
private val Shader.State.hasValueEquality
    get() = this is BlurShader.State ||
            this is ChromaticAberrationShader.State ||
            this is ComicShader.State ||
            this is RippleShader.State ||
            this is SmoothPixelationShader.State ||
            this is VignetteShader.State

internal expect fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): RenderEffect?

/**
 * Fills the bounds of this scope with [shader]'s output, returning false where shaders aren't supported - the layer's
 * own content is then drawn instead, exactly as it is when a render effect can't be made.
 */
internal expect fun <T : Shader.State> DrawScope.drawGenerativeShader(
    shader: Shader<T>,
): Boolean

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