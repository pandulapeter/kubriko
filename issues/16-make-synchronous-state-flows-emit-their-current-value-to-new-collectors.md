# Make the synchronous StateFlows emit their current value to new collectors

**Challenged:** amended — `isRunningFirstEmissionMatchesValue` raced the Swing Main dispatcher (the `Eagerly` delegate may already be updated when a test-thread `first()` runs, so it could pass at HEAD); it now runs `start()` and `first()` inside one non-suspending `runBlocking(Dispatchers.Main)` block, which fails deterministically at HEAD.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/SyncStateFlow.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/SyncStateFlowTest.kt` (new), `engine/CLAUDE.md` (Gotchas)

## Problem

`SyncStateFlow` (0008d027) computes `.value` synchronously but hands collection straight to the delegate:

```kotlin
override val replayCache: List<T> get() = delegate.replayCache
override suspend fun collect(collector: FlowCollector<T>): Nothing = delegate.collect(collector)
override val value: T get() = getSyncValue()
```

The delegates are `stateIn(scope + Dispatchers.Main, SharingStarted.WhileSubscribed(), <placeholder>)` in `ViewportManagerImpl` (`scaleFactor` with `Scale.Unit`, `topLeft`/`bottomRight` with `SceneOffset.Zero`) and `stateIn(..., Eagerly, false)` for `StateManagerImpl.isRunning`. A `WhileSubscribed` state flow only starts its upstream when the first collector arrives, so that collector first receives the placeholder, and a later collector (after the subscribers went away) receives whatever was current when the last one left. So:
- `viewportManager.scaleFactor.first()` returns `Scale.Unit` even when the real scale is 2 (reported by the reviewer), `topLeft.first()` returns `SceneOffset.Zero`.
- `collectAsState()` starts from `.value` (correct) and then receives the stale placeholder from `collect`, then the real value — UI bound to these flows flickers for a frame (e.g. a minimap or HUD positioned from `topLeft`).
- The flow contract is broken: `value` and the first emission disagree.

## Fix

Make collection derive from the synchronous getter:

```kotlin
override val replayCache: List<T> get() = listOf(getSyncValue())

override suspend fun collect(collector: FlowCollector<T>): Nothing {
    delegate.map { getSyncValue() }.distinctUntilChanged().collect(collector)
    awaitCancellation()
}
```

The delegate is still what schedules emissions (it changes whenever an input changes), but every emitted value — including the first — is the synchronously computed current one, and consecutive duplicates are dropped as a `StateFlow` requires. `awaitCancellation()` only satisfies the `Nothing` return type (a `StateFlow` never completes). This allocates per `collect` call, not per frame.

`engine/CLAUDE.md` → *Gotchas* → the `SyncStateFlow.value` bullet: add "collectors also receive `getSyncValue()`, never the delegate's placeholder."

## Tests

`SyncStateFlowTest` (desktopTest, needs `Dispatchers.Main` — the desktop test classpath has Swing's Main dispatcher via Compose; if it does not, use `kotlinx-coroutines-test`'s `Dispatchers.setMain`):
- `firstEmissionIsTheCurrentScale` — `Kubriko.newInstance(ViewportManager.newInstance(initialScaleFactor = 2f), tickSource = manual)`, start; `runBlocking { withTimeout(2_000) { assertEquals(Scale(2f, 2f), viewportManager.scaleFactor.first()) } }`.
- `firstEmissionIsTheCurrentTopLeft` — set the size with `updateSize(Size(800f, 600f))` and `setCameraPosition(SceneOffset(100f.sceneUnit, 0f.sceneUnit))`; `topLeft.first()` equals `topLeft.value`.
- `isRunningFirstEmissionMatchesValue` — inside a single `runBlocking(Dispatchers.Main) { … }` block (so the `Eagerly` delegate, which updates on Main, cannot catch up in between): `tickSource.start()` with auto-start, then `assertEquals(stateManager.isRunning.value, stateManager.isRunning.first())` with nothing suspending before `first()`. At HEAD `value` is `true` while the delegate still holds its `false` placeholder, so it fails every time; do not call `first()` from the test thread, which races the Swing event thread.
- `replayCacheHoldsTheCurrentValue` — `scaleFactor.replayCache == listOf(scaleFactor.value)`.

## Manual check

Desktop Showcase: the isometric demo's minimap and any HUD tied to `topLeft`/`bottomRight` show no one-frame jump when the screen opens.
