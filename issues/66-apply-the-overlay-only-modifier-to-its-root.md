# Apply DebugMenu.OverlayOnly's modifier to its root so the side panel gets its width

**Challenged:** amended — the overlay viewport gets no modifier instead of `Modifier.matchParentSize()`, which `KubrikoViewport` puts on its inner aspect-ratio box and would stretch a `Fixed` game's canvas (and, before plan 68, the game's shared `ViewportManager` size) to the full area.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (seen in the desktop scene editor)
**Artifact:** `tool-debug-menu`
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenu.kt`, `tools/debug-menu/CLAUDE.md`

**Decision needed:** `OverlayOnly(modifier = ...)` currently puts the caller's modifier on the internal debug-overlay viewport; should it go on the composable's root instead? — recommended: the root (option A), which is what every other Compose API does with its `modifier` and what `invoke()` already assumes.

## Problem

`invoke()` lays the game out in a `Row` next to the vertical panel and gives the game side the weight:

```kotlin
Row(
    modifier = Modifier.weight(1f),
) {
    OverlayOnly(
        modifier = Modifier.weight(1f),
        ...
    )
    Vertical(
        ...
        width = verticalDebugMenuWidth,
    )
}
```

but `OverlayOnly` (~184-197) never applies `modifier` to its root; it hands it to the debug overlay's viewport inside a `Box`, where the `RowScope` weight is meaningless:

```kotlin
Box {
    KubrikoViewport(
        modifier = Modifier.size(0.dp),
        kubriko = InternalDebugMenu.internalKubriko,
    )
    kubrikoViewport()
    val debugMenuKubriko = ...
    if (debugMenuKubriko != null) {
        KubrikoViewport(
            modifier = modifier,
            kubriko = debugMenuKubriko,
        )
    }
    ...
}
```

So the `Row` sees two unweighted children. It measures `OverlayOnly` first; its game viewport fills the maximum width, so the root `Box` takes the whole row and `Vertical` is measured with **0 px** of width. In landscape (`maxWidth >= maxHeight`) the vertical debug panel is therefore invisible. The scene editor goes through exactly this path (`EditorUserInterface.kt` ~81, `DebugMenu(kubriko = ..., isEnabled = ...) { KubrikoViewport(...); EditorOverlay(...) }`), and editor windows are landscape, so toggling the debug menu in the editor shows nothing. Portrait works only because `Horizontal` sits outside the `Row`.

## Fix

Option A (recommended) — in `OverlayOnly`:
- `Box {` → `Box(modifier = modifier) {`
- the debug overlay viewport: `modifier = modifier` → `modifier = Modifier` (no modifier). Do **not** use `Modifier.matchParentSize()` here: `KubrikoViewport`/`InternalViewport` applies its `modifier` to its *inner* canvas box (the one that takes `.align(...).aspectRatio(...)` for a `Fixed` game), while its own root is always `fillMaxSize()`. The overlay viewport therefore already covers the root `Box`, and `matchParentSize()` on the inner box would only force a `Fixed` game's aspect-ratio box to the full area — and until plan 68 lands the overlay still shares the game's `ViewportManager`, so that full-area size would be written into the game's `size`/`scaleFactorMultiplier`. With no modifier the inner box behaves exactly as it does at HEAD (where the `RowScope` weight it received was ignored).

Leave the `size(0.dp)` persistence viewport and the button `Box` as they are. `invoke()` needs no change — its `Modifier.weight(1f)` now lands on the root. The Showcase calls `OverlayOnly` without a modifier, so it is unaffected.

Option B — keep the current modifier target and instead wrap the `OverlayOnly` call inside `invoke()` in `Box(modifier = Modifier.weight(1f)) { OverlayOnly(modifier = Modifier, ...) }`. This fixes the layout without changing what a direct `OverlayOnly(modifier = ...)` caller gets, but leaves the public `modifier` parameter doing something no caller would expect.

`tools/debug-menu/CLAUDE.md` → Layout: add one line, "`OverlayOnly` applies `modifier` to its root; the debug overlay viewport fills that root."

## Tests

None: a Compose layout change with no pure logic, and plan 00 adds no Compose UI test harness.

## Manual check

Desktop, `showcase.isDebugMenuEnabled=true`: run `./gradlew :app:desktop:run`, open Annoyed Penguins → Scene Editor, enable "Debug Menu" in the editor's settings window and press the debug toggle button in the canvas corner. With the window wider than tall, a 192 dp panel with metrics and logs appears to the right of the canvas and the canvas shrinks accordingly (before the fix nothing appears). Resize the window to be taller than wide: the panel moves to the bottom as before.
