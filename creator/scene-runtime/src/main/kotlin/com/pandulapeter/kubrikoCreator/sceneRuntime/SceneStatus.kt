/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * What the Scene editor reports in the console.
 */
public enum class SceneStatus(override val english: String) : DomainText {
    NEWER_LIBRARIES("This project uses newer libraries than Kubriko Creator runs with, so its Scenes may not look as they do in the game: %1\$s."),
    OPENED("The Scene is open, in %1\$s seconds."),
}
