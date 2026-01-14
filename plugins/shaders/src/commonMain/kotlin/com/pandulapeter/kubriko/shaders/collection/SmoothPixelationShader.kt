/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Manel Martos Roldán
 * https://github.com/manuel-martos/Photo-FX
 */
data class SmoothPixelationShader(
    override var shaderState: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<SmoothPixelationShader.State> {
    override val shaderCache = Shader.Cache()
    override val shaderCode = CODE

    data class State(
        val pixelSize: Float = 2f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(PIXEL_SIZE, pixelSize)
        }
    }

    companion object {
        private const val PIXEL_SIZE = "pixelSize"
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform shader ${ContentShader.CONTENT}; 
uniform float $PIXEL_SIZE;

vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord.xy / ${Shader.RESOLUTION}.xy;
    float factor = (abs(sin(${Shader.RESOLUTION}.y * (uv.y - 0.5) / $PIXEL_SIZE)) + abs(sin(${Shader.RESOLUTION}.x * (uv.x - 0.5) / $PIXEL_SIZE))) / 2.0;
    half4 color = ${ContentShader.CONTENT}.eval(fragCoord);
    return half4(factor * color.rgb, color.a); 
}
"""
    }
}