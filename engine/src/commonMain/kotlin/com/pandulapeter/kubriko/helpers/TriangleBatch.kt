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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.implementation.drawTriangles
import kotlin.math.sqrt

/**
 * Accumulates solid-color triangles from many actors so an entire scene can be rasterized with a handful of
 * native `drawVertices` calls instead of thousands of individual canvas operations — the single biggest lever
 * on a scene built from many small shapes, and the only practical way to draw one on the web.
 *
 * Painter's order is preserved by emission order within a batch and by flushing whenever something has to be
 * drawn through a different code path (e.g. a textured shape). Buffers are reused across frames; steady-state
 * operation does not allocate. Vertices are deduplicated per quad/triangle/line and referenced through an index
 * array, since quads and anti-aliased lines otherwise repeat shared corners into every triangle that touches
 * them.
 *
 * A batch is not thread-safe and is meant to be owned by one drawing actor, filled during its `draw` and handed
 * to the canvas with [flush].
 */
class TriangleBatch {

    private var positions = FloatArray(INITIAL_VERTEX_CAPACITY * 2)
    private var colors = IntArray(INITIAL_VERTEX_CAPACITY)
    private var indices = ShortArray(INITIAL_INDEX_CAPACITY)
    private var vertexCount = 0
    private var indexCount = 0

    /**
     * The tiling pattern the vertex colors are modulated by, or null (the default) for the plain color-only
     * path — which is bit-for-bit what a batch without one has always drawn, texture coordinates included:
     * none are written and none are handed to the native call.
     *
     * Only geometry appended through [addTexturedVertex] samples it. Everything else in the same batch is
     * given [defaultU]/[defaultV], so a single pattern can serve a batch holding both, as long as those
     * coordinates address an opaque white texel — modulating by opaque white leaves a color untouched.
     */
    var texture: ImageBitmap? = null

    /** @see texture */
    var defaultU = 0f

    /** @see texture */
    var defaultV = 0f

    // Texture coordinates are written only where a caller supplies them, and the gaps between those runs are
    // filled with the default in one pass at draw time - so appending a vertex that samples nothing costs
    // nothing at all, however many of them a frame holds (outlines alone are most of the batch).
    private var texCoords = FloatArray(0)
    private var texCoordsWritten = 0

    /** Whether nothing has been appended since the last [reset]. */
    val isEmpty: Boolean get() = vertexCount == 0

    /**
     * Bumped by every [reset], so an owner caching vertex indices returned by [addVertex] can tell
     * whether they still point into this batch's current contents.
     */
    var generation = 0
        private set

    /**
     * True when adding [additionalVertexCount] more unique vertices would exceed the index range
     * (indices are [Short]-backed, so a batch holds at most [MAX_INDEXED_VERTICES] between resets).
     * An owner that builds one persistent batch across many draw calls instead of flushing every
     * frame (a cached mesh) must check this itself and roll over to a fresh batch instead of
     * tripping the hard [check] in [addQuad]/[addTriangle]/[addLine].
     */
    fun willOverflow(additionalVertexCount: Int): Boolean = vertexCount + additionalVertexCount > MAX_INDEXED_VERTICES

    /** Adds a quad of one flat color, its corners taken in order around the shape. */
    fun addQuad(
        p0x: Float, p0y: Float,
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        argb: Int,
    ) {
        ensureVertexCapacity(vertexCount + 4)
        ensureIndexCapacity(indexCount + 6)
        val positions = positions
        val base = vertexCount
        var p = base * 2
        positions[p++] = p0x; positions[p++] = p0y
        positions[p++] = p1x; positions[p++] = p1y
        positions[p++] = p2x; positions[p++] = p2y
        positions[p++] = p3x; positions[p] = p3y
        colors.fill(argb, base, base + 4)
        writeQuadIndices(base)
        vertexCount += 4
        indexCount += 6
    }

    /**
     * Adds a quad whose four corners each carry their own color; the rasterizer interpolates them
     * across the two triangles. The colors follow the positional corners ([argb0] belongs to p0,
     * and so on). The Gouraud seam along the shared diagonal is invisible at this art scale.
     */
    fun addQuad(
        p0x: Float, p0y: Float,
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        argb0: Int, argb1: Int, argb2: Int, argb3: Int,
    ) {
        ensureVertexCapacity(vertexCount + 4)
        ensureIndexCapacity(indexCount + 6)
        val positions = positions
        val colors = colors
        val base = vertexCount
        var p = base * 2
        positions[p++] = p0x; positions[p++] = p0y
        positions[p++] = p1x; positions[p++] = p1y
        positions[p++] = p2x; positions[p++] = p2y
        positions[p++] = p3x; positions[p] = p3y
        colors[base] = argb0; colors[base + 1] = argb1; colors[base + 2] = argb2; colors[base + 3] = argb3
        writeQuadIndices(base)
        vertexCount += 4
        indexCount += 6
    }

