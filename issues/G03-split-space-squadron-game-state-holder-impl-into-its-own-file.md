# Move `SpaceSquadronGameStateHolderImpl` out of `SpaceSquadronGameStateHolder.kt` into `SpaceSquadronGameStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/SpaceSquadronGameStateHolder.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/SpaceSquadronGameStateHolderImpl.kt` (new)

## Problem
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/SpaceSquadronGameStateHolder.kt` is named after the one-line `sealed interface SpaceSquadronGameStateHolder : StateHolder` (:39), but almost all of it is `internal class SpaceSquadronGameStateHolderImpl(` (:41–192) plus the file-private `LOG_TAG` / `LOG_TAG_BACKGROUND` constants (:194–195). The code style asks for one top-level type per file, named after it.

## Fix
- Create `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/SpaceSquadronGameStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package `com.pandulapeter.kubriko.gameSpaceSquadron.implementation`).
- Move verbatim into it: `internal class SpaceSquadronGameStateHolderImpl` (the whole class body), and the two `private const val LOG_TAG` / `LOG_TAG_BACKGROUND` declarations (they stay `private`; only the Impl uses them — grep the module for `LOG_TAG` to confirm).
- `SpaceSquadronGameStateHolder.kt` keeps only the license header, the package, `import com.pandulapeter.kubriko.shared.StateHolder` and `sealed interface SpaceSquadronGameStateHolder : StateHolder`.
- Split the imports: each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module, so nothing else changes. Callers import `com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolderImpl` by package, which does not change.
- Grep the repo (including `CLAUDE.md` files and skills) for `SpaceSquadronGameStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package; no declaration, visibility or initialisation order changes.

## Public API
None (examples are unpublished; `SpaceSquadronGameStateHolder` stays public in the same package).

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
