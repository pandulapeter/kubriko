/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a project can't be opened or changed as it is.
 */
public enum class ProjectProblem(override val english: String) : DomainText {
    NO_DESKTOP_MODULE("None of this project's modules can run on desktop."),
    NOT_A_GRADLE_PROJECT("Kubriko Creator couldn't open this folder as a Gradle project."),
    FORMAT_NOT_UPGRADED("Kubriko Creator couldn't bring this project's files up to its format: %1\$s"),
    FORMAT_NOT_UPGRADED_ON_OPEN("Kubriko Creator couldn't bring this project's files up to its format, since one of them couldn't be written."),
    NEWER_FORMAT("This project was saved by Kubriko Creator %1\$s or newer, so this one can only browse, build, and play it. Open it in that version to change it."),
    NEWER_FORMAT_UNKNOWN_VERSION("This project was saved by a newer Kubriko Creator, so this one can only browse, build, and play it. Open it in a newer version to change it."),
}
