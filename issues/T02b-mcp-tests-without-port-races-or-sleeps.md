<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give the MCP tests ports outside the system's ephemeral range instead of a just-released free port, replace their fixed sleeps with waits for a state, and move FakeProcessRunner into the shared testing package

**Challenged:** amended — header matched to EXECUTION.md (lane A, phase 1, after T02a); the `awaitTrue` in `runTest` relies on T02a's real-time wait, now stated.

**Kind:** tests  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** TST-8 (MCP part), TST-6 (`FakeProcessRunner`)  ·  **Depends on:** T02a (`testing/Awaiting.kt`, in the same lane), A04 (runs last in lane A, as EXECUTION.md orders it). Like T02a it only adds to T01's `testing` package, which lane A's worktree doesn't have yet
**Files:** (T = `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator`)
- New: `T/testing/TestPorts.kt`, `T/testing/TestPortsTest.kt`, `T/testing/FakeProcessRunner.kt` (moved)
- Deleted (moved): `T/mcp/clients/FakeProcessRunner.kt`
- Rewritten: `T/mcp/McpControllerTest.kt`, `T/mcp/clients/AiClientsControllerTest.kt`, and, for the `FakeProcessRunner` import, `T/ai/assistant/AssistantControllerTest.kt` or the post-A02a file that builds the runner (`grep -rln FakeProcessRunner creator/src/test`)

## Problem

`McpControllerTest` picks a port by binding port 0 and letting it go, then has the controller bind it later:

```kotlin
private fun freePort() = ServerSocket(0).use { it.localPort }
```

`AiClientsControllerTest.controller()` does the same: `created.setPort(ServerSocket(0).use { it.localPort })`. Other tests in the same JVM bind port 0 meanwhile: `McpToolServerTest`, `SessionServerTest`, and the ktor clients. The system can hand one of them the port just released, and the controller then reports `McpServerState.Failed`. That gives a flaky failure that has nothing to do with the change under test.

Port 0 itself can't be used through the controller. `McpController.setPort` refuses it (`if (port !in McpSettings.portRange) return false`, with `portRange = 1024..65535`), `McpSettingsStore.load()` discards stored settings outside that range, and `AiClientsController` writes `settings.url`, the configured port, into the AI tools' configurations. The test needs a real fixed port.

`McpControllerTest` also sleeps before two assertions:
- `turningAiFeaturesOffStopsTheServerAndTurningThemOnStartsItAsItWas`, after AI features are off:
  ```kotlin
  controller.setPort(port)
  controller.setEnabled(true)
  withContext(Dispatchers.IO) { Thread.sleep(200) }
  assertFalse(isListening(port))
  ```
- `settingsAreKeptAndANewTokenReplacesTheOldOne`, before checking the save:
  ```kotlin
  withContext(Dispatchers.IO) { Thread.sleep(200) }
  assertEquals(controller.settings.value, store.load())
  ```

`quickChangesEndWithOnlyTheLastServerRunning` already takes its ports above 24 000 with `unusedPortAbove`, and sleeps `round % 4L` on purpose to vary the timing. Leave it alone apart from reusing the shared helper.

## Fix

1. `T/testing/TestPorts.kt`: `internal class TestPorts(private val range: IntRange)` with `fun next(): Int`, and `companion object { val shared = TestPorts(20_000 until 24_000) }`, which the tests use as `TestPorts.shared.next()`. It hands out ports from a fixed range outside every OS's ephemeral range (Linux 32768–60999, macOS and Windows 49152–65535): `20_000 until 24_000`. An `AtomicInteger` counter walks the range, and each candidate is checked by binding `ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))` and closing it. The function skips taken ports and throws after a full lap. KDoc: why the range (nothing else in the test JVM binds there, since port-0 binds stay in the ephemeral range), and that it is for code that needs a fixed port (`McpController`). Code that can bind port 0 itself should.
2. `McpControllerTest`:
   - `freePort()` becomes `TestPorts.shared.next()`.
   - `unusedPortAbove` stays for `quickChangesEndWithOnlyTheLastServerRunning`. Its `RESERVED_PORTS_START = 24_000` must not overlap `TestPorts`' range, which ends at 24 000.
   - In `turningAiFeaturesOff…`, replace the sleep with `assertEquals(McpServerState.Off, controller.enable())`. `enable()` is documented to return "once it has started or failed to, or stayed off because AI features are off", then the test asserts `assertFalse(isListening(port))`.
   - In `settingsAreKept…`, replace the sleep with `awaitTrue(description = "the settings were saved") { store.load() == controller.settings.value }`. `store.load()` reads the file, so this waits for the real write. The test runs in `runTest`; T02a's `awaitTrue` switches to `Dispatchers.Default` so that it polls in real time rather than on the virtual clock, which would end the 5-second timeout before the save on the IO thread is done.
3. `AiClientsControllerTest.controller()`: `created.setPort(TestPorts.shared.next())`. Its Thread.sleep(20) polling loops already wait for a state; rewrite them with `awaitTrue` where they're inside `withContext(Dispatchers.IO) { withTimeout(5_000) { while (…) Thread.sleep(20) } }`.
4. Move `FakeProcessRunner` from `mcp/clients/` to `testing/`. Change only its package line, then update the imports: `AiClientsControllerTest` gains one, and the assistant test changes its import. Run `./gradlew spotlessApply`.
5. No production change. The alternative, allowing port 0 in `McpController` and `McpSettingsStore` behind a test-only range parameter, was rejected: it adds production seams, and `AiClientsController` would still write the configured port 0 into client configurations.

## Tests

- `TestPortsTest.portsAreDistinctAndBindable`: 20 calls give 20 distinct ports, all inside `20_000 until 24_000`, and each can be bound on 127.0.0.1.
- `TestPortsTest.aTakenPortIsSkippedAndAFullRangeThrows`: with `TestPorts(p until p + 2)`, where `p` comes from `shared.next()`, bind `p` on 127.0.0.1. `next()` returns `p + 1`. With `p + 1` also bound, the next call throws `IllegalStateException`.
- The rewritten MCP tests keep their names and assertions. Run `./gradlew :creator:test --tests "*mcp*" --rerun` several times to check that they aren't flaky.
