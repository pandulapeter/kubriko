/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

/**
 * What a project's classes are loaded next to: Creator's own Kubriko version and library versions, which the project's must
 * match to share Creator's classes ([SharedLibraries]), and the loader that loads them.
 */
public class SceneRuntimeSetup(
    public val creatorKubrikoVersion: String,
    public val creatorLibraries: Map<String, String> = emptyMap(),
    public val entryPointLoader: EntryPointLoader,
)
