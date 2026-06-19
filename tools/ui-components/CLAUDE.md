<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# ui-components

Shared Compose Multiplatform UI primitives for Kubriko tools and the Showcase app. All platforms (Android, Desktop, iOS, Web).

## Theme

### `KubrikoTheme(areResourcesLoaded, content)`
Wrap any tool or app screen with this. It:
- Loads the "Public Sans Regular" custom font asynchronously; shows a branded loading spinner (`KubrikoColors.brandPrimary` background) until the font is ready or `areResourcesLoaded` is false.
- Applies a Material3 color scheme using `KubrikoColors` for `primary`, `primaryContainer`, and `secondary`.
- Handles dark/light mode automatically via `isSystemInDarkTheme()`.

### `KubrikoColors`
- `brandPrimary` = `#6060AA` (blue-purple)
- `onBrandPrimary` = `#FDFDFD` (near-white)
- `brandSecondary` = `#9090CC` (lighter purple, used as `secondary` in dark scheme only)

Typography uses `Public Sans Regular` across all Material3 text styles.

## Components

| Component | Description | Key parameters |
|---|---|---|
| `FloatingButton` | 40 dp circular FAB, icon-only | `icon: DrawableResource`, `isSelected: Boolean` (affects color) |
| `SmallButton` | 40 dp square FAB, icon-only | `icon`, `contentDescription: StringResource?`, `contentColor: Color?` |
| `LargeButton` | Full-width FAB-style with text and optional icon | `title: StringResource`, `icon: DrawableResource?`, `isEnabled: Boolean` |
| `Panel` | `Card` container with `MaterialTheme.colorScheme.surface` background | `content: ColumnScope.() -> Unit` |
| `InfoPanel` | `Panel` with animated fade+expand for help text | `text: String` or `StringResource`, `isVisible: Boolean` |
| `SmallSlider` | Compact 24 dp tall slider | `value`, `onValueChanged`, `valueRange` |
| `SmallSliderWithTitle` | `SmallSlider` with a label in a Row | `title: String` prepended left |
| `SmallSwitch` | Compact labeled toggle switch | `title: String`, `isChecked`, `onCheckedChanged` |
| `TextInput` | `BasicTextField` styled with theme colors | `value`, `onValueChanged`, `enabled`, `onFocusChanged` (balanced — also fires `false` on dispose if focused) |
| `LoadingIndicator` | 24 dp circular progress, 3 dp stroke | — |
| `LoadingOverlay` | Fullscreen overlay; shows `LoadingIndicator` bottom-start while loading | `shouldShowLoadingIndicator`, `content` slot |

## Utilities

- `preloadedFont(FontResource)` — `@Composable` returning `State<Font?>`; null while loading. Used internally by `KubrikoTheme` to gate rendering.
- `preloadedImageBitmap(DrawableResource)` — `State<ImageBitmap?>`, null while loading.
- `preloadedImageVector(DrawableResource)` — `State<ImageVector?>`, null while loading. Used by `FloatingButton` to safely render icons.
- `preloadedString(StringResource)` — `State<String>`, empty string while loading.
- `ShareManager` — `interface` with `isSharingSupported: Boolean` and `shareText(text: String)`. Obtain via `rememberShareManager()` composable. Platform implementations share text to the OS share sheet (Android/iOS), clipboard (Desktop), or Web API.

## Usage in existing tools

- **debug-menu**: uses `SmallSwitch` (via `LogsHeader`), `KubrikoTheme` implicitly via the Showcase app shell.
- **scene-editor**: uses its own internal `Editor*` components that do NOT use `ui-components`; the editor has its own Material3 theming.
- **Showcase app**: uses `KubrikoTheme`, `LargeButton`, `SmallButton`, `FloatingButton`, `Panel`, `InfoPanel`, `LoadingOverlay`, `SmallSwitch`, `SmallSliderWithTitle`, `ShareManager`.

## Adding a new tool

1. Depend on `projects.tools.uiComponents` in your module's `build.gradle.kts`.
2. Wrap your root Composable in `KubrikoTheme { }`.
3. Use `Panel` as a container, `SmallSwitch` / `SmallSlider` for controls, `FloatingButton` / `SmallButton` for actions.
4. Use `preloadedImageVector` for icons to avoid crashes during async resource loading.
