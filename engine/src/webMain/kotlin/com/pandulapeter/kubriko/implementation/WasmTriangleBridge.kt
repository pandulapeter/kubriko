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

import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skiko.wasm.awaitSkiko
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Skiko 0.150.1 uploads Kotlin arrays through one imported setter per scalar. A large textured batch
 * therefore crosses between Wasm modules hundreds of thousands of times. Stage its live range with
 * local Wasm stores, then copy those bytes in one JavaScript call and pass the native API exact counts.
 *
 * This adapter is deliberately wasm-only and falls back to public drawVertices if the expected exports
 * or the draw signature are unavailable. It depends on Skiko's native draw ABI and Kotlin's generated
 * wasmExports binding (also used by Compose resources). The arity check cannot detect same-signature
 * semantic changes, so recheck the adapter when either dependency changes.
 *
 * Geometry storage is reused after growth. Each draw creates only the scoped allocator's small control
 * object and one typed-array subview, not arrays proportional to the mesh. Skia consumes/copies the
 * buffers synchronously, as required by its normal interop route, which frees them after each draw.
 */
internal object WasmTriangleBridge {
    private var bridge: JsAny? = null

    init {
        awaitSkiko.then { module ->
            bridge = createBridge(module)
            null
        }
    }

    fun draw(
        canvas: Canvas,
        paint: Paint,
        positions: FloatArray,
        colors: IntArray,
        indices: ShortArray,
        vertexCount: Int,
        indexCount: Int,
        texCoords: FloatArray?,
    ): Boolean {
        val bridge = bridge ?: return false
        val positionCount = vertexCount * 2
        val colorOffset = positionCount * Float.SIZE_BYTES
        val textureOffset = colorOffset + vertexCount * Int.SIZE_BYTES
        val indexOffset = textureOffset + if (texCoords == null) 0 else positionCount * Float.SIZE_BYTES
        val byteCount = indexOffset + indexCount * Short.SIZE_BYTES
        return withScopedMemoryAllocator { allocator ->
            val source = allocator.allocate(byteCount)
            var cursor = source
            for (i in 0 until positionCount) {
                cursor.storeInt(positions[i].toRawBits())
                cursor += Float.SIZE_BYTES
            }
            for (i in 0 until vertexCount) {
                cursor.storeInt(colors[i])
                cursor += Int.SIZE_BYTES
            }
            if (texCoords != null) {
                for (i in 0 until positionCount) {
                    cursor.storeInt(texCoords[i].toRawBits())
                    cursor += Float.SIZE_BYTES
                }
            }
            for (i in 0 until indexCount) {
                cursor.storeShort(indices[i])
                cursor += Short.SIZE_BYTES
            }
            drawStaged(
                bridge, source.address.toInt(), byteCount, canvas._ptr, paint._ptr,
                vertexCount, colorOffset, if (texCoords == null) -1 else textureOffset,
                indexCount, indexOffset, BlendMode.MODULATE.ordinal,
            )
        }
    }
}

@JsFun("""(module) => {
    // Diagnostic A/B switch, read once before setup; normal draws carry no flag check.
    if (globalThis.__kubrikoLegacyVertices === true) return null;
    const native = module && module.wasmExports;
    if (typeof wasmExports === 'undefined' || !wasmExports.memory || !native || !native.memory
        || typeof native.malloc !== 'function' || typeof native.free !== 'function'
        || typeof native.org_jetbrains_skia_Canvas__1nDrawVertices !== 'function'
        || native.org_jetbrains_skia_Canvas__1nDrawVertices.length !== 10) return null;
    const bridge = { native, pointer: 0, capacity: 0, sourceHeap: null, targetHeap: null };
    // A cached page can resume after pagehide: its next draw simply reacquires native scratch space.
    globalThis.addEventListener?.('pagehide', () => {
        if (bridge.pointer) native.free(bridge.pointer);
        bridge.pointer = 0;
        bridge.capacity = 0;
        bridge.sourceHeap = null;
        bridge.targetHeap = null;
    });
    return bridge;
}""")
private external fun createBridge(module: JsAny): JsAny?

@JsFun("""(bridge, sourceAddress, byteCount, canvas, paint, vertices, colors, texture, indices, indexOffset, blendMode) => {
    const native = bridge.native;
    if (byteCount > bridge.capacity) {
        const capacity = Math.ceil(byteCount / 65536) * 65536;
        const pointer = native.malloc(capacity);
        if (!pointer) return false;
        if (bridge.pointer) native.free(bridge.pointer);
        bridge.pointer = pointer;
        bridge.capacity = capacity;
    }
    // Either allocator can grow its memory, detaching previous views. Resolve both after allocation.
    const sourceBuffer = wasmExports.memory.buffer;
    const targetBuffer = native.memory.buffer;
    if (!bridge.sourceHeap || bridge.sourceHeap.buffer !== sourceBuffer) bridge.sourceHeap = new Uint8Array(sourceBuffer);
    if (!bridge.targetHeap || bridge.targetHeap.buffer !== targetBuffer) bridge.targetHeap = new Uint8Array(targetBuffer);
    const pointer = bridge.pointer;
    bridge.targetHeap.set(bridge.sourceHeap.subarray(sourceAddress, sourceAddress + byteCount), pointer);
    native.org_jetbrains_skia_Canvas__1nDrawVertices(
        canvas, 0, vertices, pointer, pointer + colors, texture < 0 ? 0 : pointer + texture,
        indices, pointer + indexOffset, blendMode, paint
    );
    return true;
}""")
private external fun drawStaged(
    bridge: JsAny,
    sourceAddress: Int,
    byteCount: Int,
    canvas: Int,
    paint: Int,
    vertexCount: Int,
    colorOffset: Int,
    textureOffset: Int,
    indexCount: Int,
    indexOffset: Int,
    blendMode: Int,
): Boolean
