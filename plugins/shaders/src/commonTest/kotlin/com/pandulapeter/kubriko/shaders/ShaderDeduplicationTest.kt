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

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ShaderDeduplicationTest {

    @Test
    fun sameShaderOnDifferentLayersIsKept() {
        val first = VignetteShader(layerIndex = 0)
        val second = VignetteShader(layerIndex = 1)

        assertKept(listOf(first, second), first, second)
    }

    @Test
    fun trueDuplicateIsDropped() {
        val first = VignetteShader()

        assertKept(listOf(first, VignetteShader()), first)
    }

    @Test
    fun sameShaderWithDifferentStatesIsKept() {
        val first = VignetteShader()
        val second = VignetteShader(shaderState = VignetteShader.State(intensity = 10f))

        assertKept(listOf(first, second), first, second)
    }

    @Test
    fun sameInstanceAddedTwiceIsKeptOnce() {
        val shader = VignetteShader()

        assertKept(listOf(shader, shader), shader)
    }

    @Test
    fun differentShaderClassesAreKept() {
        val vignette = VignetteShader()
        val chromaticAberration = ChromaticAberrationShader()

        assertKept(listOf(vignette, chromaticAberration), vignette, chromaticAberration)
    }

    @Test
    fun nonShaderActorsAreIgnoredAndOrderIsKept() {
        val chromaticAberration = ChromaticAberrationShader()
        val vignette = VignetteShader()

        assertKept(listOf(object : Actor {}, chromaticAberration, object : Actor {}, vignette), chromaticAberration, vignette)
    }

    private fun assertKept(actors: List<Actor>, vararg expected: Shader<*>) {
        val result = actors.distinctShaders()
        assertEquals(expected.size, result.size)
        expected.forEachIndexed { index, shader -> assertSame(shader, result[index]) }
    }
}
