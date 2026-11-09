/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import com.pandulapeter.kubrikoCreator.project.ProjectProblem

/**
 * A folder that Gradle could open, but that is not a project Kubriko Creator can work with.
 *
 * @param arguments The arguments of [reason]'s placeholders.
 */
public class ProjectInspectionException(
    public val reason: ProjectProblem,
    public val arguments: List<String> = emptyList(),
) : Exception()
