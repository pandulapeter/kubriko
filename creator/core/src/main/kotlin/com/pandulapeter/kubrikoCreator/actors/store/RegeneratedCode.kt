/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

/**
 * What [BlockTypeRepository.regenerate] found.
 *
 * @param changedByHand The types whose generated code was changed by hand, and so wasn't updated.
 * @param removedLeftovers The generated files, by path from the project root, that belonged to no type any more (one
 *   deleted outside Kubriko Creator, by git or a file manager) and were deleted by this regeneration.
 * @param leftoversChangedByHand The generated files, by path from the project root, that belong to no type any more but
 *   were changed by hand, so they are kept, although they may break the build.
 */
public data class RegeneratedCode(
    val changedByHand: Set<String> = emptySet(),
    val removedLeftovers: List<String> = emptyList(),
    val leftoversChangedByHand: Set<String> = emptySet(),
)
