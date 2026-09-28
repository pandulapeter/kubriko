# Give the debug overlay Kubriko its own ViewportManager instead of borrowing the game's

**Challenged:** amended — the overlay's own `ViewportManager` now mirrors the game's `targetFrameRate` (initial value plus a collector), because a default `DisplayDefault` overlay viewport would tick every vsync and, on Android, re-apply a window-wide frame-rate hint that overrides the game's `Limit` hint.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `tool-debug-menu`
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/InternalDebugMenu.kt`, `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/DebugMenuManager.kt`, `tools/debug-menu/CLAUDE.md`

**Decision needed:** With its own `ViewportManager` the overlay can no longer inherit a `Fixed` game's alignment (`AspectRatioMode` is only readable inside the engine). Accept that the overlay assumes the game canvas is centered in the overlay area (option A), or ask the engine lane for a public way to read a `ViewportManager`'s aspect-ratio mode (option B, an `engine` API addition)? — recommended: option A.

## Problem

The overlay `Kubriko` that draws body/collision outlines is created with the **game's** `ViewportManager` (`InternalDebugMenu.kt` ~114-117; plan 67 moves this code into the registry's `create` lambda but keeps it):

```kotlin
Kubriko.newInstance(
    kubriko.get<ViewportManager>(),
    DebugMenuManager(kubriko),
)
```

1. **Size fight.** Every `KubrikoViewport` writes its measured size into its instance's viewport manager (`InternalViewport.kt`, `onSizeChanged { ... updateSize(Size(widthPx, heightPx)); scaleFactorMultiplier.update { ... } }`). The overlay viewport and the game viewport now write into the same manager. Whenever the overlay area is not exactly the game canvas — the Showcase wraps its whole content in `OverlayOnly` (`ShowcaseContent.kt` ~262), and any consumer may do the same — the game's `size` and, for `FitHorizontal`/`FitVertical`/`Fixed`/`Stretched` games, its `scaleFactorMultiplier` flip to whichever viewport was measured last. That moves the game's camera math and its visible-actor culling.
2. **Disposal of a borrowed manager.** `clearGameKubriko` disposes the overlay `Kubriko`, and `KubrikoImpl.dispose()` calls `onDisposeInternal()` on all its managers, including the game's `ViewportManager`, which the game still uses. The next registration (e.g. the scene editor's "Debug Menu" setting toggled off and on — `invoke()` drops `OverlayOnly` while disabled) builds a new overlay `Kubriko` whose constructor re-initializes that manager against the overlay instance (`Manager.initializeInternal` swaps its `scope`, and `ViewportManagerImpl.onInitialize` rebinds its `actorManager` to the overlay's).

`DebugMenuManager` does not need the overlay instance's viewport manager at all: it already reads `cameraPosition`, `scaleFactor` and `size` from `gameViewportManager` (`gameKubriko.get<ViewportManager>()`), and the only manager it takes from its own instance is the `ActorManager` it adds itself to.

## Fix

1. In the overlay factory, pass `ViewportManager.newInstance(initialTargetFrameRate = gameViewportManager.targetFrameRate.value)` (default `Dynamic` aspect ratio) instead of `kubriko.get<ViewportManager>()`, where `gameViewportManager = kubriko.get<ViewportManager>()`. In `DebugMenuManager.onInitialize`, keep it in step: `gameViewportManager.targetFrameRate.onEach(kubriko.get<ViewportManager>()::setTargetFrameRate).launchIn(scope)` (`kubriko` there is the overlay instance). Reason: every `KubrikoViewport` runs its own frame loop and calls `PlatformFrameRateHint(viewportManager.targetFrameRate)`; on Android that hint is a window-wide `preferredDisplayModeId`/`preferredRefreshRate` where the last effect applied wins. The overlay viewport is composed after the game's, so a `DisplayDefault` overlay would release a game's `Limit` hint (Showcase with the debug menu on, any game with a frame-rate limit) and would also wake the overlay's loop on every vsync. Sharing the manager made both viewports agree at HEAD; mirroring the value keeps that. Both are public API (`targetFrameRate`, `setTargetFrameRate`, `initialTargetFrameRate`); no engine change. Drop the now-unused imports.
2. Option A (recommended) — in `DebugMenuManager.drawToViewport()`, the overlay canvas now always fills the overlay area while a `Fixed` game's canvas is an aspect-ratio box inside it. Offset the existing transform by the difference, assuming a centered game canvas: inside the `withTransform` `transformBlock`, before `transformViewport(...)`, add `translate(left = (size.width - gameSize.width) / 2f, top = (size.height - gameSize.height) / 2f)` where `gameSize = gameViewportManager.size.value` and `size` is the `DrawScope` size. For every non-`Fixed` mode the game canvas fills the same area, so the offset is 0 when the two areas coincide. Floats only — no allocation in this per-frame path. Add one `//` line saying the overlay assumes a centered game canvas.

   Option B — the engine lane adds a public read of the aspect-ratio mode (e.g. `ViewportManager.aspectRatioMode`) and the factory passes `ViewportManager.newInstance(aspectRatioMode = gameViewportManager.aspectRatioMode)`; no offset code. This is an `engine` public API change and needs its own decision there.
3. `tools/debug-menu/CLAUDE.md` → Key Files: "added to a separate per-game Kubriko instance sharing the game's `ViewportManager`" → "added to a separate per-game Kubriko instance with its own `ViewportManager`; it reads camera, scale and size from the game's".

The Showcase's `OverlayOnly` placement (overlay spanning more than the game canvas) keeps its outlines only approximately aligned with this or the current code; that call site belongs to the app lane.

## Tests

None: the change is a manager wiring and a draw transform, neither reachable without a composition; the offset itself is one line of arithmetic.

## Manual check

Desktop Showcase, `showcase.isDebugMenuEnabled=true`:
0. Android device with a high-refresh panel, debug menu on: open a game, change its frame rate to a `Limit` (e.g. 60) from its settings if it offers one, open the debug panel: the panel keeps running at the limited rate (developer options → "Show refresh rate") and does not jump back to the panel maximum.
1. Open Annoyed Penguins (`FitVertical`), open the debug panel, enable "Draw body bounds", then toggle the info panel / fullscreen and resize the window: the game keeps its framing (it does not jump or rescale when only the overlay's container changes) and outlines stay on the actors.
2. Open Wallbreaker (`Fixed`, centered): body bounds sit on the bricks and paddle.
3. Scene editor: enable the editor's "Debug Menu" setting, disable it, enable it again, then pan and zoom — the camera still responds and the outlines follow (before the fix the editor's viewport manager is disposed and re-initialized against the overlay instance).
