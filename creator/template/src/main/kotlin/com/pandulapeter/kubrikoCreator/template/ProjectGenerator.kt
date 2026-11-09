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
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
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
    private val lookUpKubriko: (String) -> KubrikoAvailability.Lookup = { KubrikoAvailability.Lookup.FOUND },
    private val targetHost: TargetHost = TargetHost.System,
) : ProjectCreator {

    /**
     * @return The new project's folder.
     * @throws ProjectCreationException If the folder already has files in it, Kubriko Creator may not write there, its
     *   Kubriko version can't be downloaded, or the project could not be written, saying why (no network, a full disk, or
     *   no permission).
     */
    override fun create(request: NewProjectRequest): File {
        val directory = request.directory
        if (directory.exists() && (!directory.isDirectory || !directory.list().isNullOrEmpty())) {
            throw ProjectCreationException(TemplateProblem.FOLDER_NOT_EMPTY)
        }
        val writableFolder = CreationFailure.nearestExisting(directory)
        if (!writableFolder.canWrite()) throw ProjectCreationException(TemplateProblem.NOT_CREATED_NOT_ALLOWED, arguments = listOf(writableFolder.path))
        when (lookUpKubriko(configuration.kubrikoVersion)) {
            KubrikoAvailability.Lookup.FOUND -> Unit
            KubrikoAvailability.Lookup.MISSING -> throw ProjectCreationException(TemplateProblem.KUBRIKO_UNAVAILABLE, arguments = listOf(configuration.kubrikoVersion))
            KubrikoAvailability.Lookup.UNREACHABLE -> throw ProjectCreationException(TemplateProblem.NOT_CREATED_OFFLINE)
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
            request.targets?.let { targets -> setTargets(directory, targets) }
            KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
            wrapperInstaller.install(directory, configuration.gradleVersion)
            if (request.isUnderVersionControl) startVersionControl(directory, FIRST_COMMIT_MESSAGE)
        } catch (exception: ProjectCreationException) {
            notCreated(directory, didExist, exception)
            throw exception
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
     * Turns the targets of the new project in [directory] on or off to match [targets] (the desktop is always on), with the
     * changes Project settings would make, written as the template's files are, since the project is still being made.
     *
     * @throws ProjectCreationException If one can't be set up on this computer.
     */
    private fun setTargets(directory: File, targets: Set<GameTarget>) {
        val gameTargets = GameTargets(directory, configuration.android, targetHost)
        GameTarget.entries.filter { it != GameTarget.DESKTOP }.forEach { target ->
            when (val change = gameTargets.change(target, isOn = target in targets)) {
                is TargetChange.Proposed -> change.changes.forEach { file ->
                    val written = File(directory, file.path)
                    val text = file.after
                    if (text == null) {
                        written.delete()
                    } else {
                        written.parentFile?.mkdirs()
                        written.writeTextAtomically(text)
                    }
                }
                is TargetChange.Refused -> if (change.problem != TargetProblem.ALREADY_ON && change.problem != TargetProblem.ALREADY_OFF) {
                    throw ProjectCreationException(TemplateProblem.TARGET_NOT_SET_UP, arguments = change.arguments, targetProblem = change.problem)
                }
            }
        }
    }

    /**
     * Removes what was written of a project that couldn't be created (the folder was empty or missing before), so that
     * trying again with the same name works.
     */
    private fun notCreated(directory: File, didExist: Boolean, cause: Exception): ProjectCreationException {
        directory.deleteRecursively()
        if (didExist) directory.mkdirs()
        return CreationFailure.of(directory, cause)
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
