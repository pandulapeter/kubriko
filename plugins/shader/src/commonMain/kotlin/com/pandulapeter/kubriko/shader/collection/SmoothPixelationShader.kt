package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.ContentShader
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Credit: Manel Martos Roldán
 * https://github.com/manuel-martos/Photo-FX
 */
data class SmoothPixelationShader(
    override var state: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<SmoothPixelationShader.State> {
    override val cache = Shader.Cache()
    override val code = CODE

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