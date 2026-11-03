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

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayTargetMenuEntryTest {

    @Test
    fun desktopsAndTheBrowserStandAloneWhileAndroidAndIosAreGroupedWithTheirDevicesFirst() {
        val other = PlayTarget(id = "other", kind = PlayTarget.Kind.DESKTOP, name = "Other window")
        val emulator = PlayTarget(id = "android-emulator:A", kind = PlayTarget.Kind.ANDROID, name = "A", isVirtual = true)
        val phone = PlayTarget(id = "android:B", kind = PlayTarget.Kind.ANDROID, name = "B")
        val tablet = PlayTarget(id = "android:C", kind = PlayTarget.Kind.ANDROID, name = "C")
        val simulator = PlayTarget(id = "ios-simulator:D", kind = PlayTarget.Kind.IOS, name = "D", isVirtual = true)
        assertEquals(
            listOf(
                PlayTargetMenuEntry.Place(PlayTarget.DESKTOP),
                PlayTargetMenuEntry.Place(other),
                PlayTargetMenuEntry.Place(PlayTarget.WEB),
                PlayTargetMenuEntry.Group(PlayTarget.Kind.ANDROID, listOf(phone, tablet, emulator)),
                PlayTargetMenuEntry.Group(PlayTarget.Kind.IOS, listOf(simulator)),
            ),
            PlayTargetMenuEntry.of(PlayTargets(listOf(PlayTarget.DESKTOP, simulator, emulator, phone, PlayTarget.WEB, other, tablet), PlayTarget.DESKTOP)),
        )
    }

    @Test
    fun aConfiguredKindIsAGroupEvenWithNoPlacesWhileOneThatIsntIsLeftOut() {
        assertEquals(
            listOf(PlayTargetMenuEntry.Place(PlayTarget.DESKTOP), PlayTargetMenuEntry.Group(PlayTarget.Kind.IOS, emptyList())),
            PlayTargetMenuEntry.of(PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP, setOf(PlayTarget.Kind.DESKTOP, PlayTarget.Kind.IOS))),
        )
    }
}
