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

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.actors.ActorTypeStore
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_project_not_created
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.File
import java.io.IOException

/**
 * Creates a project from a [ProjectTemplate]: renders its files into the new project's folder, generates the code of its
 * block-built Actor types, adds the Gradle wrapper, and commits it all into a new git repository.
 */
internal class ProjectGenerator(
    private val configuration: CreatorConfiguration,
    private val wrapperInstaller: WrapperInstaller = GradleWrapperInstaller,
    private val loadTemplate: (ProjectTemplate) -> List<TemplateFile> = TemplateFiles::load,
    private val startVersionControl: (directory: File, message: String) -> Unit = { directory, message -> JGitRepository.create(directory, message) },
) : ProjectCreator {

    /**
     * @return The new project's folder.
     * @throws ProjectCreationException If the folder already has files in it, or the project could not be written.
     */
    override fun create(request: NewProjectRequest): File {
        val directory = request.directory
        if (directory.exists() && (!directory.isDirectory || !directory.list().isNullOrEmpty())) {
            throw ProjectCreationException(Res.string.error_project_folder_not_empty)
        }
        val values = placeholderValues(request)
        try {
            loadTemplate(request.template).forEach { file ->
                File(directory, TemplateRenderer.renderPath(file.path, request.packageName)).apply {
                    parentFile.mkdirs()
                    writeText(TemplateRenderer.renderContent(file.content, values))
                }
            }
            ActorTypeStore.open(directory)?.regenerateAll()
            wrapperInstaller.install(directory, configuration.gradleVersion)
            if (request.isUnderVersionControl) startVersionControl(directory, FIRST_COMMIT_MESSAGE)
        } catch (exception: IOException) {
            throw ProjectCreationException(Res.string.error_project_not_created, exception)
        } catch (exception: RuntimeException) {
            throw ProjectCreationException(Res.string.error_project_not_created, exception)
        } catch (exception: GitAPIException) {
            throw ProjectCreationException(Res.string.error_project_not_created, exception)
        }
        return directory
    }

    internal fun placeholderValues(request: NewProjectRequest) = mapOf(
        "PROJECT_NAME" to request.name.trim(),
        "PROJECT_ID" to request.projectId,
        "PACKAGE" to request.packageName,
        "KUBRIKO_VERSION" to configuration.kubrikoVersion,
        "KOTLIN_VERSION" to configuration.kotlinVersion,
        "COMPOSE_VERSION" to configuration.composeVersion,
        "EXTRA_REPOSITORIES" to if (configuration.useMavenLocal) MAVEN_LOCAL_REPOSITORY else "",
    )

    internal companion object {
        const val FIRST_COMMIT_MESSAGE = "Created with Kubriko Creator."
        const val MAVEN_LOCAL_REPOSITORY = "        mavenLocal {\n            content { includeGroup(\"io.github.pandulapeter.kubriko\") }\n        }\n"
    }
}
