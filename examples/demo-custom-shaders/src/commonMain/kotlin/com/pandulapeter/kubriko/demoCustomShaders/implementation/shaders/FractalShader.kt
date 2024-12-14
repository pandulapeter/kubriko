package com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Yutaka Sato
 * https://x.com/notargs/status/1250468645030858753
 */
internal class FractalShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<FractalShader.State>, Dynamic {
    override var state = initialState
        private set
    override val cache = Shader.Cache()
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
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMillis: Float) {
        state = state.copy(time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.state = state.copy(time = this.state.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 2f,
        val red: Int = 4,
        val green: Int = 4,
        val blue: Int = 18,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
            uniform(RED, red)
            uniform(GREEN, green)
            uniform(BLUE, blue)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val RED = "red"
        private const val GREEN = "green"
        private const val BLUE = "blue"
    }
}