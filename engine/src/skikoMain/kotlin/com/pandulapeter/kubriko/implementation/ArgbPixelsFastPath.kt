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
 * Creates a bitmap of the first `width * height` unpremultiplied ARGB [pixels] without going through
 * `Bitmap.installPixels`, returning null when the platform has no such path, in which case the caller installs them
 * itself. Only the web has one (`WasmPixelBridge`).
 */
internal expect fun createArgbBitmapThroughFastPath(pixels: IntArray, width: Int, height: Int): Bitmap?
