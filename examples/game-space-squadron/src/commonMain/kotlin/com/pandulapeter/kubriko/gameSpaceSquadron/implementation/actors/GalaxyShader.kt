/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Birdmachine
 * https://www.shadertoy.com/view/Dl2XWD
 */
internal class GalaxyShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<GalaxyShader.State>, Dynamic, Unique {
    override var shaderState = initialState
        private set
    override val shaderCode = CODE
    override val shaderCache = Shader.Cache()
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    private var time = 0f

    override fun update(deltaTimeInMilliseconds: Int) {
        time += deltaTimeInMilliseconds
        shaderState = shaderState.copy(time = time / 1000f)
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
const float colorChangeMultiplier = 30.0;

float mod289(float x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 perm(vec4 x) {
    return mod289(((x * 34.0) + 1.0) * x);
}

float noise(vec3 p) {
    vec3 a = floor(p);
    vec3 d = p - a;
    d = d * d * (3.0 - 2.0 * d);
    vec4 b = a.xxyy + vec4(0.0, 1.0, 0.0, 1.0);
    vec4 k1 = perm(b.xyxy);
    vec4 k2 = perm(k1.xyxy + b.zzww);
    vec4 c = k2 + a.zzzz;
    vec4 k3 = perm(c);
    vec4 k4 = perm(c + 1.0);
    vec4 o1 = fract(k3 * (1.0 / 41.0));
    vec4 o2 = fract(k4 * (1.0 / 41.0));
    vec4 o3 = o2 * d.z + o1 * (1.0 - d.z);
    vec2 o4 = o3.yw * d.x + o3.xz * (1.0 - d.x);
    return o4.y * d.y + o4.x * (1.0 - d.y);
}

float field1(in vec3 p,float s) {
	float strength = 7.;
	float accum = s/4.;
	float prev = 0.;
	float tw = 0.;
	for (int i = 0; i < 26; ++i) {
		float mag = dot(p, p);
		p = abs(p) / mag + vec3(-.5, -.4, -1.5);
		float w = exp(-float(i) / 7.);
		accum += w * exp(-strength * pow(abs(mag - prev), 2.2));
		tw += w;
		prev = mag;
	}
	return max(0., 5. * accum / tw - .7);
}

float field2(in vec3 p, float s) {
	float strength = 8.;
	float accum = s/4.;
	float prev = 0.;
	float tw = 0.;
	for (int i = 0; i < 18; ++i) {
		float mag = dot(p, p);
		p = abs(p) / mag + vec3(-.5, -.4, -1.5);
		float w = exp(-float(i) / 7.);
		accum += w * exp(-strength * pow(abs(mag - prev), 2.2));
		tw += w;
		prev = mag;
	}
	return max(0., 5. * accum / tw - .7);
}

vec3 nrand3(vec2 co) {
	vec3 a = fract( cos( co.x*8.3e-3 + co.y )*vec3(1.3e5, 4.7e5, 2.9e5) );
	vec3 b = fract( sin( co.x*0.3e-3 + co.y )*vec3(8.1e5, 1.0e5, 0.1e5) );
	return mix(a, b, 0.5);
}

vec4 main(in vec2 fragCoord) {
    float adjustedSpeed = $TIME * $SPEED;
    vec2 uv = 2. * fragCoord.xy / ${Shader.RESOLUTION}.xy - 1.;
	vec2 uvs = uv * ${Shader.RESOLUTION}.xy / max(${Shader.RESOLUTION}.x, ${Shader.RESOLUTION}.y);
	vec3 p = vec3(uvs / 4., 0) + vec3(1., 1.3, 0.);
	
	float freqs[4];
    float colorChange = adjustedSpeed * colorChangeMultiplier;
	freqs[0] = noise(vec3(1, 0.25, colorChange));
	freqs[1] = noise(vec3(7, 0.25, colorChange));
	freqs[2] = noise(vec3(15, 0.25, colorChange));
	freqs[3] = noise(vec3(30, 0.25, colorChange));

    // Cloud layer 1
	float t = field1(p,freqs[2]);
	float v = (1. - exp((abs(uv.x) - 1.) * 6.)) * (1. - exp((abs(uv.y) - 1.) * 6.));
	
    // Cloud layer 2
	vec3 p2 = vec3(uvs / 5.0, 1.5) + vec3(2., 1.2, -1);
	float t2 = field2(p2,freqs[3]);
	vec4 c2 = mix(.4, 1., v) * vec4(1.3 * t2 * t2 * t2 ,1.8  * t2 * t2 , t2* freqs[0], t2);
	
	// Stars layer 1
	vec2 seed1 = floor((uvs / 4 + vec2(0.0, -adjustedSpeed * 6.48)) * ${Shader.RESOLUTION}.x);
	vec3 rnd1 = nrand3(seed1);
	vec4 starColor = vec4(pow(rnd1.y, 45.0));
	
	// Stars layer 2	
	vec2 seed2 = floor((uvs / 4 + vec2(0.0, -adjustedSpeed * 4.18)) * ${Shader.RESOLUTION}.x);
	vec3 rnd2 = nrand3(seed2);
	starColor += vec4(pow(rnd2.y, 75.0));

	return mix(mix(freqs[3]-.3, 1., v) * vec4(1.5*freqs[2] * t * t* t , 1.2*freqs[1] * t * t, freqs[3]*t, 1.0)+c2, starColor * 1.5, 0.65);
}
"""
    }
}