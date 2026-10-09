# Extract the debug menu, game viewport and editor overlay block of EditorUserInterface into a private SceneViewport

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt

## Problem
`EditorUserInterface` (206 lines) nests six layout levels deep (Scaffold → Box → Column → Box → Row → Box → DebugMenu), and the
innermost group — the `Box(modifier = Modifier.weight(1f)) { DebugMenu(...) { KubrikoViewport(...); EditorOverlay(...) } }`
block, lines 104-148 at 2480325f — is one conceptual unit (the scene viewport with its pointer handling) buried in the middle.
It also carries a misspelled TODO: `// TODO: Migrate to PonterInputAware`.

## Fix
Run after the plan that passes `shouldShowLoadingIndicator` into `EditorOverlay`.
- Add, in the same file, `@Composable private fun SceneViewport(modifier: Modifier, editorController: EditorController, overlayKubriko: Kubriko) = Box(modifier = modifier) { ... }`
  whose body is the current Box content verbatim (the `DebugMenu { KubrikoViewport; EditorOverlay(...) }` call with all its
  modifier chains and arguments).
- Replace the block with `SceneViewport(modifier = Modifier.weight(1f), editorController = editorController, overlayKubriko = overlayKubriko)`
  — `weight` stays at the call site (RowScope), the Box wrapper moves into `SceneViewport`, so no wrapper is added or dropped.
- Fix the TODO's spelling to `// TODO: Migrate to PointerInputAware` (keep it; the migration is still open).

The private sub-Composable takes the controller because it wires a dozen of its members into the pointer modifiers; it is part of
the screen, not a shared component.

## Behaviour
Same tree, same modifiers, same order; rendered UI unchanged.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
Open the Scene Editor: viewport size, click-select, drag, zoom and the debug menu toggle behave as before.
