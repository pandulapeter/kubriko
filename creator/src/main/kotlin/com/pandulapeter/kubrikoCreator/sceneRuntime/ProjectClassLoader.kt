/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import java.net.URL
import java.net.URLClassLoader

/**
 * Loads a project's classes, defining those of the project's own folders from [classFiles] (by class name), the bytes
 * they had when it was created, so that a build replacing them while these classes are in use can't mix two versions.
 * Everything else, the folders' resources included, it finds through its [urls].
 */
internal class ProjectClassLoader(
    urls: Array<URL>,
    private val classFiles: MutableMap<String, ByteArray>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent) {

    override fun findClass(name: String): Class<*> {
        val bytes = classFiles.remove(name) ?: return super.findClass(name)
        val packageName = name.substringBeforeLast('.', missingDelimiterValue = "")
        if (packageName.isNotEmpty() && getDefinedPackage(packageName) == null) {
            definePackage(packageName, null, null, null, null, null, null, null)
        }
        return try {
            defineClass(name, bytes, 0, bytes.size)
        } catch (error: LinkageError) {
            classFiles[name] = bytes
            throw error
        }
    }
}
