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

import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectProblem

/**
 * Why no change may be made to a project a newer Kubriko Creator saved, which its notice says too.
 */
public fun ProjectFormatStatus.ReadOnly.toRejection(): Rejection = creatorVersion?.let { Rejection(ProjectProblem.NEWER_FORMAT, it) }
    ?: Rejection(ProjectProblem.NEWER_FORMAT_UNKNOWN_VERSION)
