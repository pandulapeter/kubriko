package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class VignetteShader(
    private val intensity: Float = 30f,
    private val decayFactor: Float = 0.6f,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform shader ${ShaderManager.UNIFORM_CONTENT}; 
    uniform float $UNIFORM_INTENSITY;
    uniform float $UNIFORM_DECAY_FACTOR;

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.UNIFORM_RESOLUTION}.xy;
        half4 color = ${ShaderManager.UNIFORM_CONTENT}.eval(fragCoord);
        uv *=  1.0 - uv.yx;
        float vig = clamp(uv.x*uv.y * $UNIFORM_INTENSITY, 0., 1.);
        vig = pow(vig, $UNIFORM_DECAY_FACTOR);
        return half4(vig * color.rgb, color.a);
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_INTENSITY, intensity)
        provider.uniform("decayFactor", decayFactor)
    }

    companion object {
        private const val UNIFORM_INTENSITY = "intensity"
        private const val UNIFORM_DECAY_FACTOR = "decayFactor"
    }
}