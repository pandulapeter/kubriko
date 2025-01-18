/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.collections.immutable.PersistentMap

internal actual fun MusicPlayer.onManagerDisposed(cache: PersistentMap<String, Any?>) {
    cache.values.filterNotNull().forEach { music -> dispose(music) }
    dispose()
}