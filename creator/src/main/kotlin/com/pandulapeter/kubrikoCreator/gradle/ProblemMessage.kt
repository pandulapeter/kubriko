/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

/**
 * What the console shows for a problem Gradle reported: its details, which is where the Kotlin compiler puts the actual
 * message (its contextual label is only "Kotlin compiler error"), else its label, else the name of its kind.
 */
internal fun problemMessageOf(
    details: String?,
    contextualLabel: String?,
    displayName: String,
) = details?.takeIf { it.isNotBlank() }?.trim() ?: contextualLabel?.takeIf { it.isNotBlank() } ?: displayName
