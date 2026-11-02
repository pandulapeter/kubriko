<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Let the user drag the edges of the five side panels to resize them, within limits, remembering the widths in Creator's settings

**Challenged:** amended — the width limit is now shared by the panels in a row (half the row between them), since 45 % each let two panels leave the editor 10 %; tests adjusted; K09 note.

**Kind:** architecture (feature gap)  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** UI-12 (splitter part)  ·  **Depends on:** H02b (the side panels' composables it splits out; only a merge dependency), H03a (`CreatorSettingsController` reached through `AppGraph`). **K09:** nothing to re-locate (UI and Creator settings only); K11a, earlier in lane K, may have moved `CreatorSettingsStore` onto `JsonFileStore` (see below)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/components/PanelSplitter.kt`, new `.../ui/components/PanelWidthLimits.kt`, `.../ui/components/PanelWidths.kt`, `.../ui/components/SidePanelModifier.kt` (if its signature changes), `.../ui/WorkspaceScreen.kt` (and H02b's `WorkspaceAiSidePanel.kt`/`WorkspaceGitSidePanel.kt`), `.../ui/scenes/SceneEditorPane.kt`, `.../ui/AppContent.kt` (passing the widths down), `.../settings/CreatorSettings.kt`, `.../settings/CreatorSettingsController.kt`, `.../settings/CreatorSettingsStore.kt` (only if K11's `JsonFileStore` hasn't replaced its body), `creator/src/main/composeResources/values/strings.xml`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/components/PanelWidthLimitsTest.kt` (new), `.../ui/components/PanelSplitterTest.kt` (new), `.../settings/CreatorSettingsStoreTest.kt`, `.../settings/CreatorSettingsControllerTest.kt`, `creator/CLAUDE.md`

## Problem

Every side panel has a fixed width (`ui/components/PanelWidths.kt`):
```kotlin
internal object PanelWidths {
    val SIDEBAR = 260.dp
    val AI = 360.dp
    val VERSION_CONTROL = 300.dp
    val SCENE_ACTORS = 220.dp
    val SCENE_INSPECTOR = 260.dp
}
```
used through `Modifier.sidePanel(PanelWidths.X)` in `ui/WorkspaceScreen.kt` (sidebar, AI, version control) and
`ui/scenes/SceneEditorPane.kt` (Actor list, inspector). Long type names are cut off in the sidebar, the AI conversation is
cramped at 360 dp, and on a small screen the panels can't be narrowed to give the editor room.

## Fix

1. `PanelWidths` becomes the defaults and gains an `enum class Panel(val default: Dp, val id: String) { SIDEBAR(260.dp, "sidebar"), AI(360.dp, "ai"), VERSION_CONTROL(300.dp, "versionControl"), SCENE_ACTORS(220.dp, "sceneActors"), SCENE_INSPECTOR(260.dp, "sceneInspector") }` (keep the `val`s if nothing else is simpler; the `id` is the settings key and must never change).
2. `ui/components/PanelWidthLimits.kt`: `internal fun clampedPanelWidth(width: Dp, availableWidth: Dp, panelsInRow: Int): Dp` = `width.coerceIn(MINIMUM, maxOf(MINIMUM, minOf(MAXIMUM, availableWidth * 0.5f / panelsInRow)))` with `MINIMUM = 160.dp`, `MAXIMUM = 640.dp`, where `panelsInRow` is how many side panels the row shows now (the workspace row: the sidebar plus the AI or version control panel when one is open; `SceneEditorPane`'s row: its Actor list and inspector). A per-panel 45 % alone would let the sidebar and the AI panel take 90 % together, which is what the manual check below forbids. Applied when drawing (so a remembered width never squeezes the editor on a smaller window) and when saving.
3. `ui/components/PanelSplitter.kt`:
   ```kotlin
   /**
    * The edge between a side panel and what is beside it: a divider that can be dragged to resize the panel (on the [side]
    * of the divider it is on) and double-clicked to give it its default width again.
    */
   @Composable
   internal fun PanelSplitter(onDrag: (deltaInDp: Float) -> Unit, onDragStopped: () -> Unit, onReset: () -> Unit, side: PanelSide)
   ```
   An 8 dp wide `Box` drawing a 1 dp `VerticalDivider` in its middle (replacing the `VerticalDivider()` beside each panel), with `pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))`, `draggable(rememberDraggableState { px -> onDrag(px / density) }, Orientation.Horizontal, onDragStopped = { onDragStopped() })`, `pointerInput { detectTapGestures(onDoubleTap = { onReset() }) }`, and semantics (`contentDescription = stringResource(Res.string.panel_resize)`). `PanelSide.START` panels (sidebar, Actor list) grow with a positive delta, `END` panels (AI, version control, inspector) with a negative one.
4. State: `CreatorSettings` gets `@SerialName("panelWidths") val panelWidths: Map<String, Float> = emptyMap()` (dp values by `Panel.id`). This is Creator's own settings file, not a project file: older Creators ignore the key (`ignoreUnknownKeys = true` in `CreatorSettingsStore`), so the project format policy (lane G's G01 `formatVersion`) doesn't apply. `CreatorSettingsController.setPanelWidth(panel: Panel, width: Dp)` (`update { it.copy(panelWidths = it.panelWidths + (panel.id to width.value)) }`) and `resetPanelWidth(panel)`. A `@Composable rememberPanelWidth(panel, settings)`-style holder keeps the width being dragged in a local `mutableStateOf` (no save per pixel) and calls `setPanelWidth` only on `onDragStopped` and `onReset`, so the settings file is written once per drag.
5. Wiring: the widths reach `WorkspaceScreen` and `SceneEditorPane` as one `PanelLayout` value (`settings.panelWidths` + the controller's two setters), passed from `AppContent` (which already has `creatorSettings`) through `ScreenContent` to `WorkspaceScreen`, then to `SceneEditorPane`. Each `Modifier.sidePanel(PanelWidths.X)` becomes `Modifier.sidePanel(clampedPanelWidth(layout.widthOf(X), maxWidth, panelsInRow))` inside a `BoxWithConstraints` around the workspace row (one for `WorkspaceScreen`'s row, one for `SceneEditorPane`'s).
6. String: `<string name="panel_resize">Drag to resize, double-click for the default width</string>`.

**K11:** lane K's K11 moves JSON stores onto a shared `JsonFileStore` with `.bak`. If it landed, `CreatorSettingsStore` is already on it; only `CreatorSettings` gains the field.

`creator/CLAUDE.md`: in the Creator settings section, add that the side panels' widths are dragged at their edges and kept per panel in `settings.json` (`panelWidths`), clamped to the window; in the components list, add `PanelSplitter`.

## Tests

- `PanelWidthLimitsTest`: below minimum → 160 dp; above maximum → 640 dp; 1,000 dp available, one panel, 600 dp asked → 500 dp; two panels → 250 dp each; an available width so small that the share is under the minimum → the minimum.
- `CreatorSettingsStoreTest.panelWidthsAreKeptAndAnOlderFileWithoutThemLoads`: save with `panelWidths = mapOf("sidebar" to 320f)` → load equals; a JSON literal without the key (copy an existing test's literal) loads `emptyMap()`.
- `CreatorSettingsControllerTest.settingAndResettingAPanelWidth`: `setPanelWidth(SIDEBAR, 300.dp)` → `settings.value.panelWidths == mapOf("sidebar" to 300f)`; `resetPanelWidth(SIDEBAR)` → empty.
- `PanelSplitterTest` (offscreen, `uiTest(width = 800, height = 400)`): a `Row` of a panel using the holder and a `PanelSplitter`; `onNodeWithContentDescription(<panel_resize text>).performTouchInput { swipeRight(…, durationMillis = 200) }` (or `performMouseInput { press(); moveBy(Offset(80f, 0f)); release() }`) → the panel's width grew (assert via `onNodeWithTag("panel").getBoundsInRoot().width`), `setPanelWidth` was called exactly once; double-click → back to the default and `resetPanelWidth` called; `saveSnapshot("panel-splitter-dragged")`. Look at the PNG.

## Manual check

`:creator:run`: drag the sidebar, AI, and Scene inspector edges; quit and start again: the widths are kept; make the window small: no panel squeezes the editor below about half the window; the resize cursor shows on macOS and Windows.
