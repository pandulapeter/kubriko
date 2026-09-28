# Key the debug overlays by Kubriko instance and release every registration when it leaves composition

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `tool-debug-menu`
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/InternalDebugMenu.kt`, `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/DebugMenuContainer.kt`, `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/RefCountedRegistry.kt` (new), `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenu.kt`, `tools/debug-menu/src/commonTest/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/RefCountedRegistryTest.kt` (new), `tools/debug-menu/CLAUDE.md`

## Problem

`InternalDebugMenu` (~111-129) keeps one debug-overlay `Kubriko` per game, keyed by `instanceName`:

```kotlin
fun setGameKubriko(kubriko: Kubriko?) {
    val mutableMap = debugMenuKubriko.value.toMutableMap()
    if (mutableMap[kubriko?.instanceName] == null && kubriko != null) {
        mutableMap[kubriko.instanceName] = Kubriko.newInstance(
            kubriko.get<ViewportManager>(),
            DebugMenuManager(kubriko),
        )
        ...
    }
}

fun clearGameKubriko(kubriko: Kubriko?) {
    val mutableMap = debugMenuKubriko.value.toMutableMap()
    mutableMap[kubriko?.instanceName]?.let { debugMenuKubriko ->
        debugMenuKubriko.dispose()
        ...
    }
}
```

1. `instanceName` is `instanceNameForLogging ?: toString().substringAfterLast('@')` (`KubrikoImpl.kt` ~36), so it is not unique: every scene editor passes `instanceNameForLogging = "SceneEditor"` (`InternalSceneEditor.kt` ~70), and every instance of the same game shares its `LOG_TAG`. Two such instances share one overlay whose `DebugMenuManager` is bound to whichever game registered first — the second shows the first one's metrics and body overlays (or those of an already disposed Kubriko), and removing either one disposes the overlay under the other.
2. `Horizontal`/`Vertical` register through `DebugMenuContainer` (~31-33) with a `LaunchedEffect(kubriko) { InternalDebugMenu.setGameKubriko(kubriko) }` and never clear. Only `OverlayOnly`'s `DisposableEffect` clears. A consumer who uses the panels without `OverlayOnly` (a supported, public combination) leaves one overlay `Kubriko` — and through its `DebugMenuManager` the whole game `Kubriko` graph — in the process-wide map for every game instance they ever show.
3. Because registration is not counted, when `OverlayOnly` leaves while a panel for the same game is still shown, the overlay is disposed from under the panel.

## Fix

1. Add `RefCountedRegistry.kt` (MPL header, `internal`), a small main-thread-only holder:

   ```kotlin
   internal class RefCountedRegistry<K : Any, V : Any>(
       private val create: (K) -> V,
       private val release: (V) -> Unit,
   ) {
       private val entries = mutableMapOf<K, Entry<V>>()
       private val _values = MutableStateFlow(persistentMapOf<K, V>())
       val values = _values.asStateFlow()

       fun acquire(key: K) { /* count++, create + publish on the first acquire */ }
       fun release(key: K) { /* count--, remove + publish + release(value) when it reaches 0; ignore unknown keys */ }

       private class Entry<V>(val value: V, var count: Int)
   }
   ```

   Keys are compared with `equals`; `KubrikoImpl` does not override it, so a `Kubriko` key is identity-based.
2. In `InternalDebugMenu`, replace `_debugMenuKubriko`/`setGameKubriko`/`clearGameKubriko` with a `RefCountedRegistry<Kubriko, Kubriko>` whose `create` builds the overlay `Kubriko` exactly as today (plan 68 changes its `ViewportManager` separately) and whose `release` calls `dispose()`. Expose `debugMenuKubriko = registry.values` and `registerGameKubriko(kubriko: Kubriko)` / `unregisterGameKubriko(kubriko: Kubriko)`.
3. `DebugMenu.OverlayOnly`: `DisposableEffect(kubriko) { kubriko?.let(InternalDebugMenu::registerGameKubriko); onDispose { kubriko?.let(InternalDebugMenu::unregisterGameKubriko) } }`, and look the overlay up with `InternalDebugMenu.debugMenuKubriko.collectAsState().value[kubriko]`.
4. `DebugMenuContainer`: replace the `LaunchedEffect` with the same `DisposableEffect(kubriko)` pair.

All calls come from composition effects, so they run on the main thread; the class needs no locking (say so in one KDoc line on it).

Update `tools/debug-menu/CLAUDE.md`: "a per-game Kubriko map keyed by `instanceName`" → "a per-game overlay Kubriko, keyed by the game `Kubriko` instance and reference-counted across `OverlayOnly`, `Horizontal` and `Vertical`; the last one to leave composition disposes it" (Key Files and Architecture both say `instanceName`).

## Tests

`RefCountedRegistryTest` in `tools/debug-menu/src/commonTest/...` (pure logic, uses plain `Any()` keys and a counter for `create`/`release`):
- one `acquire` publishes one value and calls `create` once; a second `acquire` of the same key does not call `create` again;
- after two `acquire`s, one `release` keeps the value and does not call `release`; the second removes it from `values` and calls `release` exactly once;
- two distinct keys that are `equals`-different but would share a name (two `Any()` instances) get two values; releasing one leaves the other;
- `release` of an unknown key is a no-op.

Run `./gradlew :tools:debug-menu:desktopTest`.

## Manual check

Desktop Showcase with `showcase.isDebugMenuEnabled=true`: open Annoyed Penguins, open its Scene Editor, close the editor and open it again, then enable the editor's debug menu: the metrics show the editor's actor count, not a stale one. With the Showcase's debug panel open, switch between two games several times: the panel always shows the current game's metrics and body overlays.
