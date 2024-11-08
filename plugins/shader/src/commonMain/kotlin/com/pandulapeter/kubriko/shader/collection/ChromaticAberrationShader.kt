package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class ChromaticAberrationShader(
    private val intensity: Float = 20f,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform float $UNIFORM_INTENSITY;
    uniform shader ${ShaderManager.UNIFORM_CONTENT}; 

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.UNIFORM_RESOLUTION}.xy;
        half4 color = ${ShaderManager.UNIFORM_CONTENT}.eval(fragCoord);
        vec2 offset = $UNIFORM_INTENSITY / ${ShaderManager.UNIFORM_RESOLUTION}.xy;
        color.r = ${ShaderManager.UNIFORM_CONTENT}.eval(${ShaderManager.UNIFORM_RESOLUTION}.xy * ((uv - 0.5) * (1.0 + offset) + 0.5)).r;
        color.b = ${ShaderManager.UNIFORM_CONTENT}.eval(${ShaderManager.UNIFORM_RESOLUTION}.xy * ((uv - 0.5) * (1.0 - offset) + 0.5)).b;
        return color; 
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_INTENSITY, intensity)
    }

    companion object {
        private const val UNIFORM_INTENSITY = "intensity"
    }
}