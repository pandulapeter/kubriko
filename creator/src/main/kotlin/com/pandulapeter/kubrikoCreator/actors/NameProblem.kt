/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a name can't be used for an Actor or Manager type, a Property, or a folder.
 */
internal enum class NameProblem(override val english: String) : DomainText {
    FOLDER_INVALID("Start a folder's name with a letter or a number, then use letters, numbers, spaces, dashes, and underscores."),
    PROPERTY_INVALID("A Property's name starts with a lowercase letter, followed by letters and numbers only."),
    PROPERTY_RESERVED("That name is already used by the generated code. Try another one."),
    PROPERTY_TAKEN("This type already has a Property with that name, or one differing only in capital letters."),
    TYPE_BASE_SUFFIX("Names ending in \"Base\" are kept for the classes Kubriko Creator generates for parent types. Pick another one."),
    TYPE_INVALID("Start with an uppercase letter, and use only letters and numbers, like Player or Enemy2."),
    TYPE_RESERVED("That name is used by Kubriko itself. Pick another one."),
    TYPE_TAKEN("There's already an Actor type or Manager type with that name."),
}
