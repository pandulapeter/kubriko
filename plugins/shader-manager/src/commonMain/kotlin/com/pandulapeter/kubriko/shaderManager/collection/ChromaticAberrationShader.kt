package com.pandulapeter.kubriko.shaderManager.collection

import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

data class ChromaticAberrationShader(
    private val intensity: Float = 20f,
) : Shader {
    override val code = """
    uniform float2 resolution;
    uniform float intensity;
    uniform shader content; 

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / resolution.xy;
        half4 color = content.eval(fragCoord);
        vec2 offset = intensity / resolution.xy;
        color.r = content.eval(resolution.xy * ((uv - 0.5) * (1.0 + offset) + 0.5)).r;
        color.b = content.eval(resolution.xy * ((uv - 0.5) * (1.0 - offset) + 0.5)).b;
        return color; 
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform("intensity", intensity)
    }
}