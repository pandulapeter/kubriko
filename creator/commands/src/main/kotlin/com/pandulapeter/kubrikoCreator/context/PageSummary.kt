/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.screenplay.PageText

/**
 * A page of the screenplay as the Game map lists it: its name, and the title and summary its text starts with, if it has
 * them ([PageText]).
 */
public data class PageSummary(
    val name: String,
    val title: String?,
    val summary: String?,
)

/**
 * Every page of the open project's screenplay, the first page first. Reads the disk.
 */
public fun CommandContext.pageSummaries(): List<PageSummary> = screenplay.scanNames().mapNotNull { name ->
    screenplay.read(name)?.let { text -> PageSummary(name, PageText.titleOf(text), PageText.summaryOf(text)) }
}
