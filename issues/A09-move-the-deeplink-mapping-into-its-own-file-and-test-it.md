# Move the deeplink mapping out of `KubrikoShowcase.kt` into `ShowcaseDeeplink.kt` and pin it with a test

**Kind:** test  ·  **Severity:** low  ·  **Platforms:** all (the web shell drives it)  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseDeeplink.kt` (new)
- `app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseDeeplinkTest.kt` (new)
- `app/shared/CLAUDE.md`

## Problem
The deeplink mapping is pure logic that the web shell's browser history depends on, but it is `private` to the entry
Composable's file (at 2480325f, `KubrikoShowcase.kt:124-146`), so nothing can test it:

```kotlin
private val ShowcaseEntry?.deeplink
    get() = when (this) {
        ShowcaseEntry.WALLBREAKER -> "wallbreaker"
        …
        null -> null
    }

private fun String?.processDeeplink() = this?.trim()?.lowercase()?.split("/")?.filterNot { it.isBlank() }?.lastOrNull().let { deeplink ->
    ShowcaseEntry.entries.firstOrNull { it.deeplink == deeplink }
}?.takeIf { it.isAvailable }
```

`code-style`: decisions are pure `internal` functions a unit test can reach.

## Fix
Move both declarations verbatim to `.../kubrikoShowcase/implementation/ShowcaseDeeplink.kt` (package
`com.pandulapeter.kubrikoShowcase.implementation`, next to `ShowcaseEntry` and `isAvailable`; MPL-2.0 header) and widen
both from `private` to `internal` (no clash in the package). `KubrikoShowcase.kt` imports
`com.pandulapeter.kubrikoShowcase.implementation.deeplink` and `…processDeeplink`; its public signature's default values
(`deeplink: String? = selectedShowcaseEntry.value.deeplink`, `onDestinationChanged = { selectedShowcaseEntry.value = it.processDeeplink() }`)
keep compiling, since a public function's default value may use internal declarations of its module. Drop the
`isAvailable` import from `KubrikoShowcase.kt` if nothing else there uses it.

Add `ShowcaseDeeplinkTest` (package `com.pandulapeter.kubrikoShowcase.implementation`, `kotlin.test`), mirroring
`ShowcaseEntryAvailabilityTest`'s style:
- every entry: `entry.deeplink.processDeeplink()` equals `entry.takeIf { it.isAvailable }` (round trip; unavailable entries —
  the tests without `showcase.areTestExamplesEnabled`, `BLOCKYS_JOURNEY` without the unfinished games flag — resolve to
  `null`, whatever the build's flags are);
- every entry has a distinct, non-blank deeplink;
- `(null as ShowcaseEntry?).deeplink` is `null`;
- `"/kubriko/physics/".processDeeplink()`, `" Physics ".processDeeplink()` and `"PHYSICS".processDeeplink()` are
  `ShowcaseEntry.PHYSICS` (the last non-blank path segment, trimmed and lowercased);
- `"unknown"`, `""`, `"/"`, `"  "` and `null` process to `null`.

`app/shared/CLAUDE.md`: the deeplink paragraph names `String?.processDeeplink()` — add "(`implementation/ShowcaseDeeplink.kt`)";
Key files: `KubrikoShowcase.kt` line loses "deeplink logic", and add "`implementation/ShowcaseDeeplink.kt` — entry ↔
deeplink mapping." Grep the repo for `processDeeplink` in docs and skills and fix stale ones.

## Behaviour
Verbatim move plus `private` → `internal`. Unchanged.

## Public API
None (`KubrikoShowcase`'s signature and defaults are untouched).

## Tests
`ShowcaseDeeplinkTest` above — pure logic over the enum, no Skia. Should any assertion fail on the code as it is, the
mapping has a real defect: `@Ignore` that assertion with the reason and report it rather than changing production code
in this plan.

## Verify
`./gradlew :app:shared:desktopTest :app:web:compileKotlinWasmJs`

## Manual check
None.
