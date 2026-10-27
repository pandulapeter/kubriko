/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The Property a Block uses.
 *
 * @param name Empty until one is chosen.
 * @param manager The Manager type it belongs to, or null for the type's own Property.
 */
internal data class PropertyReference(
    val name: String,
    val kind: PropertyKind,
    val manager: String? = null,
)
