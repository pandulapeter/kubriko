/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubrikoCreator.gradle.CompilationMode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * The Gradle init script that adds Kubriko Creator's tasks to a project's build, copied out of the app's resources, since
 * Gradle only accepts init scripts as files. Its path and the classpath file's stay the same between builds and launches
 * of Kubriko Creator, because Gradle reuses a project's configuration cache only for the same init scripts and
 * properties.
 */
internal object SessionInitScript {

    const val SESSION_TASK_NAME = "kubrikoCreatorSession"
    const val CLASSPATH_TASK_NAME = "kubrikoCreatorClasspath"
    const val WARM_UP_TASK_NAME = "kubrikoCreatorWarmUp"
    private const val RESOURCE_NAME = "kubriko-creator-session.init.gradle.kts"

    private val directory get() = File(System.getProperty("java.io.tmpdir"), "kubriko-creator")

    /**
     * The script, named after its content, so that every version of Kubriko Creator has its own.
     */
    val file: File by lazy {
        val content = SessionInitScript::class.java.classLoader.getResourceAsStream(RESOURCE_NAME)?.use { it.readBytes() }
            ?: error("$RESOURCE_NAME is missing from the app's resources.")
        File(directory, "session-${hashOf(content)}.init.gradle.kts").apply {
            if (!isFile || !readBytes().contentEquals(content)) {
                parentFile.mkdirs()
                val written = File.createTempFile(name, ".tmp", parentFile)
                written.writeBytes(content)
                Files.move(written.toPath(), toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
        }
    }

    /**
     * Where the classpath task writes the classpath of the project in [rootDirectory].
     */
    fun classpathFileOf(rootDirectory: File) = File(directory, "classpath-${hashOf(rootDirectory.absoluteFile.normalize().path.toByteArray())}.txt")
        .apply { parentFile.mkdirs() }

    /**
     * Where the classpath task writes the classpath of the project in [rootDirectory] for a build the AI asked for, which
     * keeps its path, like the others, so that Gradle reuses its configuration cache.
     */
    fun buildClasspathFileOf(rootDirectory: File) = File(directory, "build-classpath-${hashOf(rootDirectory.absoluteFile.normalize().path.toByteArray())}.txt")
        .apply { parentFile.mkdirs() }

    /**
     * Where the classpath task writes the classpath of the project in [rootDirectory] for a play session, which the Scene
     * editor's builds of it don't touch.
     */
    fun playClasspathFileOf(rootDirectory: File) = File(directory, "play-classpath-${hashOf(rootDirectory.absoluteFile.normalize().path.toByteArray())}.txt")
        .apply { parentFile.mkdirs() }

    /**
     * Where the games played from the project in [rootDirectory] keep their copies of its classes ([GameProcess]).
     */
    fun playDirectoryOf(rootDirectory: File) = File(directory, "play-${hashOf(rootDirectory.absoluteFile.normalize().path.toByteArray())}")

    private fun hashOf(content: ByteArray) = MessageDigest.getInstance("SHA-256").digest(content).take(8).joinToString("") { "%02x".format(it) }

    /**
     * The Kotlin property that makes common code compile incrementally ([com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy]).
     */
    const val INCREMENTAL_COMMON_CODE_PROPERTY = "kotlin.internal.incremental.enableUnsafeOptimizationsForMultiplatform"

    /**
     * The arguments that run one of the script's tasks with Kubriko Creator's [properties], compiling as [mode] says.
     */
    fun gradleArguments(properties: Map<String, String>, mode: CompilationMode) =
        listOf("--init-script", file.absolutePath) +
            (if (mode.isClean) listOf("--rerun-tasks") else emptyList()) +
            (properties + (INCREMENTAL_COMMON_CODE_PROPERTY to mode.isCommonCodeIncremental.toString())).map { (key, value) -> "-P$key=$value" }
}
