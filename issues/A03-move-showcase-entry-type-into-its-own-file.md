# Move `ShowcaseEntryType` out of `ShowcaseEntry.kt` into `ShowcaseEntryType.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntry.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryType.kt` (new)
- `app/shared/CLAUDE.md`

## Problem
`ShowcaseEntry.kt` (at 2480325f, lines 178-198) declares a second top-level enum, used by `Menu.kt`, `TopBar.kt`,
`ShowcaseContent.kt` and `ShowcaseEntryAvailabilityTest`:

```kotlin
internal enum class ShowcaseEntryType(
    val titleStringResource: StringResource,
    val iconDrawableResource: DrawableResource,
) {
    GAME(
```

`code-style`: a top-level class gets a file of its own.

## Fix
Move `enum class ShowcaseEntryType` verbatim into
`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryType.kt` (same package,
MPL-2.0 header from `ShowcaseEntry.kt`), with the imports it needs: `Res`, `games`, `demos`, `tests`, `other`, `ic_games`,
`ic_demos`, `ic_tests`, `ic_other`, `DrawableResource`, `StringResource`. Remove from `ShowcaseEntry.kt` the imports it no
longer uses (`games`, `demos`, `tests`, `other`, the four `ic_*`, `DrawableResource`); it keeps `StringResource`.

`app/shared/CLAUDE.md` → Key files: "`implementation/ShowcaseEntry.kt` — enum of all entries and `ShowcaseEntryType`." →
"`implementation/ShowcaseEntry.kt` — enum of all entries and `isAvailable`." plus a new line
"`implementation/ShowcaseEntryType.kt` — the menu categories (Games / Demos / Tests / Other) with their title and icon."

Grep the whole repo for `ShowcaseEntryType` in docs and skills and fix stale file references in the same commit.

## Behaviour
Verbatim move in one package. Unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest`

## Manual check
None.
