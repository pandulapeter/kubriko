/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * Marks a function that changes the project. Only commands may call one (a test checks that nothing outside the
 * `commands` package opts in), so every change can be undone, and the AI can make it too.
 */
@RequiresOptIn(
    message = "Change the project through a command (CommandExecutor), so that the change can be undone and the AI can make it too.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
internal annotation class EditorMutation
