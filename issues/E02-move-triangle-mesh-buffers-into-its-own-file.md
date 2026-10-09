# Move TriangleMeshBuffers out of TriangleMesh.kt into a file of its own

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMeshBuffers.kt` (new)

## Problem

`TriangleMesh.kt` declares the `internal expect fun drawTriangles(...)` (:28-39) and, below it, an unrelated
`internal object TriangleMeshBuffers` (:41-160, KDoc starting "The trimmed arrays Skia is handed, since it derives the
vertex and index counts from the array sizes…"), the bucket-padding helper only the Skia actuals use.

## Fix

Move `internal object TriangleMeshBuffers` verbatim — its KDoc, every member and the `// …` comments inside it — into
`implementation/TriangleMeshBuffers.kt` (same package, MPL-2.0 header copied from `TriangleMesh.kt`), importing
`com.pandulapeter.kubriko.helpers.TriangleBatch` (used by `MAXIMUM_BUCKET_VERTEX_COUNT`).

`TriangleMesh.kt` keeps only the `drawTriangles` expect and its KDoc. It still needs `Canvas`, `ImageBitmap` and
`TriangleBatch` (its KDoc links `[TriangleBatch.addLine]`), and its KDoc's `[TriangleMeshBuffers]` link resolves in the
same package.

Grep the repo for `TriangleMesh.kt` / `TriangleMeshBuffers` afterwards: root `CLAUDE.md` mentions
`implementation/TriangleMesh.*.kt` and `TriangleMeshBuffers` by name only, which stays correct.

## Behaviour
Unchanged (a verbatim move of an internal object).

## Public API
None.

## Tests
The existing ones (`TriangleBatchTexCoordTest`, `TriangleBatchBookkeepingTest`, `HotPathAllocationTest`).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinWasmJs :engine:compileKotlinIosSimulatorArm64 :engine:compileAndroidMain :engine:desktopTest`

## Manual check
none
