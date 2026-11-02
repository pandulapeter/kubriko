<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Share one JsonFileStore between Kubriko Creator's settings stores that sets an unreadable file aside as .bak before writing, instead of six copies that replace it.

**Challenged:** amended — keeps 92b8a47ea's `narrowPermissions()` in `McpSettingsStore.load` and `save` (before any read or set-aside, so `mcp.json.bak` holding the old token is owner-only too, and an isPrivate store narrows its `.bak`); a file that can't be *read* (an `IOException`, e.g. Windows' momentary lock) is no longer set aside and replaced by a near-empty list, only one whose content can't be decoded is; the write is refused instead.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ARCH-5, AI-16 (the store part)  ·  **Depends on:** none (independent of the rest of lane K; AI-4's `isPrivate` landed in 9af5671ab and must be kept)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/foundation/JsonFileStore.kt`, `settings/CreatorSettingsStore.kt`, `mcp/McpSettingsStore.kt`, `ai/assistant/SavedConversationStore.kt`, `ai/assistant/AssistantSettingsStore.kt`, `recent/RecentProjectsStore.kt`, tests new `foundation/JsonFileStoreTest.kt`, `settings/*StoreTest.kt`, `mcp/McpSettingsStoreTest.kt`, `ai/assistant/SavedConversationStoreTest.kt`, `ai/assistant/AssistantSettingsStoreTest.kt`, `recent/RecentProjectsStoreTest.kt` (whichever exist; `ls` the test folders), `creator/CLAUDE.md` (one line in the shared building blocks / definition of done list, if F9 created one)

Lane A edits `ai/assistant/` too (A01–A04); the two files there are only touched in their `read`/`save` bodies.

## Problem

Six stores repeat `Json { prettyPrint = true; ignoreUnknownKeys = true }`, a `read()` that maps `IOException`, `SerializationException`, and `IllegalArgumentException` to the default, and a `save()` that swallows `IOException`. Because "unreadable" and "missing" both read as the default, the next write replaces data that was there:

```kotlin
// ai/assistant/SavedConversationStore.kt
    fun save(conversation: SavedConversation) {
        val others = read().filterNot { it.id == conversation.id && it.agentId == conversation.agentId }
        ...
        write(others.filterNot { it.project == conversation.project } + project)
    }

    private fun read(): List<SavedConversation> = try {
        if (file.isFile) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (_: IOException) {
        emptyList()
```

A truncated, locked, or newer-format `conversations.json` makes the next saved conversation wipe every other one; `RecentProjectsStore.add`/`remove` (`read().filter { … }.also(::write)`) wipes the recent list the same way; `McpSettingsStore.load` (`read()?.takeIf { … } ?: McpSettings(token = createToken()).also(::save)`) replaces an unreadable `mcp.json` with a new token, which breaks every configured MCP client without a trace.

## Fix

Option (a) of the verification, with the lane's `.bak` naming: never replace an unreadable file silently; set it aside first.

1. `foundation/JsonFileStore.kt`:
   ```kotlin
   internal class JsonFileStore<T>(private val file: File, private val serializer: KSerializer<T>, private val isPrivate: Boolean = false, private val json: Json = defaultJson)
   ```
   - `fun read(): Read<T>`, `sealed interface Read<out T> { data object Missing; data class Found<T>(val value: T); data class Unreadable(val reason: String, val isContentBroken: Boolean) }`. `SerializationException` and `IllegalArgumentException` → `Unreadable(first message line, isContentBroken = true)`; `IOException` → `Unreadable(…, isContentBroken = false)` (the file may be fine and only locked or unreadable for now).
   - `fun write(value: T): Boolean`: checked at write time, not remembered (so a file fixed by hand meanwhile is just overwritten): if the file exists and its content can't be decoded (`Unreadable` with `isContentBroken`), first move it to `<name>.bak` next to it (`settings.json.bak`), replacing an older `.bak`, and when `isPrivate` call `restrictToOwner()` on the `.bak`; if it exists but can't be read at all (`isContentBroken = false`), don't write and return false (setting aside a readable-later file and replacing it with what the caller built from an empty list would lose the rest of it, e.g. every other saved conversation, after a lock that lasted a moment). Then `parentFile.mkdirs()` and `writeTextAtomically(json.encodeToString(serializer, value), isPrivate)`. Return false on an `IOException` (callers keep the value in memory as today). If setting aside fails, don't write (return false), so nothing is lost.
   - `defaultJson = Json { prettyPrint = true; ignoreUnknownKeys = true }`; `CreatorSettingsStore` passes its own with `encodeDefaults = true`.
   - Blocking; every caller already runs it off the main thread or at startup (unchanged here; lane H's H03 moves startup reads out of composition).
   - KDoc says the `.bak` behaviour.
2. Each store keeps its public API and its documented fallbacks, built on `JsonFileStore`:
   - `CreatorSettingsStore.load()`: `Found` → as now; `Missing`/`Unreadable` → defaults. `save` → `write`.
   - `AssistantSettingsStore`: the same.
   - `McpSettingsStore` (`isPrivate = true`): keep 92b8a47ea's `narrowPermissions()` (an older Creator's world-readable `mcp.json` narrowed to its owner) as the first step of `load()` and of `save()`, before the store reads or sets anything aside, and keep its `IOException` catch. `load()` on `Unreadable` creates the new token as now; its `save` sets the old file aside as `mcp.json.bak` (owner-only, since the file was narrowed first and the store narrows its `.bak`), so the old token can be recovered by hand. Keep the existing `takeIf { token.isNotBlank() && port in range }` rule.
   - `SavedConversationStore.save`: on `Unreadable`, start from an empty list (the `.bak` keeps the old one). `of(...)` on `Unreadable` returns empty. Keep `@Synchronized`.
   - `RecentProjectsStore`: the same, keeping its `lock`.
   - `AgentRegistryStore`: only `cached()` reads JSON through `AgentRegistry.parse(text, platform)` (not a `KSerializer` round trip), and `refresh()` writes the downloaded text verbatim; leave it on `writeTextAtomically` and don't force it into `JsonFileStore`. Its cache is re-downloadable, so no `.bak` is needed. (K11b handles its HTTP part.)
3. Strings: none (nothing is shown; the `.bak` file is the trace). If F9's definition-of-done list names shared building blocks, add `JsonFileStore` there in the same commit.

## Tests

- `JsonFileStoreTest` (real temp folder): missing → `Missing`; valid → `Found`; garbage → `Unreadable` with a reason; `write` over garbage leaves `x.json.bak` with the garbage bytes and `x.json` with the new value; a second write over a now-valid file leaves the `.bak` alone; `isPrivate = true` on a POSIX file system gives a new file owner-only permissions (`Assume.assumeTrue(posix)`); a `.bak` that can't be created (make `x.json.bak` a non-empty directory) → `write` returns false and `x.json` is unchanged.
- `SavedConversationStoreTest.anUnreadableFileIsntLost`: garbage `conversations.json`, `save(c)` → `.bak` holds the garbage, `of(project, agent)` = `[c]`.
- `RecentProjectsStoreTest.anUnreadableListIsntLost`: the same for `add`.
- `McpSettingsStoreTest.anUnreadableFileIsSetAside`: garbage `mcp.json` → `load()` gives a new token, `mcp.json.bak` has the old bytes, the new file is owner-only (POSIX), and so is `mcp.json.bak` when the garbage file was world-readable. 92b8a47ea's permission tests in `McpSettingsStoreTest` stay green.
- `JsonFileStoreTest.aFileThatCantBeReadIsntReplaced`: a `read` that throws `IOException` (an internal `readText` hook) → `write` returns false, no `.bak`, the file's bytes unchanged.
- Existing store tests stay green.
