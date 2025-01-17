/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): RenderEffect? {
    val runtimeShaderBuilder =
        (shader.cache.runtimeShader as? RuntimeShaderBuilder) ?: RuntimeShaderBuilder(RuntimeEffect.makeForShader(shader.code.trimIndent())).also { shader.cache.runtimeShader = it }
    val shaderUniformProvider =
        (shader.cache.uniformProvider as? ShaderUniformProviderImpl) ?: ShaderUniformProviderImpl(runtimeShaderBuilder).also { shader.cache.uniformProvider = it }
    return (if (shader is ContentShader<*>) ImageFilter.makeRuntimeShader(
        runtimeShaderBuilder = runtimeShaderBuilder.apply {
            with(shader.state) { shaderUniformProvider.applyUniforms() }
            shaderUniformProvider.updateResolution(size)
        },
        shaderName = ContentShader.CONTENT,
        input = null,
    ) else ImageFilter.makeRuntimeShader(
        runtimeShaderBuilder = runtimeShaderBuilder.apply {
            with(shader.state) { shaderUniformProvider.applyUniforms() }
            shaderUniformProvider.updateResolution(size)
        },
        shaderNames = emptyArray(),
        inputs = emptyArray(),
    )).asComposeRenderEffect()
}

private class ShaderUniformProviderImpl(
    private val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(Shader.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShaderBuilder.uniform(name, value1, value2)
}