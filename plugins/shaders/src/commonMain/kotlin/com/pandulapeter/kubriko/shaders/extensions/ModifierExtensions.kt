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

/**
 * Applies [shader] to the content of the modified node.
 *
 * A [ContentShader] becomes the node's render effect in a clipped `graphicsLayer`, rebuilt only when its state or the
 * layer size changes. Any other shader replaces the content, drawn as a fill of the node's bounds (where shaders are
 * not supported, the content itself is drawn instead).
 *
 * @param shader The shader to apply.
 * @param gameTime Read on every frame so the node redraws as the game clock advances: pass the game time of the
 * Kubriko instance, as the engine does when it hands it to [com.pandulapeter.kubriko.manager.Manager.processModifier].
 */
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

/**
 * The built-in states are immutable data classes, so equal values prove equal uniforms — which none of
 * them can express through the dirtiness token, whose default forces a rebuild on every invalidation.
 * A custom state may compare by identity or hold mutable fields, so it keeps that fallback.
 */
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
