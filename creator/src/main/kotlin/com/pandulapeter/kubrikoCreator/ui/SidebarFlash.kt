/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

/**
 * The item or document the AI just changed ([name]), which lights up with every new [key]: read by the rows themselves,
 * so that a flash doesn't build the sidebar's list again.
 */
internal data class SidebarFlash(
    val name: String? = null,
    val key: Any? = null,
) {
    /**
     * What makes the row of [rowName] light up, if it is the one that changed.
     */
    fun keyFor(rowName: String): Any? = key?.takeIf { rowName == name }
}
