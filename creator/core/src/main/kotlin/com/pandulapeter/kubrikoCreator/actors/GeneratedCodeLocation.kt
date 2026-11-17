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

/**
 * Where a project's Kotlin is: its source folder (relative to the project root) and the package of its code, whose
 * `generated` subpackage holds the code Kubriko Creator generates.
 */
public data class GeneratedCodeLocation(
    val sourceDirectory: String,
    val packageName: String,
) {
    /**
     * The folder of [subpackage] of the project's package (`generated`, `ui`), relative to the project root.
     */
    public fun directoryOf(subpackage: String): String = "$sourceDirectory/${packageName.replace('.', '/')}/$subpackage"
}
