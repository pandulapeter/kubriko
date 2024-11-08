package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class RippleShader(
    private val time: Float = 0f,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform float $UNIFORM_TIME;
    uniform shader ${ShaderManager.UNIFORM_CONTENT};
    
    half4 main(float2 fragCoord) {
        float scale = 1 / ${ShaderManager.UNIFORM_RESOLUTION}.x;
        float2 scaledCoord = fragCoord * scale;
        float2 center = ${ShaderManager.UNIFORM_RESOLUTION} * 0.5 * scale;
        float dist = distance(scaledCoord, center);
        float2 dir = scaledCoord - center;
        float sin = sin(dist * 70 - $UNIFORM_TIME * 6.28);
        float2 offset = dir * sin;
        float2 textCoord = scaledCoord + offset / 30;
        return ${ShaderManager.UNIFORM_CONTENT}.eval(textCoord / scale);
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_TIME, time)
    }

    companion object {
        private const val UNIFORM_TIME = "time"
    }
}