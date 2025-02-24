/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.collection.BlurShader

internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (shader is BlurShader) {
            return RenderEffect.createBlurEffect(
                shader.shaderState.blurHorizontal,
                shader.shaderState.blurVertical,
                when (shader.shaderState.mode) {
                    BlurShader.Mode.CLAMP -> TileMode.CLAMP
                    BlurShader.Mode.REPEAT -> TileMode.REPEAT
                    BlurShader.Mode.MIRROR -> TileMode.MIRROR
                    BlurShader.Mode.DECAL -> TileMode.DECAL
                },
            ).asComposeRenderEffect()
        } else {
            val runtimeShader =
                (shader.shaderCache.runtimeShader as? RuntimeShader) ?: RuntimeShader(shader.shaderCode.trimIndent()).also {
                    shader.shaderCache.runtimeShader = it
                }
            val shaderUniformProvider =
                (shader.shaderCache.uniformProvider as? ShaderUniformProviderImpl)
                    ?: ShaderUniformProviderImpl(runtimeShader).also { shader.shaderCache.uniformProvider = it }
            return (when (shader) {
                is ContentShader<*> -> RenderEffect.createRuntimeShaderEffect(
                    runtimeShader.apply {
                        with(shader.shaderState) { shaderUniformProvider.applyUniforms() }
                        shaderUniformProvider.updateResolution(size)
                    },
                    ContentShader.CONTENT,
                )

                else -> RenderEffect.createShaderEffect(
                    runtimeShader.apply {
                        with(shader.shaderState) { shaderUniformProvider.applyUniforms() }
                        shaderUniformProvider.updateResolution(size)
                    },
                )
            }).asComposeRenderEffect()
        }
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