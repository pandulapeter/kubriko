package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.ContentShader
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class ChromaticAberrationShader(
    override var state: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<ChromaticAberrationShader.State> {
    override val cache = Shader.Cache()
    override val code = """
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
""".trimIndent()

    data class State(
        val intensity: Float = 20f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(INTENSITY, intensity)
        }
    }

    companion object {
        private const val INTENSITY = "intensity"
    }
}