    /**
     * Appends one vertex and returns its index, for geometry whose quads share corners with each other — a
     * tile grid, where a corner is otherwise written once per tile touching it. The returned index stays valid
     * until the next [reset], which [generation] reports.
     */
    fun addVertex(x: Float, y: Float, argb: Int): Int {
        ensureVertexCapacity(vertexCount + 1)
        val index = vertexCount
        positions[index * 2] = x
        positions[index * 2 + 1] = y
        colors[index] = argb
        vertexCount++
        return index
    }

    /**
     * Appends one vertex that samples [texture] at ([u], [v]) — in texels, the space the pattern's own shader
     * addresses — and returns its index. Otherwise exactly [addVertex]: a tile grid's shared corners come
     * through here, and a corner's coordinates being a pure function of where it sits means two tiles meeting
     * at one still agree on it and can go on sharing the vertex.
     */
    fun addTexturedVertex(x: Float, y: Float, argb: Int, u: Float, v: Float): Int {
        val index = addVertex(x, y, argb)
        padTexCoordsTo(index)
        texCoords[index * 2] = u
        texCoords[index * 2 + 1] = v
        texCoordsWritten = index + 1
        return index
    }

    /**
     * Adds a quad whose four corners share one color and each carry their own coordinate in [texture], in
     * texels. The counterpart of [addTexturedVertex] for geometry that shares no corners with its neighbours,
     * where the coordinates come from the shape's own extent rather than from where in the world it sits.
     */
    fun addTexturedQuad(
        p0x: Float, p0y: Float, u0: Float, v0: Float,
        p1x: Float, p1y: Float, u1: Float, v1: Float,
        p2x: Float, p2y: Float, u2: Float, v2: Float,
        p3x: Float, p3y: Float, u3: Float, v3: Float,
        argb: Int,
    ) {
        val base = vertexCount
        addQuad(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, argb)
        padTexCoordsTo(base)
        val texCoords = texCoords
        var t = base * 2
        texCoords[t++] = u0; texCoords[t++] = v0
        texCoords[t++] = u1; texCoords[t++] = v1
        texCoords[t++] = u2; texCoords[t++] = v2
        texCoords[t++] = u3; texCoords[t] = v3
        texCoordsWritten = base + 4
    }

    /**
     * Adds a triangle from three vertices already appended via [addVertex] or [addTexturedVertex] — the
     * three-corner sibling of the indexed [addQuad], for geometry fanned from shared vertices.
     */
    fun addTriangle(index0: Int, index1: Int, index2: Int) {
        ensureIndexCapacity(indexCount + 3)
        val indices = indices
        var i = indexCount
        indices[i++] = index0.toShort(); indices[i++] = index1.toShort(); indices[i] = index2.toShort()
        indexCount += 3
    }

    /**
     * Adds a quad from four vertices already appended by [addVertex], taken in the same corner order the
     * position-carrying [addQuad] takes them and split along the same diagonal — anything that reproduces the
     * interpolated surface elsewhere (a height sampler, say) has to fold it the same way.
     */
    fun addQuad(index0: Int, index1: Int, index2: Int, index3: Int) {
        ensureIndexCapacity(indexCount + 6)
        val indices = indices
        var i = indexCount
        indices[i++] = index0.toShort(); indices[i++] = index1.toShort(); indices[i++] = index2.toShort()
        indices[i++] = index0.toShort(); indices[i++] = index2.toShort(); indices[i] = index3.toShort()
        indexCount += 6
    }

