/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

/**
 * What [BlockTypesController]'s write pipeline couldn't write, and why: the first line of the exception's message.
 */
internal data class FailedWrite(
    val subject: Subject,
    val reason: String,
) {
    sealed interface Subject {

        data class Type(val name: String) : Subject

        data class Folder(val path: String) : Subject

        data object GeneratedCode : Subject
    }

    companion object {

        fun of(subject: Subject, exception: Exception) = FailedWrite(
            subject = subject,
            reason = exception.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: exception::class.simpleName.orEmpty(),
        )
    }
}
