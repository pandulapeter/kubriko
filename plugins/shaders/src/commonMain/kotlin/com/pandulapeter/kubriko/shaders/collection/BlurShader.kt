/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * A specialized shader that applies a Gaussian-like blur to a layer or the entire scene.
 *
 * Note: This shader is handled as an exception by the engine and uses native blur implementations
 * where available for better performance.
 *
 * @param shaderState The initial state of the blur (radius and edge mode).
 * @param layerIndex The index of the layer to blur. If null, the entire scene is blurred.
 */
open class BlurShader(
    override var shaderState: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<BlurShader.State> {
    final override val shaderCache = Shader.Cache()
    final override val shaderCode = CODE

    /**
     * The state of the [BlurShader].
     *
     * @param blurHorizontal The horizontal blur radius in pixels.
     * @param blurVertical The vertical blur radius in pixels.
     * @param mode The edge treatment mode (e.g., clamp, repeat).
     */
    data class State(
        val blurHorizontal: Float = 20f,
        val blurVertical: Float = 20f,
        val mode: Mode = Mode.CLAMP,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() = Unit
    }

    /**
     * Edge treatment modes for the blur effect.
     */
    enum class Mode {
        /**
         * Clamps the edge pixels.
         */
        CLAMP,

        /**
         * Repeats the content at the edges.
         */
        REPEAT,

        /**
         * Mirrors the content at the edges.
         */
        MIRROR,

        /**
         * Uses a transparent color for pixels outside the bounds.
         */
        DECAL
    }

    companion object {
        // Not used. The BlurShader is an exception that's handled differently compared to other shaders.
        private const val CODE = """
half4 main(vec2 fragCoord) {
    return half4(0);
}
"""
    }
}