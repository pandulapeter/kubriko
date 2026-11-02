<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Let the session server take messages and send commands only on a connection that said Hello, and only on one per launched game

**Challenged:** amended — over the pending limit the oldest waiting connection is closed, not the newest, so a few idle local connections can no longer lock the launched game out; a test pins it.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all (any local user or process on the machine)
**Findings:** INF-14  ·  **Depends on:** none
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/session/SessionServer.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/session/SessionChannel.kt`, `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/SessionController.kt`, tests `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/session/SessionServerTest.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/FakeSessionChannel.kt`, `workspace/WorkspaceControllerTest.kt` (only if it asserts on the channel); `creator/CLAUDE.md` (the sentence on `SessionServer`, see Fix 4)

## Problem

`session/SessionServer.kt` listens on `ServerSocket(0, 0, InetAddress.getLoopbackAddress())` and treats every connection as the session:

```kotlin
private suspend fun Socket.readMessages() = use {
    val writer = getOutputStream().bufferedWriter(Charsets.UTF_8)
    latestWriter = writer
    val reader = getInputStream().bufferedReader(Charsets.UTF_8)
    while (true) {
        ...
        decode(line)?.let { _messages.emit(it) }
    }
    if (latestWriter === writer) latestWriter = null
}
```

Any local process that finds the port (a scan of loopback ports is quick) becomes `latestWriter` the moment it connects, so the commands Creator sends to the game (and the AI's `press_keys`, `capture_game`, …) go to it instead, and its lines are emitted as the game's: a fake `Screenshot` or `CommandDone` reaches the AI, fake `Log` lines reach the console, and a fake `Hello` flips the toolbar to Playing. Nothing limits the number of connections.

The decision (`decisions.md`: "Session channel: Creator-only fix now (accept a connection only after its Hello, one per launch); token with the next protocol bump"): no protocol or `tool-creator-session` change in this plan.

## Fix

1. **SessionChannel** gains one member:

   ```kotlin
   /**
    * Starts waiting for the session of a game being launched: the first connection that says Hello after this becomes
    * the session, and connections after it are closed, until the next launch.
    */
   fun expectSession()
   ```

   Update `send`'s KDoc to "Sends [command] to the session of the current launch."

2. **SessionServer** state, all guarded by one `private val lock = Any()` (the accept loop and readers run on IO threads):
   - `private var isExpecting = false`, `private var session: BufferedWriter? = null` (replaces `latestWriter`), and the session's `Socket` so `expectSession()` can close a session left over from the previous launch.
   - `expectSession()`: under the lock, close the previous session socket if any (its reader ends on the closed socket and cleans up), set `session = null`, `isExpecting = true`.
   - Accept loop: unchanged except that the per-socket coroutine applies a Hello deadline: `socket.soTimeout = HELLO_TIMEOUT_IN_MILLISECONDS` (10 s) before the first `readLine`, reset to `0` once Hello arrived. A connection that sends nothing or garbage until the deadline is closed. Keep at most `MAXIMUM_PENDING_CONNECTIONS` (4) sockets waiting for their Hello (an `ArrayDeque<Socket>` under the lock; a socket leaves it on its Hello, its deadline, or its end): when one more is accepted, close the oldest waiting one, so the loop can't be made to hold unbounded sockets and threads. Closing the oldest rather than refusing the newest matters: the launched game connects last and says Hello at once (`CreatorSessionLauncher.main` sends it right after connecting), so a process holding four idle connections can't keep it out.
   - `readMessages`: read lines until the first one that decodes to `SessionMessage.Hello`; lines before it are dropped, not emitted (a real session's first line is always its Hello: `CreatorSessionLauncher.main` sends it before anything else, in every protocol version since 1). On that Hello, under the lock: if `isExpecting`, this socket becomes the session (`session = writer`, `isExpecting = false`) and the Hello and every later line are emitted as today; otherwise close the socket without emitting anything. On end of stream, under the lock, `if (session === writer) session = null`.
   - `send`: reads `session` under the lock, then writes under `synchronized(writer)` as now.
   - `close()`: close the server socket and the session socket.
   - `messages` must still only carry the current session's lines: since only the session socket emits, that holds.
   - Each reader must survive anything one line can throw: `decode` already maps `SerializationException`/`IllegalArgumentException` to null; a `SocketTimeoutException` from the deadline is an `IOException`, already caught by the existing `catch (_: IOException)`, which ends that reader only.

3. **SessionController.play**: call `channel.expectSession()` inside the launched job, after `previousJob?.join()` and before `prepareLaunch()` (so it is in order with the previous game's end, which already closed its connection), for both the direct launch (`runGame`) and the Gradle session task. `restart()` goes through `play`, so it is covered. Nothing else calls the channel's connection logic.

   Residual risk, which the decision accepts until the protocol token: a local process that connects and says Hello faster than the launched game during the launch window becomes the session; the real game's connection is then closed, its `Hello` never arrives, and the toolbar shows the impostor's mode. Put this in one sentence of the `SessionServer` KDoc, with "a token in the session protocol's next version closes it" — and nothing about the token anywhere else.

4. **creator/CLAUDE.md:** where the Creator paragraph says "reporting back over a versioned JSON-lines protocol on a localhost port, `SessionServer`", add "(which takes only the first connection that says Hello after each launch)". Keep it to that clause.

5. **FakeSessionChannel:** implement `expectSession()` by counting calls (`var expectedSessions = 0`).

No string, protocol, or published-artifact change.

## Tests

`SessionServerTest` (real localhost sockets, as the existing tests; every wait under `withTimeout(10_000)`):

- Update `messagesOfEveryConnectedSessionAreEmittedAndGarbageIsSkipped` into `onlyTheExpectedSessionsLinesAreEmittedAndGarbageBeforeItsHelloIsSkipped`: `server.expectSession()`, a socket writes `not json\n{"type":"unknown"}\n<hello>\n<log>`, a second socket writes `<failure>`; the received messages are exactly `[hello, log]`, and the second socket reads end of stream (closed by the server).
- `aConnectionBeforeAnyLaunchIsClosedWithoutEmitting`: no `expectSession()`; a socket sends a Hello; it reads end of stream, and `server.messages` emits nothing within a short window (collect with `withTimeoutOrNull(500)` → null), and `send(...)` returns false.
- `commandsGoOnlyToTheSession`: expect, connect A with a Hello, connect B with a Hello; `send(command)` is true and A reads the command; B reads end of stream. Replaces `commandsGoToTheSessionThatConnectedLast`.
- `aConnectionThatNeverSaysHelloIsClosed`: construct the server with an internal `helloTimeoutInMilliseconds = 200` parameter (constructor default 10 s); a socket that sends nothing reads end of stream within the test timeout; a launched session's Hello afterwards is still accepted.
- `anIdleConnectionMakesRoomForANewOne`: expect, open five sockets that send nothing, then a sixth that says Hello: the first two read end of stream (the fifth and the sixth each made room), and the sixth's Hello is emitted and `send` reaches it.
- `theNextLaunchReplacesTheSessionOfTheLastOne`: expect, A says Hello, expect again, B says Hello: A is closed, `send` reaches B.
- `messagesArriveWhileTheSessionKeepsItsConnectionOpen` and `theServerListensOnLocalhostUntilClosed`: keep, with `expectSession()` first in the former.

`WorkspaceControllerTest` or a `SessionController` case already driving `play()`: assert `FakeSessionChannel.expectedSessions` is 1 after `play()` and 2 after `restart()`.
