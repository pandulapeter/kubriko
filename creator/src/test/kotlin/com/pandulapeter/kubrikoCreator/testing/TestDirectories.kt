/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.createTempDirectory

private val testDirectories = ConcurrentLinkedQueue<File>().also { directories ->
    Runtime.getRuntime().addShutdownHook(Thread { directories.forEach(File::deleteRecursively) })
}

/**
 * A new temporary directory, deleted when the tests' JVM exits.
 */
internal fun testDirectory(): File = createTempDirectory("kubriko-creator-test").toFile().also(testDirectories::add)
