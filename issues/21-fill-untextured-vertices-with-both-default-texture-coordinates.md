# Fill untextured vertices with both default texture coordinates

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatch.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchTexCoordTest.kt` (new)

## Problem

`TriangleBatch`'s KDoc on `texture` says untextured geometry "is given [defaultU]/[defaultV]", but the padding writes `defaultU` into both components (0008d027 ~461-467):

```kotlin
// Brings the written run up to [end] with the default coordinate. Both components share one value so the
// gap - which is most of the batch - closes with a single fill rather than a strided walk.
private fun padTexCoordsTo(end: Int) {
    if (texCoords.size < vertexCount * 2) texCoords = texCoords.copyOf(maxOf(vertexCount, colors.size) * 2)
    if (texCoordsWritten >= end) return
    texCoords.fill(defaultU, texCoordsWritten * 2, end * 2)
    texCoordsWritten = end
}
```

A probe at 0008d027 with `defaultU = 3f, defaultV = 7f` read `(3.0, 3.0)` for an untextured vertex. Any consumer whose opaque-white texel is not on the texture's diagonal gets untextured geometry tinted by the wrong texel. `demo-isometric-graphics` is unaffected (it leaves both at `0f`).

## Fix

Keep the single `fill` when `defaultU == defaultV` (the common case, and what makes the pad cheap); otherwise write the pairs with a strided loop:

```kotlin
if (defaultU == defaultV) {
    texCoords.fill(defaultU, texCoordsWritten * 2, end * 2)
} else {
    var i = texCoordsWritten * 2
    val stop = end * 2
    while (i < stop) { texCoords[i] = defaultU; texCoords[i + 1] = defaultV; i += 2 }
}
```

Update the comment to say the single fill is used when both defaults are equal. No allocation.

## Tests

`TriangleBatchTexCoordTest` in **desktopTest** (the coordinates are private and drawing needs a `Canvas`, so read the private `texCoords` field and invoke the private `padTexCoordsTo(Int)` by reflection rather than adding test-only API):
- `untexturedVerticesGetBothDefaults` — `defaultU = 3f; defaultV = 7f`; `addVertex` × 2, then `addTexturedVertex(0f, 1f, 0, u = 10f, v = 20f)` (which pads the two before it); assert vertices 0 and 1 are `(3, 7)` and vertex 2 is `(10, 20)`.
- `trailingUntexturedVerticesArePadded` — then `addVertex` once more and invoke `padTexCoordsTo(4)`; vertex 3 is `(3, 7)`.
- `equalDefaultsStillPad` — `defaultU = defaultV = 5f`, same shape; vertex 0 is `(5, 5)`.

## Manual check

Showcase isometric demo on a device where `TriangleBatchSupport.isTextureSampledPerVertex()` is true: textured terrain looks unchanged.
