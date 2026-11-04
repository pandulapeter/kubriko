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

import com.pandulapeter.kubrikoCreator.foundation.AndroidVersions
import java.io.File

/**
 * The files of a project whose target is turned on or off, as they are before the change, and what the target's own
 * files are filled in with.
 *
 * @param reader A project file's text with Unix line breaks, or null if it doesn't exist.
 * @param projectId The root project's name, which names the web build's script.
 * @param androidSdk The Android SDK this computer has, if any.
 */
internal class TargetFiles(
    val packageName: String,
    val projectId: String,
    val applicationId: String,
    val android: AndroidVersions,
    val androidSdk: File?,
    private val reader: (String) -> String?,
) {
    private val texts = HashMap<String, String?>()

    fun read(path: String): String? = texts.getOrPut(path) { reader(path) }

    val build get() = read(BUILD_FILE)

    /**
     * These files as [reader] has them instead.
     */
    fun copy(reader: (String) -> String?) = TargetFiles(packageName, projectId, applicationId, android, androidSdk, reader)

    /**
     * The files of [target]'s template that the project doesn't have, by their paths, filled in.
     */
    fun missingFilesOf(target: GameTarget): Map<String, String> {
        val root = rootOf(target)
        val values = mapOf("PACKAGE" to packageName, "PROJECT_ID" to projectId, "APPLICATION_ID" to applicationId)
        return missingEntriesOf(target).filterValues { it.second == null }
            .mapValues { (_, entry) -> TemplateRenderer.renderContent(TemplateFiles.readResource("$root/${entry.first}${TemplateFiles.TEMPLATE_SUFFIX}"), values) }
    }

    /**
     * The files of [target]'s template that aren't text and that the project doesn't have, by their paths, each with the
     * resource it is copied from.
     */
    fun missingCopiesOf(target: GameTarget): Map<String, String> = missingEntriesOf(target).mapNotNull { (path, entry) -> entry.second?.let { path to it } }.toMap()

    private fun missingEntriesOf(target: GameTarget) = TemplateFiles.entriesOf(rootOf(target))
        .associateBy { (path, _) -> TemplateRenderer.renderPath(path, packageName) }
        .filterKeys { read(it) == null }

    private fun rootOf(target: GameTarget) = "targets/${target.key}"

    /**
     * The block [target] adds to the end of the build file, filled in.
     */
    fun buildBlockOf(target: GameTarget): String =
        TemplateRenderer.renderContent(TemplateFiles.readResource("targets/${target.key}/build-block.gradle.kts.template"), mapOf("PACKAGE" to packageName))

    companion object {
        const val BUILD_FILE = "build.gradle.kts"
        const val SETTINGS_FILE = "settings.gradle.kts"
        const val GITIGNORE_FILE = ".gitignore"
    }
}
