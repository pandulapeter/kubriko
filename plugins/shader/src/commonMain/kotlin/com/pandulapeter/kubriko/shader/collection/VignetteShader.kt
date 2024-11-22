package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class VignetteShader(
    override var state: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<VignetteShader.State> {
    override val cache = Shader.Cache()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform shader ${ShaderManager.CONTENT}; 
    uniform float $INTENSITY;
    uniform float $DECAY_FACTOR;

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.RESOLUTION}.xy;
        half4 color = ${ShaderManager.CONTENT}.eval(fragCoord);
        uv *=  1.0 - uv.yx;
        float vig = clamp(uv.x*uv.y * $INTENSITY, 0., 1.);
        vig = pow(vig, $DECAY_FACTOR);
        return half4(vig * color.rgb, color.a);
    }
""".trimIndent()

    data class State(
        private val intensity: Float = 30f,
        private val decayFactor: Float = 0.6f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(INTENSITY, intensity)
            uniform(DECAY_FACTOR, decayFactor)
        }
    }

    companion object {
        private const val INTENSITY = "intensity"
        private const val DECAY_FACTOR = "decayFactor"
    }
}