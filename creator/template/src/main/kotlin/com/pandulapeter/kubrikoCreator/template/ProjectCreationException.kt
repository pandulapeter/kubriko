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

/**
 * A project that could not be created, with a reason to show the user and the arguments of its message.
 *
 * @param targetProblem Why a target asked for couldn't be set up ([TemplateProblem.TARGET_NOT_SET_UP]), which the
 *   message is then, with [arguments] as its own.
 */
public class ProjectCreationException(
    public val reason: TemplateProblem,
    cause: Throwable? = null,
    public val arguments: List<String> = emptyList(),
    public val targetProblem: TargetProblem? = null,
) : Exception(cause)
