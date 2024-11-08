package com.pandulapeter.kubriko.shaderManager.collection

import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

data class FractalShader(
    private val time: Float = 0f,
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform float $UNIFORM_TIME;
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
        return ((sin(p) + float3(2, 5, 12)) / length(p)).xyz1;
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_TIME, time)
    }

    companion object {
        private const val UNIFORM_TIME = "time"
    }
}