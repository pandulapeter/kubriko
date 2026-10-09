# Define the per-entry feature predicates (debug menu, info button, logo) once, next to `ShowcaseEntry`, and test them

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryFeatures.kt` (new)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/TopBar.kt`
- `app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryFeaturesTest.kt` (new)
- `app/shared/CLAUDE.md`

## Problem
The same predicate is written twice under two names (at 2480325f):

```kotlin
// ShowcaseContent.kt:366-370
private val ShowcaseEntry?.hasDebugMenu
    get() = when (this?.type) {
        null, ShowcaseEntryType.OTHER -> false
        else -> true
    }
// TopBar.kt:218
private val ShowcaseEntry?.shouldShowDebugButton get() = this != null && this.type != ShowcaseEntryType.OTHER
```

(equal for every input: `null` → false, `OTHER` → false, otherwise true), next to two more per-entry decisions private to
`TopBar.kt`:

```kotlin
private val ShowcaseEntry?.shouldShowLogo get() = this == null || this == ShowcaseEntry.ABOUT || this == ShowcaseEntry.LICENSES
private val ShowcaseEntry?.shouldShowInfoButton get() = this?.type == ShowcaseEntryType.DEMO || this?.type == ShowcaseEntryType.TEST
```

The CLAUDE.md says `ShowcaseEntryType` "controls … which features (debug menu, info panel, fullscreen) are shown", but the
rules are scattered and untested.

## Fix
New `.../implementation/ShowcaseEntryFeatures.kt` (MPL-2.0 header) with three `internal` extension properties:
- `ShowcaseEntry?.hasDebugMenu` — `TopBar`'s one-line body (`this != null && this.type != ShowcaseEntryType.OTHER`);
- `ShowcaseEntry?.shouldShowInfoButton` — verbatim;
- `ShowcaseEntry?.shouldShowLogo` — verbatim.

Delete `hasDebugMenu` from `ShowcaseContent.kt` and the three from `TopBar.kt`; `TopBar` uses `hasDebugMenu` where it used
`shouldShowDebugButton`. Add the imports (`com.pandulapeter.kubrikoShowcase.implementation.hasDebugMenu`, …) and drop
`ShowcaseEntryType` imports that become unused. `isAvailable` stays in `ShowcaseEntry.kt`.

Add `ShowcaseEntryFeaturesTest` (same package, `kotlin.test`):
- `hasDebugMenu`: false for `null` and every `OTHER` entry, true for every other entry;
- `shouldShowInfoButton`: true exactly for `DEMO` and `TEST` entries, false for `null`;
- `shouldShowLogo`: true exactly for `null`, `ABOUT` and `LICENSES`.

`app/shared/CLAUDE.md` → Key files: add "`implementation/ShowcaseEntryFeatures.kt` — which top bar buttons and panels an
entry gets (debug menu, info button, logo)."

## Behaviour
`hasDebugMenu` and `shouldShowDebugButton` agree on every input, so both call sites see the same values; the other two
move verbatim. Unchanged.

## Public API
None.

## Tests
`ShowcaseEntryFeaturesTest` above.

## Verify
`./gradlew :app:shared:desktopTest`

## Manual check
None.
