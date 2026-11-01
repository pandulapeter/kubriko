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

/**
 * What the AI panel shows: the in-app assistant, connecting an AI tool of the user's own, or what the AI is told about
 * the game.
 */
internal enum class AiPanelView {
    ASSISTANT,
    OWN_TOOL,
    GAME_MAP,
}
