# Derive the menu's scroll-to index from the rows the menu actually emits

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/Menu.kt`,
`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`,
`app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/MenuItemIndexTest.kt` (new)

## Problem

`ShowcaseContent.kt` (~line 372) hard-codes where each entry's row sits in the menu `LazyColumn`:

```kotlin
private val ShowcaseEntry?.menuItemIndex
    get() = when (this?.type) {
        null -> 0
        ShowcaseEntryType.GAME -> ShowcaseEntry.entries.indexOf(this) + 2
        ShowcaseEntryType.DEMO -> ShowcaseEntry.entries.indexOf(this) + 3
        ShowcaseEntryType.TEST -> ShowcaseEntry.entries.indexOf(this) + 4
        ShowcaseEntryType.OTHER -> ShowcaseEntry.entries.indexOf(this) + if (BuildConfig.ARE_TEST_EXAMPLES_ENABLED) 4 else 5
    }
```

Both lists (the side menu in `ExpandedContent` and the compact list in `CompactContent`) are one leading item
(Welcome) followed by `menu()`, which emits a category label and then its entries per non-empty group. With all flags on
(`gradle.properties` defaults) the rows are:

| row | 0 | 1 | 2–5 | 6 | 7–12 | 13 | 14–16 | 17 | 18 | 19 |
|---|---|---|---|---|---|---|---|---|---|---|
| item | Welcome | Games label | 4 games | Demos label | 6 demos | Tests label | 3 tests | Other label | Licenses | About |

So `OTHER` needs `+5` with tests enabled (enum indices 13/14 → rows 18/19) and `+1` without them (rows 14/15). The code
has the two values inverted: with tests on it targets one row too high (Licenses → the "Other" label, About →
Licenses); with tests off it targets four rows past the entry, beyond the end of the list. With
`shouldShowUnfinishedGames=false` Blocky's Journey is not emitted, so every row after the games shifts up by one and
the `DEMO`/`TEST`/`OTHER` offsets are all one too high. The index drives `scrollToItem`/`animateScrollToItem` and the
"is it visible" checks in the two `LaunchedEffect`s of `ExpandedContent`, so selecting an entry (by click, deeplink,
or back navigation) can leave it scrolled out of view or scroll to the wrong place.

## Fix

Compute the index from the same grouping `menu()` renders, so the two cannot drift:

1. In `Menu.kt`, extract the grouping `menu()` uses into one private helper and use it in `menu()`:

   ```kotlin
   private fun List<ShowcaseEntry>.groupedForMenu() = filter { it.isAvailable }.groupBy { it.type }
   ```

   (`isAvailable` comes from plan 84. If plan 84 was skipped, move `menu()`'s two existing `.filter { … }` calls into
   this helper instead.)
2. Also in `Menu.kt`, add:

   ```kotlin
   /**
    * The index of [showcaseEntry]'s row in a list that has exactly one item before [menu] (the Welcome row), or 0 for
    * the Welcome row itself and for an entry the menu does not list.
    */
   internal fun List<ShowcaseEntry>.menuItemIndex(showcaseEntry: ShowcaseEntry?): Int {
       if (showcaseEntry == null) return 0
       var index = 1
       groupedForMenu().forEach { (_, entries) ->
           index++ // the category label
           val position = entries.indexOf(showcaseEntry)
           if (position >= 0) return index + position
           index += entries.size
       }
       return 0
   }
   ```

   Drop the `// the category label` comment if the code reads clearly without it. This runs only from the two
   `LaunchedEffect`s on selection/layout changes, not per frame, so the small allocations are fine.
3. In `ShowcaseContent.kt`, delete the `menuItemIndex` extension property and replace its two uses in
   `ExpandedContent` with `allShowcaseEntries.menuItemIndex(selectedShowcaseEntry)` and
   `allShowcaseEntries.menuItemIndex(if (shouldUseCompactUi) selectedShowcaseEntry ?: previouslyFocusedShowcaseEntry.value else selectedShowcaseEntry)`.
   Remove the `BuildConfig`/`ShowcaseEntryType` imports only if nothing else in the file uses them
   (`hasDebugMenu` still uses `ShowcaseEntryType`; `BuildConfig` is still used for the debug menu).

Both lists keep exactly one item before `menu()` — if that ever changes, the KDoc above is where it is stated.

## Tests

After plan 00, `app/shared` has a `commonTest` source set (it applies `kubriko-compose-library`). Add
`app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/MenuItemIndexTest.kt` (MPL
header) for `ShowcaseEntry.entries.menuItemIndex(…)`:

- `null` → 0; every entry with `isAvailable == false` → 0.
- The indices of the available entries, in enum order, are strictly increasing, and the first one is 2.
- When `BuildConfig.ARE_TEST_EXAMPLES_ENABLED && BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES` (the defaults): WALLBREAKER
  → 2, BLOCKYS_JOURNEY → 5, CONTENT_SHADERS → 7, SHADER_ANIMATIONS → 12, AUDIO → 14, INPUT → 16, LICENSES → 18,
  ABOUT → 19. Guard these exact values with that condition so the test holds under any flag combination.

Run `./gradlew :app:shared:desktopTest`. If plan 00 did not land or does not reach `app/shared`, skip the test rather
than adding test infrastructure to the app.

## Manual check

Desktop, `./gradlew :app:desktop:run -Pshowcase.shouldShowUnfinishedGames=false` (the case where the offset is
clearly visible, because `LaunchedEffect(shouldUseCompactUi)` uses `scrollToItem`, which puts the target row at the
top):

1. Make the window about 800 × 420 so the side menu shows only the first handful of rows.
2. Scroll the side menu down, select Content Shaders, then scroll the side menu back to the top.
3. Narrow the window below 640 dp (compact layout), then widen it again.
4. Expected after the fix: the side menu is scrolled so the selected Content Shaders row is the top row. Before: the
   Isometric Graphics row is at the top and the selected row is hidden just above it.
5. With the default flags, select a demo (e.g. Particles) and repeat steps 2–3: its row is the top row both before
   and after the fix (the `DEMO` offset is right when every game is shown), which confirms nothing regressed.
