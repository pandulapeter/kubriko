/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

/**
 * Actors of the open Scene that light up, the canvas drawing them and the Actor list their rows in the flash's color,
 * to show what the AI just changed.
 *
 * @param key What makes them light up, a new one each time.
 */
internal data class SceneHighlight(
    val key: Any,
    val actorIds: Set<String>,
)
