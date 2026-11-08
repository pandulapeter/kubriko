/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * One [TextureMipChain] per texture, shared by everything that draws it, so each of its levels is built only once.
 *
 * Chains are keyed by anything that names a texture (a file name, a resource), and a key whose texture has been
 * replaced by another bitmap gets a new chain. A lookup of a chain that exists allocates nothing; the chains
 * themselves are still not thread-safe (see [TextureMipChain]).
 */
class TextureMipChainCache internal constructor(
    internal val createChain: (ImageBitmap) -> TextureMipChain,
) {

    /**
     * Creates a cache whose chains hold [levelCount] levels each, the base included.
     */
    constructor(levelCount: Int = TextureMipChain.DEFAULT_LEVEL_COUNT) : this({ TextureMipChain(it, levelCount) })

    private val chains = MutableStateFlow(persistentMapOf<Any, TextureMipChain>())

    /**
     * The chain of the texture named [key], whose full-resolution bitmap is [base]: the existing one while it was built
     * from that same bitmap, otherwise a new one replacing it.
     */
    fun get(key: Any, base: ImageBitmap): TextureMipChain {
        chains.value[key]?.let { if (it.base === base) return it }
        val chain = createChain(base)
        chains.update { it.put(key, chain) }
        return chain
    }

    /** Forgets the chain of the texture named [key], if there is one. */
    fun remove(key: Any) = chains.update { it.remove(key) }

    /** Forgets every chain. */
    fun clear() = chains.update { it.clear() }
}
