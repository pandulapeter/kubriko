# Move the engine tests that sit in the root package into the package of the code they test

**Kind:** test  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (engine tests)
**Files:**
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/BatchCallbackPairingTest.kt` → `.../kubriko/manager/BatchCallbackPairingTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/DuplicateAdditionTest.kt` → `.../kubriko/manager/DuplicateAdditionTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/GroupFlatteningTest.kt` → `.../kubriko/manager/GroupFlatteningTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/RemovedActorUpdateTest.kt` → `.../kubriko/manager/RemovedActorUpdateTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ActorCallbackFailureTest.kt` → `.../kubriko/manager/ActorCallbackFailureTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/HeadlessActorUpdateTest.kt` → `.../kubriko/manager/HeadlessActorUpdateTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/SyncStateFlowTest.kt` → `.../kubriko/implementation/SyncStateFlowTest.kt`
- `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/TickSourceLifecycleTest.kt` → `.../kubriko/helpers/TickSourceLifecycleTest.kt`

## Problem

`code-style` says tests sit in the package of the code they test. At 2480325f these desktop tests are in the root
package `com.pandulapeter.kubriko` although they exercise `ActorManagerImpl` (the batch processor, group flattening,
duplicate additions, callback failures, removed/headless actor updates), `SyncStateFlow` and `TickSource`:

```kotlin
package com.pandulapeter.kubriko

class BatchCallbackPairingTest {        // also DuplicateAdditionTest, GroupFlatteningTest, RemovedActorUpdateTest,
                                        // ActorCallbackFailureTest, HeadlessActorUpdateTest, SyncStateFlowTest,
                                        // TickSourceLifecycleTest
```

The ones that test the `KubrikoImpl` instance itself stay in the root package: `ActorProcessingStartTest`,
`DisposeDuringTickTest`, `KubrikoDisposeActorsTest`, `ManagerLookupConcurrencyTest`, `SharedManagerTest`, and the
shared helper `ActorTestHarness.kt` (`internal fun newTestKubriko`, already imported by
`manager/ViewportContractTest`, `lifecycle/ActorLifecycleChurnTest`, `performance/HotPathAllocationTest`).

## Fix

`git mv` each file listed above to its target directory, change its `package` line, and fix imports only:
- add the root-package names the file uses: `com.pandulapeter.kubriko.Kubriko`, `com.pandulapeter.kubriko.KubrikoImpl`,
  `com.pandulapeter.kubriko.newTestKubriko` (each only where used; e.g. `BatchCallbackPairingTest` uses `KubrikoImpl`
  in `private fun KubrikoImpl.inOneBatch`, `HeadlessActorUpdateTest`/`SyncStateFlowTest` use
  `Kubriko.newInstance(...) as KubrikoImpl`, the actor-manager tests call `newTestKubriko`);
- drop imports that become same-package (`com.pandulapeter.kubriko.manager.ActorManager` in the files moving to
  `manager`; `com.pandulapeter.kubriko.helpers.TickSource` in `TickSourceLifecycleTest`).

Nothing else changes in the test bodies. None of the moved files declares a top-level `private` or `internal`
symbol, so there is no name clash in the target packages (checked at 2480325f).

Grep for stale fully-qualified names afterwards. At 2480325f nothing references these classes by FQN: the only
`--tests` filter in the repository is root `CLAUDE.md`'s
`./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.TestSetupTest"`, and `TestSetupTest` is a
`commonTest` that does not move; `.github/workflows/tests.yml` runs `./gradlew desktopTest --continue
--no-configuration-cache` without filters; no `CLAUDE.md`, README, `documentation/*.md` or skill names a moved test.

## Behaviour
Unchanged: the same tests run, under new FQNs.

## Public API
None (test sources).

## Tests
The moved ones; the run must report the same number of tests as before (compare the count of
`engine/build/test-results/desktopTest/*.xml` test cases before and after).

## Verify
`./gradlew :engine:desktopTest`

## Manual check
none
