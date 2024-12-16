package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Credit:  deusnovus
 * https://www.shadertoy.com/view/7ldGWf
 */
internal class FogShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<FogShader.State>, Dynamic, Unique {
    override var state = initialState
        private set
    override val code = CODE
    override val cache = Shader.Cache()
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    private var time = 0f

    override fun update(deltaTimeInMillis: Float) {
        time += deltaTimeInMillis
        state = state.copy(time = time / 1000f)
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
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
uniform float $SPEED;

const vec3 fogColor = vec3(0.3, 0.3, 0.3);
const vec3 backgroundColor = vec3(0.0, 0.0, 0.0);
const float zoom = 2.0;
const int octaves = 4;
const float intensity = 2.;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9818,79.279)))*43758.5453123);
}

vec2 random2(vec2 st){
    st = vec2( dot(st,vec2(127.1,311.7)), dot(st,vec2(269.5,183.3)) );
    return -1.0 + 2.0 * fract(sin(st) * 7.);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    vec2 u = f*f*(3.0-2.0*f);
    return mix( mix( dot( random2(i + vec2(0.0,0.0) ), f - vec2(0.0,0.0) ),
                     dot( random2(i + vec2(1.0,0.0) ), f - vec2(1.0,0.0) ), u.x),
                mix( dot( random2(i + vec2(0.0,1.0) ), f - vec2(0.0,1.0) ),
                     dot( random2(i + vec2(1.0,1.0) ), f - vec2(1.0,1.0) ), u.x), u.y);
}

float fractal_brownian_motion(vec2 coord) {
	float value = 0.0;
	float scale = 0.2;
	for (int i = 0; i < 4; i++) {
		value += noise(coord) * scale;
		coord *= 2.0;
		scale *= 0.5;
	}
	return value + 0.2;
}

float4 main(in vec2 fragCoord)
{
    vec2 st = fragCoord.xy / ${Shader.RESOLUTION}.xy;
	st *= ${Shader.RESOLUTION}.xy  / ${Shader.RESOLUTION}.y;    
    vec2 pos = vec2(st * zoom);
	vec2 motion = vec2(fractal_brownian_motion(pos + vec2($TIME * -0.5, $TIME * -0.3)));
	float final = fractal_brownian_motion(pos + motion) * intensity;
    return vec4(mix(backgroundColor, fogColor, final), 1.0);
}
"""
    }
}