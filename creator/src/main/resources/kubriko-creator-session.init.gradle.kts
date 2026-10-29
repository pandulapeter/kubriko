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
// own output folders in place of its jar) to a file, for Kubriko Creator to load the project's classes itself, followed by what it
// takes to launch the game from them as kubrikoCreatorSession does (the "launch." lines), for Kubriko Creator to play the game
// without Gradle. kubrikoCreatorWarmUp
// compiles one generated file next to the module's code, so that Kotlin's compile daemon is running and warm before the first change,
// even when the classpath build at opening found everything up to date. Nothing here keeps the configuration cache from being used.
if (gradle.startParameter.projectProperties["kubrikoCreatorMode"] != null) {
    gradle.allprojects {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            try {
                addWarmUpCompilations()
            } catch (exception: Exception) {
                logger.info("Kubriko Creator can't warm up Kotlin's compile daemon in $path.", exception)
            }
        }
    }
}

gradle.projectsEvaluated {
    val properties = gradle.startParameter.projectProperties
    // Included builds (such as build-logic) run this script too, without Kubriko Creator's properties.
    if (properties["kubrikoCreatorMode"] == null) return@projectsEvaluated
    val port = properties["kubrikoCreatorPort"]
    val classpathFile = properties["kubrikoCreatorClasspathFile"]
    val firstScene = properties["kubrikoCreatorScene"]
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
            val java = run.javaLauncher.map { it.executablePath.asFile.absolutePath }.orElse("")
            val mainClass = run.mainClass.orElse("")
            val jvmArgs = run.jvmArgs.orEmpty()
            val workingDirectory = run.workingDir
            // Lenient, so that a project whose Kubriko version has no session library still gets its classpath.
            val sessionFiles = session.incoming.artifactView { lenient(true) }.files
            val ownFiles = run.classpath.minus(runtime)
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
                // The session's classpath is resolved on its own (its libraries may raise versions), so the game runs with the module's own
                // files and that, as kubrikoCreatorSession runs it.
                val launch = if (java.get().isEmpty() || mainClass.get().isEmpty()) {
                    emptyList()
                } else {
                    listOf("launch.java=${java.get()}", "launch.mainClass=${mainClass.get()}", "launch.workingDir=${workingDirectory.absolutePath}") +
                        jvmArgs.map { "launch.jvmArg=$it" } +
                        ((if (isPackedJarOnly) folders else ownFiles.files) + sessionFiles.files).map { "launch.classpath=${it.absolutePath}" }
                }
                file.writeText((listOf("kubrikoVersion=$version") + (folders + classpath.files).map { it.absolutePath } + launch).joinToString("\n"))
            }
        }
        val warmUpCompile = extra.properties[warmUpTaskProperty(runtime.name.removeSuffix("RuntimeClasspath"))]
        tasks.register("kubrikoCreatorWarmUp") {
            group = "kubriko creator"
            if (warmUpCompile != null) dependsOn(warmUpCompile)
        }
        tasks.register<JavaExec>("kubrikoCreatorSession") {
            group = "kubriko creator"
            classpath = files(run.classpath.minus(runtime), session)
            mainClass.set("com.pandulapeter.kubriko.creatorSession.CreatorSessionLauncherKt")
            javaLauncher.set(run.javaLauncher)
            jvmArgs(run.jvmArgs ?: emptyList<String>())
            workingDir = run.workingDir
            if (port != null) systemProperty("kubriko.creator.port", port)
            if (firstScene != null) systemProperty("kubriko.creator.scene", firstScene)
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

// The Kotlin Gradle plugin isn't on an init script's classpath, so its model is reached through Groovy's dynamic calls. The warm-up
// compilation sees the main one's code and classpath, and its file changes on every build, so that it always compiles.
fun Project.addWarmUpCompilations() {
    @Suppress("UNCHECKED_CAST")
    val targets = extensions.getByName("kotlin").withGroovyBuilder { getProperty("targets") } as NamedDomainObjectCollection<Any>
    targets.matching { it.withGroovyBuilder { getProperty("platformType") }.toString() == "jvm" }.all {
        val target = withGroovyBuilder { getProperty("name") } as String
        @Suppress("UNCHECKED_CAST")
        val compilations = withGroovyBuilder { getProperty("compilations") } as NamedDomainObjectContainer<Any>
        val warmUp = compilations.create("kubrikoCreatorWarmUp")
        warmUp.withGroovyBuilder { "associateWith"(compilations.getByName("main")) }
        val directory = layout.buildDirectory.dir("kubrikoCreator/warmUp/$target")
        val source = tasks.register("kubrikoCreatorWarmUpSource${target.replaceFirstChar { it.uppercase() }}") {
            outputs.dir(directory)
            outputs.upToDateWhen { false }
            doLast {
                directory.get().file("KubrikoCreatorWarmUp.kt").asFile.writeText("internal object KubrikoCreatorWarmUp { const val TIME = ${System.nanoTime()}L }\n")
            }
        }
        (warmUp.withGroovyBuilder { getProperty("defaultSourceSet") }.withGroovyBuilder { getProperty("kotlin") } as SourceDirectorySet).srcDir(source)
        val compileTaskName = warmUp.withGroovyBuilder { getProperty("compileKotlinTaskName") } as String
        tasks.named(compileTaskName) { outputs.cacheIf { false } }
        extra[warmUpTaskProperty(target)] = compileTaskName
    }
}

fun warmUpTaskProperty(target: String) = "kubrikoCreatorWarmUpTask.$target"
