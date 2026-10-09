# Move Blocker out of CountingActor.kt into Blocker.kt in test-fixtures

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop (tests)  ·  **Class:** Now
**Artifact:** unpublished (test-fixtures)
**Files:** tools/test-fixtures/src/desktopMain/kotlin/com/pandulapeter/kubriko/testFixtures/CountingActor.kt, tools/test-fixtures/src/desktopMain/kotlin/com/pandulapeter/kubriko/testFixtures/Blocker.kt (new)

## Problem
`CountingActor.kt` also declares an unrelated class (lines 60-81 at 2480325f):
```kotlin
class Blocker : Actor {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    override fun onAdded(kubriko: Kubriko) { entered.countDown(); release.await(5, TimeUnit.SECONDS) }
}
```
One top-level class per file, named after it.

## Fix
Move `Blocker` with its KDoc and member KDocs verbatim to `Blocker.kt` (same package, MPL-2.0 header, imports `Kubriko`,
`Actor`, `CountDownLatch`, `TimeUnit`). Remove `CountDownLatch` and `TimeUnit` imports from `CountingActor.kt`.
`tools/test-fixtures/README.md` lists helpers by name only — no change needed.

## Behaviour
Verbatim move; a class keeps its JVM name.

## Public API
None (unpublished module).

## Tests
The existing ones (every module's `desktopTest` uses the fixtures).

## Verify
`./gradlew :tools:test-fixtures:compileKotlinDesktop :engine:desktopTest`

## Manual check
None.
