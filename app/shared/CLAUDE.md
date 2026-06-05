<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/shared — Kubriko Showcase shared UI module

Shared Compose Multiplatform UI consumed by all four platform entry points (`android`, `desktop`, `ios`, `web`). This is **not** a best-practices game development example — it intentionally runs many Kubriko instances in parallel.

## Public surface

`KubrikoShowcase(isInFullscreenMode, getIsInFullscreenMode, onFullscreenModeToggled, deeplink, onDestinationChanged)` — the single entry-point Composable called by every platform target. Platform modules own the fullscreen mechanism; shared code only receives/requests it.

## Navigation model

There is no navigation library. Navigation state is a single `mutableStateOf<ShowcaseEntry?>` (file-level private in `KubrikoShowcase.kt`). `null` means the welcome/menu screen; a non-null value means that entry is active.

`ShowcaseEntry` is a sealed enum (Games / Demos / Tests / Other). Each entry carries:
- `type: ShowcaseEntryType` — controls menu grouping and which features (debug menu, info panel, fullscreen) are shown.
- `areResourcesLoaded: @Composable () -> Boolean` — queried per entry; the whole app blocks on a loading screen until all return true.
- `isProductionReady` — gates entries behind `showcase.shouldShowUnfinishedGames` build flag.
- Test entries additionally require `showcase.areTestExamplesEnabled`; if disabled their `-noop` implementations are linked instead.

Deeplinks are simple strings (`"wallbreaker"`, `"physics"`, etc.) processed by `String?.processDeeplink()`. The web target uses these for browser history; other platforms expose the current deeplink via `onDestinationChanged` for optional deep-link support.

## StateHolder lifecycle and multi-instance management

Each example exposes a `StateHolder` (defined in `examples/shared`):
- `kubriko: Flow<Kubriko?>` — the active Kubriko instance(s) exposed for the debug menu.
- `backNavigationIntent: Flow<Unit>` — emitted when the example requests navigation back.
- `navigateBack(isInFullscreenMode, onFullscreenModeToggled): Boolean` — back-press hook; returns true if consumed.
- `stopMusic()` — called before crossfade exit so music stops at the right moment.
- `dispose()` — frees all Kubriko resources.

`stateHolders` in `ExampleScreen.kt` is a **file-level** `mutableStateOf<List<StateHolder>>` — all live `StateHolder` instances accumulate here across the app session. `getOrCreateState<T>` retrieves an existing instance by type or creates one. On Compose `DisposableEffect` cleanup, if the departing entry is no longer selected, its `StateHolder` is disposed and removed from the list. This is how switching entries disposes the previous example's Kubriko instance while the crossfade is still running (music is stopped earlier via `stopMusic()`).

`StateHolder.isInfoPanelVisible` (companion object) is global shared state toggled by the top bar info button.

## Responsive layout

`ShowcaseContent` adapts based on `maxWidth`:
- `< 640dp` (compact): full-screen navigation — menu list → selected entry. Back navigation is visible in the top bar.
- `>= 640dp` (expanded): side-by-side layout with a side menu panel and content area.
- `>= 1200dp` (wide): wider side menu (320dp vs 192dp).

The debug menu is placed differently per breakpoint: `DebugMenu.Vertical` on the right for expanded, `DebugMenu.Horizontal` at the bottom for compact.

## BuildConfig flags (from `gradle.properties`)

Injected at build time via the `buildkonfig` plugin:
- `ARE_TEST_EXAMPLES_ENABLED` — whether test entries appear in the menu.
- `IS_DEBUG_MENU_ENABLED` — whether `DebugMenu` wrappers are compiled in.
- `IS_SCENE_EDITOR_ENABLED` — passed to example `StateHolder` factories.
- `SHOULD_SHOW_UNFINISHED_GAMES` — gates `isProductionReady = false` entries.
- `WEB_ROOT_PATH_NAME` — used in deeplink path construction for the web target.

## Resource loading gate

`KubrikoTheme` receives `areResourcesLoaded` — a boolean computed by `ResourceLoader.areResourcesLoaded()` AND all `ShowcaseEntry.areResourcesLoaded()` values. Until all resources are ready the UI shows a loading state. Resources are preloaded via `preloadedImageVector`, `preloadedImageBitmap`, and `preloadedString` utilities from `tools/ui-components`.

## Key files

- `KubrikoShowcase.kt` — entry Composable, deeplink logic, back-press handling, navigation state.
- `implementation/ShowcaseEntry.kt` — enum of all entries and `ShowcaseEntryType`.
- `implementation/ui/ExampleScreen.kt` — `StateHolder` pool, per-entry `ExampleScreen` Composable, disposal logic.
- `implementation/ui/ShowcaseContent.kt` — responsive layout orchestration.
- `implementation/ui/Menu.kt` — `LazyListScope.menu()` extension, `MenuItem`, `MenuCategoryLabel`.
- `implementation/ui/TopBar.kt` — top app bar with back/info/debug buttons.
- `implementation/ui/ResourceLoader.kt` — resource preload gate for shared UI resources.
