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
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
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
        val runtimeShaderBuilder =
            (shader.shaderCache.runtimeShader as? RuntimeShaderBuilder)
                ?: RuntimeShaderBuilder(RuntimeEffect.makeForShader(shader.shaderCode.trimIndent())).also { shader.shaderCache.runtimeShader = it }
        val shaderUniformProvider =
            (shader.shaderCache.uniformProvider as? ShaderUniformProviderImpl)
                ?: ShaderUniformProviderImpl(runtimeShaderBuilder).also { shader.shaderCache.uniformProvider = it }
        return (when (shader) {
            is ContentShader<*> -> ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = runtimeShaderBuilder.apply {
                    with(shader.shaderState) { shaderUniformProvider.applyUniforms() }
                    shaderUniformProvider.updateResolution(size)
                },
                shaderName = ContentShader.CONTENT,
                input = null,
            )

            else -> ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = runtimeShaderBuilder.apply {
                    with(shader.shaderState) { shaderUniformProvider.applyUniforms() }
                    shaderUniformProvider.updateResolution(size)
                },
                shaderNames = emptyArray(),
                inputs = emptyArray(),
            )
        }).asComposeRenderEffect()
    }
}

private class ShaderUniformProviderImpl(
    private val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(Shader.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShaderBuilder.uniform(name, value1, value2)

    // Cached by bitmap identity so re-applying the same instance every frame skips the conversion.
    private val childShaderCache = mutableMapOf<String, Pair<ImageBitmap, SkiaShader>>()

    override fun uniform(name: String, value: ImageBitmap) {
        val cached = childShaderCache[name]
        val childShader = if (cached != null && cached.first === value) cached.second else Image
            .makeFromBitmap(value.asSkiaBitmap())
            .makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR)
            .also { childShaderCache[name] = value to it }
        runtimeShaderBuilder.child(name, childShader)
    }
}