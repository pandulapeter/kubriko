/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
data class ChromaticAberrationShader(
    override var state: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<ChromaticAberrationShader.State> {
    override val cache = Shader.Cache()
    override val code = CODE

    data class State(
        val intensity: Float = 20f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(INTENSITY, intensity)
        }
    }

    companion object {
        private const val INTENSITY = "intensity"
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform float $INTENSITY;
uniform shader ${ContentShader.CONTENT}; 

half4 main(vec2 fragCoord) {
    vec2 uv = fragCoord.xy / ${Shader.RESOLUTION}.xy;
    half4 color = ${ContentShader.CONTENT}.eval(fragCoord);
    vec2 offset = $INTENSITY / ${Shader.RESOLUTION}.xy;
    color.r = ${ContentShader.CONTENT}.eval(${Shader.RESOLUTION}.xy * ((uv - 0.5) * (1.0 + offset) + 0.5)).r;
    color.b = ${ContentShader.CONTENT}.eval(${Shader.RESOLUTION}.xy * ((uv - 0.5) * (1.0 - offset) + 0.5)).b;
    return color; 
}
"""
    }
}