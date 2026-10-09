# Turn the remaining declaration-level `//` comments of the engine into KDoc

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/MetadataManagerImpl.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMeshBuffers.kt` (created by E02)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatch.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/AxesAlignedBoundingBoxExtensions.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/body/BoxBody.kt`
- `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.android.kt`
- `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.android.kt`
- `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.android.kt`
- `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/PointerIconExtensions.android.kt`
- `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.desktop.kt`
- `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.desktop.kt`
- `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.ios.kt`
- `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.ios.kt`
- `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.web.kt`
- `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.web.kt`
- `engine/src/webMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.web.kt`

Last plan of the lane's Now set: it runs after E02 (moves `TriangleMeshBuffers`), E07/E08 (edit `InternalViewport.kt`)
and E09 (edits `PointerIconExtensions.ios.kt`, which this plan leaves alone). E55 later moves the desktop/iOS/web
actuals and quotes them as they read after this plan.

## Problem

`code-style`: "Documenting a declaration always uses KDoc (`/** … */`), never a `//` block — even for internal or
private declarations." At 2480325f these top-level and member declarations carry `//` blocks (found by scanning
every engine main source for `//` lines directly above a declaration; `ActorManagerImpl` is E10's):

| File | Declaration under the `//` block |
|---|---|
| `KubrikoImpl.kt:70` | `private val managersForTick` |
| `MetadataManagerImpl.kt:30-33` | `private val _gameTime` |
| `InternalViewport.kt:280` | `private const val NANOSECONDS_PER_MILLISECOND` |
| `TriangleMesh.kt:115-116` (→ `TriangleMeshBuffers.kt`) | `private fun trimmedTexCoords` |
| `TriangleMesh.kt:148-150` (→ `TriangleMeshBuffers.kt`) | `private fun nextBucketCapacity` |
| `TriangleMesh.kt:157-158` (→ `TriangleMeshBuffers.kt`) | `private const val MAXIMUM_BUCKET_VERTEX_COUNT` |
| `TriangleBatch.kt:56-58` | `private var texCoords` |
| `TriangleBatch.kt:461-462` | `private fun padTexCoordsTo` |
| `TriangleBatchSupport.kt:38-39` | `internal expect suspend fun probeTextureSampling()` |
| `TriangleBatchSupport.{desktop,ios,web}.kt:12-13` | `internal actual suspend fun probeTextureSampling() = true` |
| `TriangleBatchSupport.android.kt:26-29, 32-35, 38-40, 56-57, 112-113` | `PROBE_SIZE`, `PROBE_TEXEL_OFFSET`, `PROBE_POSITIONS`, `MINIMUM_HALF_DIFFERENCE`, `probeTile()` |
| `AxesAlignedBoundingBoxExtensions.kt:34-35` | `internal fun AxisAlignedBoundingBox.isWithinViewportBounds(scaledHalfViewportSize, …)` |
| `AxesAlignedBoundingBoxExtensions.kt:52-53` (between the KDoc and the declaration) | `fun AxisAlignedBoundingBox.isOverlapping` |
| `BoxBody.kt:109-110` | `override fun updateAxisAlignedBoundingBox` |
| `TriangleMesh.android.kt:21-23, 26, 32-35, 38-40` | `trianglePaint`, `replaceTrianglePaint`, `MAX_ANISOTROPY`, `TEXTURE_PAINT_CACHE_SIZE` |
| `TriangleMesh.{desktop,ios,web}.kt:25, 28, 31-33, 39-40` | `trianglePaint`, `replaceTrianglePaint`, `TEXTURE_PAINT_CACHE_SIZE`, `texturePaintFor` |
| `PlatformUtils.android.kt:124` | `private const val SYSTEM_DEFAULT_REFRESH_RATE` |
| `PlatformUtils.web.kt:56-57` | `internal actual fun PlatformMaximumDisplayRefreshRateEffect` (below `@Composable`) |
| `PointerIconExtensions.android.kt:14` | `internal actual val pointerIconInvisible` |

## Fix

Convert each block into a KDoc on the same declaration with the same words (`// @see X` becomes a real `@see X` tag;
on `@Composable`-annotated declarations the KDoc goes above the annotation). Exceptions, rewritten so they state the
constraint rather than its history or a stale fact:

- `TriangleMesh.{android,desktop,ios,web}.kt` `TEXTURE_PAINT_CACHE_SIZE`: "A handful of identity-keyed slots, so
  textured batches drawn in a stable A, B order don't evict each other and rebuild the image wrapper and shader (on
  Android: the Paint and BitmapShader) on every draw. The fixed size keeps native resources bounded." (desktop, iOS
  and web must stay byte-identical to each other apart from the web's bridge call, see E55).
- `AxesAlignedBoundingBoxExtensions.kt:34-35`: the comment says "this runs for every actor on every visibility
  refresh", which stopped being true when `ActorManagerImpl` started culling inline; it now only backs the public
  overload. → `/** Raw-float math, so the per-frame checks games run through the public overload don't box SceneUnits through the operator chain. */`
- `AxesAlignedBoundingBoxExtensions.kt:52-53`: fold into the existing public KDoc of `isOverlapping` as a second
  paragraph: "It compares raw floats and never boxes, since it is the broad-phase test every collision and raycast
  query funnels through."
- `MetadataManagerImpl.kt:30-33`: write the reference as `[ActorManagerImpl]`'s layer Canvas rather than
  "`ActorManagerImpl.Composable`'s `gameTime`", so E50's split of that file does not leave it stale.

Statement-level `//` comments inside function bodies stay (`InternalViewport`'s frame-loop locals, `BoxBody.kt:114`,
`PlatformUtils.desktop.kt:54-55`, `TriangleMeshBuffers.prepare`'s padding notes, `TriangleBatchSupport.android.kt:98`).
No code changes.

## Behaviour
Unchanged (comments only).

## Public API
None; only the KDoc of the public `isOverlapping` gains a sentence.

## Tests
The existing ones.

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileAndroidMain :engine:compileKotlinIosSimulatorArm64 :engine:compileKotlinWasmJs`

## Manual check
none
