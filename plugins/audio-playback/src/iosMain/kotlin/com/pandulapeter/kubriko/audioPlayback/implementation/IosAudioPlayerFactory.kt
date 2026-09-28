/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSError
import platform.Foundation.NSURL

/** Creates a player for [uri] that is ready to play, or returns `null` when the URI is malformed or the file can't be opened. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun createPreparedAudioPlayer(uri: String): AVAudioPlayer? = runCatching {
    val url = NSURL.URLWithString(URLString = uri) ?: return null
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val player = AVAudioPlayer(url, error.ptr)
        if (error.value != null) null else player.apply { prepareToPlay() }
    }
}.getOrNull()
