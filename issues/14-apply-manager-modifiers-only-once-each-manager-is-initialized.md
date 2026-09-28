# Apply a Manager's modifiers only once that Manager is initialized

**Challenged:** sound

**Decision needed:** `processModifier`/`processOverlayModifier` are called in the viewport's first composition, before `TickSource.start()` has run the Manager's `onInitialize`. Stop calling them until the Manager is initialized? — recommended: yes (the same gate `Composable()` already has).

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/CLAUDE.md` (Manager Composable Extension Points)

Apply after plans 12-13 (same file) and after the ActorManager plans.

## Problem

`Manager.ComposableInternal` renders `Composable()` only once the Manager is initialized, but the modifier hooks have no such gate:

```kotlin
// InternalViewport.kt ~209-212
Box(
    modifier = kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { overlayModifierToProcess, manager ->
        manager.processOverlayModifierInternal(overlayModifierToProcess)
    }
)
```

```kotlin
// ActorManagerImpl.kt ~551-555
val isKubrikoInitialized = isInitialized.collectAsState().value
Box(
    modifier = if (isKubrikoInitialized) kubrikoImpl.managers.fold(Modifier.clipToBounds()) { modifierToProcess, manager ->
        manager.processModifierInternal(modifierToProcess, null, gameTime)
    } else Modifier.clipToBounds(),
```

The second gate reads the **ActorManager's own** `isInitialized`, which is true from construction (built-in Managers initialize in `KubrikoImpl.init`); the per-layer `Canvas` fold (~588-590) has no gate at all. The first composition happens before `InternalViewport`'s `LaunchedEffect` calls `TickSource.start()`, so a custom Manager whose `processModifier` override reads a `manager<T>()` delegate or `scope` throws `IllegalStateException` during composition. The in-repo overrides survive only by accident (constructor-injected dependencies in the examples' `UIManager`s; `ShaderManagerImpl` gates itself with `if (!isInitialized.collectAsState().value) return modifier`).

## Fix

1. In `Manager`, make both internal entry points gate on the Manager's own state:
   ```kotlin
   @Composable
   internal fun processOverlayModifierInternal(modifier: Modifier) =
       if (isInitialized.collectAsState().value) processOverlayModifier(modifier) else modifier
   ```
   and the same for `processModifierInternal`. `collectAsState` on a `StateFlow` does not allocate per frame after the first composition, and the Manager list is fixed, so the call count per fold is stable.
2. In `ActorManagerImpl.Composable`, drop the `isKubrikoInitialized` special case (the per-manager gate covers it).
3. `ShaderManagerImpl`'s own gate becomes redundant but harmless — leave it (plugins lane owns it).

Observable change: the modifiers of a Manager appear in the composition right after its initialization instead of from the first frame (pointer handlers of `PointerInputManager` are attached one recomposition later, still before the first tick).

KDoc: `Manager.processModifier` and `processOverlayModifier`: "Only called once this Manager has been initialized." `engine/CLAUDE.md` → *Manager Composable Extension Points*: same sentence.

## Tests

No unit test (composition-only behaviour; no Compose UI test setup).

## Manual check

Desktop Showcase: Space Squadron and Wallbreaker hide the pointer while running (their `processModifier` sets `pointerHoverIcon`) and pointer input works in every input-driven example (Input test, Annoyed Penguins slingshot, Physics demo).
