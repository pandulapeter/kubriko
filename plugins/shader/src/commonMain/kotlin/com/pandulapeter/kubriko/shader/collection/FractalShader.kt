package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class FractalShader(
    private val time: Float = 0f,
    private val red: Int = 2,
    private val green: Int = 5,
    private val blue: Int = 12,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform float $UNIFORM_TIME;
    uniform int $UNIFORM_RED;
    uniform int $UNIFORM_GREEN;
    uniform int $UNIFORM_BLUE;
    uniform shader ${ShaderManager.UNIFORM_CONTENT};
    
    float f(float3 p) {
        p.z -= $UNIFORM_TIME * 5.;
        float a = p.z * .1;
        p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
        return .1 - length(cos(p.xy) + sin(p.yz));
    }
    
    half4 main(float2 fragcoord) { 
        float3 d = .5 - fragcoord.xy1 / ${ShaderManager.UNIFORM_RESOLUTION}.y;
        float3 p=float3(0);
        for (int i = 0; i < 32; i++) {
          p += f(p) * d;
        }
        return ((sin(p) + float3($UNIFORM_RED, $UNIFORM_GREEN, $UNIFORM_BLUE)) / length(p)).xyz1;
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_TIME, time)
        provider.uniform(UNIFORM_RED, red)
        provider.uniform(UNIFORM_GREEN, green)
        provider.uniform(UNIFORM_BLUE, blue)
    }

    companion object {
        private const val UNIFORM_TIME = "time"
        private const val UNIFORM_RED = "red"
        private const val UNIFORM_GREEN = "green"
        private const val UNIFORM_BLUE = "blue"
    }
}