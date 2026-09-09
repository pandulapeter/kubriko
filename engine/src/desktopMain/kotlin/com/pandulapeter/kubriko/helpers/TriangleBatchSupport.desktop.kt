/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

// Skia rasterizes these batches itself instead of handing them to a device driver, so the texture coordinates
// are honoured wherever this build runs and there is nothing to ask. @see TriangleBatchSupport
internal actual suspend fun probeTextureSampling() = true
