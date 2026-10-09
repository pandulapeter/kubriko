# Correct `app/shared/CLAUDE.md`'s entry-point signature, the `ShowcaseEntry` description and where the breakpoints are computed

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/CLAUDE.md`

Lands after A01-A11, which each update their own Key files lines; this plan fixes what was wrong before them.

## Problem
At 2480325f `app/shared/CLAUDE.md` says:

1. Public surface: "`KubrikoShowcase(isInFullscreenMode, getIsInFullscreenMode, onFullscreenModeToggled, deeplink, onDestinationChanged)`"
   — the function (`KubrikoShowcase.kt:44-52`) also takes `onFirstFrameDrawn: () -> Unit = {}` (the web shell hides
   its loading screen with it) and `onBackgroundColorChanged: (Color) -> Unit = {}` (the desktop shell colors the area a
   fast resize uncovers).
2. Navigation model: "`ShowcaseEntry` is a sealed enum (Games / Demos / Tests / Other)." — it is a plain
   `internal enum class` (`ShowcaseEntry.kt:67`); Games / Demos / Tests / Other are its `ShowcaseEntryType`s.
3. Responsive layout: "`ShowcaseContent` adapts based on `maxWidth`: `< 640dp` … `>= 1200dp` …" — the breakpoints are
   computed in `KubrikoShowcase`'s `BoxWithConstraints` (`KubrikoShowcase.kt:91-103`:
   `shouldUseCompactUi = maxWidth < 640.dp`, `shouldUseWideSideMenu = maxWidth >= 1200.dp`) and passed to
   `ShowcaseContent` as two booleans.

## Fix
1. Public surface → "`KubrikoShowcase(isInFullscreenMode, getIsInFullscreenMode, onFullscreenModeToggled, deeplink, onDestinationChanged, onFirstFrameDrawn, onBackgroundColorChanged)`"
   and add one sentence: "`onFirstFrameDrawn` fires once the first frame has been presented (the web shell hides its
   loading screen there); `onBackgroundColorChanged` reports the theme's surface color (the desktop shell paints the area
   a fast resize uncovers with it)."
2. Navigation model → "`ShowcaseEntry` is an internal enum; each entry's `ShowcaseEntryType` (Games / Demos / Tests /
   Other) groups it in the menu."
3. Responsive layout → "`KubrikoShowcase` measures the window in a `BoxWithConstraints` and passes `ShowcaseContent` two
   flags:" followed by the existing three bullets unchanged.

Re-read the whole file against the code after the lane's earlier plans and fix any other statement they left stale
(file names in Key files: `MenuItem.kt`, `ShowcaseEntryType.kt`, `ShowcaseStateHolders.kt`, `ShowcaseDeeplink.kt`,
`ShowcaseEntryFeatures.kt` must all be listed).

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
None needed (docs only).

## Manual check
None.
