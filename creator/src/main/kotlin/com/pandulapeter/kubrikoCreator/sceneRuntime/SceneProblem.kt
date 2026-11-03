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
 * Why a Scene can't be drawn.
 */
internal enum class SceneProblem(override val english: String) : DomainText {
    ENTRY_POINT_MISSING("The project has no CreatorEntryPoint, so its Scenes can't be edited."),
    ENTRY_POINT_MULTIPLE("The project registers more than one CreatorEntryPoint."),
    ENTRY_POINT_NOT_LOADED("The project's CreatorEntryPoint couldn't be loaded."),
    VIEW_FAILED("The project's code failed while the Scene was being shown: %1\$s"),
    BUILD_FAILED("The project didn't build, so this Scene can't be shown. The console says what went wrong."),
    UNSUPPORTED("Kubriko Creator can only edit the Scenes of projects on its own Kubriko version, %1\$s. This project uses %2\$s."),
    UNSUPPORTED_LIBRARY("Kubriko Creator can't draw this project's Scenes: it uses %1\$s %2\$s, newer than the %3\$s Kubriko Creator runs with. Playing the game still works."),
}
