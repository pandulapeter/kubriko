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

import com.pandulapeter.kubrikoCreator.session.GameProcess

/**
 * How long [WorkspaceController.close] waits for the game to stop, which is as long as stopping it can take.
 */
public const val CLOSE_GAME_TIMEOUT_IN_MILLISECONDS: Long = GameProcess.STOP_TIMEOUT_IN_MILLISECONDS

/**
 * How long [WorkspaceController.close] waits for a failed write to succeed before it stops the workspace anyway.
 */
public const val CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS: Long = 10_000L
