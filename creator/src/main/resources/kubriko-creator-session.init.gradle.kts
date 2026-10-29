/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
// Kubriko Creator passes this script to the project's build, without touching the project's files. It adds two tasks to every Compose
// desktop application module: kubrikoCreatorSession runs the module's own classpath plus tool-creator-session at the module's Kubriko
// version, and kubrikoCreatorClasspath compiles the module and writes its Kubriko version and runtime classpath (with the module's
// own output folders in place of its jar) to a file, for Kubriko Creator to load the project's classes itself. Nothing here keeps the configuration cache from being used.
gradle.projectsEvaluated {
    val properties = gradle.startParameter.projectProperties
    // Included builds (such as build-logic) run this script too, without Kubriko Creator's properties.
    if (properties["kubrikoCreatorMode"] == null) return@projectsEvaluated
    val port = properties["kubrikoCreatorPort"]
    val classpathFile = properties["kubrikoCreatorClasspathFile"]
    allprojects {
        val run = tasks.findByName("run") as? JavaExec ?: return@allprojects
        val runtime = listOf("runtimeClasspath", "desktopRuntimeClasspath", "jvmRuntimeClasspath")
            .firstNotNullOfOrNull { configurations.findByName(it) } ?: return@allprojects
        val session = configurations.create("kubrikoCreatorSession") {
            isCanBeConsumed = false
            extendsFrom(runtime)
            @Suppress("UNCHECKED_CAST")
            runtime.attributes.keySet().forEach { key ->
                attributes.attribute(key as Attribute<Any>, runtime.attributes.getAttribute(key)!!)
            }
            withDependencies {
                add(project.dependencies.create("io.github.pandulapeter.kubriko:tool-creator-session:${kubrikoVersion(runtime)}"))
            }
        }
        // The classpath task hands Kubriko Creator the folders the module's jar is packed from, so that it isn't packed on every build.
        val jar = tasks.findByName(runtime.name.removeSuffix("RuntimeClasspath").let { if (it.isEmpty()) "jar" else "${it}Jar" }) as? Jar
        tasks.register("kubrikoCreatorClasspath") {
            group = "kubriko creator"
            val version = kubrikoVersion(runtime)
            val isPackedJarOnly = jar != null && run.classpath.minus(runtime).files == setOf(jar.archiveFile.get().asFile)
            val classpath = if (isPackedJarOnly) runtime else run.classpath
            val packedFiles = if (isPackedJarOnly) jar.source else files()
            val movedFiles = layout.buildDirectory.dir("kubrikoCreator/moved").get().asFile
            inputs.files(classpath, packedFiles)
            doLast {
                val folders = linkedSetOf<File>()
                movedFiles.deleteRecursively()
                packedFiles.asFileTree.visit(Action<FileVisitDetails> {
                    if (!isDirectory) {
                        val path = relativePath.pathString
                        if (file.invariantSeparatorsPath.endsWith("/$path")) {
                            folders += File(file.invariantSeparatorsPath.removeSuffix("/$path"))
                        } else {
                            file.copyTo(File(movedFiles, path), overwrite = true)
                            folders += movedFiles
                        }
                    }
                })
                val file = File(classpathFile ?: throw GradleException("kubrikoCreatorClasspath needs kubrikoCreatorClasspathFile."))
                file.writeText((listOf("kubrikoVersion=$version") + (folders + classpath.files).map { it.absolutePath }).joinToString("\n"))
            }
        }
        tasks.register<JavaExec>("kubrikoCreatorSession") {
            group = "kubriko creator"
            classpath = files(run.classpath.minus(runtime), session)
            mainClass.set("com.pandulapeter.kubriko.creatorSession.CreatorSessionLauncherKt")
            javaLauncher.set(run.javaLauncher)
            jvmArgs(run.jvmArgs ?: emptyList<String>())
            workingDir = run.workingDir
            if (port != null) systemProperty("kubriko.creator.port", port)
            systemProperty("kubriko.creator.logging", "true")
            args("play", run.mainClass.get())
        }
    }
}

// The requested version rather than the resolved one, so that a version that can't be found is reported as such.
fun Project.kubrikoVersion(runtime: Configuration) = runtime.incoming.resolutionResult.allDependencies
    .mapNotNull { it.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector }
    .firstOrNull { it.group == "io.github.pandulapeter.kubriko" && it.module == "engine" }
    ?.version ?: throw GradleException("$path does not depend on the Kubriko engine.")
