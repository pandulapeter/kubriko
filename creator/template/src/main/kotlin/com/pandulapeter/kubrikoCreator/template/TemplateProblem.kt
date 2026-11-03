/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a new project can't be created.
 */
public enum class TemplateProblem(override val english: String) : DomainText {
    PACKAGE_NAME_INVALID("Use at least two parts separated by dots, like com.example.mygame, each starting with a lowercase letter."),
    FOLDER_NOT_EMPTY("That folder already exists and isn't empty."),
    NAME_EMPTY("Give the project a name."),
    NAME_INVALID("Start with a letter, and use only letters, numbers, spaces, dashes, and underscores."),
    NOT_CREATED("Kubriko Creator couldn't create the project."),
    KUBRIKO_UNAVAILABLE(
        "Kubriko %1\$s, the version of this Kubriko Creator, can't be downloaded, so a project made with it wouldn't build. A Kubriko " +
            "Creator run from its repository needs ./gradlew :creator:run, which publishes the libraries to Maven Local first.",
    ),
}
