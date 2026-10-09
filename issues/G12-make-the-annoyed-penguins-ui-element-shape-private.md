# Make `AnnoyedPenguinsUIElementShape` private to the theme file.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/AnnoyedPenguinsTheme.kt`

## Problem
`internal val AnnoyedPenguinsUIElementShape: CornerBasedShape = RoundedCornerShape(` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/AnnoyedPenguinsTheme.kt:64`) is `internal`, but its only users are the five `Shapes(...)` entries in the same file (:45–49) — grep the module to confirm. Its visibility suggests a shared building block that does not exist.

## Fix
Change `internal val AnnoyedPenguinsUIElementShape` to `private val AnnoyedPenguinsUIElementShape`. While there, drop the second of the two blank lines before it (:62–63) so the file has single blank lines between declarations.

Drop this plan if, at execution time, any other file in the module references `AnnoyedPenguinsUIElementShape`.

## Behaviour
Visibility only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
