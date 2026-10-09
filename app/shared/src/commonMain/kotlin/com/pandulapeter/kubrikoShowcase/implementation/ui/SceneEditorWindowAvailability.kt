/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import com.pandulapeter.kubriko.shared.SceneEditorConnection

/** Whether the examples have scene editor windows on this platform (only on desktop); elsewhere they get no [SceneEditorConnection]. */
internal expect val isSceneEditorWindowAvailable: Boolean
