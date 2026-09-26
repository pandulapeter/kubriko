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

import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader

internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (shader is BlurShader) {
            return shader.shaderState.blurHorizontal.let { blurHorizontal ->
                shader.shaderState.blurVertical.let { blurVertical ->
                    if (blurHorizontal <= 0 || blurVertical <= 0) null else RenderEffect.createBlurEffect(
                        blurHorizontal,
                        blurVertical,
                        when (shader.shaderState.mode) {
                            BlurShader.Mode.CLAMP -> TileMode.CLAMP
                            BlurShader.Mode.REPEAT -> TileMode.REPEAT
                            BlurShader.Mode.MIRROR -> TileMode.MIRROR
                            BlurShader.Mode.DECAL -> TileMode.DECAL
                        },
                    ).asComposeRenderEffect()
                }
            }
        } else {
            val shaderUniformProvider = shader.applyUniforms(size)
            // Nothing the effect was built from has moved, so the one already built still shows exactly this.
            shader.shaderCache.cachedRenderEffect?.let { if (!shaderUniformProvider.uniforms.hasChanges) return it }
            val runtimeShader = shaderUniformProvider.runtimeShader
            return (when (shader) {
                is ContentShader<*> -> RenderEffect.createRuntimeShaderEffect(runtimeShader, ContentShader.CONTENT)
                else -> RenderEffect.createShaderEffect(runtimeShader)
            }).asComposeRenderEffect()
        }
    } else {
        return null
    }
}

internal actual fun <T : Shader.State> DrawScope.drawGenerativeShader(
    shader: Shader<T>,
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    drawGenerativeRuntimeShader(shader)
    return true
}

// The runtime shader is updated in place and the paint keeps pointing at it: a frame records the values the shader
// holds when it is drawn, so neither needs rebuilding for new ones.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun <T : Shader.State> DrawScope.drawGenerativeRuntimeShader(shader: Shader<T>) {
    val cache = shader.shaderCache
    val shaderState = shader.shaderState
    val dirtinessToken = shaderState.dirtinessToken
    val cachedPaint = cache.cachedPaint as? Paint
    if (cachedPaint == null || !cache.isUpToDate(shaderState, dirtinessToken, size)) {
        shader.applyUniforms(size)
        cache.markUpToDate(shaderState, dirtinessToken, size)
    }
    val paint = cachedPaint ?: Paint().also {
        it.shader = (cache.uniformProvider as ShaderUniformProviderImpl).runtimeShader
        cache.cachedPaint = it
    }
    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
}

/** Applies the uniforms of [Shader.shaderState] and the [Shader.RESOLUTION] of [size], starting a new change count. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun <T : Shader.State> Shader<T>.applyUniforms(size: Size): ShaderUniformProviderImpl {
    val runtimeShader = (shaderCache.runtimeShader as? RuntimeShader)
        ?: RuntimeShader(shaderCode.trimIndent()).also { shaderCache.runtimeShader = it }
    val shaderUniformProvider = (shaderCache.uniformProvider as? ShaderUniformProviderImpl)
        ?: ShaderUniformProviderImpl(runtimeShader).also { shaderCache.uniformProvider = it }
    shaderUniformProvider.uniforms.hasChanges = false
    with(shaderState) { shaderUniformProvider.applyUniforms() }
    shaderUniformProvider.updateResolution(size)
    return shaderUniformProvider
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ShaderUniformProviderImpl(
    val runtimeShader: RuntimeShader,
) : ShaderUniformProvider {

    val uniforms = UniformCache()

    fun updateResolution(size: Size) = uniform(Shader.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) {
        if (uniforms.hasChanged(name, UniformCache.KIND_INT, value)) runtimeShader.setIntUniform(name, value)
    }

    override fun uniform(name: String, value: Float) {
        if (uniforms.hasChanged(name, UniformCache.KIND_FLOAT, value.toRawBits())) runtimeShader.setFloatUniform(name, value)
    }

    override fun uniform(name: String, value1: Float, value2: Float) {
        if (uniforms.hasChanged(name, UniformCache.KIND_FLOAT2, value1.toRawBits(), value2.toRawBits())) {
            runtimeShader.setFloatUniform(name, value1, value2)
        }
    }

    // Cached by bitmap identity so re-applying the same instance every frame skips the conversion.
    private val childShaderCache = mutableMapOf<String, Pair<ImageBitmap, BitmapShader>>()

    override fun uniform(name: String, value: ImageBitmap) {
        if (!uniforms.hasChildChanged(name, value)) return
        val cached = childShaderCache[name]
        val childShader = if (cached != null && cached.first === value) cached.second else BitmapShader(
            value.asAndroidBitmap(),
            TileMode.CLAMP,
            TileMode.CLAMP,
        ).also {
            it.filterMode = BitmapShader.FILTER_MODE_LINEAR
            childShaderCache[name] = value to it
        }
        runtimeShader.setInputShader(name, childShader)
    }
}
