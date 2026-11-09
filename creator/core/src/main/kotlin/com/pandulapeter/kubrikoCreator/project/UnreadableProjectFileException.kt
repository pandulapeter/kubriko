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

import com.pandulapeter.kubrikoCreator.foundation.englishWith
import java.io.IOException

/**
 * A project's `.kubriko/project.json` that couldn't be read or doesn't hold what Kubriko Creator needs. It is an
 * [IOException], so whatever already gives up on a file it can't read gives up on this one too.
 *
 * @param reason What is wrong in the file, which names it as its first argument.
 * @param arguments The arguments of [reason]: the file's path, then what it names in the file.
 */
public class UnreadableProjectFileException(
    public val reason: ProjectProblem,
    public val arguments: List<String>,
    cause: Throwable? = null,
) : IOException(reason.englishWith(arguments), cause)
