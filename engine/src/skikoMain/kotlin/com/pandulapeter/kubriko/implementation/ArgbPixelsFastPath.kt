/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import org.jetbrains.skia.Bitmap

/**
 * Writes the first `width * height` unpremultiplied ARGB [pixels] into this freshly allocated N32 bitmap without going
 * through `Bitmap.installPixels`, returning false when the platform has no such path (or it is not available yet), in
 * which case the caller installs them itself. Only the web has one (`WasmPixelBridge`).
 */
internal expect fun Bitmap.writeArgbPixelsThroughFastPath(pixels: IntArray, width: Int, height: Int): Boolean
