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

/**
 * How a build compiles the project ([IncrementalCompilationPolicy]).
 *
 * @param isCommonCodeIncremental Whether Kotlin compiles common code incrementally, which it doesn't by default.
 * @param isClean Whether every task runs again (`--rerun-tasks`), which compiles everything from scratch.
 */
internal data class CompilationMode(
    val isCommonCodeIncremental: Boolean,
    val isClean: Boolean,
)
