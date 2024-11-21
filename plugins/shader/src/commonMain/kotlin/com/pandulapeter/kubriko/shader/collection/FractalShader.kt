package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class FractalShader(
    private val properties: Properties = Properties(),
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $TIME;
    uniform float $SPEED;
    uniform int $RED;
    uniform int $GREEN;
    uniform int $BLUE;
    uniform shader ${ShaderManager.CONTENT};
    
    float f(float3 p) {
        p.z -= $TIME * $SPEED;
        float a = p.z * .1;
        p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
        return .1 - length(cos(p.xy) + sin(p.yz));
    }
    
    half4 main(float2 fragcoord) { 
        float3 d = .5 - fragcoord.xy1 / ${ShaderManager.RESOLUTION}.y;
        float3 p=float3(0);
        for (int i = 0; i < 32; i++) {
          p += f(p) * d;
        }
        return ((sin(p) + float3($RED, $GREEN, $BLUE)) / length(p)).xyz1;
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(TIME, properties.time)
        provider.uniform(SPEED, properties.speed)
        provider.uniform(RED, properties.red)
        provider.uniform(GREEN, properties.green)
        provider.uniform(BLUE, properties.blue)
    }

    data class Properties(
        val time: Float = 0f,
        val speed: Float = 2f,
        val red: Int = 4,
        val green: Int = 4,
        val blue: Int = 18,
    )

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val RED = "red"
        private const val GREEN = "green"
        private const val BLUE = "blue"
    }
}