# Correct tools/debug-menu-api/CLAUDE.md: remove the stray license header and list the real overloads

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/debug-menu-api/CLAUDE.md

## Problem
At 2480325f:
- A second MPL-2.0 `<!-- ... -->` header is pasted mid-file (lines 18-26), between "Key Files" and "Public API".
- The "Public API" block lists four entry points as `(kubriko: Kubriko, isEnabled: Boolean, content: @Composable () -> Unit)`.
  The real simple overloads in `DebugMenuContract.kt` are `invoke(kubriko: Kubriko?, isEnabled: Boolean, kubrikoViewport)`,
  `Horizontal(kubriko: Kubriko?, isEnabled: Boolean, windowInsets: WindowInsets)`, `Vertical(kubriko, isEnabled, windowInsets)` and
  `OverlayOnly(kubriko: Kubriko?, kubrikoViewport, buttonAlignment: Alignment?)`; the detailed ones (with `modifier`,
  `debugMenuTheme`, sizes, …) are the ones with `= Unit` bodies that both implementations override.
- "Module Dependency Rule" says the build-logic convention plugin swaps the real and noop modules; in fact each consuming module's
  own `build.gradle.kts` picks `projects.tools.debugMenu` or `projects.tools.debugMenuNoop` from `showcase.isDebugMenuEnabled`
  (e.g. `examples/demo-physics/build.gradle.kts`, `app/shared/build.gradle.kts`), and both of those expose this module with `api(...)`.

## Fix
Delete lines 18-26. Rewrite the API block with the four simple overloads (real parameter names, nullable `Kubriko?`) and one line per
detailed overload's extra parameters (`modifier`, `windowInsets`, `buttonAlignment`, `debugMenuTheme`, `verticalDebugMenuWidth` /
`horizontalDebugMenuHeight`, `height`, `width`), keeping the sentence that the detailed overloads have `= Unit` bodies and both
implementations override them. Note the simple overloads' sizes (`invoke`: 192 dp vertical / 160 dp horizontal; `Horizontal`:
180 dp; `Vertical`: 192 dp). Rewrite the dependency rule as above. Re-read `DebugMenuContract.kt` while editing; quote nothing that
is not there.

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
