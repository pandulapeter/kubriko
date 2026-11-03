/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.console

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * A line of the workspace console.
 */
public sealed interface ConsoleEntry {

    /**
     * Something Kubriko Creator itself reports, such as a session starting or ending.
     *
     * @param arguments The values of the [text]'s placeholders.
     */
    public data class Status(
        val text: DomainText,
        val isError: Boolean = false,
        val arguments: List<String> = emptyList(),
    ) : ConsoleEntry

    /**
     * A compiler or build error, with its location when Gradle reported one.
     */
    public data class Problem(
        val message: String,
        val location: String?,
    ) : ConsoleEntry

    /**
     * A raw line of build output, shown only when a build fails without reporting a [Problem].
     */
    public data class BuildOutput(
        val line: String,
    ) : ConsoleEntry

    /**
     * A log entry of the running game or Scene editor.
     */
    public data class Log(
        val message: String,
        val source: String?,
        val isImportant: Boolean,
    ) : ConsoleEntry

    /**
     * The session process could not start what it was asked to, in its own words.
     */
    public data class SessionFailure(
        val message: String,
    ) : ConsoleEntry
}
