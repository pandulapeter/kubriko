/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
// Kubriko Creator passes this script to the project's build: it adds a kubrikoCreatorSession task to every Compose desktop application module,
// running the module's own classpath plus tool-creator-session at the module's Kubriko version, without touching the project's files.
gradle.projectsEvaluated {
    val properties = gradle.startParameter.projectProperties
    // Included builds (such as build-logic) run this script too, without Kubriko Creator's properties.
    val mode = properties["kubrikoCreatorMode"] ?: return@projectsEvaluated
    val port = properties["kubrikoCreatorPort"]
    val sceneFolder = properties["kubrikoCreatorSceneFolder"]
    val sceneFile = properties["kubrikoCreatorSceneFile"]
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
                // The requested version rather than the resolved one, so that a version that can't be found is reported as such.
                val version = runtime.incoming.resolutionResult.allDependencies
                    .mapNotNull { it.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector }
                    .firstOrNull { it.group == "io.github.pandulapeter.kubriko" && it.module == "engine" }
                    ?.version ?: throw GradleException("${project.path} does not depend on the Kubriko engine.")
                add(project.dependencies.create("io.github.pandulapeter.kubriko:tool-creator-session:$version"))
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
            when (mode) {
                "edit" -> args(listOfNotNull("edit", sceneFolder ?: throw GradleException("Edit mode needs kubrikoCreatorSceneFolder."), sceneFile))
                else -> args("play", run.mainClass.get())
            }
        }
    }
}
