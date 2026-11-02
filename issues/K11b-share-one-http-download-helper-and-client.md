<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Download through one shared HTTP helper and client instead of four copies that build a new client per request.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** AI-16 (the HTTP part)  ·  **Depends on:** K11a (order only)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/foundation/HttpDownloads.kt`, `context/KubrikoSources.kt`, `ai/assistant/AgentRegistryStore.kt`, `ai/assistant/NodeRuntime.kt`, `ai/assistant/BinaryAgentInstaller.kt`, test new `foundation/HttpDownloadsTest.kt`

Lane A edits `ai/assistant/` (A01–A04); here only the private `download`/`httpDownload` functions of three files there change.

## Problem

Four identical blocks, each building a new `HttpClient` (with its own selector thread and connection pool) per request:

```kotlin
// context/KubrikoSources.kt, ai/assistant/AgentRegistryStore.kt, ai/assistant/NodeRuntime.kt, ai/assistant/BinaryAgentInstaller.kt
            val client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build()
```

followed by slightly different status checks and interrupt handling: two return `String?` on any failure (`AgentRegistryStore.download`, `NodeRuntime.download`), two throw `IOException("The download of $url failed (HTTP …)")` and handle `InterruptedException` by re-interrupting and throwing (`KubrikoSources.httpDownload`, `BinaryAgentInstaller.httpDownload`, which also streams with progress).

## Fix

1. `foundation/HttpDownloads.kt`, `internal object HttpDownloads`:
   - one lazily built client (`HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()`, 30 s connect timeout; `HttpClient` is thread-safe and meant to be shared);
   - `fun <T> get(url: String, bodyHandler: HttpResponse.BodyHandler<T>, timeout: Duration? = null): HttpResponse<T>`: sends a GET, turns `InterruptedException` into re-interrupting the thread and throwing `IOException("The download of $url was interrupted.")`, and throws `IOException("The download of $url failed (HTTP $code).")` for a status other than 200 (closing an `InputStream` body first, and deleting a `Path` body's file);
   - `fun text(url: String, timeout: Duration): String?`: `get` with `ofString()`, null on `IOException` (the two optional callers).
   - Blocking, as all four callers already are (each runs on IO). KDoc says so.
2. Callers: `AgentRegistryStore.download()` and `NodeRuntime.download()` → `HttpDownloads.text(url, timeout)`; `KubrikoSources.httpDownload` → `HttpDownloads.get(url, BodyHandlers.ofFile(target.toPath()))`; `BinaryAgentInstaller.httpDownload` → `HttpDownloads.get(url, BodyHandlers.ofInputStream()).body().use { … copyReporting … }`, keeping its progress code. Per-request timeouts stay what each caller had (15 s registry, NodeRuntime's own, none for the two large downloads).
3. Behaviour and messages stay the same (the exception texts above are today's).

## Tests

`HttpDownloadsTest` against a real `com.sun.net.httpserver.HttpServer` on `localhost:0` (no internet):
- `textReturnsTheBody` (200) and `textIsNullOnAnError` (404, and a closed port);
- `getThrowsWithTheStatus`: 500 → `IOException` whose message contains `HTTP 500`, and a `Path` body's file doesn't exist afterwards;
- `getFollowsRedirects`: 302 to a second path → its body;
- `anInterruptedDownloadKeepsTheInterrupt`: interrupt the calling thread before `get` against a handler that sleeps → `IOException`, and `Thread.interrupted()` is true afterwards (clear it in the test).
Existing `NodeRuntimeTest`/`BinaryAgentInstallerTest`/`KubrikoSourcesTest` (if they use injected downloaders) stay green.
