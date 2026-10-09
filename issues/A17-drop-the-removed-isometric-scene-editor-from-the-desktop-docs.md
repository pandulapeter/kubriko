# Drop the removed isometric demo scene editor from the desktop app's CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/CLAUDE.md`

## Problem
`app/desktop/CLAUDE.md` → Scene Editors (at 2480325f) lists

```
- `IsometricGraphicsDemoSceneEditor` → `examples/demo-isometric-graphics/.../files/scenes`
```

but `KubrikoShowcaseApp.kt` registers only `AnnoyedPenguinsGameSceneEditor`, `BlockysJourneyGameSceneEditor`,
`PerformanceDemoSceneEditor` and `PhysicsDemoSceneEditor`, and no `IsometricGraphicsDemoSceneEditor` exists anywhere in
the repository (grep).

## Fix
Delete that line. Grep the other `CLAUDE.md` files, `documentation/` and `.claude/skills` for
`IsometricGraphicsDemoSceneEditor` and remove any other stale mention in the same commit.

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
None needed (docs only).

## Manual check
None.
