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
    PACKAGE_NAME_PARTS("Use at least two parts separated by dots, like com.example.mygame."),
    PACKAGE_NAME_PART_EMPTY("Put a part on both sides of every dot, like com.example.mygame."),
    PACKAGE_NAME_FIRST_CHARACTER("Start every part with a lowercase letter, like com.example.mygame."),
    PACKAGE_NAME_CHARACTERS("Use only lowercase letters, numbers, and underscores in every part, like com.example.my_game."),
    PACKAGE_NAME_LETTER_BEYOND_A_TO_Z("Use only the letters a to z, without accents, since the package is a name in the game's code."),
    PACKAGE_NAME_KEYWORD("One of the parts is a word that means something in Kotlin, the language of the game's code, such as fun or object. Try another one."),
    FOLDER_NOT_EMPTY("That folder already exists and isn't empty."),
    NAME_EMPTY("Give the project a name."),
    NAME_FIRST_CHARACTER("Start the name with a letter."),
    NAME_CHARACTERS("Use only letters, numbers, spaces, dashes, and underscores."),
    NOT_CREATED("Kubriko Creator couldn't create the project: %1\$s"),
    NOT_CREATED_OFFLINE("Kubriko Creator couldn't create the project, since it couldn't reach the internet, where a new project's Kubriko and Gradle come from. Check the connection, and try again."),
    KUBRIKO_REFUSED("Kubriko Creator couldn't create the project, since Maven Central, where a new project's Kubriko comes from, answered with an error. A proxy may be in the way, or it may be down for a while. Try again later."),
    NOT_CREATED_DISK_FULL("Kubriko Creator couldn't create the project, since the disk is full. Make some room, and try again."),
    NOT_CREATED_NOT_ALLOWED("Kubriko Creator isn't allowed to write in %1\$s. Choose another location, or change that folder's permissions."),
    TARGET_NOT_SET_UP("The project couldn't be built for one of its targets on this computer, so it wasn't created."),
    KUBRIKO_UNAVAILABLE(
        "Kubriko %1\$s, the version of this Kubriko Creator, can't be downloaded, so a project made with it wouldn't build. A Kubriko " +
            "Creator run from its repository needs ./gradlew :creator:run, which publishes the libraries to Maven Local first.",
    ),
}
