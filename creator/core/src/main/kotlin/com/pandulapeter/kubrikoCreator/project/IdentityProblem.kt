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
 * Why a value of the game's identity can't be used.
 */
public enum class IdentityProblem(override val english: String) : DomainText {
    APPLICATION_ID_PARTS("\"%1\$s\" isn't an application ID: it needs at least two parts joined by dots, like com.example.mygame."),
    APPLICATION_ID_PART_EMPTY("\"%1\$s\" isn't an application ID: put a part on both sides of every dot."),
    APPLICATION_ID_FIRST_CHARACTER("\"%1\$s\" isn't an application ID: start every part with a letter."),
    APPLICATION_ID_CHARACTERS("\"%1\$s\" isn't an application ID: use only letters, numbers, and underscores in every part."),
    APPLICATION_ID_LETTER_BEYOND_A_TO_Z("\"%1\$s\" isn't an application ID: use only the letters a to z, without accents."),
    NAME_EMPTY("Give the game a name."),
    NAME_INVALID("The game's name is one line of at most %1\$s characters."),
    TEXT_INVALID("The vendor and the description are one line of at most %1\$s characters each."),
    VERSION_NAME_INVALID("\"%1\$s\" isn't a version name installers accept: one to three numbers, like 1.2.0, the first not 0."),
    VERSION_NUMBER_INVALID("The version number is a whole number from 1 to %1\$s."),
}
