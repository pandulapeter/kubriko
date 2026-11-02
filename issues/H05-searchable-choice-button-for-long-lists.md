<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Add a `SearchableChoiceButton` (lazy list, filter field once there are more than eight options, Enter picks the first match) and use it for every project-content chooser

**Challenged:** amended — K09 note only; checked against 4ab2bdf08 (its filter is a plain `CompactTextField`, not a buffered field, so `SentValues` doesn't apply).

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-11 (decided: build, decisions.md "Searchable menus")  ·  **Depends on:** none. **K09:** nothing to re-locate (UI components only; no file K09a/K09b changes)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/components/SearchableChoiceButton.kt`, new `.../ui/components/ChoiceMatching.kt`, `.../ui/actorTypes/ActorTypeChoice.kt`, `.../ui/actorTypes/SceneChoice.kt`, `.../ui/actorTypes/SpriteChoice.kt`, `.../ui/actorTypes/AudioFileChoice.kt`, `.../ui/actorTypes/PropertyChoice.kt`, `.../ui/actorTypes/ParentChoice.kt`, `.../ui/actorTypes/KeyChoiceButton.kt` (its matcher only), `creator/src/main/composeResources/values/strings.xml`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/components/SearchableChoiceButtonTest.kt` (new), `.../ui/components/ChoiceMatchingTest.kt` (new), `.../ui/actorTypes/KeyChoiceButtonTest.kt`, `creator/CLAUDE.md`

## Problem

`ui/components/MenuButton.kt` renders every option of a `DropdownMenu` as a plain, non-lazy column with no filter:
```kotlin
DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
    options.forEach { option ->
        DropdownMenuItem(text = { Text(optionText(option)) }, onClick = { isExpanded = false; onSelected(option) })
    }
}
```
It is the chooser for the project's own contents, which grow without bound: Actor types (`ActorTypeChoice`), Scenes (`SceneChoice`),
sprites (`SpriteChoice`), sound and music files (`AudioFileChoice`), Properties (`PropertyChoice`), and parents (`ParentChoice`).
A project with 80 sprites gets an 80-item menu with no way to type. `ui/actorTypes/KeyChoiceButton.kt` already has the right
pattern (a `Popup` with a `CompactTextField` and a `LazyColumn`, ranked by `matchingKeys`), private to keys.

## Fix

1. `ui/components/ChoiceMatching.kt`: lift `matchingKeys`' ranking into a generic pure function:
   ```kotlin
   /**
    * The [options] whose [titleOf] (or one of [alsoMatching]'s names) contains [query], with or without its spaces: exact
    * matches first, then those starting with it, otherwise in their order.
    */
   internal fun <T> matchingOptions(query: String, options: List<T>, titleOf: (T) -> String, alsoMatching: (T) -> String? = { null }): List<T>
   ```
   Same rules as today (`trim`, `compact = trimmed.replace(" ", "")`, `ignoreCase`, stable `sortedBy` of rank 0/1/2). `matchingKeys(query, titleOf)` in `KeyChoiceButton.kt` becomes `matchingOptions(query, KeyChoice.all, titleOf, alsoMatching = { it.composeKeyName })`; `KeyChoiceButtonTest` stays green unchanged.
2. `ui/components/SearchableChoiceButton.kt`:
   ```kotlin
   /**
    * An outlined button showing the chosen option, which opens [options] in a panel: a lazy list, with a field filtering it
    * ([matchingOptions]) once there are more than [SEARCH_THRESHOLD], where Enter picks the first match and Escape closes.
    */
   @Composable
   internal fun <T> SearchableChoiceButton(
       label: String,
       options: List<T>,
       optionText: @Composable (T) -> String,
       onSelected: (T) -> Unit,
   )
   ```
   - Face: `MenuButtonFace(label, onClick = { isExpanded = true })`, as `MenuButton`.
   - Panel: `Popup(onDismissRequest, properties = PopupProperties(focusable = true))` → `Surface(width 260.dp, shapes.small, surfaceContainer, shadowElevation 4.dp)`, as `KeyChoicePanel`. Titles are read once per composition of the open panel: `val titles = options.map { optionText(it) }` (composable lambda, called only while open), then `remember(query, titles) { matchingOptions(query, options.indices.toList(), titleOf = { titles[it] }) }`.
   - Field (only `options.size > SEARCH_THRESHOLD`, `const val SEARCH_THRESHOLD = 8`): `CompactTextField` with placeholder `choice_search`, focused on open (`FocusRequester` + `LaunchedEffect(Unit)`), `onPreviewKeyEvent`: Enter (`Key.Enter`, `Key.NumPadEnter`, `KeyDown`) picks the first match if any; Escape closes.
   - List: `LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp))`, `items(matches, key = { it })` (option index: options have no identity of their own, and the list is rebuilt each time it opens), each row a `Text` with `clickable` and `padding(horizontal = 12.dp, vertical = 6.dp)`, the chosen one (`titles[index] == label`) in `colorScheme.primary`.
   - No matches: `choice_none_found` in `bodySmall`, `onSurfaceVariant`.
   - Popup positioning: the default `Popup` aligns to the button's top-left like `KeyChoiceButton`; keep it identical so they look the same.
3. Use it in the six choosers, replacing `MenuButton(...)` one for one (same `label`, `options`, `optionText`, `onSelected`): `ActorTypeChoice`, `SceneChoice`, `SpriteChoice`, `AudioFileChoice`, `PropertyChoice`, `ParentChoice`. Leave `MenuButton` for fixed short lists (`TimerChoice`, `ConditionParameters`, `BlockRow`, `BlockTypeHeader`, `ScreenTextList`, `SceneSettingsInspector`, the assistant menus) and keep it.
4. Strings (`values/strings.xml`, next to `key_search`): `<string name="choice_search">Search</string>`, `<string name="choice_none_found">Nothing matches.</string>`. `StringResourcesTest` then sees both used.

`creator/CLAUDE.md`: where `ui/components` is listed (the "Desktop-sized UI" bullet), add `SearchableChoiceButton` (the chooser for the project's contents: types, Scenes, sprites, audio files, Properties; filtered once it has more than eight options); where `KeyChoiceButton` is described, note it shares `matchingOptions`.

## Tests

- `ChoiceMatchingTest`: empty query → all, in order; exact before prefix before contains (`["Ballistic", "Ball", "Football"]` with `"ball"` → `["Ball", "Ballistic", "Football"]`); spaces ignored (`"big ball"` matches `"BigBall"`); `alsoMatching` matches a second name; nothing matching → empty.
- `SearchableChoiceButtonTest` (offscreen, `uiTest(width = 400, height = 500)`, `showing`, `saveSnapshot`):
  - `aShortListOpensWithoutAFilter`: five options; click the face → all five rows shown, no node `hasSetTextAction()`; `saveSnapshot("searchable-choice-short")` (the popup is saved as `-popup.png`).
  - `typingNarrowsALongListAndEnterPicksTheFirstMatch`: 20 options `"Enemy01"…"Enemy19"` + `"Player"`; open; `performTextInput("play")` → only `Player` shown; `saveSnapshot("searchable-choice-filtered")`; `performKeyInput { pressKey(Key.Enter) }` → `onSelected` got `"Player"`, and the popup is gone (`onAllNodes(isRoot()).fetchSemanticsNodes().size == 1`).
  - `clickingAnOptionPicksIt` and `nothingMatchingSaysSo` (`choice_none_found` text shown).
  Look at the three PNGs before calling it done.
