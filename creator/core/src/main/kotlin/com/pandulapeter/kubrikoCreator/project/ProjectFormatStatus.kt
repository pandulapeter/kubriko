/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

/**
 * Whether this Kubriko Creator may change a project, judged by the format version of its files ([ProjectFormat]).
 */
public sealed interface ProjectFormatStatus {

    /**
     * The project's files have a format this Kubriko Creator writes.
     */
    public data object Writable : ProjectFormatStatus

    /**
     * The project was saved by a newer Kubriko Creator, whose files this one may misread or lose fields of, so it is only
     * browsed, built, and played.
     *
     * @param formatVersion The project's format version.
     * @param creatorVersion The Kubriko Creator version the project names as reading it, if it names one.
     */
    public data class ReadOnly(
        val formatVersion: Int,
        val creatorVersion: String?,
    ) : ProjectFormatStatus
}
