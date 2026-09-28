# Document the real rules for sharing a Manager between Kubriko instances, and stop updating a disposed Manager

**Challenged:** amended — the documented rule "bound to the first instance that starts" is only true for custom/plugin Managers; a shared Manager of one of the four built-in types is initialized in `KubrikoImpl`'s constructor, so it binds to the first instance *created*; and the list of shareable Managers now reads as examples, since the examples also share custom Managers (`sharedLoadingManager`, `sharedUserPreferencesManager`).

**Decision needed:** a Manager shared by two instances is bound to whichever instance initializes it first, is updated once per tick of *each* instance, and is disposed (for both) when *either* instance is disposed. Reference-count it, or document these rules? — recommended: document them, and additionally skip `onUpdate` for a Manager that has already been disposed.

**Kind:** docs + bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt` (KDoc of `newInstance`/`dispose`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/SharedManagerTest.kt` (new), `CLAUDE.md` (Common patterns → Multiple Kubriko instances), `engine/README.md` (Manager Lifecycle paragraph mentioning multiple instances)

Apply after plan 14 (same file, `Manager.kt`).

## Problem

`CLAUDE.md` → *Multiple Kubriko instances*: "valid (e.g. a background layer and a game layer sharing some Managers). Each instance has its own Manager set. Managers can be shared between instances." The Showcase does exactly that (Space Squadron and Annoyed Penguins share `MusicManager`, `SoundManager`, `SpriteManager` between `backgroundKubriko` and the game instance). What actually happens (`Manager.kt`, 0008d027 ~88-106 and ~141-154):

```kotlin
internal fun initializeInternal(kubriko: Kubriko) {
    if (!isInitialized.value) {
        _scope = kubriko as CoroutineScope
        autoInitializingLazyManagers.forEach { it.initialize(kubriko) }
        ...
```

- Only the first instance to start initializes it: its `scope` is that instance's scope and its `manager<T>()` delegates resolve that instance's Managers (e.g. a shared `SpriteManager` reading the *background* instance's `StateManager`).
- Each instance calls `onUpdate` on every tick, so a shared Manager advances once per instance per frame (a shared `PhysicsManager` would step twice).
- `onDisposeInternal` runs on the first `dispose()` of either instance; the live stress run saw a shared Manager's `onDispose` run on `k1.dispose()` while `k2` kept calling its `onUpdate`, and a shared `ActorManager` stop processing (its processor lived on `k1`'s cancelled scope).

## Fix

1. **Code (small):** in `Manager.onUpdateInternal`, return early when the Manager is not initialized: `if (!_isInitialized.value) return` — a disposed Manager is no longer updated by the other instance. One volatile read per Manager per tick; no allocation.
2. **Docs — recommended:**
   - Root `CLAUDE.md` → *Multiple Kubriko instances*: replace the last sentence with: "A Manager can be passed to several instances, with these rules: it is initialized by — and its `scope` and `manager<T>()` delegates belong to — the first instance that starts (for a Manager of one of the four built-in types, which every instance initializes in its constructor: the first instance created); it receives `onUpdate` on every tick of every instance it belongs to; it is disposed as soon as any of those instances is disposed, so dispose them together. Share only Managers that tolerate this — such as `MusicManager`, `SoundManager`, `SpriteManager`, or a custom Manager holding no per-instance state; never share an `ActorManager`, `PhysicsManager`, or other per-scene state."
   - `Kubriko.newInstance` KDoc `@param manager`: one sentence pointing at the same rules; `Kubriko.dispose` KDoc: "Managers shared with other instances are disposed too."
   - `engine/README.md`: extend "multiple instances of Kubriko could be used" with the one-line rule about disposing sharing instances together.
   **Alternative (reference counting):** count owning instances per Manager, initialize on the first start, dispose on the last dispose, and skip `onUpdate` from all but one owner. Rejected as the default: which instance's scope a shared Manager runs on would still change on the first owner's disposal, which needs re-binding `scope` and delegates — a much larger change.

## Tests

`SharedManagerTest` (desktopTest):
- `disposedSharedManagerIsNotUpdated` — `CountingManager` shared by `k1` and `k2` (both `TickSource.manual()`, both started); `t2.tick(16)` increments it; `k1.dispose()`; assert `disposes == 1`; `t2.tick(16)` does not increment `updates`.
- `sharedManagerIsBoundToTheFirstStartedInstance` — a shared Manager with `private val stateManager by manager<StateManager>()` exposes it; start `k2` first; assert it returns `k2.get<StateManager>()`.

## Manual check

Desktop Showcase: open and leave Space Squadron and Annoyed Penguins repeatedly (they share audio and sprite Managers across two instances); music, sounds and sprites keep working on re-entry.
