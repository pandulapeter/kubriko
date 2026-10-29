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

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_entry_point_missing
import kubriko.creator.generated.resources.error_entry_point_multiple
import kubriko.creator.generated.resources.error_entry_point_not_loaded
import java.io.File
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.io.path.createTempDirectory

/**
 * Loads a project's classes into a class loader whose parent is Kubriko Creator's own, so that everything Creator
 * already has - Kubriko, Compose, Kotlin - is shared, and only the project's own classes come from the project. That
 * is what lets Creator draw the project's Actors in its own window, and why it only works when the project uses the
 * Kubriko version Creator was built with.
 *
 * The project's own jars are copied, and the class files of its own folders read, before they are loaded, so that the
 * next build can replace them while these classes are still in use (Windows can't overwrite a jar that is open, and a
 * class loaded later from a rebuilt folder wouldn't match the ones loaded before; [ProjectClassLoader]).
 */
internal object ClassLoaderEntryPointLoader : EntryPointLoader {

    override fun load(classpath: ProjectClasspath): LoadedEntryPoint {
        val copies = createTempDirectory("kubriko-creator-classes").toFile().apply { deleteOnExit() }
        val classLoader = classLoaderOf(classpath.files, copies)
        try {
            val entryPoints = ServiceLoader.load(CreatorEntryPoint::class.java, classLoader).toList()
            val entryPoint = when (entryPoints.size) {
                0 -> throw EntryPointLoadingException(Res.string.error_entry_point_missing)
                1 -> entryPoints.single()
                else -> throw EntryPointLoadingException(Res.string.error_entry_point_multiple)
            }
            return LoadedEntryPoint(
                entryPoint = entryPoint,
                close = {
                    classLoader.close()
                    copies.deleteRecursively()
                },
            )
        } catch (exception: EntryPointLoadingException) {
            classLoader.close()
            throw exception
        } catch (error: ServiceConfigurationError) {
            classLoader.close()
            throw EntryPointLoadingException(Res.string.error_entry_point_not_loaded, error)
        } catch (error: LinkageError) {
            classLoader.close()
            throw EntryPointLoadingException(Res.string.error_entry_point_not_loaded, error)
        }
    }

    /**
     * A class loader for [files] over [parent], with the project's jars copied into [copies] and the class files of
     * its folders read first.
     */
    internal fun classLoaderOf(
        files: List<File>,
        copies: File,
        parent: ClassLoader = ClassLoaderEntryPointLoader::class.java.classLoader,
    ) = ProjectClassLoader(
        urls = files.map { file ->
            if (isProjectOutput(file) && file.isFile) file.copyTo(File(copies, "${copies.list().orEmpty().size}-${file.name}")) else file
        }.map { it.toURI().toURL() }.toTypedArray(),
        classFiles = files.filter { isProjectOutput(it) && it.isDirectory }.reversed().fold(emptyMap()) { classFiles, folder -> classFiles + classFilesOf(folder) },
        parent = parent,
    )

    private fun classFilesOf(folder: File) = folder.walkTopDown()
        .filter { it.isFile && it.extension == "class" }
        .associate { it.relativeTo(folder).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.') to it.readBytes() }

    /**
     * Whether [file] is a jar or a folder built by the project (rather than downloaded into Gradle's or Maven's caches),
     * and so may be replaced by the next build.
     */
    internal fun isProjectOutput(file: File) = (file.isDirectory || (file.isFile && file.extension == "jar")) && file.invariantSeparatorsPath.let { path ->
        "/.gradle/caches/" !in path && "/.m2/repository/" !in path
    }
}
