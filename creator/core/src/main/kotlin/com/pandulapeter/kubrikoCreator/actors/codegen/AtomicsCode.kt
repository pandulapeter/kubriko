/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

/**
 * The atomics of `kotlin.concurrent.atomics` the generated classes use, named in full rather than imported so that an
 * Actor type may be called `AtomicInt`, which is why `kotlin` is a reserved Property name of Manager types.
 */
internal object AtomicsCode {
    const val PACKAGE = "kotlin.concurrent.atomics"

    /**
     * What a generated class using the atomics is annotated with.
     */
    const val OPT_IN = "@OptIn($PACKAGE.ExperimentalAtomicApi::class)"
}
