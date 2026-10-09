# Remove the gotcha about `unloadAll()` / `unload()` from the persistence CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-persistence (docs only)
**Files:**
- `plugins/persistence/CLAUDE.md`

## Problem
At 2480325f the Gotchas (:70) say "`unloadAll()` / `unload()` are fire-and-forget coroutines; resources are not freed
synchronously". `PersistenceManager` has no such functions (its API is `boolean`, `int`, `float`, `string`, `generic`
and `newInstance`; `grep -rn unload plugins/persistence/src` finds nothing) — the line was copied from an audio/sprite
manager's notes.

## Fix
Delete that bullet. Nothing else.

## Behaviour
Unchanged — Markdown only.

## Public API
None.

## Tests
none

## Verify
none (Markdown).

## Manual check
none
