# Retry failed sprite loads with backoff, and discard a load whose sprite was unloaded while it ran

**Challenged:** amended — the test no longer creates `ImageBitmap(1, 1)`: that allocates a Skia bitmap, and the plugin's desktop test classpath has only `skiko-awt`, not the native `skiko-awt-runtime-<os>` (nothing in `kubriko-compose-library` adds `compose.desktop.currentOs`), so it would fail to load the native library; the fake loader returns a small test implementation of the `ImageBitmap` interface instead. Testing extension: sound — nothing here depends on ticks or on the actor queue (loads and the warm-up run on the Kubriko scope's `Dispatchers.Default`), so `awaitCondition` replaces the polling loop one for one; the "wait past the 100 ms warm-up" step stays a plain sleep, because it proves that something does not happen.

**Extended (testing extension):** uses the shared `:tools:test-fixtures` harness instead of a hand-rolled manual-tick instance and polling loop.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (transient failures mostly on Web)  ·  **Artifact:** `plugin-sprites`
**Files:** `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/SpriteManagerImpl.kt`, `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/SpriteManager.kt` (KDoc only), `plugins/sprites/src/desktopTest/kotlin/com/pandulapeter/kubriko/sprites/SpriteLoadRetryTest.kt` (new), `plugins/sprites/CLAUDE.md`

## Problem

`SpriteManagerImpl.get` (at 0008d027) marks a load as in flight with a `null` entry and only ever replaces it on success:

```kotlin
if (resource in currentCache) return null
if (resource in pendingWarmingUp.value) return null
cache.update { it.putting(resource, null) }
scope.launch {
    val bitmap = loadImage(resource)
    if (bitmap != null) {
        pendingWarmingUp.update { it.putting(resource, bitmap) }
        ...
```

and `loadImage` turns every failure into `null` (`catch (e: Exception) { e.printStackTrace(); null }`).

1. **A failed load is permanent.** The `null` marker stays, so every later `get()` returns `null` without retrying, and `getLoadingProgress`/`getSpriteLoadingProgress` never reach `1f`. A game that waits for progress `1f` behind a loading screen (the Showcase games do) **hangs on the loading screen forever** after one transient failure — on the web that is a single failed `fetch` on a flaky connection. The only way out is `unload()` and `get()` again, which nothing does.
2. **`unload()` during a load is undone.** `unload` removes the marker and any pending bitmap, but the running coroutine still puts its bitmap into `pendingWarmingUp` when it finishes, and `promoteToCache` then puts it back into `cache` — the sprite is resident again although the game released it.
3. On Kotlin/Wasm a rejected JavaScript promise surfaces as `JsException`, which is not an `Exception` subclass (confirm against the stdlib at execution time); if the resource loader lets one through, it escapes `catch (e: Exception)`, is reported to the scope's uncaught handler, and leaves the marker behind exactly as in 1.

## Fix

In `get`'s load coroutine:
1. Loop: call `loadImage(resource)`; if it returns a bitmap, stop; otherwise, if the resource is **no longer requested** (`resource !in cache.value` — `unload` removed the marker), stop without publishing; otherwise `delay` with exponential backoff (e.g. 500 ms doubling, capped at 8 s; `private const val`s in the companion) and try again. The loop lives as long as the resource is requested, so a transient failure recovers on its own and a permanently missing asset costs one attempt every 8 s. `unload` and `Kubriko.dispose()` (scope cancellation, which `delay` honours) both end it.
2. Before publishing a successful bitmap, check `resource in cache.value` again and drop the bitmap if the sprite was unloaded meanwhile. Do the check inside the `pendingWarmingUp.update { }` lambda so the window is as small as the two-flow design allows.
3. `loadImage`: catch `Throwable`, rethrowing `CancellationException` (it is a `suspend` function, so cancellation must propagate — today `catch (e: Exception)` swallows it). Print the stack trace only for the first failed attempt of a resource, not on every retry; later failures go through `log(...)` (guarded by `isLoggingEnabled`).
4. KDoc of both `get` overloads in `SpriteManager.kt`: add "A resource that fails to load is retried in the background until it loads or is unloaded, so the result stays null meanwhile." `plugins/sprites/CLAUDE.md` → "Null-while-loading contract": describe the retry and that a load finishing after `unload()` is discarded.

To make this testable, give `SpriteManagerImpl` an internal constructor parameter for the loader, defaulting to the current `loadImage` (e.g. `private val imageLoader: suspend (SpriteResource) -> ImageBitmap? = { loadImage(it) }`), and an internal parameter for the initial backoff so the test does not wait seconds. `SpriteManager.newInstance` keeps its signature.

## Tests

`SpriteLoadRetryTest` in `desktopTest` (the internal constructor is visible to the module's tests). Build a `Kubriko` instance with the `SpriteManagerImpl`, `TickSource.manual()`, `start()` it; construct `SpriteResource`s from any `DrawableResource` (the loader is faked, so the resource is never read — `DrawableResource("fake", emptySet())` under `@OptIn(InternalResourceApi::class)`); the fake loader returns a test-only `ImageBitmap` implementation (`ImageBitmap` is a plain interface: `width`/`height`
1, `colorSpace = ColorSpaces.Srgb`, `hasAlpha = true`, `config = ImageBitmapConfig.Argb8888`, empty `readPixels` and
`prepareToDraw`). Do not use `ImageBitmap(1, 1)`: it allocates a Skia bitmap, and the plugin's desktop test classpath
has `skiko-awt` but not the native `skiko-awt-runtime-<os>` (only `compose.desktop.currentOs` brings it, and the
library modules don't depend on it), so it fails with a native library load error. Nothing in the test draws, so no
real bitmap is needed.
- A loader that fails twice and then succeeds, with a 10 ms initial backoff: poll `get(resource)` (2 s timeout) → eventually non-null; `getSpriteLoadingProgress(listOf(resource)).first { it == 1f }` completes.
- A loader that suspends on a `CompletableDeferred` until released: `get(resource)`, `unload(resource)`, release the deferred with a bitmap, wait past the 100 ms warm-up → `get` returns `null` again **and** starts a new load (the loader's call count goes up), i.e. the old bitmap was not published.
- Dispose the instance at the end of each test.

Build the instance with `newManualKubriko(spriteManager)` from `:tools:test-fixtures` (plan `00`), and poll with the fixtures' `awaitCondition` instead of a hand-rolled loop.

Run `./gradlew :plugins:sprites:desktopTest`.

## Manual check

Web: `./gradlew :app:web:wasmJsBrowserDevelopmentRun`, open DevTools → Network, block one sprite URL of a game (right-click → Block request URL), open that game → the loading screen waits; unblock the URL → within ~8 s the game finishes loading. Before the fix it stays on the loading screen until the page is reloaded.
