# Add a unit test setup and shared test fixtures to the library modules so the sweep's plans can land tests

**Challenged:** amended — the Wasm browser and iOS simulator test tasks are disabled, so `./gradlew build` does not start running tests that need a browser or a simulator; `commonTest` must still compile on every target. Testing extension: amended — the fixtures also need `kotlin-test-junit` (with `kotlin-test` alone, `org.junit.Assume` is unresolved in a non-test source set; proven in the probe worktree), `awaitProcessed`'s KDoc names the plan-`08` and re-entrancy limits under which it would hang, and `blockerForcesOneBatch` now proves the single batch instead of asserting something that holds for any batching. From the lane C/E challenge: `ManualKubriko.tick` takes the delta first, because the plans write `tick(16)` and `tick(Int.MAX_VALUE)` for one tick of that length.

**Extended (testing extension, 2026-09-28):** adds the unpublished `:tools:test-fixtures` module (step 4). It holds the
manual-tick instance, the deterministic actor-queue drain, the counting actors and the measuring helpers that plans
`01`–`09`, `28`, `29`, `47`, `49`, `51`, `57`, `62` and `64` use, so no lane invents its own copy. It is built only
on public API, so it needs no API decision. It was proven with a throwaway worktree at `e86d3748`: the engine's
`desktopTest` depending on a module that depends on `:engine` resolves, compiles and runs, engine tests still see
`internal` members, and `awaitProcessed` drains the queue as described.

