/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

/**
 * What [BlockTypesController]'s write pipeline couldn't write, and why: the first line of the exception's message.
 */
public data class FailedWrite(
    val subject: Subject,
    val reason: String,
) {
    /**
     * What couldn't be written.
     */
    public sealed interface Subject {

        /**
         * The definition file of the type [name].
         */
        public data class Type(val name: String) : Subject

        /**
         * The type folder at [path].
         */
        public data class Folder(val path: String) : Subject

        /**
         * The code generated from the types.
         */
        public data object GeneratedCode : Subject
    }

    internal companion object {

        public fun of(subject: Subject, exception: Exception): FailedWrite = FailedWrite(
            subject = subject,
            reason = exception.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: exception::class.simpleName.orEmpty(),
        )
    }
}
