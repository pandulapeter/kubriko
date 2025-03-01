/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.utilities

import kotlinx.browser.window

fun getFixedUri(
    path: String,
    rootPathName: String = "kubriko", // TODO: Use BuildConfig.WEB_PATH_NAME
) = "${window.location.origin}${resolveRootPathName(rootPathName)}/./$path"

/**
 * I'm hardcoding the path here. I made sure not to break local builds, and nobody is going to deploy this anywhere else anyway.
 * Web navigation breaking the URI management introduced enough complexity already, this is an acceptable workaround.
 */
private fun resolveRootPathName(webPathName: String) = window.location.pathname.split("/").filter { it.isNotBlank() }.let { sections ->
    if ((sections.isNotEmpty() && sections.first() == webPathName) || sections.size > 1) "/${sections.first()}" else ""
}