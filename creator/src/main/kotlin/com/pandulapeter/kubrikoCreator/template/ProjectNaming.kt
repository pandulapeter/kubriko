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

import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers

/**
 * The rules for what a new project may be called, and the identifiers derived from its name.
 */
internal object ProjectNaming {

    private val namePattern = Regex("[A-Za-z][A-Za-z0-9 _-]*")
    private val packageSegmentPattern = Regex("[a-z][a-z0-9_]*")

    /**
     * Why [name] can't be a project's name, or null if it can.
     */
    fun validateName(name: String): TemplateProblem? = when {
        name.isBlank() -> TemplateProblem.NAME_EMPTY
        !namePattern.matches(name.trim()) -> TemplateProblem.NAME_INVALID
        else -> null
    }

    /**
     * Why [packageName] can't be a project's package, or null if it can.
     */
    fun validatePackageName(packageName: String): TemplateProblem? = packageName.split('.').let { segments ->
        if (segments.size >= 2 && segments.all { packageSegmentPattern.matches(it) && it !in KotlinIdentifiers.hardKeywords }) null else TemplateProblem.PACKAGE_NAME_INVALID
    }

    /**
     * The folder and Gradle name of a project: "My Game" becomes "my-game".
     */
    fun projectId(name: String) = name.trim().lowercase().split(' ', '_', '-').filter { it.isNotEmpty() }.joinToString("-")

    /**
     * The package suggested for a project: "My Game" becomes "com.example.mygame".
     */
    fun defaultPackageName(name: String) = "com.example." + name.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        .dropWhile { it in '0'..'9' }
        .ifEmpty { "game" }
        .let { if (it in KotlinIdentifiers.hardKeywords) "${it}game" else it }
}
