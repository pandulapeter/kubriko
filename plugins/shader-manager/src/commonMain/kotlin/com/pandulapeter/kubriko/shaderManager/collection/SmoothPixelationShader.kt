package com.pandulapeter.kubriko.shaderManager.collection

import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

data class SmoothPixelationShader(
    private val pixelSize: Float = 2f,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform shader ${ShaderManager.UNIFORM_CONTENT}; 
    uniform float $UNIFORM_PIXEL_SIZE;

    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.UNIFORM_RESOLUTION}.xy;
        float factor = (abs(sin(${ShaderManager.UNIFORM_RESOLUTION}.y * (uv.y - 0.5) / $UNIFORM_PIXEL_SIZE)) + abs(sin(${ShaderManager.UNIFORM_RESOLUTION}.x * (uv.x - 0.5) / $UNIFORM_PIXEL_SIZE))) / 2.0;
        half4 color = ${ShaderManager.UNIFORM_CONTENT}.eval(fragCoord);
        return half4(factor * color.rgb, color.a); 
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_PIXEL_SIZE, pixelSize)
    }

    companion object {
        private const val UNIFORM_PIXEL_SIZE = "pixelSize"
    }
}