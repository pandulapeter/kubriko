# Restart the viewport's effects when a different Kubriko instance is passed to it

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`, `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.web.kt`, `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.desktop.kt`, `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.ios.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt` (KDoc)

## Problem

`KubrikoViewport(kubriko = …)` accepts a new instance on recomposition — several examples expose the instance as a `StateFlow<Kubriko>` (`SpaceSquadronGameStateHolder._kubriko`, `AnnoyedPenguinsGameStateHolder._kubriko`) precisely so it can be replaced. `InternalViewport` re-derives `kubrikoImpl` with `remember(kubriko)`, but its effects are keyed on `Unit` and keep the instance they first captured (0008d027):

```kotlin
// InternalViewport.kt ~64
LaunchedEffect(Unit) {
    val tickSource = kubrikoImpl.tickSource
    val viewportTickSource = tickSource as? ViewportFrameTickSource
    viewportTickSource?.start()
    ...
```

- The new instance's `TickSource.start()` is never called, so its custom Managers are never initialized and it never ticks; the frame loop keeps feeding the old (possibly disposed) instance.
- Web `PlatformFocusEffect` uses `DisposableEffect(Unit)` and captures the first `onFocusChanged` lambda, so focus/blur keeps going to the old instance; the new one stays "focused" even in a hidden tab.
- Desktop and iOS `PlatformMaximumDisplayRefreshRateEffect` use `DisposableEffect(Unit)`, so the new instance's `MetadataManager.maximumDisplayRefreshRate` is never published.
- Android/desktop/iOS `LifecycleFocusEffect` uses `rememberUpdatedState`, but only reports on lifecycle events, so the new instance does not learn the current focus until the next one.

## Fix

1. In `InternalViewport`, wrap everything after the `remember(kubriko)` block in `key(kubrikoImpl) { … }`. A new instance then gets a fresh composition: its focus effect reports immediately, the frame-loop `LaunchedEffect` starts its tick source, the refresh-rate effects publish, and the old instance's effects are disposed.
2. Defensive, for the effects that capture a callback: in `PlatformUtils.web.kt` `PlatformFocusEffect` and in the desktop/iOS `PlatformMaximumDisplayRefreshRateEffect`, read the callback through `val current by rememberUpdatedState(callback)` inside the effect (as `LifecycleFocusEffect` and the Android actual already do).
3. `KubrikoViewport` KDoc for `kubriko`: "Passing a different instance restarts the viewport for it; the previous instance is not disposed."

## Tests

No unit test: this is Compose effect wiring, and the engine has no Compose UI test setup. (If plan 00's setup ever adds `compose.uiTest`, a test would swap the instance passed to `KubrikoViewport` and assert the new `ViewportFrameTickSource` reaches `isRunningInternal == true`.)

## Manual check

Desktop: temporarily make an example swap its `_kubriko` value (e.g. Space Squadron on "play again") and confirm the new instance runs, pauses on window blur and resumes on focus. Web (`./gradlew :app:web:wasmJsBrowserDevelopmentRun`): same, then switch tabs and confirm the new instance pauses.
