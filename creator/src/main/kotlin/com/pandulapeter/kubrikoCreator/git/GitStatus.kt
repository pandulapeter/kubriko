/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * What version control reports in the console, and why its operations fail.
 */
internal enum class GitStatus(override val english: String) : DomainText {
    COMMITTED("The changes are committed."),
    FAILED("The version control operation failed."),
    REPOSITORY_CREATED("The project is now under version control."),
}
