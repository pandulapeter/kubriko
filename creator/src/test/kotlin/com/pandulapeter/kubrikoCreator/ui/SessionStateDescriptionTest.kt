/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.session_editing
import kubriko.creator.generated.resources.session_playing
import kubriko.creator.generated.resources.session_playing_in_browser
import kubriko.creator.generated.resources.session_playing_on_android
import kubriko.creator.generated.resources.session_playing_on_ios
import kubriko.creator.generated.resources.session_starting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionStateDescriptionTest {

    @Test
    fun theToolbarDescribesEveryActiveSessionState() {
        assertNull(SessionState.Idle.description())
        assertEquals(Res.string.session_starting, SessionState.Starting(SessionMode.PLAY).description())
        assertEquals(Res.string.session_playing, SessionState.Running(SessionMode.PLAY).description())
        assertEquals(Res.string.session_editing, SessionState.Running(SessionMode.EDIT).description())
        assertEquals(Res.string.session_playing_in_browser, SessionState.Running(SessionMode.PLAY, PlayTarget.WEB).description())
        assertEquals(
            Res.string.session_playing_on_android,
            SessionState.Running(SessionMode.PLAY, PlayTarget(id = "android:serial", kind = PlayTarget.Kind.ANDROID, name = "Pixel 7")).description(),
        )
        assertEquals(
            Res.string.session_playing_on_ios,
            SessionState.Running(SessionMode.PLAY, PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS, name = "iPhone 16 (iOS 18.2)")).description(),
        )
    }
}
