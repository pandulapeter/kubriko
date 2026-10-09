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

internal enum class NavigateBackAction {
    DESELECT_ACTOR,
    DESELECT_TYPE,
    CLOSE,
    NONE,
}

/**
 * Decides what Escape does. [NavigateBackAction.CLOSE] closes the Settings window when it is open, and the
 * editor itself only when the scene has no unsaved changes.
 */
internal fun navigateBackAction(
    hasSelectedActor: Boolean,
    hasSelectedType: Boolean,
    isSettingsOpen: Boolean,
    isSceneModified: Boolean,
    isTextInputFocused: Boolean,
) = when {
    isTextInputFocused -> NavigateBackAction.NONE
    hasSelectedActor -> NavigateBackAction.DESELECT_ACTOR
    hasSelectedType -> NavigateBackAction.DESELECT_TYPE
    isSettingsOpen -> NavigateBackAction.CLOSE
    isSceneModified -> NavigateBackAction.NONE
    else -> NavigateBackAction.CLOSE
}
