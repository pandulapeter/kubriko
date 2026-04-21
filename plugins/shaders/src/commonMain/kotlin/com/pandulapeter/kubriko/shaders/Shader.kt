/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * An interface for actors that apply custom SKSL shaders to the scene.
 *
 * Shaders can be applied to specific layers or the entire scene.
 *
 * @param T The type of the shader state which holds the uniforms.
 */
interface Shader<T : Shader.State> : LayerAware {

    /**
     * The current state of the shader, containing uniform values.
     */
    val shaderState: T

    /**
     * Internal cache for the compiled shader and uniform provider.
     */
    val shaderCache: Cache

    /**
     * The SKSL source code of the shader.
     */
    val shaderCode: String

    /**
     * The index of the layer this shader should be applied to.
     * If null, the shader is applied to the entire scene.
     */
    override val layerIndex: Int? get() = null

    /**
     * Interface for the shader's uniform state.
     */
    interface State {
        /**
         * Applies the uniforms to the shader.
         *
         * @receiver The provider used to set uniform values.
         */
        fun ShaderUniformProvider.applyUniforms() = Unit
    }

    /**
     * Cache container for platform-specific shader objects.
     */
    class Cache {
        internal var runtimeShader: Any? = null
        internal var uniformProvider: ShaderUniformProvider? = null
    }

    companion object {
        /**
         * The reserved uniform name for the viewport resolution.
         */
        const val RESOLUTION = "resolution"
    }
}
