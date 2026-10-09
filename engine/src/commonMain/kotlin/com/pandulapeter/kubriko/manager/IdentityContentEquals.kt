/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

/**
 * Reference-equality, allocation-free comparison of a published list against a freshly culled scratch buffer.
 * Actors don't override equals, so identity comparison is the correct notion of "same set in the same order" and
 * tells when re-publishing the StateFlow is unnecessary. The receiver is the published persistent list (iterated,
 * since its indexed get() is a trie walk); `other` is the scratch ArrayList (indexed, genuinely O(1)).
 */
internal fun <T> List<T>.contentEquals(other: List<T>): Boolean {
    if (size != other.size) return false
    var i = 0
    for (element in this) {
        if (element !== other[i]) return false
        i++
    }
    return true
}
