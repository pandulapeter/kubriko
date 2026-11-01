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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_project_newer_format
import kubriko.creator.generated.resources.error_project_newer_format_unknown_version

/**
 * Why no change may be made to a project a newer Kubriko Creator saved, which its notice says too.
 */
internal fun ProjectFormatStatus.ReadOnly.toRejection() = creatorVersion?.let { Rejection(Res.string.error_project_newer_format, it) }
    ?: Rejection(Res.string.error_project_newer_format_unknown_version)
