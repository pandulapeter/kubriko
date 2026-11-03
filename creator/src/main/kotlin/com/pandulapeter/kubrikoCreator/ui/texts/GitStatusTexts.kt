/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.git.GitStatus
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_git_committed
import kubriko.creator.generated.resources.status_git_failed
import kubriko.creator.generated.resources.status_git_repository_created
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [GitStatus].
 */
internal fun GitStatus.toResource(): StringResource = when (this) {
    GitStatus.COMMITTED -> Res.string.status_git_committed
    GitStatus.FAILED -> Res.string.status_git_failed
    GitStatus.REPOSITORY_CREATED -> Res.string.status_git_repository_created
}
