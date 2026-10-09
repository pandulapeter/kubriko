# Move `WallbreakerGameStateHolderImpl` out of `WallbreakerGameStateHolder.kt` into `WallbreakerGameStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/WallbreakerGameStateHolder.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/WallbreakerGameStateHolderImpl.kt` (new)

## Problem
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/WallbreakerGameStateHolder.kt` is named after the one-line `sealed interface WallbreakerGameStateHolder : StateHolder` (:37), but almost all of it is `internal class WallbreakerGameStateHolderImpl(` (:39–177) plus the file-private `LOG_TAG` / `LOG_TAG_BACKGROUND` constants (:179–180). The code style asks for one top-level type per file, named after it.

## Fix
- Create `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/WallbreakerGameStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package `com.pandulapeter.kubriko.gameWallbreaker.implementation`).
- Move verbatim into it: `internal class WallbreakerGameStateHolderImpl` (the whole class body), and the two `private const val LOG_TAG` / `LOG_TAG_BACKGROUND` declarations (they stay `private`; only the Impl uses them — grep the module for `LOG_TAG` to confirm).
- `WallbreakerGameStateHolder.kt` keeps only the license header, the package, `import com.pandulapeter.kubriko.shared.StateHolder` and `sealed interface WallbreakerGameStateHolder : StateHolder`.
- Split the imports: each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module, so nothing else changes. Callers import `com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolderImpl` by package, which does not change.
- Grep the repo (including `CLAUDE.md` files and skills) for `WallbreakerGameStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package; no declaration, visibility or initialisation order changes.

## Public API
None (examples are unpublished; `WallbreakerGameStateHolder` stays public in the same package).

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
