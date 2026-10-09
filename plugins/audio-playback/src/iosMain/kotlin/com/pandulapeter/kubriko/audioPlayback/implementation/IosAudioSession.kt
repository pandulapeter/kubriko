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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionCategorySoloAmbient
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
internal fun configureAudioSession() {
    val session = AVAudioSession.sharedInstance()
    // SoloAmbient is the system default, so any other category was chosen by the host app and is kept.
    if (session.category == AVAudioSessionCategorySoloAmbient) {
        session.setCategory(AVAudioSessionCategoryAmbient, error = null)
    }
    session.setActive(true, error = null)
}
