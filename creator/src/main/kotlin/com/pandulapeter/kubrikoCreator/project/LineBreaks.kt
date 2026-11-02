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

/**
 * [this] with Unix line breaks, the only ones the build file editors write.
 */
internal fun String.withUnixLineBreaks() = replace("\r\n", "\n")

/**
 * [this] (with Unix line breaks) with Windows ones if [original] has them, so that an edited file keeps its line breaks.
 */
internal fun String.withLineBreaksOf(original: String) = if ("\r\n" in original) withUnixLineBreaks().replace("\n", "\r\n") else this
