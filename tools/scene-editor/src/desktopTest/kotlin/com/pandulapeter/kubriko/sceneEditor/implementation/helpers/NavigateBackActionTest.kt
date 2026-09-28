/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigateBackActionTest {

    private fun action(
        hasSelectedActor: Boolean = false,
        hasSelectedType: Boolean = false,
        isSettingsOpen: Boolean = false,
        isSceneModified: Boolean = false,
        isTextInputFocused: Boolean = false,
    ) = navigateBackAction(
        hasSelectedActor = hasSelectedActor,
        hasSelectedType = hasSelectedType,
        isSettingsOpen = isSettingsOpen,
        isSceneModified = isSceneModified,
        isTextInputFocused = isTextInputFocused,
    )

    @Test
    fun focusedTextInputAlwaysDoesNothing() {
        val flags = listOf(false, true)
        for (actor in flags) for (type in flags) for (settings in flags) for (modified in flags) {
            assertEquals(NavigateBackAction.NONE, action(actor, type, settings, modified, isTextInputFocused = true))
        }
    }

    @Test
    fun selectedActorIsDeselectedFirst() {
        assertEquals(NavigateBackAction.DESELECT_ACTOR, action(hasSelectedActor = true))
        assertEquals(NavigateBackAction.DESELECT_ACTOR, action(hasSelectedActor = true, hasSelectedType = true))
    }

    @Test
    fun selectedTypeIsDeselectedNext() {
        assertEquals(NavigateBackAction.DESELECT_TYPE, action(hasSelectedType = true))
    }

    @Test
    fun closesOnlyAnUnmodifiedScene() {
        assertEquals(NavigateBackAction.CLOSE, action())
        assertEquals(NavigateBackAction.NONE, action(isSceneModified = true))
    }

    @Test
    fun openSettingsAreClosedRegardlessOfModification() {
        assertEquals(NavigateBackAction.CLOSE, action(isSettingsOpen = true))
        assertEquals(NavigateBackAction.CLOSE, action(isSettingsOpen = true, isSceneModified = true))
        assertEquals(NavigateBackAction.DESELECT_ACTOR, action(hasSelectedActor = true, isSettingsOpen = true))
    }
}
