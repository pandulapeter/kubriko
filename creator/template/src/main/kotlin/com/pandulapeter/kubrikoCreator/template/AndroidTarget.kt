/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.project.BuildScript
import com.pandulapeter.kubrikoCreator.project.GradleProperties
import com.pandulapeter.kubrikoCreator.project.VersionCatalog
import java.io.File

/**
 * The Android target: an Android library target of the game's module, which holds its activity, and the `androidApp`
 * module that packages it as an app, since the Android Gradle plugin's application plugin can't share a module with
 * Kotlin Multiplatform.
 */
internal object AndroidTarget : TargetEdits {
    private const val APPLICATION_PLUGIN = "alias(libs.plugins.android.application) apply false"
    private const val LIBRARY_PLUGIN = "alias(libs.plugins.android.kotlinMultiplatformLibrary)"
    private const val INCLUDE = "include(\":androidApp\")"
    private const val COMMON_DEPENDENCIES = "commonMain.dependencies {"
    private const val ANDROID_DEPENDENCIES = "androidMain.dependencies {"
    private const val ACTIVITY_LIBRARY = "libs.androidx.activity.compose"

    override val target = GameTarget.ANDROID

    override val keptPaths = setOf(SystemTargetHost.LOCAL_PROPERTIES)

    override fun isOn(files: TargetFiles) = files.build?.lines()?.any { it.trim() == LIBRARY_PLUGIN } == true

    override fun turnedOn(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val catalog = files.read(VersionCatalog.FILE) ?: return null
        val settings = files.read(TargetFiles.SETTINGS_FILE) ?: return null
        val withPlugins = withPlugins(build) ?: return null
        val withTarget = if (hasTarget(withPlugins)) withPlugins else BuildFileLines.withTarget(withPlugins, targetBlock(files.packageName)) ?: return null
        val withDependencies = withDependencies(withTarget) ?: return null
        val changes = files.missingFilesOf(target) + mapOf(
            TargetFiles.BUILD_FILE to withDependencies,
            VersionCatalog.FILE to entries(files).fold(catalog) { text, entry -> VersionCatalog.withEntry(text, entry.header, entry.alias, entry.value) },
            TargetFiles.SETTINGS_FILE to BuildFileLines.withLine(settings, INCLUDE, isSeparated = true),
        )
        val sdk = files.androidSdk
        return if (sdk == null || localSdkDirectory(files)?.let { File(it).absoluteFile.normalize() == sdk.absoluteFile.normalize() } == true) {
            changes
        } else {
            val localProperties = files.read(SystemTargetHost.LOCAL_PROPERTIES).orEmpty()
            changes + (SystemTargetHost.LOCAL_PROPERTIES to GradleProperties.withValue(localProperties, SystemTargetHost.SDK_DIRECTORY_KEY, sdk.path))
        }
    }

    /**
     * The Android SDK the project's `local.properties` names, which the Android Gradle plugin uses before any other, if it
     * names one.
     */
    fun localSdkDirectory(files: TargetFiles) = files.read(SystemTargetHost.LOCAL_PROPERTIES)?.let { GradleProperties.valueOf(it, SystemTargetHost.SDK_DIRECTORY_KEY) }

    override fun turnedOff(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val withoutPlugins = BuildFileLines.withoutLine(BuildFileLines.withoutLine(build, APPLICATION_PLUGIN), LIBRARY_PLUGIN)
        val withoutTarget = BuildFileLines.withoutBlock(withoutPlugins) { lines, index -> lines[index].trim() == "android {" } ?: return null
        val withoutDependencies = BuildFileLines.withoutBlock(withoutTarget) { lines, index -> lines[index].trim() == ANDROID_DEPENDENCIES } ?: return null
        val settings = files.read(TargetFiles.SETTINGS_FILE)?.let { BuildFileLines.withoutLine(it, INCLUDE) }
        val catalog = files.read(VersionCatalog.FILE)?.let { text ->
            val (versions, others) = entries(files).partition { it.header == VersionCatalog.VERSIONS }
            val withoutOthers = others.fold(text) { result, entry -> withoutUnusedEntry(result, entry, withoutDependencies) }
            versions.fold(withoutOthers) { result, entry -> withoutUnusedEntry(result, entry, withoutDependencies) }
        }
        return listOfNotNull(
            TargetFiles.BUILD_FILE to withoutDependencies,
            settings?.let { TargetFiles.SETTINGS_FILE to it },
            catalog?.let { VersionCatalog.FILE to it },
        ).toMap()
    }

