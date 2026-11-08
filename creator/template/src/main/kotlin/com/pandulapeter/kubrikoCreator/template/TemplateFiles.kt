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
 * Reads a template's files from this module's resources, as listed by the template's `files.txt` (resources can't be
 * listed once they are packaged into a jar).
 */
internal object TemplateFiles {

    const val TEMPLATE_SUFFIX = ".template"

    fun load(template: ProjectTemplate): List<TemplateFile> {
        val own = ownPathsOf(template).map { path -> TemplateFile(path = path, content = readResource("${template.root}/$path$TEMPLATE_SUFFIX")) }
        val base = template.base ?: return own
        val replaced = own.mapTo(HashSet()) { it.path } + listOf("${template.root}/removed.txt").flatMap { readResource(it).lines() }.filter { it.isNotBlank() }
        return (load(base).filter { it.path !in replaced } + own).sortedBy { it.path }
    }

    /**
     * The paths of the files [template] lists itself, which its own folder holds.
     */
    fun ownPathsOf(template: ProjectTemplate): List<String> = readResource("${template.root}/files.txt").lines().filter { it.isNotBlank() }

    internal val ProjectTemplate.root get() = "templates/$id"

    internal fun readResource(name: String) = TemplateFiles::class.java.classLoader.getResourceAsStream(name)?.use { it.readBytes().decodeToString().replace("\r\n", "\n") }
        ?: error("$name is missing from this module's resources.")
}
