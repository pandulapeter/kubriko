# Rename plugin-physics' `implementation/Helpers.kt` to `LineIntersection.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/implementation/Helpers.kt` → `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/implementation/LineIntersection.kt`

## Problem
The code-style skill forbids files named `Helpers.kt`. At 2480325f this file holds only
`internal fun lineIntersect(line1Start, line1End, line2Start, line2End): SceneOffset?` (:17),
`internal fun isPointOnLine(lineStart, lineEnd, point): Boolean` (:48) and
`private const val ON_LINE_TOLERANCE_SQUARED = 0.0001f` (:68), all about segment intersection.

## Fix
`git mv` the file to `LineIntersection.kt` in the same package, contents unchanged. Then grep the whole repo
(including `plugins/physics/CLAUDE.md`, skills, docs) for `Helpers.kt` / `HelpersKt` under physics and fix any
reference (none exist at 2480325f).

## Behaviour
Unchanged — the code is byte-identical; only the file name changes.

## Public API
None. Both functions are `internal`, so only the module-internal `HelpersKt` facade is renamed.

## Tests
The existing ones (`RayIntersectionTest` exercises both functions).

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop :plugins:physics:desktopTest`

## Manual check
none
