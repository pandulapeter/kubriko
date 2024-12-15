package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Pablo Roman Andrioli
 * https://www.shadertoy.com/view/XlfGRj
 */
internal class SpaceBackgroundShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<SpaceBackgroundShader.State>, Dynamic {
    override var state = initialState
        private set
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $TIME;
    uniform float $SPEED;
    uniform shader ${ShaderManager.CONTENT};
    
    const int starDensity = 18;
    const float formuParam = 0.73;
    
    const int volumeSteps = 15;
    const float stepSize = 0.12;
    const float zoom = 0.5;
    const float tile = 1;
    const float brightness = 0.0015;
    const float darkMatter = 0.2;
    const float distanceFade = 0.8;
    const float saturation = 0.55;
    
    vec4 main(in vec2 fragCoord)
    {
        vec2 uv = fragCoord.xy / float2(${ShaderManager.RESOLUTION}.x, ${ShaderManager.RESOLUTION}.x);
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