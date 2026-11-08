/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(org.jetbrains.skiko.InternalSkikoApi::class, kotlin.wasm.unsafe.UnsafeWasmMemoryApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.pandulapeter.kubriko.implementation

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.impl.skia_memSetInt
import org.jetbrains.skiko.wasm.awaitSkiko
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Writes ARGB pixels straight into a Skia bitmap's own memory. `Bitmap.installPixels` copies its byte array into Skia's
 * memory with one cross-module call per byte, and then copies it once more. Skia's N32 byte order (blue, green, red,
 * alpha) is exactly how WebAssembly's little-endian memory lays out a packed ARGB integer, so each row is staged in
 * Kotlin's own memory and copied across in one JavaScript call, with one store per pixel as the fallback while Skiko's
 * module is unavailable.
 *
 * Like `WasmTriangleBridge`, it reaches Skiko's module and Kotlin's generated `wasmExports` binding directly, so it
 * depends on that layout: recheck it when Skiko or Kotlin/Wasm is bumped. Only one row is staged at a time, since Wasm
 * memory grown for a whole image never shrinks.
 */
internal object WasmPixelBridge {
    private var skikoModule: JsAny? = null

    init {
        awaitSkiko.then { module ->
            skikoModule = module
            null
        }
    }

    fun write(bitmap: Bitmap, pixels: IntArray, width: Int, height: Int): Boolean {
        val pixmap = bitmap.peekPixels() ?: return false
        try {
            val address = pixmap.addr
            val rowBytes = pixmap.rowBytes
            if (!copyRows(pixels, width, height, address, rowBytes)) {
                var index = 0
                for (y in 0 until height) {
                    var pixelAddress = address + y * rowBytes
                    for (x in 0 until width) {
                        skia_memSetInt(pixelAddress, pixels[index++])
                        pixelAddress += Int.SIZE_BYTES
                    }
                }
            }
        } finally {
            pixmap.close()
        }
        return true
    }

    private fun copyRows(pixels: IntArray, width: Int, height: Int, targetAddress: Int, targetRowBytes: Int): Boolean {
        val module = skikoModule ?: return false
        val sourceRowBytes = width * Int.SIZE_BYTES
        return withScopedMemoryAllocator { allocator ->
            val row = allocator.allocate(sourceRowBytes)
            val rowAddress = row.address.toInt()
            var index = 0
            for (y in 0 until height) {
                var cursor = row
                for (x in 0 until width) {
                    cursor.storeInt(pixels[index++])
                    cursor += Int.SIZE_BYTES
                }
                if (!copyIntoSkikoMemory(module, rowAddress, sourceRowBytes, targetAddress + y * targetRowBytes)) {
                    return@withScopedMemoryAllocator false
                }
            }
            true
        }
    }
}

// The views are created per call: either memory growing detaches the previous ones.
@JsFun(
    """(module, sourceAddress, byteCount, targetAddress) => {
    const native = module && module.wasmExports;
    if (typeof wasmExports === 'undefined' || !wasmExports.memory || !native || !native.memory) return false;
    new Uint8Array(native.memory.buffer).set(new Uint8Array(wasmExports.memory.buffer, sourceAddress, byteCount), targetAddress);
    return true;
}""",
)
private external fun copyIntoSkikoMemory(module: JsAny, sourceAddress: Int, byteCount: Int, targetAddress: Int): Boolean
