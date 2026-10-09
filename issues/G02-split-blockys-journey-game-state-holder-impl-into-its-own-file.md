# Move `BlockysJourneyGameStateHolderImpl` out of `BlockysJourneyGameStateHolder.kt` into `BlockysJourneyGameStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolder.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt` (new)

## Problem
`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolder.kt` is named after the one-line `sealed interface BlockysJourneyGameStateHolder : StateHolder` (:39), but almost all of it is `internal class BlockysJourneyGameStateHolderImpl(` (:41–212) plus the file-private `LOG_TAG` / `LOG_TAG_BACKGROUND` constants (:214–215). The code style asks for one top-level type per file, named after it.

## Fix
- Create `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package `com.pandulapeter.kubriko.gameBlockysJourney.implementation`).
- Move verbatim into it: `internal class BlockysJourneyGameStateHolderImpl` (the whole class body), and the two `private const val LOG_TAG` / `LOG_TAG_BACKGROUND` declarations (they stay `private`; only the Impl uses them — grep the module for `LOG_TAG` to confirm).
- `BlockysJourneyGameStateHolder.kt` keeps only the license header, the package, `import com.pandulapeter.kubriko.shared.StateHolder` and `sealed interface BlockysJourneyGameStateHolder : StateHolder`.
- Split the imports: each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module, so nothing else changes. Callers import `com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolderImpl` by package, which does not change.
- Grep the repo (including `CLAUDE.md` files and skills) for `BlockysJourneyGameStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package; no declaration, visibility or initialisation order changes.

## Public API
None (examples are unpublished; `BlockysJourneyGameStateHolder` stays public in the same package).

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