    /**
     * [catalog] without [entry] if it is still as turning Android on added it, and neither [build] nor another entry of
     * [catalog] uses it.
     */
    private fun withoutUnusedEntry(catalog: String, entry: CatalogEntry, build: String): String {
        val isAsAdded = VersionCatalog.valueOf(catalog, entry.header, entry.alias) == entry.value
        val isReferenced = BuildScript.refersTo(build, entry.accessor) ||
            (entry.header == VersionCatalog.VERSIONS && Regex("""version\.ref\s*=\s*"${Regex.escape(entry.alias)}"""").containsMatchIn(catalog))
        return if (isAsAdded && !isReferenced) VersionCatalog.withoutEntry(catalog, entry.header, entry.alias) else catalog
    }

    private fun hasTarget(build: String) = build.lines().any { it.trim() == "android {" }

    private fun targetBlock(packageName: String) = listOf(
        "android {",
        "    namespace = \"$packageName\"",
        "    compileSdk = libs.versions.android.compileSdk.get().toInt()",
        "    minSdk = libs.versions.android.minSdk.get().toInt()",
        "    androidResources.enable = true",
        "}",
    )

    /**
     * [build] applying the library plugin, and declaring the application plugin that `androidApp` applies, since both
     * have to come from the same class loader.
     */
    private fun withPlugins(build: String): String? {
        val lines = build.lines()
        val missing = listOf(APPLICATION_PLUGIN, LIBRARY_PLUGIN).filter { plugin -> lines.none { it.trim() == plugin } }
        if (missing.isEmpty()) return build
        val start = lines.indexOfFirst { it.trim() == "plugins {" }.takeIf { it >= 0 } ?: return null
        val indent = lines.getOrNull(start + 1)?.takeWhile(Char::isWhitespace)?.takeIf { it.isNotEmpty() } ?: "    "
        return (lines.take(start + 1) + missing.map { indent + it } + lines.drop(start + 1)).joinToString("\n")
    }

    private fun withDependencies(build: String): String? {
        val lines = build.lines()
        if (lines.any { it.trim() == ANDROID_DEPENDENCIES }) return build
        val common = lines.indices.singleOrNull { lines[it].trim() == COMMON_DEPENDENCIES } ?: return null
        val end = BuildScript.blockEnd(lines, common) ?: return null
        val indent = lines[common].takeWhile(Char::isWhitespace)
        val block = listOf(ANDROID_DEPENDENCIES, "    implementation($ACTIVITY_LIBRARY)", "}").map { indent + it }
        return (lines.take(end + 1) + block + lines.drop(end + 1)).joinToString("\n")
    }

    private fun entries(files: TargetFiles) = listOf(
        CatalogEntry(VersionCatalog.VERSIONS, "android-compileSdk", "\"${files.android.compileSdk}\"", "libs.versions.android.compileSdk"),
        CatalogEntry(VersionCatalog.VERSIONS, "android-gradlePlugin", "\"${files.android.gradlePlugin}\"", "libs.versions.android.gradlePlugin"),
        CatalogEntry(VersionCatalog.VERSIONS, "android-minSdk", "\"${files.android.minSdk}\"", "libs.versions.android.minSdk"),
        CatalogEntry(VersionCatalog.VERSIONS, "androidx-activity", "\"${files.android.activity}\"", "libs.versions.androidx.activity"),
        CatalogEntry(
            VersionCatalog.LIBRARIES,
            "androidx-activity-compose",
            "{ group = \"androidx.activity\", name = \"activity-compose\", version.ref = \"androidx-activity\" }",
            ACTIVITY_LIBRARY,
        ),
        CatalogEntry(VersionCatalog.PLUGINS, "android-application", "{ id = \"com.android.application\", version.ref = \"android-gradlePlugin\" }", "libs.plugins.android.application"),
        CatalogEntry(
            VersionCatalog.PLUGINS,
            "android-kotlinMultiplatformLibrary",
            "{ id = \"com.android.kotlin.multiplatform.library\", version.ref = \"android-gradlePlugin\" }",
            "libs.plugins.android.kotlinMultiplatformLibrary",
        ),
    )

    /**
     * @param accessor How the build file names the entry.
     */
    private class CatalogEntry(val header: String, val alias: String, val value: String, val accessor: String)
}
