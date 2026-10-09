/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney

import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolderImpl
import com.pandulapeter.kubriko.shared.SceneEditorConnection

fun createBlockysJourneyGameStateHolder(
    webRootPathName: String,
    isSceneEditorEnabled: Boolean,
    sceneEditorConnection: SceneEditorConnection?,
    isLoggingEnabled: Boolean,
): BlockysJourneyGameStateHolder = BlockysJourneyGameStateHolderImpl(
    webRootPathName = webRootPathName,
    isSceneEditorEnabled = isSceneEditorEnabled,
    sceneEditorConnection = sceneEditorConnection,
    isLoggingEnabled = isLoggingEnabled,
)