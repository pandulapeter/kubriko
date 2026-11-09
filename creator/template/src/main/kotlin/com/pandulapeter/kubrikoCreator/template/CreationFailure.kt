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
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import java.nio.file.AccessDeniedException
import javax.net.ssl.SSLException

/**
 * Why creating a project failed, told from what was thrown: no network for Gradle's download, a full disk, a folder Kubriko
 * Creator may not write in, or else the cause's own message.
 */
internal object CreationFailure {
    private val networkFailures = listOf(
        UnknownHostException::class,
        ConnectException::class,
        NoRouteToHostException::class,
        SocketTimeoutException::class,
        SocketException::class,
        HttpTimeoutException::class,
        SSLException::class,
    )

    /**
     * What the operating systems say, in English, when a write fails for want of room (macOS and Linux, then Windows).
     */
    private val diskFullMessages = listOf("No space left on device", "not enough space on the disk")

    /**
     * What the operating systems say, in English, when a write isn't allowed (macOS and Linux, then Windows).
     */
    private val permissionMessages = listOf("Permission denied", "Access is denied", "Operation not permitted")

    /**
     * The exception telling why the project in [directory] couldn't be created, after [cause].
     */
    fun of(directory: File, cause: Throwable): ProjectCreationException {
        val chain = generateSequence(cause) { it.cause.takeUnless { next -> next === it } }.take(MAXIMUM_CHAIN_LENGTH).toList()
        val messages = chain.mapNotNull { it.message }
        return when {
            chain.any { failure -> networkFailures.any { it.isInstance(failure) } } -> ProjectCreationException(TemplateProblem.NOT_CREATED_OFFLINE, cause)
            messages.any { message -> diskFullMessages.any { message.contains(it, ignoreCase = true) } } -> ProjectCreationException(TemplateProblem.NOT_CREATED_DISK_FULL, cause)
            chain.any { it is AccessDeniedException || it is SecurityException } || messages.any { message -> permissionMessages.any { message.contains(it, ignoreCase = true) } } ->
                ProjectCreationException(TemplateProblem.NOT_CREATED_NOT_ALLOWED, cause, arguments = listOf(nearestExisting(directory).path))
            else -> ProjectCreationException(TemplateProblem.NOT_CREATED, cause, arguments = listOf(messages.firstOrNull { it.isNotBlank() } ?: cause.toString()))
        }
    }

    /**
     * [directory], or the closest folder above it that exists: the one a write was refused in.
     */
    fun nearestExisting(directory: File): File = generateSequence(directory.absoluteFile) { it.parentFile }.firstOrNull { it.exists() } ?: directory

    private const val MAXIMUM_CHAIN_LENGTH = 16
}
