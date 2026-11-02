/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles

/**
 * Whether a project's classes can be drawn with the libraries Kubriko Creator shares with them: its classes come first
 * from Creator's class loader, so a project compiled against a newer Compose or Kotlin may call what Creator's copies
 * don't have. A newer major or minor of those is refused; any other newer library only warns. Compose comes under two
 * groups: `org.jetbrains.compose` and the `androidx.compose` modules the JetBrains ones depend on, which hold its runtime
 * and can be resolved newer on their own, by a direct dependency or another library's.
 */
internal object SharedLibraries {
    private const val KUBRIKO_GROUP = "io.github.pandulapeter.kubriko"
    private val COMPOSE_GROUPS = listOf("org.jetbrains.compose", "androidx.compose")
    private const val KOTLIN_STANDARD_LIBRARY = "org.jetbrains.kotlin:kotlin-stdlib"

    /**
     * A library both sides have, at a newer version in the project.
     */
    data class NewerLibrary(val module: String, val projectVersion: String, val creatorVersion: String)

    sealed interface Verdict {
        data object Compatible : Verdict

        /**
         * Loads, but the console says which libraries are newer.
         */
        data class Newer(val libraries: List<NewerLibrary>) : Verdict

        /**
         * Doesn't load: [library] is a newer major or minor of Compose or Kotlin.
         */
        data class Refused(val library: NewerLibrary) : Verdict
    }

    /**
     * Compares the [project]'s libraries with [creator]'s (both `group:module` to version), leaving Kubriko's own to the
     * exact Kubriko version check.
     */
    fun check(project: Map<String, String>, creator: Map<String, String>): Verdict {
        val newer = project.entries
            .filter { (module, _) -> module.substringBefore(':') != KUBRIKO_GROUP }
            .mapNotNull { (module, projectVersion) ->
                creator[module]
                    ?.takeIf { KubrikoBuildFiles.compareVersions(projectVersion, it) > 0 }
                    ?.let { NewerLibrary(module, projectVersion, it) }
            }
            .sortedBy { it.module }
        newer.firstOrNull { it.isGated() && KubrikoBuildFiles.compareVersions(it.projectVersion.majorAndMinor(), it.creatorVersion.majorAndMinor()) > 0 }
            ?.let { return Verdict.Refused(it) }
        return if (newer.isEmpty()) Verdict.Compatible else Verdict.Newer(newer)
    }

    private fun NewerLibrary.isGated() = module.substringBefore(':').let { group ->
        COMPOSE_GROUPS.any { group == it || group.startsWith("$it.") }
    } || module == KOTLIN_STANDARD_LIBRARY

    private fun String.majorAndMinor() = substringBefore('-').split('.').take(2).joinToString(".")
}
