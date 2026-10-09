# Split the Licenses screen's state holder, `LicenseType` and `Dependency` out of `LicensesScreen.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicensesScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicensesScreenStateHolder.kt` (new)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicenseType.kt` (new)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/Dependency.kt` (new)

## Problem
`LicensesScreen.kt` (284 lines at 2480325f) holds the screen and four other things:

- `fun createLicensesScreenStateHolder(): LicensesScreenStateHolder = LicensesScreenStateHolderImpl()` (:58)
- `private enum class LicenseType(val licenseName: StringResource)` (:119-140)
- `private enum class Dependency(val dependencyName: String, val url: String, val type: LicenseType)` — 23 entries (:142-262)
- `sealed interface LicensesScreenStateHolder : StateHolder { companion object { … areResourcesLoaded() … } }` (:264-279)
- `private class LicensesScreenStateHolderImpl : LicensesScreenStateHolder` (:281-285)

`code-style`: one file, one thing, named after it; the dependency table alone is 120 lines of data.

## Fix
Verbatim moves, same package, MPL-2.0 header from `LicensesScreen.kt`:

| New file | Declarations | Visibility |
|---|---|---|
| `LicensesScreenStateHolder.kt` | factory, `sealed interface LicensesScreenStateHolder` (with its companion), `LicensesScreenStateHolderImpl` | factory/interface unchanged; impl stays `private` (the screen never casts to it) |
| `LicenseType.kt` | `enum class LicenseType` | `private` → `internal` (used by `Dependency.kt` and the screen; A09 also reads it from the state holder) |
| `Dependency.kt` | `enum class Dependency` | `private` → `internal` (used by the screen) |

No other `LicenseType` / `Dependency` exists in package `…ui.licenses`. Keep the enums' trailing `;` (`MPL_2_0(...);`,
`KUBRIKO(...);`) as they are — A19 (trailing commas) removes them. `LicensesScreen.kt` keeps only `LicensesScreen`.
Move the imports with their users (`preloadedString`, `Kubriko`, `StateHolder`, `Flow`, `emptyFlow` to the state holder;
the `other_licenses_<license>` strings to `LicenseType.kt` and, for `areResourcesLoaded`, the state holder;
`other_licenses_content` / `other_licenses_music_note` stay with the screen as well).

Grep the repo for `LicensesScreen.kt` in docs and skills in the same commit.

## Behaviour
Verbatim moves; only `private` → `internal` widenings. `Dependency.entries.groupBy { it.type }` iterates the same entries
in the same order. Unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop`

## Manual check
None.
