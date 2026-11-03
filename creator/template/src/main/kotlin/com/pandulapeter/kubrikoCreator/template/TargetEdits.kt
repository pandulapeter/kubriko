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

/**
 * How one [GameTarget] is turned on and off in a template's build files.
 *
 * Turning one on adds only the parts the project lacks, and creates only the target's files it doesn't have; turning it
 * off removes it from the build files and leaves its files, so that turning it on again keeps what the user changed in them.
 */
internal interface TargetEdits {
    val target: GameTarget

    fun isOn(files: TargetFiles): Boolean

    /**
     * The new text of each file that changes, by its path, or null if the build files don't have the template's shape.
     */
    fun turnedOn(files: TargetFiles): Map<String, String>?

    /**
     * The new text of each file that changes, by its path, or null if the build files don't have the template's shape.
     */
    fun turnedOff(files: TargetFiles): Map<String, String>?

    companion object {
        val ALL: Map<GameTarget, TargetEdits> = listOf(WebTarget, AndroidTarget, IosTarget).associateBy { it.target }
    }
}