    /**
     * Adds a single solid-color triangle.
     */
    fun addTriangle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
        argb: Int,
    ) {
        ensureVertexCapacity(vertexCount + 3)
        ensureIndexCapacity(indexCount + 3)
        val positions = positions
        val base = vertexCount
        var p = base * 2
        positions[p++] = ax; positions[p++] = ay
        positions[p++] = bx; positions[p++] = by
        positions[p++] = cx; positions[p] = cy
        colors.fill(argb, base, base + 3)
        val indices = indices
        var i = indexCount
        indices[i++] = base.toShort(); indices[i++] = (base + 1).toShort(); indices[i] = (base + 2).toShort()
        vertexCount += 3
        indexCount += 3
    }

    /**
     * Adds a triangle whose three corners each carry their own color; the rasterizer interpolates
     * them across the face ([argbA] belongs to the first corner, and so on) — a gradient costs the
     * same as a flat fill here, so a band fading out across a triangle needs no extra geometry.
     */
    fun addTriangle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
        argbA: Int, argbB: Int, argbC: Int,
    ) {
        ensureVertexCapacity(vertexCount + 3)
        ensureIndexCapacity(indexCount + 3)
        val positions = positions
        val colors = colors
        val base = vertexCount
        var p = base * 2
        positions[p++] = ax; positions[p++] = ay
        positions[p++] = bx; positions[p++] = by
        positions[p++] = cx; positions[p] = cy
        colors[base] = argbA; colors[base + 1] = argbB; colors[base + 2] = argbC
        val indices = indices
        var i = indexCount
        indices[i++] = base.toShort(); indices[i++] = (base + 1).toShort(); indices[i] = (base + 2).toShort()
        vertexCount += 3
        indexCount += 3
    }

    /**
     * Appends one cross-section of a stroke - the vertices spanning [halfWidth] either side of ([x], [y]) along
     * ([offsetX], [offsetY]) - and returns its first vertex index for [addStrokePiece]. Consecutive sections
     * joined into pieces make a polyline that tiles exactly at its joints, where [addLine]'s square-capped
     * segments would overlap and double-blend a translucent stroke; the offset is a unit normal on a straight
     * run and the mitre of the two directions meeting at a corner, lengthened past one so the section still
     * spans the full width there. Anti-aliased, the section carries the fringe [addLine] draws: a hair of
     * transparency outside a core narrowed to keep the width reading the same.
     */
    fun addStrokeSection(x: Float, y: Float, offsetX: Float, offsetY: Float, halfWidth: Float, argb: Int, isAntiAlias: Boolean, pixelsPerUnit: Float): Int {
        ensureVertexCapacity(vertexCount + if (isAntiAlias) 4 else 2)
        val base = vertexCount
        val positions = positions
        val colors = colors
        var p = base * 2
        if (isAntiAlias) {
            val fadeWidth = 1f / pixelsPerUnit
            val coreHalfWidth = maxOf(0f, halfWidth - fadeWidth * 0.5f)
            val outerHalfWidth = coreHalfWidth + fadeWidth
            val transparent = argb and 0x00FFFFFF
            positions[p++] = x + offsetX * outerHalfWidth; positions[p++] = y + offsetY * outerHalfWidth
            positions[p++] = x + offsetX * coreHalfWidth; positions[p++] = y + offsetY * coreHalfWidth
            positions[p++] = x - offsetX * coreHalfWidth; positions[p++] = y - offsetY * coreHalfWidth
            positions[p++] = x - offsetX * outerHalfWidth; positions[p] = y - offsetY * outerHalfWidth
            colors[base] = transparent; colors[base + 1] = argb; colors[base + 2] = argb; colors[base + 3] = transparent
            vertexCount += 4
        } else {
            positions[p++] = x + offsetX * halfWidth; positions[p++] = y + offsetY * halfWidth
            positions[p++] = x - offsetX * halfWidth; positions[p] = y - offsetY * halfWidth
            colors[base] = argb; colors[base + 1] = argb
            vertexCount += 2
        }
        return base
    }

    /**
     * Fills the stroke between two sections appended by [addStrokeSection] with the same [isAntiAlias] - the
     * core, and anti-aliased the fringe either side of it.
     */
    fun addStrokePiece(sectionA: Int, sectionB: Int, isAntiAlias: Boolean) {
        addQuad(sectionA, sectionB, sectionB + 1, sectionA + 1)
        if (isAntiAlias) {
            addQuad(sectionA + 1, sectionB + 1, sectionB + 2, sectionA + 2)
            addQuad(sectionA + 2, sectionB + 2, sectionB + 3, sectionA + 3)
        }
    }

    /**
     * Emits a line segment as a thin quad. Endpoints are extended by their half width (square caps) so that
     * segments meeting at shared corners close the joint instead of leaving notches.
     *
     * With [isAntiAlias], up to three quads are emitted instead: an opaque core flanked by two fringes fading
     * to transparent over one screen pixel ([pixelsPerUnit] converts that pixel into the caller's geometry
     * units). The anti-aliased path writes its 8 shared corners once (outer-left, inner-left, inner-right,
     * outer-right, each at both endpoints) and indexes the three quads from them, instead of appending an
     * independent quad per stripe.
     *
     * [argbB] and [halfWidthB] give the B end its own color and width, the line's counterpart of the
     * per-corner quads: a shaded outline has to interpolate along exactly the surface it borders, or it
     * crosses over it somewhere in the middle, and a stroke that swells along its length has to taper
     * within each segment rather than step at the joints. Both default to the A end's value, which is
     * the flat line every other caller wants.
     */
    fun addLine(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        halfWidth: Float,
        argbA: Int,
        isAntiAlias: Boolean = false,
        pixelsPerUnit: Float = 1f,
        argbB: Int = argbA,
        halfWidthB: Float = halfWidth,
    ) {
        var dx = bx - ax
        var dy = by - ay
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.001f) return
        val invLength = 1f / length
        dx *= invLength
        dy *= invLength
        val nx = -dy
        val ny = dx
        val exA = dx * halfWidth
        val eyA = dy * halfWidth
        val exB = dx * halfWidthB
        val eyB = dy * halfWidthB

        if (isAntiAlias) {
            val fadeWidth = 1f / pixelsPerUnit
            val coreHalfWidthA = maxOf(0f, halfWidth - fadeWidth * 0.5f)
            val coreHalfWidthB = maxOf(0f, halfWidthB - fadeWidth * 0.5f)
            val innerXA = nx * coreHalfWidthA
            val innerYA = ny * coreHalfWidthA
            val outerXA = nx * (coreHalfWidthA + fadeWidth)
            val outerYA = ny * (coreHalfWidthA + fadeWidth)
            val innerXB = nx * coreHalfWidthB
            val innerYB = ny * coreHalfWidthB
            val outerXB = nx * (coreHalfWidthB + fadeWidth)
            val outerYB = ny * (coreHalfWidthB + fadeWidth)
            val transparentA = argbA and 0x00FFFFFF
            val transparentB = argbB and 0x00FFFFFF

            ensureVertexCapacity(vertexCount + 8)
            ensureIndexCapacity(indexCount + 18)
            val positions = positions
            val colors = colors
            val base = vertexCount
            var p = base * 2
            // Rows across the normal axis (outer-left, inner-left, inner-right, outer-right), each
            // written as its A-end then its B-end.
            positions[p++] = ax - exA + outerXA; positions[p++] = ay - eyA + outerYA
            positions[p++] = bx + exB + outerXB; positions[p++] = by + eyB + outerYB
            positions[p++] = ax - exA + innerXA; positions[p++] = ay - eyA + innerYA
            positions[p++] = bx + exB + innerXB; positions[p++] = by + eyB + innerYB
            positions[p++] = ax - exA - innerXA; positions[p++] = ay - eyA - innerYA
            positions[p++] = bx + exB - innerXB; positions[p++] = by + eyB - innerYB
            positions[p++] = ax - exA - outerXA; positions[p++] = ay - eyA - outerYA
            positions[p++] = bx + exB - outerXB; positions[p] = by + eyB - outerYB
            colors[base] = transparentA; colors[base + 1] = transparentB
            colors[base + 2] = argbA; colors[base + 3] = argbB
            colors[base + 4] = argbA; colors[base + 5] = argbB
            colors[base + 6] = transparentA; colors[base + 7] = transparentB
            vertexCount += 8

            val indices = indices
            var i = indexCount
            // Left fringe: outer-left -> inner-left.
            indices[i++] = base.toShort(); indices[i++] = (base + 1).toShort(); indices[i++] = (base + 3).toShort()
            indices[i++] = base.toShort(); indices[i++] = (base + 3).toShort(); indices[i++] = (base + 2).toShort()
            // Right fringe: inner-right -> outer-right.
            indices[i++] = (base + 4).toShort(); indices[i++] = (base + 5).toShort(); indices[i++] = (base + 7).toShort()
            indices[i++] = (base + 4).toShort(); indices[i++] = (base + 7).toShort(); indices[i++] = (base + 6).toShort()
            indexCount += 12
            if (coreHalfWidthA > 0f || coreHalfWidthB > 0f) {
                // Core: inner-left -> inner-right.
                indices[i++] = (base + 2).toShort(); indices[i++] = (base + 3).toShort(); indices[i++] = (base + 5).toShort()
                indices[i++] = (base + 2).toShort(); indices[i++] = (base + 5).toShort(); indices[i] = (base + 4).toShort()
                indexCount += 6
            }
        } else {
            val cxA = nx * halfWidth
            val cyA = ny * halfWidth
            val cxB = nx * halfWidthB
            val cyB = ny * halfWidthB
            addQuad(
                p0x = ax - exA + cxA, p0y = ay - eyA + cyA,
                p1x = bx + exB + cxB, p1y = by + eyB + cyB,
                p2x = bx + exB - cxB, p2y = by + eyB - cyB,
                p3x = ax - exA - cxA, p3y = ay - eyA - cyA,
                argb0 = argbA, argb1 = argbB, argb2 = argbB, argb3 = argbA,
            )
        }
    }

    /**
     * Draws the accumulated triangles without clearing them — for callers that build geometry once and replay
     * it across frames under a changing canvas transform, such as a cached grid.
     *
     * With [replace] the triangles composite by source-replace instead of source-over, so overlapping colors do
     * not blend. That is what a translucent group drawn into its own offscreen layer needs: a nearer shape has
     * to occlude a farther one (and its own overlapping parts) rather than double-darken it inside the buffer,
     * with the whole layer then composited once at the group's alpha.
     */
    fun draw(canvas: Canvas, replace: Boolean = false, isAntiAlias: Boolean = false) {
        if (vertexCount == 0) return
        val texture = texture
        val hasTexCoords = texture != null && texCoordsWritten > 0
        if (hasTexCoords) padTexCoordsTo(vertexCount)
        drawTriangles(
            canvas = canvas,
            positions = positions,
            colors = colors,
            indices = indices,
            vertexCount = vertexCount,
            indexCount = indexCount,
            texCoords = if (hasTexCoords) texCoords else null,
            texture = if (hasTexCoords) texture else null,
            replace = replace,
            isAntiAlias = isAntiAlias,
        )
    }

    /** Empties the batch without drawing it, bumping [generation] so cached vertex indices read as stale. */
    fun reset() {
        vertexCount = 0
        indexCount = 0
        texCoordsWritten = 0
        generation++
    }

    /** [draw] followed by [reset] — what a batch filled and handed over once per frame wants. */
    fun flush(canvas: Canvas, replace: Boolean = false, isAntiAlias: Boolean = false) {
        draw(canvas, replace, isAntiAlias)
        reset()
    }

    // Brings the written run up to [end] with the default coordinate. Both components share one value so the
    // gap - which is most of the batch - closes with a single fill rather than a strided walk.
    private fun padTexCoordsTo(end: Int) {
        if (texCoords.size < vertexCount * 2) texCoords = texCoords.copyOf(maxOf(vertexCount, colors.size) * 2)
        if (texCoordsWritten >= end) return
        texCoords.fill(defaultU, texCoordsWritten * 2, end * 2)
        texCoordsWritten = end
    }

    private fun writeQuadIndices(base: Int) {
        val indices = indices
        var i = indexCount
        indices[i++] = base.toShort(); indices[i++] = (base + 1).toShort(); indices[i++] = (base + 2).toShort()
        indices[i++] = base.toShort(); indices[i++] = (base + 2).toShort(); indices[i] = (base + 3).toShort()
    }

    private fun ensureVertexCapacity(requiredVertexCount: Int) {
        check(requiredVertexCount <= MAX_INDEXED_VERTICES) {
            "TriangleBatch cannot hold more than $MAX_INDEXED_VERTICES unique vertices between flushes " +
                "(index range overflow) — the batch owner must flush more often."
        }
        if (colors.size < requiredVertexCount) {
            val newSize = maxOf(requiredVertexCount, colors.size * 2)
            positions = positions.copyOf(newSize * 2)
            colors = colors.copyOf(newSize)
        }
    }

    private fun ensureIndexCapacity(requiredIndexCount: Int) {
        if (indices.size < requiredIndexCount) {
            indices = indices.copyOf(maxOf(requiredIndexCount, indices.size * 2))
        }
    }

    companion object {

        /**
         * The most unique vertices one batch can hold between resets: its indices are [Short]-backed, and a
         * mesh padded past this is silently dropped by some GPU backends rather than clipped.
         */
        const val MAX_INDEXED_VERTICES = 65535

        private const val INITIAL_VERTEX_CAPACITY = 1024
        private const val INITIAL_INDEX_CAPACITY = 1536
    }
}
