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
 * listed once they are packaged into a jar): a line per file, its path, whose text is the resource beside it ending in
 * [TEMPLATE_SUFFIX], or `path <- resource` for a file that isn't text, copied from a resource of this module as it is.
 */
internal object TemplateFiles {

    const val TEMPLATE_SUFFIX = ".template"
    private const val COPY_SEPARATOR = " <- "

    fun load(template: ProjectTemplate): List<TemplateFile> = entriesOf(template.root).map { (path, resource) ->
        if (resource == null) TemplateFile(path = path, content = readResource("${template.root}/$path$TEMPLATE_SUFFIX")) else TemplateFile(path = path, content = "", resource = resource)
    }

    /**
     * The files listed by the `files.txt` in [root], each with the resource it is copied from, if it is copied.
     */
    fun entriesOf(root: String): List<Pair<String, String?>> = readResource("$root/files.txt")
        .lines()
        .filter { it.isNotBlank() }
        .map { line -> if (COPY_SEPARATOR in line) line.substringBefore(COPY_SEPARATOR) to line.substringAfter(COPY_SEPARATOR) else line to null }

    internal val ProjectTemplate.root get() = "templates/$id"

    internal fun readResource(name: String) = readBytes(name).decodeToString().replace("\r\n", "\n")

    internal fun readBytes(name: String) = TemplateFiles::class.java.classLoader.getResourceAsStream(name)?.use { it.readBytes() }
        ?: error("$name is missing from this module's resources.")
}