**Kind:** build  ·  **Severity:** high (blocks every other plan's Tests section)  ·  **Platforms:** build only
**Files:** `gradle/libs.versions.toml`, `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/extensions/KotlinMultiplatform.kt`, `settings.gradle.kts`, `tools/test-fixtures/**` (new), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/TestSetupTest.kt` (new), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/testFixtures/TestFixturesTest.kt` (new), `tools/README.md`, `CLAUDE.md`, `.claude/skills/codebase-review/SKILL.md`

**Lands before the lanes are cut.** The orchestrator applies this in the main checkout right after the plans are
committed. `START` is the commit after it, so every lane's worktree can compile tests.

## Problem

At 0008d027 no module has a test source set or a test dependency. `gradle/libs.versions.toml` has no `kotlin-test`
entry and `configureKotlinMultiplatform` (`KotlinMultiplatform.kt`) wires no `commonTest` dependencies. `CLAUDE.md`
and the `codebase-review` skill both say to verify with `./gradlew test`, which finds no test task. The live stress
run had to inject `kotlin-test-junit` through an untracked Gradle init script before it could run at all.

The sweep's tests also need a common harness. As written, plan `01` creates one in `engine/src/desktopTest`, which
plugin and tool test source sets cannot see. As a result plans `47`, `49`, `51` and `57` each hand-roll their own
manual-tick instance and 2 s polling loop. Every actor test also waits by polling `allActors` with `Thread.sleep(2)`,
which is slow and timing-dependent.

## Fix

1. `gradle/libs.versions.toml` — add under `[libraries]`:
   ```toml
   kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
   kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
   kotlin-test-junit = { group = "org.jetbrains.kotlin", name = "kotlin-test-junit", version.ref = "kotlin" }
   ```
   `kotlin-test-junit` is for `:tools:test-fixtures` only (step 4). Test source sets get the JUnit flavour of
   `kotlin-test` inferred by the Kotlin Gradle plugin, but a main source set does not.
2. `configureKotlinMultiplatform` — after the targets are declared:
   - Add `kotlin-test` and `kotlinx-coroutines-test` to the `commonTest` dependencies. Plain Kotlin in build-logic has
     no generated accessors, so use `sourceSets.getByName("commonTest").dependencies { implementation(libs.findLibrary("kotlin-test").get()) … }`.
     The Kotlin Gradle plugin resolves `kotlin-test` to the JUnit flavour for the `desktop` JVM target automatically.
   - Unless `path == ":tools:test-fixtures"`, add `implementation(project(":tools:test-fixtures"))` to the
     `desktopTest` dependencies. Every library, plugin, tool and example module then gets the fixtures in its JVM
     tests without editing its own build file. The dependency is on the JVM variant only, so no iOS or Wasm test
     compilation pulls it in.
   - Disable the test *run* tasks of the non-JVM targets, with one comment line saying why. Without this,
     `./gradlew build` (`check` → `allTests`) would try to run every `commonTest` in a browser through Karma (none is
     configured) and on an iOS simulator:
     ```kotlin
     tasks.withType(KotlinJsTest::class.java).configureEach { enabled = false }
     tasks.withType(KotlinNativeTest::class.java).configureEach { enabled = false }
     ```
     The imports are `org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest` and
     `org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest`.

   `build` still runs the test *compilations* for those targets, because Gradle executes a disabled task's
   dependencies. That is intended: `commonTest` code must compile on every target, so JVM-only APIs (`Thread`,
   `java.util.concurrent.*`, `ThreadMXBean`) belong in `desktopTest`.

   All of this is test-scoped and must not enter the published POMs or Gradle metadata. Confirm with
   `./gradlew :engine:generateMetadataFileForKotlinMultiplatformPublication`, then check that
   `engine/build/publications/kotlinMultiplatform/module.json` contains neither `kotlin-test` nor `test-fixtures`.
3. `settings.gradle.kts` — include `":tools:test-fixtures"`, in alphabetical position after `":tools:scene-editor-noop"`.
4. **New module `tools/test-fixtures`** (unpublished, so no `kubriko-public-artifact` and no `artifactMetadata`):
   - `build.gradle.kts` applies `kubriko-library`, sets `android { namespace = "com.pandulapeter.kubriko.testFixtures" }`,
     puts `api(projects.engine)` in `commonMain` and `api(libs.kotlin.test)` plus `api(libs.kotlin.test.junit)` in
     `desktopMain`. The second one is required: the fixtures are a main source set, where the plugin does not
     infer the JUnit variant, so with `kotlin-test` alone `org.junit.Assume` (used by `Allocations.kt`) does not
     resolve.
   - `README.md` states that the module is for this repository's tests, is not published, and holds only JVM code.
     It lists the helpers below with one line each.
   - All code goes in `src/desktopMain/kotlin/com/pandulapeter/kubriko/testFixtures/`, one file per topic. Each file
     gets the MPL header and each public declaration a short KDoc (see `code-style`). Nothing may use `internal`
     engine API, because this module cannot see it. Allocate freely: this is test code.
     - `ManualKubriko.kt` — `class ManualKubriko(val kubriko: Kubriko, val tickSource: ManualTickSource)` with:
       - `val actorManager: ActorManager`
       - `fun tick(deltaTimeInMilliseconds: Int = 16, count: Int = 1)`. The delta comes first on purpose: the plans
         write `tick(16)`, `tick(20)` and `tick(Int.MAX_VALUE)` for one tick of that many milliseconds, as
         `ManualTickSource.tick` reads, so `tick(16)` must never mean sixteen ticks.
       - `fun tickUntil(timeoutInMilliseconds: Long = 2_000, deltaTimeInMilliseconds: Int = 16, condition: () -> Boolean)`,
         which ticks once, checks, sleeps 1 ms and repeats, and fails the test with the elapsed time on timeout. This
         is for the plugins' asynchronous registration, where a Manager's actor list reaches it through
         `Dispatchers.Default` and a `Dispatchers.Main` hop.
       - `fun dispose()`

       Also `fun newManualKubriko(vararg managers: Manager, shouldStart: Boolean = true): ManualKubriko`, which builds
       the instance with `TickSource.manual()` and calls `start()` unless told not to. Its KDoc says three things:
       - Without a `KubrikoViewport` the viewport has no size.
       - Until plan `07` lands, a test that relies on `Dynamic` updates must pass
         `ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false)` (plugin lanes run on the pre-`07` engine).
       - Engine tests that need a sized viewport use the engine's own `newTestKubriko` (plan `01`).
     - `ActorManagerExtensions.kt` — `fun ActorManager.awaitProcessed(timeoutInMilliseconds: Long = 5_000)`. It adds
       then removes a private sentinel actor and waits on a `CountDownLatch` that the sentinel's `onRemoved` counts
       down. The sentinel is an `Actor` only, with no other traits. The queue is FIFO and `onRemoved` fires after that
       batch's `allActors` is published (`ActorManagerImpl.processBatch`: `_allActors.value = …` precedes the
       `dispose()`/`onRemoved()` loop). So when the call returns, every operation issued before it on the calling
       thread has been applied and published. The KDoc states the limits:
       - Call it only after `start()`. After plan `05`, nothing is processed before then.
       - It covers `allActors` only. The derived lists (`activeDynamicActors` and the others) are published
         synchronously only after plan `06`.
       - The sentinel is briefly visible in `allActors`.
       - Call it only between `start()` and `kubriko.dispose()`. After plan `08`, disposing cancels the processor and
         skips the removal callbacks of a batch that races it, so a call during or after `dispose()` fails on
         timeout.
       - Never call it from an actor callback (the processor thread would wait on itself) or while a `Blocker` holds
         the processor. Either fails on timeout.

       The sentinel's removal is the last operation of its batch, and callbacks run one after another on the processor
       thread. So when the call returns, every callback of the earlier operations (`onAdded`, `dispose`, `onRemoved`)
       has also returned. Plan `01`'s per-callback isolation keeps a throwing callback from skipping the sentinel's
       `onRemoved`, and its rethrow is launched on the Kubriko scope's `SupervisorJob`, so the processor survives it.
     - `Await.kt` — `fun awaitCondition(timeoutInMilliseconds: Long = 5_000, condition: () -> Boolean)`, which polls
       every 2 ms and fails on timeout. The KDoc calls it the fallback for asynchronous work that has no deterministic
       signal. Prefer `awaitProcessed` or `tickUntil`.
     - `CountingActor.kt`:
       - `open class CountingActor(var onAddedAction: (() -> Unit)? = null, var onRemovedAction: (() -> Unit)? = null, var disposeAction: (() -> Unit)? = null) : Actor, Dynamic, Disposable`
         with `AtomicInteger` counters `added`, `removed`, `disposed` and `updates`. It also records the thread
         `onAdded` last ran on in `val onAddedThread: AtomicReference<Thread?>`.
       - `class Blocker : Actor`, whose `onAdded` signals `entered` (a `CountDownLatch`) and then blocks on `release`
         (another latch, 5 s cap). A test uses it to force several operations into one batch: add the blocker, await
         `entered`, issue the operations, then call `release.countDown()`.
     - `UncaughtExceptions.kt` — `fun <T> recordingUncaughtExceptions(block: (recorded: List<Throwable>) -> T): T`.
       It installs a default uncaught-exception handler that appends to a `CopyOnWriteArrayList`, runs `block`, and
       restores the previous handler in `finally`. Its KDoc warns against using it inside `runTest`, because
       `kotlinx-coroutines-test` captures coroutine exceptions before the default handler sees them.
     - `Allocations.kt` — `fun measureAllocatedBytesPerRun(warmUpRuns: Int = 20_000, measuredRuns: Int = 2_000, block: () -> Unit): Double`.
       It uses `(ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean).getThreadAllocatedBytes(Thread.currentThread().threadId())`
       around the measured runs. The warm-up must be long enough for the JIT to compile `block`, because interpreted
       code allocates boxes that compiled code does not. If `isThreadAllocatedMemorySupported` is false, it skips
       with `org.junit.Assume.assumeTrue`, reached through kotlin-test's JUnit dependency. Its KDoc says the result
       is an upper-bound check that only makes sense for work done on the calling thread.
