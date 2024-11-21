package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class SmoothPixelationShader(
    private val pixelSize: Float = 2f,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform shader ${ShaderManager.CONTENT}; 
    uniform float $UNIFORM_PIXEL_SIZE;

    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.RESOLUTION}.xy;
        float factor = (abs(sin(${ShaderManager.RESOLUTION}.y * (uv.y - 0.5) / $UNIFORM_PIXEL_SIZE)) + abs(sin(${ShaderManager.RESOLUTION}.x * (uv.x - 0.5) / $UNIFORM_PIXEL_SIZE))) / 2.0;
        half4 color = ${ShaderManager.CONTENT}.eval(fragCoord);
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