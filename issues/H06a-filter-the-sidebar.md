<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Add a filter field at the top of the sidebar that narrows types, Scenes, and files to those whose names contain what is typed, opening the folders that hold them

**Challenged:** amended — K09 note; reuses H05's `choice_none_found`, which always lands before it in lane H, instead of a conditional new string.

**Kind:** architecture (feature gap)  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** UI-12 (filter part; the widths part landed as `PanelWidths` and `Modifier.sidePanel`)  ·  **Depends on:** H01 (selection callbacks in `rememberFolderTreeActions`; only a merge dependency), H05 (reuse its `choice_none_found`). **K09:** nothing to re-locate (sidebar UI only)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/ProjectSidebar.kt`, `.../ui/FolderTree.kt`, new `.../ui/SidebarFilter.kt`, `creator/src/main/composeResources/values/strings.xml`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/SidebarFilterTest.kt` (new), `.../ui/FolderTreeTest.kt`, `.../ui/ProjectSidebarTest.kt`, `creator/CLAUDE.md`

## Problem

`ui/ProjectSidebar.kt` lists every Actor type, Manager type, Scene, and file (sprites, sounds, music, Kotlin files) with no way to
narrow them: `ProjectSidebar` is a `LazyColumn` of `folderTreeSection(...)` per `FolderGroup` and one item per resource. A
project with a few hundred types and files (the 200-type benchmark project) means scrolling and opening folders by hand to
find one. The Scene editor's Actor list already filters ("Filter by type or ID", `scene_actor_search`), the sidebar doesn't.

## Fix

1. `ui/SidebarFilter.kt`, pure functions:
   ```kotlin
   /** Whether [name] (a type, Scene, or file name; for a path, its last part counts as well as the whole) matches [query]. */
   internal fun matchesSidebarFilter(name: String, query: String): Boolean   // trimmed, ignoreCase, contains; empty query matches all

   /** [state] narrowed to the items matching [query], with only the folders that hold one (and their parents). */
   internal fun FolderTreeGroupState.filtered(query: String): FolderTreeGroupState
   ```
   `filtered`: `items.filter { (name, _) -> matchesSidebarFilter(name, query) }`, `folders = matchingItems.map { it.second }.filter(String::isNotEmpty).toSet().withParents()` (`actors/FolderPaths.withParents`, as `folderTreeRows` uses), `selectedName` unchanged.
2. `ProjectSidebar`: `var query by remember { mutableStateOf("") }` and, above the `LazyColumn`, a `CompactTextField` (placeholder `sidebar_filter`, `Modifier.fillMaxWidth().padding(8.dp)`, leading search icon if `ic_search` exists; check `drawable/` — add none), with Escape clearing it (`onPreviewKeyEvent`). Wrap the existing `Column` structure: `Column(modifier) { field; LazyColumn(Modifier.weight(1f).pointerInput(...)) { … } }` — keep `detectTapGestures { onSelectionCleared() }` on the list only. While `query.isNotBlank()`:
   - each group's state is `state.filtered(query)` and `collapsed = emptySet()` (folders holding matches show open; the user's collapsed folders come back when the filter is cleared, since `collapsedFolders` isn't touched);
   - groups with no matching item are left out entirely (header too), and the new-item/new-folder fields aren't offered (hide the header's + buttons by not rendering the section);
   - resource groups show only matching resources and are shown expanded, ignoring `collapsedGroups`; empty ones are left out;
   - nothing matching anywhere: one item with `choice_none_found`.
   Dragging and clicking work unchanged on what is shown.
3. Strings: `<string name="sidebar_filter">Filter types, Scenes, and files</string>`. For nothing matching, reuse H05's `choice_none_found` ("Nothing matches."), which lands first in lane H; only if H05 was skipped add `<string name="sidebar_filter_none">Nothing matches.</string>` instead.

`creator/CLAUDE.md`, the sidebar paragraph ("The sidebar shows the three groups as trees …"): add that a field above them filters everything by name, showing the folders that hold matches opened.

## Tests

- `SidebarFilterTest` (pure): `matchesSidebarFilter("enemies/Bat", "bat")` true, `("Ball", " BA ")` true, `("Ball", "x")` false, empty query true; `filtered` keeps `Bat` in `enemies/flying` and folders `{"enemies", "enemies/flying"}` only, drops empty folders and non-matching items, keeps `selectedName`.
- `ProjectSidebarTest` (offscreen, `uiTest(width = 300, height = 600)`): a `ProjectSidebar` with `folderGroups` from `folderTreeGroups(...)` (two Actor types, one in a collapsed folder, three Scenes) and a sprite resource; type `"bat"` into the field → the `Bat` row shows, `Ball` and the Scenes don't; `saveSnapshot("sidebar-filtered")`; clear with Escape → all rows back and the collapsed folder collapsed again; type `"zzz"` → the `choice_none_found` text (or `sidebar_filter_none` if H05 was skipped); `saveSnapshot("sidebar-filter-none")`. Look at both PNGs.
