/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * How long a page can be: pages live in git and in the AI's context, so a longer one is split by topic.
 */
internal const val PAGE_MAXIMUM_LENGTH = 30_000

/**
 * Why there is no page called [name], or null if there is one.
 */
internal fun CommandContext.validatePage(name: String): Rejection? = if (screenplay.existingName(name) == name) null else Rejection(CommandProblem.PAGE_MISSING, name)

/**
 * Why [name] can't be the name of a new page (or of [renamed] moved), or null if it can.
 */
internal fun CommandContext.validateNewPageName(name: String, renamed: String? = null): Rejection? = when {
    !PageNaming.isValid(name) -> Rejection(CommandProblem.PAGE_NAME_INVALID, name)
    screenplay.existingName(name)?.let { it != renamed } == true -> Rejection(CommandProblem.PAGE_TAKEN, name)
    else -> null
}

internal fun validatePageText(text: String): Rejection? = if (text.length > PAGE_MAXIMUM_LENGTH) Rejection(CommandProblem.PAGE_TOO_LONG, PAGE_MAXIMUM_LENGTH.toString()) else null

internal const val PAGE_NAME_DESCRIPTION = "The name of the page: its path in the screenplay folder without .md, like hero or levels/cave. README is the first page, The game."
