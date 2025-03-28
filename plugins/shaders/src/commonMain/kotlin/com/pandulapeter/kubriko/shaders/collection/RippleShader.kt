/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Juraj Kusnier
 * https://github.com/jurajkusnier/agsl-fun
 */
class RippleShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<RippleShader.State>, Dynamic {
    override var shaderState = initialState
        private set
    override val shaderCache = Shader.Cache()
    override val shaderCode = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        shaderState = shaderState.copy(time = shaderState.time + deltaTimeInMilliseconds)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 6.28f,
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
uniform shader ${ContentShader.CONTENT};
uniform float $TIME;
uniform float $SPEED;

half4 main(float2 fragCoord) {
    float scale = 1 / ${Shader.RESOLUTION}.x;
    float2 scaledCoord = fragCoord * scale;
    float2 center = ${Shader.RESOLUTION} * 0.5 * scale;
    float dist = distance(scaledCoord, center);
    float2 dir = scaledCoord - center;
    float sin = sin(dist * 70 - $TIME * $SPEED);
    float2 offset = dir * sin;
    float2 textCoord = scaledCoord + offset / 30;
    return ${ContentShader.CONTENT}.eval(textCoord / scale);
}
"""
    }
}