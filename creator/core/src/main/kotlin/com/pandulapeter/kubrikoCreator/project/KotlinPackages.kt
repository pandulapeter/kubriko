/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import java.io.File
import java.io.IOException

/**
 * The packages of the project's Kotlin files, for the resource tree.
 */
internal object KotlinPackages {

    /**
     * The package [file] declares, read from its header (empty when it declares none), or, when it can't be read, the
     * package its folders below the source set's `kotlin` folder stand for ([relativePath] being its path in the project).
     */
    fun of(file: File, relativePath: String): String = try {
        file.bufferedReader().useLines(::declaredPackage)
    } catch (_: IOException) {
        packageOfFolders(relativePath)
    }

    /**
     * The package the [lines] of a Kotlin file declare: what follows `package` on the first line that isn't blank, a
     * comment, or a file annotation, or empty when that line is something else.
     */
    fun declaredPackage(lines: Sequence<String>): String {
        var isInBlockComment = false
        for (rawLine in lines) {
            var line = rawLine.trim()
            while (line.isNotEmpty()) {
                if (isInBlockComment) {
                    val end = line.indexOf("*/")
                    if (end < 0) {
                        line = ""
                    } else {
                        isInBlockComment = false
                        line = line.substring(end + 2).trim()
                    }
                } else if (line.startsWith("/*")) {
                    isInBlockComment = true
                    line = line.substring(2)
                } else if (line.startsWith("//") || line.startsWith("@file:") || line.startsWith("#!")) {
                    line = ""
                } else {
                    return if (line.startsWith("package ") || line.startsWith("package\t")) {
                        line.removePrefix("package").substringBefore("//").substringBefore("/*").substringBefore(';').trim().replace("`", "")
                    } else {
                        ""
                    }
                }
            }
        }
        return ""
    }

    /**
     * The package the folders of a file at [relativePath] stand for, below the `kotlin` folder of its source set
     * (`src/commonMain/kotlin/game/ui/Menu.kt` is in `game.ui`), or empty outside one.
     */
    fun packageOfFolders(relativePath: String): String {
        val segments = relativePath.split('/')
        val source = segments.indexOf("src")
        val kotlinFolder = if (source < 0) -1 else segments.subList(source, segments.size).indexOf("kotlin").let { if (it < 0) -1 else source + it }
        return if (kotlinFolder < 0) "" else segments.subList(kotlinFolder + 1, segments.size - 1).joinToString(".")
    }
}
