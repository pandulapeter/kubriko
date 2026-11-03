/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.desktop)
    alias(libs.plugins.kotlin.serialization)
    id("kubriko-style")
}

dependencies {
    implementation(projects.creator.foundation)
    implementation(projects.creator.core)
    implementation(projects.creator.gradleSession)
    implementation(projects.creator.git)
    implementation(projects.creator.template)
    implementation(projects.creator.commands)
    implementation(projects.creator.ai)
    implementation(projects.creator.sceneRuntime)
    implementation(projects.creator.workspace)
    implementation(projects.creator.uiComponents)
    implementation(projects.tools.creatorSession)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.jbr.api)
    implementation(libs.kotlinx.coroutines.desktop)
    implementation(libs.kotlinx.serialization)
    testImplementation(testFixtures(projects.creator.foundation))
    testImplementation(testFixtures(projects.creator.commands))
    testImplementation(testFixtures(projects.creator.gradleSession))
    testImplementation(testFixtures(projects.creator.ai))
    testImplementation(testFixtures(projects.creator.workspace))
    testImplementation(testFixtures(projects.creator.uiComponents))
    testImplementation(libs.kotlin.test)
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.mcp.client)
}

/**
 * The versions of the libraries Kubriko Creator runs with, which a project's classes loaded next to Creator's share
 * (sceneRuntime/SharedLibraries.kt): every external module of the runtime classpath, as `group:module=version` lines.
 */
val generateCreatorLibraries = tasks.register("generateCreatorLibraries") {
    val outputDirectory = layout.buildDirectory.dir("generated/creatorLibraries")
    val libraries = configurations.runtimeClasspath.flatMap { it.incoming.artifacts.resolvedArtifacts }.map { artifacts ->
        artifacts
            .mapNotNull { it.id.componentIdentifier as? org.gradle.api.artifacts.component.ModuleComponentIdentifier }
            .map { "${it.group}:${it.module}=${it.version}" }
            .distinct()
            .sorted()
    }
    inputs.property("libraries", libraries)
    outputs.dir(outputDirectory)
    doLast {
        outputDirectory.get().file("creator-libraries.txt").asFile.writeText(libraries.get().joinToString("\n"))
    }
}

sourceSets.main {
    resources.srcDir(generateCreatorLibraries)
}

tasks.test {
    systemProperty("kubrikoCreator.directory", projectDir.absolutePath)
    // The end-to-end checks (src/test/.../endToEnd) start the app in a JVM of its own, from what :creator:run runs it with.
    val runtimeClasspath = sourceSets.main.get().runtimeClasspath
    inputs.files(runtimeClasspath).withPropertyName("creatorRuntimeClasspath").withNormalizer(ClasspathNormalizer::class.java)
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-DkubrikoCreator.runtimeClasspath=${runtimeClasspath.asPath}") })
}

// Every Creator module's unit tests: :creator:test runs only the app's.
tasks.register("allTests") {
    group = "verification"
    dependsOn(tasks.test)
    dependsOn(subprojects.map { "${it.path}:test" })
}

// Pinned, since each module's resources need a package of their own (:creator:ui-components has another).
compose.resources {
    packageOfResClass = "kubriko.creator.generated.resources"
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt"
        // The JetBrains Runtime is what lets the content be laid out under a title bar that still behaves as one (ExtendedTitleBar.kt).
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }.get().metadata.installationPath.asFile.absolutePath
        if (System.getProperty("os.name").orEmpty().lowercase().contains("mac")) {
            jvmArgs += "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"
        }
        nativeDistributions {
            packageName = "Kubriko Creator"
            packageVersion = "1.0.0"
            // The assistant downloads the ACP Registry's agent list and agents.
            modules("java.net.http")
        }
    }
}
