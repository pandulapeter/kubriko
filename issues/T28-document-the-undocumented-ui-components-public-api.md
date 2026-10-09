# Add KDoc to KubrikoColors, ShareManager and the resource preloaders in ui-components

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-ui-components (published; KDoc only)
**Files:** tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/theme/KubrikoColors.kt, tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ShareManager.kt, tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ResourceLoaders.kt

## Problem
Public declarations of a published module without KDoc (the code style requires 100%):
- `object KubrikoColors` and its `brandPrimary`, `onBrandPrimary`, `brandSecondary`;
- `interface ShareManager` with `isSharingSupported` and `shareText(text)`, and `@Composable expect fun rememberShareManager()`;
- `preloadedString`, `expect fun preloadedFont`, `expect fun preloadedImageBitmap`, `expect fun preloadedImageVector`.

## Fix
Add KDoc on the common declarations (the `expect` ones; `actual`s inherit). Use what `tools/ui-components/CLAUDE.md` already
states and the platform `actual`s confirm — e.g. `preloadedImageVector`: "State that holds null until the resource has loaded";
`preloadedString`: "empty until loaded"; `ShareManager.shareText`: OS share sheet on Android/iOS, clipboard on Desktop, Web Share
API on the web — check each `actual` before writing a platform claim; `isSharingSupported`: whether `shareText` does anything on
this platform/device. Colours: the role of each (`brandSecondary` is used as `secondary` in the dark scheme only — verify in
`KubrikoTheme.kt`).

## Behaviour
Docs only.

## Public API
None (KDoc only).

## Tests
None.

## Verify
`./gradlew :tools:ui-components:compileKotlinDesktop`

## Manual check
None.
