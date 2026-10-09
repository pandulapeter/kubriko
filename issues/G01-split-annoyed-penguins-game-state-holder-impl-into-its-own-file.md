# Move `AnnoyedPenguinsGameStateHolderImpl` out of `AnnoyedPenguinsGameStateHolder.kt` into `AnnoyedPenguinsGameStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolder.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt` (new)

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolder.kt` is named after the one-line `sealed interface AnnoyedPenguinsGameStateHolder : StateHolder` (:45), but almost all of it is `internal class AnnoyedPenguinsGameStateHolderImpl(` (:47–267) plus the file-private `LOG_TAG` / `LOG_TAG_BACKGROUND` constants (:269–270). The code style asks for one top-level type per file, named after it.

## Fix
- Create `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package `com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation`).
- Move verbatim into it: `internal class AnnoyedPenguinsGameStateHolderImpl` (the whole class body), and the two `private const val LOG_TAG` / `LOG_TAG_BACKGROUND` declarations (they stay `private`; only the Impl uses them — grep the module for `LOG_TAG` to confirm).
- `AnnoyedPenguinsGameStateHolder.kt` keeps only the license header, the package, `import com.pandulapeter.kubriko.shared.StateHolder` and `sealed interface AnnoyedPenguinsGameStateHolder : StateHolder`.
- Split the imports: each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module, so nothing else changes. Callers import `com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl` by package, which does not change.
- Grep the repo (including `CLAUDE.md` files and skills) for `AnnoyedPenguinsGameStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package; no declaration, visibility or initialisation order changes.

## Public API
None (examples are unpublished; `AnnoyedPenguinsGameStateHolder` stays public in the same package).

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
