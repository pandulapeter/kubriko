# Give the Skia-backed targets (desktop, iOS, Wasm) a shared skikoMain source set so their identical actuals exist once

**Kind:** build  ·  **Severity:** medium  ·  **Platforms:** desktop, iOS, web  ·  **Class:** Planned
**Artifact:** engine, plugin-shaders, plugin-sprites (no public change); build-logic (unpublished)
**Challenged:** amended — dropped the stale "merges E14 with P18" reviewer numbering (P18 is now an unrelated gamepad KDoc plan) and stated that P03/P10/P14 are Now plans already landed when this runs, P10 touching only the common `ModifierExtensions.kt`.
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:**
- `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/extensions/KotlinMultiplatform.kt`
- `gradle/build-logic/CLAUDE.md`
- engine:
  - `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.desktop.kt` (deleted)
  - `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.ios.kt` (deleted)
  - `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.web.kt`
  - `engine/src/skikoMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMesh.skiko.kt` (new)
  - `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMeshFastPath.desktop.kt` (new)
  - `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/TriangleMeshFastPath.ios.kt` (new)
  - `engine/src/{desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.{desktop,ios,web}.kt` (deleted)
  - `engine/src/skikoMain/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchSupport.skiko.kt` (new)
  - `engine/CLAUDE.md`, root `CLAUDE.md` (the `TriangleBatch` paragraph naming `implementation/TriangleMesh.*.kt`)
- plugins (outside lane E — the plugins lane must not touch these files in the same sweep):
  - `plugins/shaders/src/{desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/shaders/extensions/ModifierExtensions.{desktop,ios,web}.kt` (deleted)
  - `plugins/shaders/src/{desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/shaders/extensions/PlatformExtensions.{desktop,ios,web}.kt` (deleted)
  - `plugins/shaders/src/skikoMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/ModifierExtensions.skiko.kt` (new)
  - `plugins/shaders/src/skikoMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/PlatformExtensions.skiko.kt` (new)
  - `plugins/sprites/src/{desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/sprites/implementation/ImageLoader.{desktop,ios,web}.kt` (deleted)
  - `plugins/sprites/src/skikoMain/kotlin/com/pandulapeter/kubriko/sprites/implementation/ImageLoader.skiko.kt` (new)
  - `plugins/shaders/CLAUDE.md`, `plugins/sprites/CLAUDE.md`

One plan, one commit, for the engine and the two plugins. Every prerequisite has landed: E11 (KDoc in the engine
actuals) in 8c981458, and in the plugins lane P14 (dead imports of `ImageLoader.desktop.kt`) in 5973a0eb, P03
(`ShaderUniformProvider` out of the common `ModifierExtensions.kt` into `ShaderUniformProvider.kt`; the actuals are
unaffected) in 3e4d3162 and P10 (KDoc in that same common file only) in c4bb7b6d.

## Problem

Desktop, iOS and Wasm all draw through Skia via Skiko, and the build gives them no shared source set, so the same
`actual` is written out three times (re-diffed at 70de96c6):

| File | desktop vs iOS | desktop vs web |
|---|---|---|
| engine `TriangleMesh.*.kt` (93 lines) | byte-identical | web adds the `WasmTriangleBridge.draw(...)` fast path (and its comment) after `paint.isAntiAlias = isAntiAlias` (:82), before the public-Skiko fallback |
| engine `TriangleBatchSupport.*.kt` (`internal actual suspend fun probeTextureSampling() = true`) | identical | identical |
| shaders `ModifierExtensions.*.kt` (143 lines: `createRenderEffect`, `drawGenerativeShader`, `applyUniforms`, `ShaderUniformProviderImpl`) | identical | identical |
| shaders `PlatformExtensions.*.kt` (`internal actual val areShadersSupported = true`) | identical | identical |
| sprites `ImageLoader.*.kt` | differs only in the mis-indented `val scale = if (…)` block (:30-33; P14 removed the four unused imports in 5973a0eb); iOS = web byte-identical | same |

Every fix to one copy has to be repeated by hand in the other two (the paint-cache rework and the uniform cache both
were). All moved declarations are `internal`, so there is no facade or binary concern.

## Fix

1. **build-logic** — in `configureKotlinMultiplatform`, after the targets are declared, extend the default hierarchy:
   ```kotlin
   @OptIn(ExperimentalKotlinGradlePluginApi::class)
   applyDefaultHierarchyTemplate {
       common {
           group("skiko") {
               withJvm()
               group("ios")
               group("web")
           }
       }
   }
   ```
   `group("ios")` / `group("web")` reuse the default template's groups, so `iosMain` and `webMain` gain `skikoMain` as
   a second parent and keep `appleMain`/`nativeMain` as before. Check after the change that `withJvm()` matches only
   `jvm("desktop")` and not the `com.android.kotlin.multiplatform.library` target (print the graph with a throwaway
   task, e.g. `kotlin.sourceSets.forEach { println("${it.name} -> ${it.dependsOn.map { d -> d.name }}") }`, and confirm
   `androidMain` does not depend on `skikoMain`). If the Kotlin version in `libs.versions.toml` no longer needs the
   opt-in, drop it. Every module using the convention plugins gets an (empty) `skikoMain`, which is harmless.

   Name: `skikoMain` (recommended) matches Compose Multiplatform's own intermediate source set for the same targets,
   whose `asSkiaBitmap`/`skiaCanvas` APIs the moved code uses; `skiaMain` is the alternative.

