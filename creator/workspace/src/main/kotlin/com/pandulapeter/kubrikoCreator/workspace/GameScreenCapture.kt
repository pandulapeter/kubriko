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

import com.pandulapeter.kubrikoCreator.commands.GameStep

/**
 * A screenshot of the running game, or null when none is running or taking it failed.
 */
public suspend fun WorkspaceController.captureGameScreen(): ByteArray? = (commands.context.runner.captureScreen() as? GameStep.Done)?.png
