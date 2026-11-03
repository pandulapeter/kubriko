/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

/**
 * Which side of its [PanelSplitter] a panel is on: [START] panels (left of it) grow when it is dragged to the right,
 * [END] panels when it is dragged to the left, and [BOTTOM] panels (below it) when it is dragged up.
 */
public enum class PanelSide {
    START,
    END,
    BOTTOM,
}