2. **engine `TriangleMesh`** — one actual for the three targets, the web fast path behind a tiny expect:
   - `skikoMain/.../implementation/TriangleMesh.skiko.kt`: the desktop file's content (paints, the texture paint
     cache, `texturePaintFor`, `internal actual fun drawTriangles`), with one line added after
     `paint.isAntiAlias = isAntiAlias`:
     ```kotlin
     if (drawTrianglesThroughFastPath(canvas.skiaCanvas, paint, positions, colors, indices, vertexCount, indexCount, texCoords)) return
     ```
     followed by the web file's existing comment ("Use public Skiko while its module loads or the expected
     exports/signature are unavailable.") reworded for all three targets ("Otherwise the public Skiko path…"), then
     `TriangleMeshBuffers.prepare(...)` and `drawVertices(...)` as today. Declare in the same file
     `internal expect fun drawTrianglesThroughFastPath(canvas: org.jetbrains.skia.Canvas, paint: Paint, positions: FloatArray, colors: IntArray, indices: ShortArray, vertexCount: Int, indexCount: Int, texCoords: FloatArray?): Boolean`
     with a KDoc saying only the web has one (`WasmTriangleBridge`).
   - `webMain/.../TriangleMesh.web.kt` shrinks to
     `internal actual fun drawTrianglesThroughFastPath(...) = WasmTriangleBridge.draw(canvas, paint, positions, colors, indices, vertexCount, indexCount, texCoords)`
     (keep the file name, so `engine/src/webMain/README.md` and the root `CLAUDE.md` web paragraph stay accurate).
   - `desktopMain/.../TriangleMeshFastPath.desktop.kt` and `iosMain/.../TriangleMeshFastPath.ios.kt`:
     `internal actual fun drawTrianglesThroughFastPath(...) = false`.
   - Delete `TriangleMesh.desktop.kt` and `TriangleMesh.ios.kt`.

   Alternative: share only the paint cache and keep three small `drawTriangles` actuals — less indirection, but the
   `drawVertices` call and its argument list stay triplicated. Not recommended.

3. **engine `TriangleBatchSupport`** — one `skikoMain/.../helpers/TriangleBatchSupport.skiko.kt` with the KDoc the three
   (still identical) copies carry since E11 and `internal actual suspend fun probeTextureSampling() = true`; delete the three copies.
   Android's actual is untouched.
4. **shaders** — move `ModifierExtensions.desktop.kt` to `skikoMain/.../ModifierExtensions.skiko.kt` and
   `PlatformExtensions.desktop.kt` to `skikoMain/.../PlatformExtensions.skiko.kt` with `git mv` (so history follows
   one copy), delete the iOS and web copies. Re-diff the three right before (`diff -q`): they must still be identical.
5. **sprites** — move the iOS `ImageLoader.ios.kt` (no dead imports, correct indentation) to
   `skikoMain/.../ImageLoader.skiko.kt`; delete the desktop and web copies. Re-diff first: the desktop copy must
   differ from iOS only in the indentation of the `val scale = if (…)` block (true at 70de96c6); any other difference
   stops the plan.
6. **Docs** — `gradle/build-logic/CLAUDE.md` "KMP targets configured by both library plugins": add "Desktop, iOS and
   Wasm share a `skikoMain` source set (Skia through Skiko); put an actual that is identical on the three there." Root
   `CLAUDE.md` `TriangleBatch` paragraph: "Platform draw paths live in `implementation/TriangleMesh.*.kt`" →
   "…in `implementation/TriangleMesh.android.kt` and, shared by the Skia targets, `TriangleMesh.skiko.kt`; the web adds
   its fast path in `TriangleMesh.web.kt`". `plugins/shaders/CLAUDE.md` ("Desktop/iOS/Web it is `RuntimeShaderBuilder`
   (Skia)" at :30, "Desktop, iOS, Web: always supported" at :68) and `plugins/sprites/CLAUDE.md` ("Desktop / iOS / Web: uses Skia
   `Image.makeFromEncoded`…" at :44) each gain "(one `skikoMain` actual)". Grep the repo for the deleted file names afterwards
   (none referenced at 70de96c6).

Out of scope, for a follow-up once the source set exists: other modules with desktop/iOS or iOS/web twins found by the
same diff (`ResourceLoader.*.kt` in four games and `test-audio`, `PlatformSpecificContent.*.kt` in three examples,
`tools/ui-components` `ResourceLoaders`/`DynamicDarkTheme`, `plugins/audio-playback` `MusicManagerDisposer`) are not
Skia-specific and need their own groupings, if any.

## Behaviour
Unchanged on every target: the same code runs; desktop and iOS make one extra call that returns `false` per batch
draw (no allocation), web takes its fast path through the same function as before.

## Public API
None (every moved declaration is `internal`).

## Tests
The existing ones (`TriangleBatchTexCoordTest`, `HotPathAllocationTest`, the shaders/sprites desktop tests).
`node engine/src/webMain/checkTriangleBridge.mjs` still passes (the bridge's JavaScript is untouched).

## Verify
`./gradlew :engine:build :plugins:shaders:build :plugins:sprites:build` (compiles `skikoMain` metadata and every
target), `./gradlew :app:desktop:compileKotlin :app:web:compileKotlinWasmJs`, `./gradlew desktopTest`, and
`./gradlew :engine:build --dry-run | wc -l` before/after to see the added `compileSkikoMainKotlinMetadata`-type tasks
are the only difference.

## Manual check
Run the Showcase on desktop, iOS (simulator) and web: the Isometric graphics demo (textured triangle batches), a shader
demo and a sprite-based game render as before; on web, confirm the bridge fast path is still taken (as
`engine/src/webMain/README.md` describes).
