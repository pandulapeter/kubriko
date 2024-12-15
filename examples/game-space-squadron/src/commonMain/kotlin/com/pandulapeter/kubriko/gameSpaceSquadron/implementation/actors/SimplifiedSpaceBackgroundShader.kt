package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * https://www.shadertoy.com/view/cdj3DW
 */
internal class SimplifiedSpaceBackgroundShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<SimplifiedSpaceBackgroundShader.State>, Dynamic {
    override var state = initialState
        private set
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $TIME;
    uniform float $SPEED;
    uniform shader ${ShaderManager.CONTENT};
    
    const int MAX_ITER = 20;
    
    float field(vec3 p, float s, int iter)
    {
        float accum = s / 4.0;
        float prev = 0.0;
        float tw = 0.0;
        for (int i = 0; i < MAX_ITER; ++i) 
        {
            if (i >= iter) // drop from the loop if the number of iterations has been completed - workaround for GLSL loop index limitation
            {
                break;
            }
            float mag = dot(p, p);
            p = abs(p) / mag + vec3(-0.5, -0.4, -1.487);
            float w = exp(-float(i) / 5.0);
            accum += w * exp(-9.025 * pow(abs(mag - prev), 2.2));
            tw += w;
            prev = mag;
        }
        return max(0.0, 5.2 * accum / tw - 0.65);
    }
    
    vec3 nrand3(vec2 co)
    {
        vec3 a = fract(cos(co.x*8.3e-3 + co.y) * vec3(1.3e5, 4.7e5, 2.9e5));
        vec3 b = fract(sin(co.x*0.3e-3 + co.y) * vec3(8.1e5, 1.0e5, 0.1e5));
        vec3 c = mix(a, b, 0.5);
        return c;
    }
    
    vec4 starLayer(vec2 p, float time)
    {
        vec2 seed = 1.9 * p.xy;
        seed = floor(seed * max( ${ShaderManager.RESOLUTION}.x, 600.0) / 1.5);
        vec3 rnd = nrand3(seed);
        vec4 col = vec4(pow(rnd.y, 17.0));
        float mul = 10.0 * rnd.x;
        col.xyz *= sin(time * mul + mul) * 0.25 + 1.0;
        return col;
    }

    vec4 main(in vec2 fragCoord)
    {
        float time =  $TIME / (${ShaderManager.RESOLUTION}.x / 1000.0);
        vec2 uv = 2.0 * fragCoord /  ${ShaderManager.RESOLUTION}.xy - 1.0;
        vec2 uvs = uv *  ${ShaderManager.RESOLUTION}.xy / max( ${ShaderManager.RESOLUTION}.x,  ${ShaderManager.RESOLUTION}.y);
        vec3 p = vec3(uvs / 2.5, 0.0) + vec3(0.8, -1.3, 0.0);
        p += vec3(0.0, -time / 16.0, 0.0);
        float freqs[4];
        freqs[0] = 0.45;
        freqs[1] = 0.4;
        freqs[2] = 0.15;
        freqs[3] = 0.9;
    
        float t = field(p, freqs[2], 13);
        float v = (1.0 - exp((abs(uv.x) - 1.0) * 6.0)) * (1.0 - exp((abs(uv.y) - 1.0) * 6.0));
        vec3 p2 = vec3(uvs / (4.0 + sin(time * 0.11) * 0.2 + 0.2 + sin(time * 0.15) * 0.3 + 0.4), 4.0) + vec3(2.0, -1.3, -1.0);
        p2 += vec3(0.0, -time / 14.0, 0.0);
        float t2 = field(p2, freqs[3], 18);
        vec4 c2 = mix(0.5, 0.2, v) * vec4(5.5 * t2 * t2 * t2, 2.1 * t2 * t2, 2.2 * t2 * freqs[0], t2);
        
        // add stars (source: https://glslsandbox.com/e#6904.0)
        vec4 starColour = vec4(0.0);
        starColour += starLayer(p.xy, time); // add first layer of stars
        starColour += starLayer(p2.xy, time); // add second layer of stars
    
        const float brightness = 1.0;
        vec4 colour = mix(freqs[3] - 0.3, 1.0, v) * vec4(1.5 * freqs[2] * t * t * t, 1.2 * freqs[1] * t * t, freqs[3] * t, 1.0) + c2 + starColour;
        return vec4(brightness * colour.xyz, 1.0);
    }
""".trimIndent()
    override val cache = Shader.Cache()
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
        val speed: Float = 0.01f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
    }
}