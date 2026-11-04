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

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.project.GradleProperties
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.File
import java.io.IOException

/**
 * Creates a project from a [ProjectTemplate]: renders its files into the new project's folder, generates the code of its
 * block-built Actor types (and makes its build depend on the Kubriko libraries that code uses), adds the Gradle wrapper,
 * and commits it all into a new git repository.
 */
public class ProjectGenerator(
    private val configuration: CreatorConfiguration,
    private val wrapperInstaller: WrapperInstaller = GradleWrapperInstaller,
    private val loadTemplate: (ProjectTemplate) -> List<TemplateFile> = TemplateFiles::load,
    private val startVersionControl: (directory: File, message: String) -> Unit = { directory, message -> JGitRepository.create(directory, message) },
    private val isKubrikoAvailable: (String) -> Boolean = { true },
) : ProjectCreator {

    /**
     * @return The new project's folder.
     * @throws ProjectCreationException If the folder already has files in it, Kubriko Creator's Kubriko version can't be
     *   downloaded, or the project could not be written.
     */
    override fun create(request: NewProjectRequest): File {
        val directory = request.directory
        if (directory.exists() && (!directory.isDirectory || !directory.list().isNullOrEmpty())) {
            throw ProjectCreationException(TemplateProblem.FOLDER_NOT_EMPTY)
        }
        if (!isKubrikoAvailable(configuration.kubrikoVersion)) throw ProjectCreationException(TemplateProblem.KUBRIKO_UNAVAILABLE, arguments = listOf(configuration.kubrikoVersion))
        val values = placeholderValues(request)
        val didExist = directory.exists()
        try {
            loadTemplate(request.template).forEach { file ->
                File(directory, TemplateRenderer.renderPath(file.path, request.packageName)).apply {
                    parentFile.mkdirs()
                    if (file.resource == null) writeText(TemplateRenderer.renderContent(file.content, values)) else writeBytes(TemplateFiles.readBytes(file.resource))
                }
            }
            BlockTypeStore.open(directory)?.regenerateAll()
            KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
            wrapperInstaller.install(directory, configuration.gradleVersion)
            if (request.isUnderVersionControl) startVersionControl(directory, FIRST_COMMIT_MESSAGE)
        } catch (exception: IOException) {
            throw notCreated(directory, didExist, exception)
        } catch (exception: RuntimeException) {
            throw notCreated(directory, didExist, exception)
        } catch (exception: GitAPIException) {
            throw notCreated(directory, didExist, exception)
        }
        return directory
    }

    /**
     * Removes what was written of a project that couldn't be created (the folder was empty or missing before), so that
     * trying again with the same name works.
     */
    private fun notCreated(directory: File, didExist: Boolean, cause: Exception): ProjectCreationException {
        directory.deleteRecursively()
        if (didExist) directory.mkdirs()
        return ProjectCreationException(TemplateProblem.NOT_CREATED, cause)
    }

    internal fun placeholderValues(request: NewProjectRequest) = mapOf(
        "PROJECT_NAME" to request.name.trim(),
        "GAME_NAME" to GradleProperties.escape(request.name.trim()),
        "PROJECT_ID" to request.projectId,
        "PACKAGE" to request.packageName,
        "KUBRIKO_VERSION" to configuration.kubrikoVersion,
        "FORMAT_VERSION" to ProjectFormat.CURRENT_VERSION.toString(),
        "TEMPLATE_VERSION" to ProjectUpgrader.CURRENT_VERSION.toString(),
        "KOTLIN_VERSION" to configuration.kotlinVersion,
        "COMPOSE_VERSION" to configuration.composeVersion,
        "EXTRA_REPOSITORIES" to if (configuration.useMavenLocal) KubrikoBuildFiles.MAVEN_LOCAL_REPOSITORY else "",
    )

    internal companion object {
        const val FIRST_COMMIT_MESSAGE = "Created with Kubriko Creator."
    }
}
