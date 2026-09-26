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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader as SkiaShader

internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): RenderEffect? {
    if (shader is BlurShader) {
        return shader.shaderState.blurHorizontal.let { blurHorizontal ->
            shader.shaderState.blurVertical.let { blurVertical ->
                if (blurHorizontal <= 0 || blurVertical <= 0) null else ImageFilter.makeBlur(
                    sigmaX = blurHorizontal,
                    sigmaY = blurVertical,
                    mode = when (shader.shaderState.mode) {
                        BlurShader.Mode.CLAMP -> FilterTileMode.CLAMP
                        BlurShader.Mode.REPEAT -> FilterTileMode.REPEAT
                        BlurShader.Mode.MIRROR -> FilterTileMode.MIRROR
                        BlurShader.Mode.DECAL -> FilterTileMode.DECAL
                    },
                ).asComposeRenderEffect()
            }
        }
    } else {
        val shaderUniformProvider = shader.applyUniforms(size)
        // Nothing the effect was built from has moved, so the one already built still shows exactly this.
        shader.shaderCache.cachedRenderEffect?.let { if (!shaderUniformProvider.uniforms.hasChanges) return it }
        val runtimeShaderBuilder = shaderUniformProvider.runtimeShaderBuilder
        return (when (shader) {
            is ContentShader<*> -> ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = runtimeShaderBuilder,
                shaderName = ContentShader.CONTENT,
                input = null,
            )

            else -> ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = runtimeShaderBuilder,
                shaderNames = emptyArray(),
                inputs = emptyArray(),
            )
        }).asComposeRenderEffect()
    }
}

internal actual fun <T : Shader.State> DrawScope.drawGenerativeShader(
    shader: Shader<T>,
): Boolean {
    val cache = shader.shaderCache
    val shaderState = shader.shaderState
    val dirtinessToken = shaderState.dirtinessToken
    val cachedPaint = cache.cachedPaint as? Paint
    val paint = cachedPaint ?: Paint().also {
        it.isAntiAlias = false
        cache.cachedPaint = it
    }
    if (cachedPaint == null || !cache.isUpToDate(shaderState, dirtinessToken, size)) {
        val shaderUniformProvider = shader.applyUniforms(size)
        if (cachedPaint == null || shaderUniformProvider.uniforms.hasChanges) {
            // The paint holds a reference of its own, and every frame already drawn with it holds a copy of it.
            val skiaShader = shaderUniformProvider.runtimeShaderBuilder.makeShader()
            paint.shader = skiaShader
            skiaShader.close()
        }
        cache.markUpToDate(shaderState, dirtinessToken, size)
    }
    drawIntoCanvas { it.skiaCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
    return true
}

/** Applies the uniforms of [Shader.shaderState] and the [Shader.RESOLUTION] of [size], starting a new change count. */
private fun <T : Shader.State> Shader<T>.applyUniforms(size: Size): ShaderUniformProviderImpl {
    val runtimeShaderBuilder = (shaderCache.runtimeShader as? RuntimeShaderBuilder)
        ?: RuntimeShaderBuilder(RuntimeEffect.makeForShader(shaderCode.trimIndent())).also { shaderCache.runtimeShader = it }
    val shaderUniformProvider = (shaderCache.uniformProvider as? ShaderUniformProviderImpl)
        ?: ShaderUniformProviderImpl(runtimeShaderBuilder).also { shaderCache.uniformProvider = it }
    shaderUniformProvider.uniforms.hasChanges = false
    with(shaderState) { shaderUniformProvider.applyUniforms() }
    shaderUniformProvider.updateResolution(size)
    return shaderUniformProvider
}

private class ShaderUniformProviderImpl(
    val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    val uniforms = UniformCache()

    fun updateResolution(size: Size) = uniform(Shader.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) {
        if (uniforms.hasChanged(name, UniformCache.KIND_INT, value)) runtimeShaderBuilder.uniform(name, value)
    }

    override fun uniform(name: String, value: Float) {
        if (uniforms.hasChanged(name, UniformCache.KIND_FLOAT, value.toRawBits())) runtimeShaderBuilder.uniform(name, value)
    }

    override fun uniform(name: String, value1: Float, value2: Float) {
        if (uniforms.hasChanged(name, UniformCache.KIND_FLOAT2, value1.toRawBits(), value2.toRawBits())) {
            runtimeShaderBuilder.uniform(name, value1, value2)
        }
    }

    // Cached by bitmap identity so re-applying the same instance every frame skips the conversion.
    private val childShaderCache = mutableMapOf<String, Pair<ImageBitmap, SkiaShader>>()

    override fun uniform(name: String, value: ImageBitmap) {
        if (!uniforms.hasChildChanged(name, value)) return
        val cached = childShaderCache[name]
        val childShader = if (cached != null && cached.first === value) cached.second else Image
            .makeFromBitmap(value.asSkiaBitmap())
            .makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR)
            .also { childShaderCache[name] = value to it }
        runtimeShaderBuilder.child(name, childShader)
    }
}
