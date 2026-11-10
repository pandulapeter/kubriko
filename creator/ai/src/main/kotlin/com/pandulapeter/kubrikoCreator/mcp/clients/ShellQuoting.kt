/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

/**
 * Quotes text so that a shell reads it back as it is.
 */
internal object ShellQuoting {

    /**
     * Quotes [argument] for a POSIX shell, so that tokens and URLs are passed as they are.
     */
    fun posix(argument: String) = "'" + argument.replace("'", "'\\''") + "'"

    /**
     * [text] as a PowerShell single-quoted string; PowerShell also takes the typographic single quotes as quotes.
     */
    fun powerShell(text: String) = "'" + text.replace(Regex("['‘’‚‛]")) { it.value + it.value } + "'"
}
