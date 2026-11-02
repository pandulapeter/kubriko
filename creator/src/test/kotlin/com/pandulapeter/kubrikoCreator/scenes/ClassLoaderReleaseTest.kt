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

import com.pandulapeter.kubrikoCreator.scenes.leakProbe.LeakProbeEntryPoint
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.ref.WeakReference
import kotlin.io.path.createTempDirectory
import kotlin.io.path.toPath
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassLoaderReleaseTest {
    private val root = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aReleasedProjectClassLoaderIsCollected() {
        val reference = loadShowAndRelease()
        awaitCollection(reference)
        assertNull(reference.get(), "something outlived the Scene's dispose() and the loader's close() and keeps the project's classes alive")
    }

    @Test
    fun aLoaderStillInUseIsntCollected() {
        val loaded = loadProbe()
        val reference = WeakReference(loaded.entryPoint.javaClass.classLoader)
        try {
            awaitCollection(reference)
            assertNotNull(reference.get())
        } finally {
            loaded.close()
        }
    }

    private fun loadShowAndRelease(): WeakReference<ClassLoader> {
        val loaded = loadProbe()
        val view = SceneView(loaded.entryPoint)
        try {
            runBlocking { view.load(listOf(SceneActorEntry(LeakProbeEntryPoint.TYPE_ID, "10.0,20.0", "a"))) }
            assertNotNull(view.actorOf("a"), "a project Actor is in the Scene")
        } finally {
            view.dispose()
            loaded.close()
        }
        return WeakReference(loaded.entryPoint.javaClass.classLoader)
    }

    private fun loadProbe(): LoadedEntryPoint {
        val classes = File(root, "game/build/classes")
        copyProbeClassesInto(classes)
        File(classes, "META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint")
            .apply { parentFile.mkdirs() }
            .writeText(LeakProbeEntryPoint::class.java.name)
        val loaded = ClassLoaderEntryPointLoader(root, HidingClassLoader(javaClass.classLoader))
            .load(ProjectClasspath(kubrikoVersion = "1.0.0", files = listOf(classes)))
        assertIs<ProjectClassLoader>(loaded.entryPoint.javaClass.classLoader, "the probe comes from the project")
        return loaded
    }

    private fun copyProbeClassesInto(classes: File) {
        val packagePath = PROBE_PACKAGE.removeSuffix(".").replace('.', '/')
        val testClasses = LeakProbeEntryPoint::class.java.protectionDomain.codeSource.location.toURI().toPath().toFile()
        val probeFolder = File(testClasses, packagePath)
        assertTrue(probeFolder.isDirectory, "the test classes are a folder")
        probeFolder.listFiles().orEmpty().filter { it.extension == "class" }.forEach { it.copyTo(File(classes, "$packagePath/${it.name}")) }
    }

    private fun awaitCollection(reference: WeakReference<*>) {
        repeat(POLL_ROUNDS) {
            if (reference.get() == null) return
            @Suppress("UNUSED_VARIABLE")
            val garbage = ByteArray(1 shl 20)
            System.gc()
            Thread.sleep(POLL_INTERVAL_IN_MILLISECONDS)
        }
    }

    private class HidingClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> = if (name.startsWith(PROBE_PACKAGE)) throw ClassNotFoundException(name) else super.loadClass(name, resolve)
    }

    private companion object {
        val PROBE_PACKAGE = LeakProbeEntryPoint::class.java.packageName + "."
        const val POLL_ROUNDS = 50
        const val POLL_INTERVAL_IN_MILLISECONDS = 20L
    }
}
