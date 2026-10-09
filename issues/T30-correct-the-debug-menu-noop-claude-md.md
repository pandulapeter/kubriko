# Correct which overloads the debug-menu noop inherits in tools/debug-menu-noop/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/debug-menu-noop/CLAUDE.md

## Problem
Line 23 at 2480325f: "The simple overloads with `= Unit` bodies in `DebugMenuContract` are inherited without override". In
`DebugMenuContract.kt` the *simple* overloads are the ones that delegate to the detailed ones; the *detailed* overloads have the
`= Unit` bodies, and the noop (`tools/debug-menu-noop/.../DebugMenu.kt`) overrides all four detailed ones.

## Fix
Replace the line with: "The simple overloads are inherited from `DebugMenuContract`, where they delegate to the detailed ones; the
noop overrides the four detailed overloads." The rest of the file (permanently-false `isVisible`, `invoke`/`OverlayOnly` render
`kubrikoViewport()` in a `Box(modifier)`, `Horizontal`/`Vertical` render nothing) matches the code.

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
None (docs).

## Manual check
None.
