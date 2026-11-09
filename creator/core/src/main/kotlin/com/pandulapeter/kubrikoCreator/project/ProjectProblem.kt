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
    PROJECT_FILE_NOT_READ("%1\$s couldn't be read: %2\$s"),
    PROJECT_FILE_NOT_JSON("%1\$s isn't valid JSON: something on line %2\$s is out of place. Fix it in a text editor, or bring it back from version control."),
    PROJECT_FILE_NOT_AN_OBJECT("%1\$s should start with { and end with }, as a JSON object does."),
    PROJECT_FILE_VALUE_MISSING("%1\$s has no \"%2\$s\", which Kubriko Creator needs."),
    PROJECT_FILE_VALUE_NOT_TEXT("\"%2\$s\" in %1\$s should be a text between quotation marks."),
    PROJECT_FILE_VALUE_NOT_NUMBER("\"%2\$s\" in %1\$s should be a whole number."),
    PROJECT_FILE_NOT_DECODED("%1\$s doesn't hold what Kubriko Creator expects: %2\$s"),
}
