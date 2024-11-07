package com.pandulapeter.kubriko.shaderManager.collection

import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

data class VignetteShader(
    private val intensity: Float = 30f,
    private val decayFactor: Float = 0.6f,
) : Shader {
    override val code = """
    uniform float2 resolution;
    uniform shader content; 
    uniform float intensity;
    uniform float decayFactor;

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / resolution.xy;
        half4 color = content.eval(fragCoord);
        uv *=  1.0 - uv.yx;
        float vig = clamp(uv.x*uv.y * intensity, 0., 1.);
        vig = pow(vig, decayFactor);
        return half4(vig * color.rgb, color.a);
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform("intensity", intensity)
        provider.uniform("decayFactor", decayFactor)
    }
}