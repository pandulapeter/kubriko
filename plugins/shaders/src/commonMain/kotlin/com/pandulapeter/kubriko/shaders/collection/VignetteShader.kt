package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Manel Martos Roldán
 * https://github.com/manuel-martos/Photo-FX
 */
data class VignetteShader(
    override var state: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<VignetteShader.State> {
    override val cache = Shader.Cache()
    override val code = CODE

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
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform shader ${ContentShader.CONTENT}; 
uniform float $INTENSITY;
uniform float $DECAY_FACTOR;

half4 main(vec2 fragCoord) {
    vec2 uv = fragCoord.xy / ${Shader.RESOLUTION}.xy;
    half4 color = ${ContentShader.CONTENT}.eval(fragCoord);
    uv *=  1.0 - uv.yx;
    float vig = clamp(uv.x*uv.y * $INTENSITY, 0., 1.);
    vig = pow(vig, $DECAY_FACTOR);
    return half4(vig * color.rgb, color.a);
}
"""
    }
}