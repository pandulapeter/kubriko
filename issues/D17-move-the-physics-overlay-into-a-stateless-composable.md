# Move `PhysicsDemoManager`'s overlay UI into `ui/PhysicsDemoOverlay.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/ui/PhysicsDemoOverlay.kt` (new), `examples/demo-physics/CLAUDE.md`

## Problem
`examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt` (a Manager, `PointerInputAware` actor and scene loader) also hosts its overlay: `override fun Composable(windowInsets: WindowInsets) = Box {` (:108–153: `LoadingOverlay`, a Column with `InfoPanel`, `Spacer(weight)` and a Row holding `PlatformSpecificContent()`, a `Spacer(width 8.dp)` and the action `FloatingButton`).

## Fix
1. Create `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/ui/PhysicsDemoOverlay.kt` (license header) with `@Composable internal fun PhysicsDemoOverlay(windowInsets: WindowInsets, shouldShowLoadingIndicator: Boolean, actionType: ActionType, onActionTypeButtonPressed: () -> Unit, isSceneEditorEnabled: Boolean)`, its body the override's `Box { … }` verbatim: same Column modifiers (`windowInsetsPadding(windowInsets).fillMaxSize().padding(16.dp)`, `spacedBy(8.dp)`, `Alignment.End`), same Row, both `when (actionType)` blocks for icon and content description (D18 replaces them next), `onButtonPressed = onActionTypeButtonPressed`.
2. The override collects `shouldShowLoadingIndicator` and `actionType` and calls the overlay with `::changeSelectedActionType` and `isSceneEditorEnabled`. Trim the Manager's imports (`Res.drawable.ic_*`, `Res.string.shape/chain/explosion`, layout imports).
3. `examples/demo-physics/CLAUDE.md`: "`PhysicsDemoManager` — loads scene JSON, handles input, owns UI composable" becomes "…handles input; its `Composable` override delegates to `ui/PhysicsDemoOverlay`".

Leave `StateHolder.isInfoPanelVisible` (lane G). Lane G's planned scene-editor connection plan rewrites `PlatformSpecificContent` later.

## Behaviour
Same tree and modifiers; the two flows are read in the override instead of the Column scope (recomposes on load/tap only).

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop` and `./gradlew :examples:demo-physics:compileKotlinWasmJs`

## Manual check
Open Physics: loading overlay fades, the action button cycles shape → chain → explosion with the right icon, taps spawn the right thing.
