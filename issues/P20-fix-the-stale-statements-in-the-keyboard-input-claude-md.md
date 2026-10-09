# Fix the stale file name, call syntax and fix history in the keyboard-input CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-keyboard-input (docs only)
**Challenged:** amended — the same two `KeyExtensions` lines also misstate the result types (`SceneOffset`/`Float`; they are `KeyboardDirectionState`/`KeyboardZoomState`), fixed in the same edit.
**Files:**
- `plugins/keyboard-input/CLAUDE.md`

## Problem
At 2480325f:
- Key Files (:18): "`src/*/kotlin/.../KeyboardInputSource.kt` — platform-specific key-event source (one per
  target)". No such file exists; the platform sources are `implementation/KeyboardEventHandler.kt` (the `expect`
  interface/factory) and `implementation/KeyboardEventHandler.{android,desktop,ios,web}.kt`.
- The `KeyExtensions` block (:59–60) shows `keys.directionState()` and `keys.zoomState()`, but both are extension
  **properties** (`val Set<Key>.directionState`, KeyExtensions.kt:26; `val Set<Key>.zoomState`, :52).
- :34–35: "Discrete `onKeyPressed`/`onKeyReleased` already fire off-tick and were never affected; this only fixes the
  polling path." — fix history.

## Fix
- :18 → "`src/commonMain/.../implementation/KeyboardEventHandler.kt` — the platform key-event source interface and its
  `expect` factory; `src/*Main/.../implementation/KeyboardEventHandler.*.kt` — one actual per target".
- :59–60 → `keys.directionState    // KeyboardDirectionState from WASD/arrow keys` and
  `keys.zoomState         // KeyboardZoomState from +/- keys` (no parentheses; the trailing comments also said
  `SceneOffset` and `Float`, but both properties return the enums named here, KeyExtensions.kt:26 / :52).
- :34–35 → "Discrete `onKeyPressed`/`onKeyReleased` fire off-tick and need no latch; it only serves the polling path."

## Behaviour
Unchanged — Markdown only.

## Public API
None.

## Tests
none

## Verify
none (Markdown) — re-read the edited lines against `KeyExtensions.kt` and the `implementation/` folder.

## Manual check
none
