<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's MCP server and in-app assistant (`mcp`, `ai`) into `:creator:ai`

**Challenged:** amended — moves T02b's `FakeProcessRunner` (a fake of `mcp.clients.ProcessRunner`, in the app's `testing` package) into this module; the other T02 helpers come from foundation's fixtures (M03).

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** Modularization proposal (ai = mcp + assistant)  ·  **Depends on:** M11
**Files:**
- `settings.gradle.kts`; new `creator/ai/build.gradle.kts`; `creator/build.gradle.kts`
- moved (main and test): `mcp/**` (incl. `mcp/clients`), `ai/**` (incl. the package guide `ai/assistant/CLAUDE.md`) → `creator/ai/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/…`
- importers of newly public declarations (visibility only); `PackageLayeringTest.kt`
- docs: `creator/CLAUDE.md` (the intro's path to the assistant guide; Modules list; the live-agent test command if it names a task), `creator/plan/HANDOVER.md` (any live-agent command naming `:creator:test`)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

`mcp/` (17 files) and `ai/assistant/` (54) import only `commands`, `context`, `console`, and `foundation` among Creator's packages
(`ai/assistant -> mcp` is inside the layer), and own the heaviest third-party dependencies of the app:

```kotlin
// creator/build.gradle.kts
    implementation(libs.acp)
    implementation(libs.ktor.server.cio)
    implementation(libs.mcp.server)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.mcp.client)
```

The app's UI (`ui/assistant`, `ui/mcp`, `AppController`, H03's `AppGraph`) uses the controllers. The assistant's fakes
(`FakeAgent`, `FakeAgentLauncher`, `FakeSecretStore`, `FakeTerminalLauncher`) were used only by `ai/assistant` tests at `ecf76faf6`.

## Fix

1. `settings.gradle.kts`: `":creator:ai",`. `creator/ai/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.commands)
       implementation(libs.acp)
       implementation(libs.ktor.server.cio)
       implementation(libs.mcp.server)
       testImplementation(testFixtures(projects.creator.commands))
       testImplementation(libs.ktor.client.cio)
       testImplementation(libs.mcp.client)
   }
   ```
   (`api` for a library only if a public signature exposes its types: the ACP SDK's types appear in `AcpAgent`'s internals; keep
   them out of the public surface.) Remove the five lines above from `creator/build.gradle.kts` once nothing in the app imports
   `com.agentclientprotocol`, `io.ktor`, or `io.modelcontextprotocol` (`grep -rln` the app's sources and tests), and add
   `implementation(projects.creator.ai)`.
2. `git mv` both packages, their tests, and `ai/assistant/CLAUDE.md`. T02b moved `FakeProcessRunner` (it fakes
   `mcp.clients.ProcessRunner`) into the app's `creator/src/test/kotlin/…/testing/`: move it to `creator/ai/src/testFixtures/…/testing/`
   (`public`) if an app test still uses it, else to `creator/ai/src/test/…/testing/`. `blockingTest`, `awaitTrue`,
   `IdleTrackingDispatcher`, and `TestPorts` are in foundation's fixtures since M03 and reach this module through commands' fixtures
   (`testFixturesApi(testFixtures(projects.creator.foundation))`); `grep -rh "import com.pandulapeter.kubrikoCreator.testing"` the
   moved tests and confirm each name is in a fixture this module sees. If an app test (`ui/assistant/*Test`, `AiFeaturesOffTest`)
   uses a fake of this module at execution, move that fake to `creator/ai/src/testFixtures/…` (`public`) and depend on it from the
   app; otherwise the fakes stay in `creator/ai/src/test`.
3. **API.** Public: what the app constructs or observes (`McpController`, `McpSettingsStore`, `AiClientsController`, the
   assistant's controllers and state types after A02's split, `AgentChoice`, …). Keep the ACP adapter, the permission handler,
   the registry parsing, and the installers `internal`.
4. `PackageLayeringTest`: the ai layer's files must all be under `creator/ai/src`.
5. Docs: `creator/CLAUDE.md`'s intro path becomes `ai/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`;
   the live-agent tests run as `./gradlew :creator:ai:test --tests "*AssistantLiveScenariosTest*" --rerun` (update it in
   `creator/CLAUDE.md` → Running and testing and anywhere in `creator/plan/` that quotes `:creator:test` with it); add `ai` to the
   Modules list.

## Tests

- `:creator:ai:test` runs the moved tests (AI and MCP on localhost, T02's virtual-time tests).
- With `KUBRIKO_CREATOR_CLAUDE_AGENT` unset, the live tests are skipped through `Assume`, as before.
- `./gradlew :creator:allTests` with the same test count as before.

## Manual check

`./gradlew :creator:run` with AI features on: the MCP server starts on its port (a client connects), and the assistant panel
starts Claude Agent. The packaged app (`createDistributable`) still contains `java.net.http` (the `modules(...)` line stays in the
app's build).
