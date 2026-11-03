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
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubrikoCreator.project.ProjectOutput
import com.pandulapeter.kubrikoCreator.session.ProjectClasspath
import java.io.File
import java.io.IOException
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.io.path.createTempDirectory

/**
 * Loads a project's classes into a class loader whose parent is Kubriko Creator's own, so that everything Creator
 * already has - Kubriko, Compose, Kotlin - is shared, and only the project's own classes come from the project. That
 * is what lets Creator draw the project's Actors in its own window, and why it only works when the project uses the
 * Kubriko version Creator was built with.
 *
 * The jars under the project's root are copied, and the class files of the folders under it read, before they are loaded, so that the
 * next build can replace them while these classes are still in use (Windows can't overwrite a jar that is open, and a
 * class loaded later from a rebuilt folder wouldn't match the ones loaded before; [ProjectClassLoader]).
 */
public class ClassLoaderEntryPointLoader(
    private val rootDirectory: File,
    private val parent: ClassLoader = ClassLoaderEntryPointLoader::class.java.classLoader,
) : EntryPointLoader {

    override fun load(classpath: ProjectClasspath): LoadedEntryPoint {
        val copies = createTempDirectory("kubriko-creator-classes").toFile().apply { deleteOnExit() }
        val classLoader = try {
            classLoaderOf(classpath.files, copies, parent)
        } catch (exception: IOException) {
            copies.deleteRecursively()
            throw EntryPointLoadingException(SceneProblem.ENTRY_POINT_NOT_LOADED, exception)
        }
        try {
            val entryPoints = ServiceLoader.load(CreatorEntryPoint::class.java, classLoader).toList()
            val entryPoint = when (entryPoints.size) {
                0 -> throw EntryPointLoadingException(SceneProblem.ENTRY_POINT_MISSING)
                1 -> entryPoints.single()
                else -> throw EntryPointLoadingException(SceneProblem.ENTRY_POINT_MULTIPLE)
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
            copies.deleteRecursively()
            throw exception
        } catch (error: ServiceConfigurationError) {
            classLoader.close()
            copies.deleteRecursively()
            throw EntryPointLoadingException(SceneProblem.ENTRY_POINT_NOT_LOADED, error)
        } catch (error: LinkageError) {
            classLoader.close()
            copies.deleteRecursively()
            throw EntryPointLoadingException(SceneProblem.ENTRY_POINT_NOT_LOADED, error)
        }
    }

    /**
     * A class loader for [files] over [parent], with the project's jars copied into [copies] and the class files of
     * its folders read first.
     */
    internal fun classLoaderOf(
        files: List<File>,
        copies: File,
        parent: ClassLoader = this.parent,
    ): ProjectClassLoader {
        val root = rootDirectory.canonicalFile
        return ProjectClassLoader(
            urls = files.map { file ->
                if (isProjectOutput(file, root) && file.isFile) file.copyTo(File(copies, "${copies.list().orEmpty().size}-${file.name}")) else file
            }.map { it.toURI().toURL() }.toTypedArray(),
            classFiles = HashMap<String, ByteArray>().apply { files.filter { isProjectOutput(it, root) && it.isDirectory }.reversed().forEach { putAll(classFilesOf(it)) } },
            parent = parent,
        )
    }

    private fun classFilesOf(folder: File) = folder.walkTopDown()
        .filter { it.isFile && it.extension == "class" }
        .associate { it.relativeTo(folder).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.') to it.readBytes() }

    /**
     * Whether [file] is a jar or a folder under the project's [canonicalRoot], and so may be replaced by the next build.
     */
    private fun isProjectOutput(file: File, canonicalRoot: File) =
        (file.isDirectory || (file.isFile && file.extension == "jar")) && ProjectOutput.isOwnUnderCanonicalRoot(file, canonicalRoot)
}
