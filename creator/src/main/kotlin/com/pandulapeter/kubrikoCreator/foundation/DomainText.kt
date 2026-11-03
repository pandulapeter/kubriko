/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import java.util.Locale

/**
 * A text the model, the commands, or the workspace hands to both the user and the AI, as a typed value: the user interface
 * words it from its strings (`ui/DomainTexts.kt`), and the AI reads [english], which translating the user interface never
 * changes. [english] uses the placeholders of the matching string (`%1$s`, …).
 */
internal interface DomainText {
    val english: String
}

/**
 * [DomainText.english] with [arguments] put into its placeholders.
 */
internal fun DomainText.englishWith(arguments: List<String>): String = String.format(Locale.ROOT, english, *arguments.toTypedArray())
