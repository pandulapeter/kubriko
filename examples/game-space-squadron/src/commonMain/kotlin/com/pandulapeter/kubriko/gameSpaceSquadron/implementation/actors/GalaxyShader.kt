package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Kali, jurnip
 * https://www.shadertoy.com/view/XlfGRj
 * https://www.shadertoy.com/view/cdj3DW
 */
internal class GalaxyShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<GalaxyShader.State>, Dynamic, Unique {
    override var state = initialState
        private set
    override val code = CODE
    override val cache = Shader.Cache()
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    private var time = 0f

    override fun update(deltaTimeInMilliseconds: Float) {
        time += deltaTimeInMilliseconds
        state = state.copy(time = time / 1000f)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 0.003f,
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

const int starDensity = 16;
const float formuParam = 0.73;

const int volumeSteps = 10;
const float stepSize = 0.12;
const float zoom = 0.43;
const float tile = 1.133;
const float brightness = 0.0015;
const float darkMatter = 0.2;
const float distanceFade = 0.8;
const float saturation = 0.55;

const int MAX_ITER = 20;
const vec3 NOISE_SCALE = vec3(8.3e-3, 0.3e-3, 0.11);
const vec3 NOISE_BASE = vec3(1.3e5, 4.7e5, 2.9e5);

float field(vec3 p, float s, int iter) {
    float accum = s / 4.0;
    float mag = 0.0;
    float prev = 0.0;
    float tw = 0.0;

    for (int i = 0; i < MAX_ITER; ++i) {
        if (i >= iter) {
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

vec4 shader2(in vec2 fragCoord) {
    vec2 uv2 = 2.0 * fragCoord / ${Shader.RESOLUTION}.xy - 1.0;
    vec2 uvs = uv2 * ${Shader.RESOLUTION}.xy / max(${Shader.RESOLUTION}.x, ${Shader.RESOLUTION}.y);
    vec3 p = vec3(uvs / 2.5, 0.0) + vec3(0.8, -1.3, 0.0);
    float freqs[4];
    freqs[0] = 0.45;
    freqs[1] = 0.4;
    freqs[2] = 0.15;
    freqs[3] = 0.9;
    float t = field(p, freqs[2], 13);
    vec3 p2 = vec3(uvs / 4.0, 4.0) + vec3(2.0, -1.3, -1.0);        
    float t2 = field(p2, freqs[3], 18);
    vec4 c2 = mix(0.5, 0.2, 1.0 - exp((abs(uv2.x) - 1.0) * 6.0)) * vec4(5.5 * t2 * t2 * t2, 2.1 * t2 * t2, 2.2 * t2 * freqs[0], t2);
    vec4 starColor = vec4(0.0);
    const float brightness = 1.0;
    vec4 color = mix(freqs[3] - 0.3, 1.0, 1.0 - exp((abs(uv2.x) - 1.0) * 6.0)) * vec4(1.5 * freqs[2] * t * t * t, 1.2 * freqs[1] * t * t, freqs[3] * t, 1.0) + c2 + starColor;
    return vec4(brightness * color.xyz, 1.0);
}

vec4 shader1(in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / float2(${Shader.RESOLUTION}.x, ${Shader.RESOLUTION}.x);
    vec3 dir = vec3(uv * zoom, 10.0);
    float time = $TIME * $SPEED + 0.25;
    vec3 from = vec3(0.0, -time, 0.0);

    // Volumetric rendering
    float s = 0.1, fade = 1.0;
    vec3 accumulatedColor = vec3(0.0);

    // Precompute constants for loops
    float invTile = 1.0 / (tile * 2.0);

    for (int r = 0; r < volumeSteps; r++) {
        vec3 p = from + s * dir * 0.5;

        // Tiling fold optimized
        p = abs(vec3(tile) - fract(p * invTile) * tile * 2.0);
        float pa = 0.0, a = 0.0;
        for (int i = 0; i < starDensity; i++) {
            p = abs(p) / dot(p, p) - formuParam;
            float lenP = length(p);
            a += abs(lenP - pa);
            pa = lenP;
        }

        float dm = max(0.0, darkMatter - a * a * 0.001); // Dark matter
        a *= a * a; // Add contrast

        fade *= (r > 6) ? 1.0 - dm : 1.0; // Avoid near dark matter
        accumulatedColor += fade;
        accumulatedColor += vec3(s, s * s, s * s * s * s) * a * brightness * fade; // Coloring

        fade *= distanceFade; // Distance fading
        s += stepSize;
    }

    // Color adjustment
    accumulatedColor = mix(vec3(length(accumulatedColor)), accumulatedColor, saturation);
    return vec4(accumulatedColor * 0.01, 1.0);
}

vec4 main(in vec2 fragCoord) {
    return mix(shader1(fragCoord), shader2(fragCoord), 0.4);
}
"""
    }
}