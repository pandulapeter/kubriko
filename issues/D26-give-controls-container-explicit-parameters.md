# Give `ControlsContainer` explicit parameters instead of a `Pair` and the whole holder map, and name its callback after what it changes.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt` (after D23), `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`

## Problem
`examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt:72-77`:
```kotlin
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    state: Pair<ShaderAnimationDemoType, ControlsState>,
    onIsExpandedChanged: (ControlsState) -> Unit,
    shaderAnimationDemoHolders: PersistentMap<ShaderAnimationDemoType, ShaderAnimationDemoHolder<*, *>>
)
```
The body reads `state.first`/`state.second`; the callback changes a three-value `ControlsState`, not an "is expanded" flag; every holder (each with its own `Kubriko`) is handed to the UI although it only needs a Manager per type; the two-FAB `Row` (:123–143) is inline.

**Do not** pass just the selected Manager (the reviewer's suggestion): the `AnimatedContent(targetState = state, …)` keeps composing the *outgoing* content during the fade with the *old* demo type, and `Controls` casts the Manager by that old type (`manager as ShaderAnimationsDemoManager<CloudShader, CloudShader.State>`, then `manager.shaderState.collectAsState().value` into `CloudControls`), so switching tabs while the controls panel is open would hand the old type's branch the new Manager and throw a `ClassCastException`. The lookup must stay per type.

## Fix
- Signature: `modifier: Modifier = Modifier, selectedDemoType: ShaderAnimationDemoType, controlsState: ControlsState, onControlsStateChanged: (ControlsState) -> Unit, getManager: (ShaderAnimationDemoType) -> ShaderAnimationsDemoManager<*, *>,` (trailing comma).
- Inside, keep `AnimatedContent(targetState = selectedDemoType to controlsState, …)` (same `Pair` equality, so the transition behaves as before), and call `Controls(manager = getManager(targetState.first), demoType = targetState.first)`. Replace `state.second` with `controlsState` in the two `animateFloatAsState` targets and the FABs.
- Extract the Row into `@Composable private fun ControlButtons(modifier: Modifier, controlsState: ControlsState, onControlsStateChanged: (ControlsState) -> Unit)` with the Row taking `modifier`; call it with `modifier = Modifier.align(Alignment.BottomEnd)` at the BoxScope call site. Body verbatim.
- Impl: add `fun getManager(demoType: ShaderAnimationDemoType) = shaderAnimationDemoHolders.getValue(demoType).shaderAnimationsDemoManager`.
- `ShaderAnimationsDemo`: pass `selectedDemoType = selectedDemoType, controlsState = controlsState, onControlsStateChanged = stateHolder::onControlsStateChanged, getManager = stateHolder::getManager`.
- Drop the `PersistentMap`/`ShaderAnimationDemoHolder` imports from `ControlsContainer.kt`.

## Behaviour
Same tree; the AnimatedContent target keeps the same equality semantics; each content branch still gets the Manager of its own demo type.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
Open Shader Animations, expand the controls (brush), switch tabs while expanded and while the code view is open — no crash, the panel crossfades to the new shader's controls/code.
