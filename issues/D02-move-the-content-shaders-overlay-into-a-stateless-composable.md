# Move `ContentShadersDemoManager`'s overlay UI into `ui/ContentShadersOverlay.kt`, taking only state and callbacks.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/managers/ContentShadersDemoManager.kt`, `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersState.kt` (new), `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ui/ContentShadersOverlay.kt` (new), `examples/demo-content-shaders/CLAUDE.md`

## Problem
`examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/managers/ContentShadersDemoManager.kt` (226 lines) mixes the actor/shader lifecycle with the whole overlay: `override fun Composable(windowInsets: WindowInsets) = Column(` (:126–173: Column > InfoPanel > Spacer(weight) > Box > `this@Column.AnimatedVisibility` > Box > Panel > `Controls`, plus the `FloatingButton`), the private member Composable `private fun Controls(` (:175–215, six `SmallSwitch`es) and `private data class State(` (:219–226). The UI reads Manager members (`state`, `areControlsExpanded`, `::toggleControlsExpanded`) directly, so it can neither be previewed nor reused, and a Manager owns a UI tree.

## Fix
1. Create `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersState.kt` holding `internal data class ContentShadersState(` — the former `private data class State` verbatim (same six properties and defaults), renamed.
2. In the Manager:
   - `private val state = MutableStateFlow(State())` becomes `private val _state = MutableStateFlow(ContentShadersState())` and `val state = _state.asStateFlow()`; keep `onInitialize`'s reads (`state.value.isComicShaderEnabled` in the `shouldDrawBorder` lambda, `state.onEach { … }`) on the read-only `state`.
   - Add `fun onStateChanged(state: ContentShadersState) = _state.update { state }` (`update` is already imported).
   - The override becomes a one-liner:
     ```kotlin
     @Composable
     override fun Composable(windowInsets: WindowInsets) = ContentShadersOverlay(
         windowInsets = windowInsets,
         areControlsExpanded = areControlsExpanded.collectAsState().value,
         state = state.collectAsState().value,
         onStateChanged = ::onStateChanged,
         onControlsToggled = ::toggleControlsExpanded,
     )
     ```
   - Delete `Controls` and `State` from the Manager and trim its imports.
3. Create `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ui/ContentShadersOverlay.kt` (license header from a sibling) with `@Composable internal fun ContentShadersOverlay(windowInsets: WindowInsets, areControlsExpanded: Boolean, state: ContentShadersState, onStateChanged: (ContentShadersState) -> Unit, onControlsToggled: () -> Unit)` whose body is the old `Composable` body **byte-identical in layout**: same Column modifiers, `InfoPanel(stringResource = Res.string.description, isVisible = StateHolder.isInfoPanelVisible.value)`, `Spacer(Modifier.weight(1f))`, `Box(Modifier.fillMaxWidth())`, `this@Column.AnimatedVisibility(...)` with the same enter/exit specs, inner `Box(Modifier.fillMaxSize())`, `Panel(Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 16.dp))`, and the `FloatingButton`. Replace `state.collectAsState().value` with the `state` parameter, `{ state.value = it }` with `onStateChanged`, `::toggleControlsExpanded` with `onControlsToggled`, and the local `val areControlsExpanded = areControlsExpanded.collectAsState().value` with the parameter.
4. Move `Controls` into the same file as a `private` Composable verbatim (parameter type `ContentShadersState`).
5. `examples/demo-content-shaders/CLAUDE.md`: "`ContentShadersDemoManager` — custom Manager that owns the UI and actor lifecycle" becomes "owns the actor and shader lifecycle; its `Composable` override delegates to `ui/ContentShadersOverlay`".

Leave `StateHolder.isInfoPanelVisible` as is (lane G's plan replaces that global everywhere). Lane D's planned `D58` may later replace this overlay with a shared one in `examples/shared`.

## Behaviour
Same composition tree, wrappers and modifiers. The only difference is where the two flows are collected (the Manager's override instead of the nested Box/AnimatedVisibility scopes), so a toggle recomposes the whole overlay instead of the nested scope — user-driven and rare, no visible change.

## Public API
None (examples are unpublished).

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:demo-content-shaders:compileKotlinDesktop`

## Manual check
Open Content Shaders in the Showcase: toggle the brush button, flip every switch, check the panel scales in from the bottom-right corner and each shader turns on/off as before.
