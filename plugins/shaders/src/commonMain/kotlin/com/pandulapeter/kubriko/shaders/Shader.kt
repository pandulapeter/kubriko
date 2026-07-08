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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
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
         * A cheap marker for whether the uniform values held by this state changed since it was last
         * read. When it reports the same value as the previous frame (and the layer size is also
         * unchanged), the native `RenderEffect` is reused instead of rebuilt. Defaults to
         * [DIRTINESS_UNKNOWN], which always forces a rebuild — the safe behavior for implementations
         * that don't override it. Override with e.g. a value derived from the uniforms themselves
         * (bumped only when they actually change) to skip rebuilding while a shader is static, such as
         * during an idle-throttled tick rate.
         */
        val dirtinessToken: Int get() = DIRTINESS_UNKNOWN

        /**
         * Applies the uniforms to the shader.
         *
         * @receiver The provider used to set uniform values.
         */
        fun ShaderUniformProvider.applyUniforms() = Unit

        companion object {
            /**
             * The default [dirtinessToken]: never compares as equal to a previous token, so the
             * `RenderEffect` is rebuilt on every frame.
             */
            const val DIRTINESS_UNKNOWN = Int.MIN_VALUE
        }
    }

    /**
     * Cache container for platform-specific shader objects.
     */
    class Cache {
        internal var runtimeShader: Any? = null
        internal var uniformProvider: ShaderUniformProvider? = null
        internal var cachedRenderEffect: RenderEffect? = null
        internal var cachedDirtinessToken: Int = State.DIRTINESS_UNKNOWN
        internal var cachedSize: Size? = null
    }

    companion object {
        /**
         * The reserved uniform name for the viewport resolution.
         */
        const val RESOLUTION = "resolution"
    }
}