5. Add `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/TestSetupTest.kt` (MPL header) with one test. It
   creates `Kubriko.newInstance(tickSource = TickSource.manual())`, starts it, ticks once, disposes it, and asserts
   that `get<ActorManager>()` then throws `IllegalStateException`. It proves the setup and gives later lanes a
   pattern to copy.
6. Add `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/testFixtures/TestFixturesTest.kt` (MPL header). It
   proves the fixtures on the engine at `START`:
   - `awaitProcessedAppliesEarlierOperations` — `newManualKubriko()`, `add(a, b)`, `remove(a)`, `awaitProcessed()`.
     Assert `allActors.value == listOf(b)`, which also shows that the sentinel is gone.
     Repeat 200 times with fresh instances.
   - `tickUntilFailsOnTimeout` — `assertFails { tickUntil(timeoutInMilliseconds = 50) { false } }`.
   - `blockerForcesOneBatch` — add the `Blocker`, await `entered`, then add three `CountingActor`s with three
     separate `add` calls. Each one's `onAddedAction` records whether `allActors.value` already contains any of the
     three. Release, then `awaitProcessed()`. All three have `added == 1`, and none of them saw another in
     `allActors`. `onAdded` runs before its batch is published, so that is true only if all three landed in one
     batch. Asserting `added == 1` alone would pass with any batching.
   - `allocationMeasurementSeesAnAllocation` — `measureAllocatedBytesPerRun { sink = ByteArray(1024) }` is at least
     1024, where `sink` is a `@Volatile` field so the allocation cannot be eliminated.
7. The canonical test command becomes `./gradlew desktopTest`. It runs the JVM tests of every module; the Wasm
   browser and iOS simulator test tasks need a browser or Xcode and are not part of verification.
   - `CLAUDE.md` → "Build, run, test": replace `./gradlew test` with `./gradlew desktopTest`, and replace the
     single-test example with `./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.TestSetupTest"`.
   - Add a short **Testing** paragraph under "Build configuration flags". It says: `commonTest` runs on the desktop
     JVM only (the Wasm and iOS test tasks are disabled), so JVM-only APIs go in `desktopTest`. Every module's
     `desktopTest` already depends on `:tools:test-fixtures` (manual-tick instance, `awaitProcessed`, counting
     actors, allocation measurement), so use it instead of hand-rolling a harness. Library test classpaths have no
     native Skia runtime, so tests must not create a real `ImageBitmap` or draw.
   - Add `test-fixtures` to the tool list in `CLAUDE.md` → Tools and in `tools/README.md`, marked unpublished.
   - Update the skill's "Tests:" bullet the same way as `CLAUDE.md`.

## Tests

Steps 5 and 6 are the tests. Run `./gradlew :engine:desktopTest`, then `./gradlew desktopTest`, then
`./gradlew :engine:build :tools:test-fixtures:build`. The last must pass without a browser and must not report a
Wasm or iOS test run. Then run the metadata check from step 2.

## Manual check

None.
