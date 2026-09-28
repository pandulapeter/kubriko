# Apply the menu's visibility rules to deeplinks, so hidden unfinished games cannot be opened by URL

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Web (the only platform that feeds deeplinks); all for the shared
predicate
**Files:** `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntry.kt`,
`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/Menu.kt`,
`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`, `app/shared/CLAUDE.md`,
`app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryAvailabilityTest.kt` (new)

## Problem

The menu hides two kinds of entries (`Menu.kt`, ~line 51):

```kotlin
) = allShowcaseEntries
    .filter { if (BuildConfig.ARE_TEST_EXAMPLES_ENABLED) true else it.type != ShowcaseEntryType.TEST }
    .filter { if (BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES) true else it.isProductionReady }
```

but the deeplink parser only applies the first rule (`KubrikoShowcase.kt`, ~line 140):

```kotlin
private fun String?.processDeeplink() = this?.trim()?.lowercase()?.split("/")?.filterNot { it.isBlank() }?.lastOrNull().let { deeplink ->
    ShowcaseEntry.entries.firstOrNull { it.deeplink == deeplink }
}.let {
    if (it?.type == ShowcaseEntryType.TEST && !BuildConfig.ARE_TEST_EXAMPLES_ENABLED) null else it
}
```

In a build with `showcase.shouldShowUnfinishedGames=false`, `…/kubriko/blockys-journey` still opens Blocky's Journey
(its module is always linked; the flag only hides the menu row), with no menu row selected. The release workflows all
pass `shouldShowUnfinishedGames=true` today, so this only bites a build that turns the flag off — which is exactly
what the flag exists for.

## Fix

1. In `ShowcaseEntry.kt`, add one predicate that states which entries this build offers, e.g.

   ```kotlin
   /** Whether this build offers the entry at all: test entries need the test examples, unfinished ones the unfinished games flag. */
   internal val ShowcaseEntry.isAvailable: Boolean
       get() = when {
           type == ShowcaseEntryType.TEST && !BuildConfig.ARE_TEST_EXAMPLES_ENABLED -> false
           !isProductionReady && !BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES -> false
           else -> true
       }
   ```

   (import `com.pandulapeter.kubrikoShowcase.BuildConfig`). If the IDE flags "condition is always true/false" on the
   constants, mirror the existing `if (FLAG) true else …` shape instead.
2. In `Menu.kt`, replace the two `.filter { … }` lines with `.filter { it.isAvailable }` and drop the imports that
   become unused.
3. In `KubrikoShowcase.kt`, replace the last `.let { … TEST … }` of `processDeeplink` with
   `.takeIf { it?.isAvailable == true }` (or equivalent), and drop the `ShowcaseEntryType` import if nothing else
   uses it.

In `app/shared/CLAUDE.md` ("Navigation model"), note that deeplinks resolve only to entries the menu shows
(`isAvailable`).

Plan 85 builds on `isAvailable`; run this plan first.

## Tests

`app/shared` applies `kubriko-compose-library`, so after plan 00 it has a `commonTest` source set with `kotlin-test`.
Add `app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntryAvailabilityTest.kt`
(MPL header) asserting, for every `ShowcaseEntry`: an entry with `type == TEST` has
`isAvailable == BuildConfig.ARE_TEST_EXAMPLES_ENABLED`, an entry with `isProductionReady == false` has
`isAvailable == BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES`, and every other entry is available. `processDeeplink` is
private and not worth widening for a test. Run `./gradlew :app:shared:desktopTest`. If plan 00 did not land or does
not reach `app/shared`, skip the test rather than adding test infrastructure to the app.

## Manual check

Web (desktop Chrome), with the flag off:
`./gradlew :app:web:wasmJsBrowserDevelopmentRun -Pshowcase.shouldShowUnfinishedGames=false`, open the root URL the dev
server prints, wait for the Welcome screen, then in the DevTools console run
`history.pushState(null, '', '/blockys-journey'); dispatchEvent(new PopStateEvent('popstate'))` (the dev server may not
serve deep paths directly, so this drives the same `popstate` path a real deeplink uses).

- Before: Blocky's Journey opens. After: the Welcome screen stays.
- The same with `'/wallbreaker'` still opens Wallbreaker; with `-Pshowcase.areTestExamplesEnabled=false`,
  `'/collision'` still stays on the Welcome screen.
