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
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges

/**
 * What the block-built types of a project that opted in to them need from outside the workspace.
 *
 * @param repository Where the types are kept.
 * @param generatedPath Where their code is generated, which version control groups with its types.
 * @param syncBuildFiles Brings the project's Kubriko dependencies in line with the code after each batch of writes.
 * @param writeDelayInMilliseconds How long changes wait to be written together with the next ones.
 */
public class BlockTypesSetup(
    internal val repository: BlockTypeRepository,
    internal val generatedPath: String? = null,
    internal val syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    internal val writeDelayInMilliseconds: Long = 0,
)
