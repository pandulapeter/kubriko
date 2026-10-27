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
 * Reads a template's files from the app's resources, as listed by the template's `files.txt` (resources can't be
 * listed once they are packaged into a jar).
 */
internal object TemplateFiles {

    const val TEMPLATE_SUFFIX = ".template"

    fun load(template: ProjectTemplate): List<TemplateFile> = readResource("${template.root}/files.txt")
        .lines()
        .filter { it.isNotBlank() }
        .map { path -> TemplateFile(path = path, content = readResource("${template.root}/$path$TEMPLATE_SUFFIX")) }

    internal val ProjectTemplate.root get() = "templates/$id"

    private fun readResource(name: String) = TemplateFiles::class.java.classLoader.getResourceAsStream(name)?.use { it.readBytes().decodeToString() }
        ?: error("$name is missing from the app's resources.")
}
