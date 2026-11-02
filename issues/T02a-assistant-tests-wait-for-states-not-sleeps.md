<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Make the assistant tests wait for an idle state instead of sleeping before negative assertions, share one blocking-test helper, and give FakeAgent's delayed notification a structured scope

**Challenged:** amended — blocking pipe reads of a live fake agent or sign-in kept the tracked IO dispatcher busy forever (fakes' streams now park); `awaitTrue` waits in real time even under `runTest` (T02b relies on it); idle must hold for two polls; header matched to EXECUTION.md (lane A, phase 1).

**Kind:** tests  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** TST-8 (assistant part), TST-6 (the five `test(block)` copies)  ·  **Depends on:** A01, A02a, A03 (the same lane, run after A04 as EXECUTION.md orders it: lane A rewrites the same test files and `FakeAgent.kt`). It only adds new files to T01's `testing` package, which lane A's worktree (cut from `START`) doesn't have yet; nothing here uses T01's files, so create the folder if it is missing.
**Files:** (T = `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator`)
- New: `T/testing/BlockingTest.kt`, `T/testing/Awaiting.kt`, `T/testing/IdleTrackingDispatcher.kt`, `T/testing/IdleTrackingDispatcherTest.kt`
- Rewritten: `T/ai/assistant/AssistantTest.kt`, `T/ai/assistant/AssistantChatTest.kt`, `T/ai/assistant/SignInSessionTest.kt`, `T/ai/assistant/AcpAgentTest.kt`, `T/ai/assistant/FakeAgent.kt`, `T/ai/assistant/FakeAgentLauncher.kt` (`FakeAgentChannel`, its streams parked), `T/ai/assistant/FakeTerminalLauncher.kt` (its streams parked), and `T/ai/assistant/AssistantControllerTest.kt`, or, after A02a's split, the test files that now hold the tests named below (`AgentSelectionTest.kt`, `AssistantHostTest.kt`, `AssistantSignInTest.kt`)
- Docs: `creator/CLAUDE.md` (Running and testing, one clause)

## Problem

At HEAD, before lane A's changes, the assistant tests make negative assertions after fixed sleeps. They pass whatever the code does in the background once the sleep is over, and they are slow:

- `AssistantControllerTest.withAiFeaturesOffNoConversationOpensNoAgentStartsAndTheRegistryIsNotFetched`:
  ```kotlin
  onMain {
      controller.setProject(projectDirectory)
      controller.newChat()
      controller.installNode()
  }
  delay(200)
  assertNull(controller.currentChat.value)
  assertTrue(commands.isEmpty())
  assertEquals(0, fetches.get())
  ```
- `AssistantControllerTest.aSecondSignInStopsTheFirst`:
  ```kotlin
  assertEquals(1, terminals.channels.count { it.isStopped })
  delay(100)
  assertNull(controller.signIn.value!!.exitCode.value)
  ```
- `AcpAgentTest.closingTheAgentStopsItsProcess`:
  ```kotlin
  assertFailsWith<IllegalStateException> { agent.start() }
  delay(100)
  assertEquals(1, launcher.agents.size)
  ```

`FakeAgent`'s `"later"` prompt sends its notification from an unstructured scope that nothing cancels, so it can outlive its test and land in the next one:

```kotlin
"later" -> {
    val client = currentCoroutineContext().client
    CoroutineScope(Dispatchers.Default).launch {
        delay(50)
        client.notify(message(LATER))
    }
}
```

The same runner is written five times: `private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }` in `AssistantTest`, `AssistantControllerTest`, `SignInSessionTest`, and `AssistantChatTest`, and `runBlocking { withTimeout(TIMEOUT) { block() } }` in `AcpAgentTest`. `AcpAgentTest` also has its own polling `awaitTrue`.

These tests can't run on `runTest` virtual time: `FakeAgentChannel` talks to the code under test through real `Pipe`s read on real threads. So the fix keeps real threads, but waits for a state it can observe. **Re-locate every quoted test after lane A:** A02a splits `AssistantController` into `AgentSelection`, `AssistantHost`, and `AssistantSignIn`, along with their tests, and A01 changes how the agent is restarted. Find each test by its name or its assertions. Drop a bullet if lane A already removed its sleep.

## Fix

1. In `T/testing/` (T01's package), add these `internal` helpers:
   - `BlockingTest.kt`: `fun blockingTest(timeout: Duration = 10.seconds, context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.() -> Unit) = runBlocking(context) { withTimeout(timeout) { block() } }`, with KDoc saying it is for tests whose code runs on real threads and real pipes. Tests on virtual time use `runTest`.
   - `Awaiting.kt`: `suspend fun awaitTrue(timeout: Duration = 5.seconds, description: String? = null, condition: () -> Boolean)`. It runs `withContext(Dispatchers.Default) { withTimeout(timeout) { while (!condition()) delay(10) } }`, so it waits in real time even when called from `runTest` (whose virtual clock would otherwise run the whole timeout out in a few milliseconds; T02b calls it from `runTest`), and its timeout message names what it waited for. KDoc both. This is a wait for a state, the accepted replacement for a fixed sleep.
   - `IdleTrackingDispatcher.kt`: `class IdleTrackingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher()`.
     - `dispatch(context, block)` increments an `AtomicInteger` before handing `Runnable { current.set(this); try { block.run() } finally { current.remove(); pending.decrementAndGet() } }` to the delegate, where `current` is a private `ThreadLocal<IdleTrackingDispatcher?>` of the companion. Delegate `isDispatchNeeded` too.
     - A top-level `fun <T> parkedWhile(block: () -> T): T`: if the calling thread runs a block of a tracked dispatcher, it decrements that dispatcher's `pending` for the duration of `block` and increments it again in `finally`; otherwise it just runs `block`. It is for a blocking wait on something only a test or a fake can end. The code under test reads the fakes' pipes with blocking calls on the IO dispatcher it was given (`SignInSession`'s `reader.read(buffer)`, `AcpAgent`'s error reader and its `StdioTransport` input), so without this a tracked IO dispatcher never goes idle while a fake agent or sign-in is alive, and `awaitIdle` would time out in `aSecondSignInStopsTheFirst`, whose second sign-in is still running.
     - A private `ParkedInputStream(delegate: InputStream) : FilterInputStream` whose `read()`, `read(ByteArray, Int, Int)`, and `available()` go through `parkedWhile`. `FakeAgentChannel` and `FakeTerminalChannel` wrap their `fromAgent` and `errors` streams in it (`fun InputStream.parked(): InputStream` in the same file).
     - `suspend fun awaitIdle(timeout: Duration = 5.seconds)` returns once `pending` is 0 on two polls in a row, through `awaitTrue`; two, because a read that just returned is counted again only once `parkedWhile`'s `finally` runs.
     - A top-level `suspend fun awaitIdle(vararg dispatchers: IdleTrackingDispatcher)` requires every dispatcher to be idle at the same time, on two polls in a row. It checks them all again after the last one reported idle, because one can hand work to another.

     The KDoc says what idle means: no coroutine queued or running on it. A coroutine suspended on a flow or a delay is idle, which is what a negative assertion wants ("nothing is going to happen without a new event").
2. Replace the five private `test` functions with `blockingTest`. `AcpAgentTest` passes `context = EmptyCoroutineContext, timeout = TIMEOUT`. Replace `AcpAgentTest`'s private `awaitTrue` with the shared one.
3. Track the dispatchers of the tests with sleeps:
   - In `AssistantControllerTest`, or whichever post-A02a test files hold the AI-off test and `aSecondSignInStopsTheFirst`:
     - `mainThread` becomes `IdleTrackingDispatcher(Executors.newSingleThreadExecutor().asCoroutineDispatcher())`, keeping the executor dispatcher to close in `tearDown`.
     - A test-owned `io = IdleTrackingDispatcher(Dispatchers.IO)` replaces every `Dispatchers.IO` passed to the controller, `AcpAgent`, and `Assistant` there.
     - Replace `delay(200)` and `delay(100)` with `awaitIdle(mainThread, io)`.
   - In `AcpAgentTest`, the scope's `Dispatchers.Default` and the `ioDispatcher = Dispatchers.IO` become tracked dispatchers, and `delay(100)` becomes `awaitIdle(default, io)`.

   Leave the positive waits as they are. Loops such as `while (fetches.get() == 0) delay(10)` already wait for a state. Rewrite them with `awaitTrue` only where it reads better.
4. `FakeAgent`'s `"later"`: make `FakeAgentChannel`'s private `scope` reachable as `internal val agentScope`. In `support(channel)`, launch the delayed notification in `channel.agentScope` instead of `CoroutineScope(Dispatchers.Default)`. `crash()` and `stop()` already cancel that scope, so the notification dies with its agent. Lane A01 makes `FakeAgent` closable, so follow whatever owner it gives it if `agentScope` no longer fits.
5. `creator/CLAUDE.md` → Running and testing: add to the fakes bullet: "Tests on real threads use `testing/blockingTest`, and assert that nothing happened only after `awaitIdle` on the dispatchers they gave the code under test, never after a fixed delay."

## Tests

- `IdleTrackingDispatcherTest.aDispatcherIsIdleOnlyOnceItsQueuedWorkRan`: launch a block that waits on a `CountDownLatch` onto a tracked `Dispatchers.Default`. `awaitIdle` with a 200 ms timeout throws `TimeoutCancellationException`. Release the latch, and `awaitIdle` returns.
- `IdleTrackingDispatcherTest.aCoroutineSuspendedOnAFlowCountsAsIdle`: a coroutine collecting `MutableStateFlow(false).first { it }` on a tracked dispatcher. `awaitIdle` returns while it is suspended.
- `IdleTrackingDispatcherTest.aBlockingReadOfAParkedStreamCountsAsIdle`: on a tracked `Dispatchers.IO`, a coroutine reading one byte from `Pipe.open().source()` through `.parked()`. `awaitIdle` returns while it blocks; the same read without `.parked()` makes `awaitIdle` with a 200 ms timeout throw.
- `IdleTrackingDispatcherTest.awaitTrueWaitsInRealTimeUnderRunTest`: inside `runTest`, a flag a plain `Thread` sets after 100 ms; `awaitTrue(timeout = 2.seconds) { flag }` returns instead of timing out.
- `IdleTrackingDispatcherTest.workHandedToAnotherDispatcherKeepsBothBusy`: on tracked A, a block that does `withContext(B) { latch.await() }`. `awaitIdle(A, B)` times out until the latch is released.
- The rewritten assistant tests keep their names and assertions. To show they now catch background work, temporarily make `installNode()` launch its `isAiEnabled` check inside `scope.launch` (a local change, reverted): the AI-off test must still fail with `awaitIdle`, which shows that the wait covers launched work. Report the check to the orchestrator; it goes in neither the code nor the commit.
- Run `./gradlew :creator:test --tests "*ai.assistant*"` several times (`--rerun`) to check that it isn't flaky, and the whole suite once.
