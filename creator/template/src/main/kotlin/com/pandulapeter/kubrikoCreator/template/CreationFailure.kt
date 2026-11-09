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

import java.io.File
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.http.HttpConnectTimeoutException
import java.nio.file.AccessDeniedException
import java.nio.file.Files

/**
 * Why creating a project failed, told from what was thrown: no network for Gradle's download, a full disk, a folder Kubriko
 * Creator may not write in, or else the root cause's own message.
 */
public object CreationFailure {
    /**
     * The failures that only a missing network causes, by name: what the Gradle daemon throws reaches the Tooling API as a
     * placeholder that keeps only its class's name, which its `toString` starts with.
     */
    private val networkFailures = listOf(
        UnknownHostException::class,
        ConnectException::class,
        NoRouteToHostException::class,
        SocketTimeoutException::class,
        HttpConnectTimeoutException::class,
    ).mapNotNull { it.qualifiedName }

    /**
     * What the operating systems say, in English, when a write fails for want of room (macOS and Linux, then Windows).
     */
    private val diskFullMessages = listOf("No space left on device", "not enough space on the disk")

    /**
     * What `java.io` says after the path, in English, when a file can't be opened for writing (macOS and Linux, then Windows).
     */
    private val permissionReasons = listOf(" (Permission denied)", " (Access is denied)")

    /**
     * The exception telling why the project in [directory] couldn't be created, after [cause].
     */
    public fun of(directory: File, cause: Throwable): ProjectCreationException {
        val chain = generateSequence(cause) { it.cause.takeUnless { next -> next === it } }.take(MAXIMUM_CHAIN_LENGTH).toList()
        val messages = chain.mapNotNull { it.message?.takeIf(String::isNotBlank) }
        val deniedPath = chain.firstNotNullOfOrNull(::deniedPathOf)
        return when {
            chain.any { failure -> networkFailures.any { failure.javaClass.name == it || failure.toString().startsWith(it) } } ->
                ProjectCreationException(TemplateProblem.NOT_CREATED_OFFLINE, cause)
            messages.any { message -> diskFullMessages.any { message.contains(it, ignoreCase = true) } } ->
                ProjectCreationException(TemplateProblem.NOT_CREATED_DISK_FULL, cause)
            deniedPath != null -> ProjectCreationException(TemplateProblem.NOT_CREATED_NOT_ALLOWED, cause, arguments = listOf(refusedFolderOf(directory, deniedPath).path))
            else -> ProjectCreationException(TemplateProblem.NOT_CREATED, cause, arguments = listOf(messages.lastOrNull() ?: cause.toString()))
        }
    }

    /**
     * The closest folder at or above [directory] that exists, or null when none does (a drive that isn't there).
     */
    public fun nearestExisting(directory: File): File? = generateSequence(directory.absoluteFile) { it.parentFile }.firstOrNull { it.exists() }

    /**
     * Whether Kubriko Creator may write in [folder], by its access control lists too, which `File.canWrite` ignores on Windows.
     */
    internal fun isWritable(folder: File) = Files.isWritable(folder.toPath())

    private fun deniedPathOf(failure: Throwable): File? = when {
        failure is AccessDeniedException -> failure.file?.let(::File)
        else -> failure.message?.let { message -> permissionReasons.firstOrNull { message.endsWith(it) }?.let { File(message.removeSuffix(it)) } }
    }

    /**
     * The folder to name for a write refused at [deniedPath]: the project's location when it was inside the project, where
     * nothing exists yet but what Kubriko Creator wrote, and the refused file's own folder otherwise (Gradle's caches).
     */
    private fun refusedFolderOf(directory: File, deniedPath: File): File {
        val project = directory.absoluteFile
        val denied = deniedPath.absoluteFile
        return if (denied.startsWith(project)) nearestExisting(project) ?: project else denied.parentFile ?: denied
    }

    private const val MAXIMUM_CHAIN_LENGTH = 16
}
