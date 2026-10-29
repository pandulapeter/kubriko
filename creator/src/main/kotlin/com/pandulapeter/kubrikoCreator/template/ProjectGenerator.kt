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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_project_not_created
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.File
import java.io.IOException

/**
 * Creates a project from a [ProjectTemplate]: renders its files into the new project's folder, generates the code of its
 * block-built Actor types (and makes its build depend on the Kubriko libraries that code uses), adds the Gradle wrapper,
 * and commits it all into a new git repository.
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
        val didExist = directory.exists()
        try {
            loadTemplate(request.template).forEach { file ->
                File(directory, TemplateRenderer.renderPath(file.path, request.packageName)).apply {
                    parentFile.mkdirs()
                    writeText(TemplateRenderer.renderContent(file.content, values))
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
        return ProjectCreationException(Res.string.error_project_not_created, cause)
    }

    internal fun placeholderValues(request: NewProjectRequest) = mapOf(
        "PROJECT_NAME" to request.name.trim(),
        "PROJECT_ID" to request.projectId,
        "PACKAGE" to request.packageName,
        "KUBRIKO_VERSION" to configuration.kubrikoVersion,
        "KOTLIN_VERSION" to configuration.kotlinVersion,
        "COMPOSE_VERSION" to configuration.composeVersion,
        "EXTRA_REPOSITORIES" to if (configuration.useMavenLocal) KubrikoBuildFiles.MAVEN_LOCAL_REPOSITORY else "",
    )

    internal companion object {
        const val FIRST_COMMIT_MESSAGE = "Created with Kubriko Creator."
    }
}
