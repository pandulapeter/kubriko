/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader

internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val runtimeShader = (shader.cache.runtimeShader as? RuntimeShader) ?: RuntimeShader(shader.code.trimIndent()).also { shader.cache.runtimeShader = it }
        val shaderUniformProvider =
            (shader.cache.uniformProvider as? ShaderUniformProviderImpl) ?: ShaderUniformProviderImpl(runtimeShader).also { shader.cache.uniformProvider = it }
        return (if (shader is ContentShader<*>) RenderEffect.createRuntimeShaderEffect(
            runtimeShader.apply {
                with(shader.state) { shaderUniformProvider.applyUniforms() }
                shaderUniformProvider.updateResolution(size)
            },
            ContentShader.CONTENT,
        ) else RenderEffect.createShaderEffect(
            runtimeShader.apply {
                with(shader.state) { shaderUniformProvider.applyUniforms() }
                shaderUniformProvider.updateResolution(size)
            },
        )).asComposeRenderEffect()
    } else {
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ShaderUniformProviderImpl(
    private val runtimeShader: RuntimeShader,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(Shader.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShader.setIntUniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShader.setFloatUniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShader.setFloatUniform(name, value1, value2)
}