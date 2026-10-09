# Correct "Usage in existing tools" in tools/ui-components/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/ui-components/CLAUDE.md
**Challenged:** amended — notes that G20–G23 (games lane) make the four games use `LoadingIndicator`, so the examples bullet must list it whichever lane merges first.

## Problem
At 2480325f the section says:
- "**debug-menu**: uses `SmallSwitch` (via `LogsHeader`), `KubrikoTheme` implicitly…" — `LogsHeader` uses `TextInput`; nothing in
  `tools/debug-menu` uses `SmallSwitch` (its switches are Material3 `Switch`es) or `KubrikoTheme`.
- "**scene-editor**: uses its own internal `Editor*` components that do NOT use `ui-components`" — `EditorUserInterface` and
  `Settings` wrap their content in `KubrikoTheme`, and `EditorTextInput` is built on `TextInput`.
- "**Showcase app**: uses `KubrikoTheme`, `LargeButton`, `SmallButton`, `FloatingButton`, `Panel`, `InfoPanel`, `LoadingOverlay`,
  `SmallSwitch`, `SmallSliderWithTitle`, `ShareManager`" — `app/` itself uses `KubrikoTheme`, `LargeButton`, `InfoPanel` and
  `rememberShareManager`; `Panel`, `FloatingButton`, `LoadingOverlay`, `SmallSwitch`, `SmallSlider`/`SmallSliderWithTitle` are
  used by the examples embedded in it; `SmallButton` has no in-repo user (it is public API — keep it).

## Fix
Rewrite the three bullets accordingly (re-run `grep -rln "<Component>" --include='*.kt' app examples tools` for each before
writing). Plans G20–G23 (games lane) switch the four games' loading spinners to `LoadingIndicator`; list it among the
components the examples use even if that lane has not merged yet, so the text is true once both lanes land. Leave the rest of
the file.

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
