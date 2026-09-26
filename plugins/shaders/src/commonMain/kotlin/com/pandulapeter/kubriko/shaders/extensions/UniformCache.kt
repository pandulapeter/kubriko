/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

/**
 * What each uniform of one platform shader object was last set to, so a value that object already holds isn't handed
 * over again. Every set is a lookup by name on the native side, and on the web it also copies the name into Skia's
 * memory a byte per call - while most of a shader's uniforms stay put from one frame to the next.
 *
 * [hasChanges] records whether anything did change since it was last reset, which is what lets a render effect or a
 * shader built from the same values be reused rather than built again.
 */
internal class UniformCache {

    var hasChanges = false

    private val values = HashMap<String, UniformValue>()
    private val children = HashMap<String, Any>()

    /** Values are compared by their raw bits, so a change between -0 and 0 or between two NaNs still counts. */
    fun hasChanged(name: String, kind: Int, first: Int, second: Int = 0): Boolean {
        val value = values.getOrPut(name) { UniformValue() }
        if (value.kind == kind && value.first == first && value.second == second) return false
        value.kind = kind
        value.first = first
        value.second = second
        hasChanges = true
        return true
    }

    fun hasChildChanged(name: String, child: Any): Boolean {
        if (children[name] === child) return false
        children[name] = child
        hasChanges = true
        return true
    }

    private class UniformValue {
        var kind = KIND_UNSET
        var first = 0
        var second = 0
    }

    companion object {
        private const val KIND_UNSET = 0
        const val KIND_INT = 1
        const val KIND_FLOAT = 2
        const val KIND_FLOAT2 = 3
    }
}
