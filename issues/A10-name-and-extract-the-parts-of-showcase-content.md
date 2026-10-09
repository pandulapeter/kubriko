# Give `ShowcaseContent.kt`'s private Composables names that say what they are, and extract the side menu, the entry content and the menu scroll sync

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/CLAUDE.md` (only if it names the old private Composables — it does not at 2480325f)

## Problem
At 2480325f, `ShowcaseContent.kt`:
- `private fun ExpandedContent(...)` (:168-311) is called unconditionally from `ShowcaseContent` (:117) in **both** the
  compact and the expanded layout; it is the content area plus the side menu, not "expanded" content.
- `private fun CompactContent(...)` (:313-364) is the home view (what shows while no entry is selected), in both layouts
  (`Crossfade(targetState = !shouldUseCompactUi)` picks the expanded or the compact welcome inside it).
- `ExpandedContent` is 140 lines: two scroll-sync `LaunchedEffect`s (:194-227), a local Composable declared inside a
  `Row` and closing over seven outer values (:236-260):

  ```kotlin
  @Composable
  fun Content() {
      val compactTransitionSpec: AnimatedContentTransitionScope<ShowcaseEntry?>.() -> ContentTransform = …
  ```

  and the side menu's `Surface { LazyColumn { … } }` (:276-309) nested three layout levels deep.
- The scroll sync reads the global `ShowcaseEntry.entries.last()` (:214) although the function receives
  `allShowcaseEntries`, which is always `ShowcaseEntry.entries` (its only caller passes exactly that,
  `KubrikoShowcase.kt:104`).

`code-style`: name a Composable for what it is in the UI; extract groups with a standalone name that nest more than ~2
levels; each extracted Composable takes only what it uses.

## Fix
All in `ShowcaseContent.kt`, all `private`:
1. Rename `ExpandedContent` → `ContentWithSideMenu` and `CompactContent` → `HomeContent` (and their call sites).
2. Turn the local `Content()` into a top-level `private fun ShowcaseEntryContent(...)` taking explicitly what the
   body reads: `shouldUseCompactUi`, `selectedShowcaseEntry`, `windowInsets`, `isInFullscreenMode`,
   `onFullscreenModeToggled`, `getSelectedShowcaseEntry`, `homeLazyListState` (today's `collapsedLazyListState`),
   `allShowcaseEntries`, `onShowcaseEntrySelected`. Body verbatim. Both call sites (`kubrikoViewport = { ShowcaseEntryContent(…) }`
   inside `DebugMenu.OverlayOnly`, and the plain call in the `else`) pass the same values.
3. Extract `private fun SideMenu(width: Dp, lazyListState: LazyListState, allShowcaseEntries, selectedShowcaseEntry, onShowcaseEntrySelected)`
   whose root is today's `Surface(modifier = Modifier.padding(end = 8.dp).width(width).fillMaxHeight(), tonalElevation = …, shadowElevation = …) { LazyColumn { … } }`,
   verbatim. The `AnimatedVisibility(visible = shouldShowSideMenu, enter = …, exit = …)` stays at the call site in
   `ContentWithSideMenu`, wrapping `SideMenu(width = sideMenuWidth, lazyListState = expandedLazyListState, …)` — no layout
   wrapper is added or dropped.
4. Extract `private fun MenuScrollPositionEffect(shouldUseCompactUi, compactLazyListState, expandedLazyListState, allShowcaseEntries, selectedShowcaseEntry)`
   holding, verbatim and in the same order, `val previouslyFocusedShowcaseEntry = remember { mutableStateOf(selectedShowcaseEntry) }`,
   `val coroutineScope = rememberCoroutineScope()` and the two `LaunchedEffect`s (keys `shouldUseCompactUi` and
   `selectedShowcaseEntry`, unchanged). `coroutineScope` and `previouslyFocusedShowcaseEntry` are used by nothing else in
   `ExpandedContent`, so both move. Call it unconditionally where the block was (before the `Row`). Inside it,
   `ShowcaseEntry.entries.last()` becomes `allShowcaseEntries.last()`.

`ContentWithSideMenu` then reads: `shouldShowSideMenu`, `sideMenuWidth`, `MenuScrollPositionEffect(…)`, the `Row` (spacer
`AnimatedVisibility` + the debug-menu-or-plain `ShowcaseEntryContent`), the side menu `AnimatedVisibility`.

The `hasDebugMenu` / elevation duplication is not touched here (A11, A53).

## Behaviour
Renames and extractions of `private` Composables; every extracted body is verbatim and called from the same place,
unconditionally where the original code ran unconditionally, so the remembered state, effect keys and the coroutine
scope's lifetime (tied to the call that is always present) are the same. `allShowcaseEntries.last()` is
`ShowcaseEntry.entries.last()` (`ABOUT`) because the only caller passes `ShowcaseEntry.entries`. The
transition-spec lambdas are created per recomposition exactly as before. Rendered UI unchanged.

## Public API
None.

## Tests
The existing ones (Composable structure; no pure logic is introduced).

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest`

## Manual check
`./gradlew :app:desktop:run`: in a wide window select entries far down the side menu and back to Welcome (the menu
scrolls the selection into view as before); narrow the window below 640dp and back (the compact list scrolls to the
selection); toggle fullscreen in a game (the side menu slides out and back).
