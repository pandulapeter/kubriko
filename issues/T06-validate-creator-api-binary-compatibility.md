<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Check tool-creator-api's binary API against a committed ABI dump in every Tests run, and make the docs say creator-api keeps its binary API

**Challenged:** amended — DOC-23 already landed (61aec7629 removed the "promise no binary compatibility" clause from `creator/CLAUDE.md` and keeps the rule in the root `CLAUDE.md` only), so `creator/CLAUDE.md` is no longer edited and the clause goes to the root file's `creator-api` line alone.

**Kind:** process  ·  **Severity:** low  ·  **Platforms:** all (the dump covers the JVM targets: desktop and Android)
**Findings:** TST-15 (DOC-23's rewording landed during the review)  ·  **Depends on:** T04 (both edit `.github/workflows/tests.yml`; same lane, land in order)
**Files:** `tools/creator-api/build.gradle.kts`, `tools/creator-api/api/**` (new, generated dump), `.github/workflows/tests.yml`, `tools/creator-api/README.md`, root `CLAUDE.md` (Tools → `creator-api` line), `.claude/skills/code-style/SKILL.md` ("Published modules keep their binary API" rule, one clause)

## Problem

`tool-creator-api` is the contract between a game and Kubriko Creator. Games compile an implementation of `CreatorEntryPoint` (`tools/creator-api/src/commonMain/kotlin/com/pandulapeter/kubriko/creator/CreatorEntryPoint.kt`), and Creator loads it through `ServiceLoader` from the project's classpath at the project's Kubriko version. A binary-incompatible change therefore fails at runtime in the user's project, not at compile time in this repository. The promise is only written down:
- root `CLAUDE.md`: "Members are only ever added with default implementations."
- `tools/creator-api/README.md`: "New members always come with default implementations, so an implementation written for an older version keeps working."

No module applies ABI validation: nothing in `gradle/` or any build file, and no `*.api` dump. The docs no longer contradict each other: during the review `creator/CLAUDE.md` lost its "`scene-editor-api` and `creator-api` promise no binary compatibility across versions" clause, and its Library side section now defers to the root `CLAUDE.md`'s public API rules, so `.claude/skills/code-style/SKILL.md`'s "**Published modules keep their binary API.** … (those applying `kubriko-public-artifact`)" covers `creator-api`, which applies that plugin. What is missing is a check.

Decision (`decisions.md`): "ABI validation for creator-api; session protocol guarded by the literal test (TST-2)." TST-2 landed (`SessionProtocolTest`'s literal lines), so `tool-creator-session` needs no ABI dump: its public surface is the wire format.

## Fix

1. **Enable Kotlin's built-in ABI validation for `tools/creator-api` only**, in its own `build.gradle.kts`, not in a convention plugin (the decision covers this one module). The Kotlin Gradle plugin is 2.4.21 (`gradle/libs.versions.toml`), which has ABI validation built in. Its DSL is experimental:
   ```kotlin
   @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
   kotlin {
       abiValidation {
           enabled.set(true)
       }
       ...
   }
   ```
   Verify the exact DSL and task names for 2.4.21 before writing anything else. Use the Kotlin docs ("Check the binary compatibility of your library" / ABI validation) and `./gradlew :tools:creator-api:tasks --all | grep -i abi`. Older 2.2 releases named the tasks `checkLegacyAbi`/`updateLegacyAbi`, and later ones may name them `checkKotlinAbi`/`updateKotlinAbi`. Use whatever 2.4.21 has, and write that name in steps 3 and 4.
   - **Scope: the JVM targets** (`desktop`, Android). Creator loads the entry point only on the desktop JVM, and Android consumers link against the same JVM bytecode. Turn klib (iOS and Wasm) dumps off if the DSL allows it (`klib { enabled.set(false) }` or its 2.4 equivalent). Otherwise keep them, as long as `update…` on Linux CI can infer the Apple targets without failing. Check this by running the check task on a machine or job without Xcode; if it can't, turn klib off.
   - Alternative not taken: the standalone `org.jetbrains.kotlinx.binary-compatibility-validator` plugin. The built-in one needs no new dependency or plugin-portal entry.
2. **Generate and commit the dump**: run `./gradlew :tools:creator-api:<update task> --no-configuration-cache` and commit `tools/creator-api/api/…` exactly as generated. The dump has no MPL header, and `check_license_headers.py` doesn't check `.api` files (its `CHECKED_EXTENSIONS` lists `.kt`, `.kts`, `.py`, `.yml`, …). Leave the file as generated. Read the dump: it must list `CreatorEntryPoint` with `createSerializationManager`, `createCustomManagers`, and `getRunningInstances`, and the default-method bridge (`CreatorEntryPoint$DefaultImpls`, or none if the module compiles with `-Xjvm-default=all`). Note in the commit report which one it is, since it decides whether adding a defaulted member stays binary compatible for implementers compiled against an older version.
3. **CI**: in `tests.yml` → `unit-tests` → "Run unit tests", append the check task to the existing Gradle invocation, so a failure is reported with the others under `--continue`:
   `./gradlew spotlessCheck desktopTest :creator:test :tools:creator-api:<check task> --continue --no-configuration-cache`.
   Leave `.githooks/pre-commit` alone. It runs only quick checks, and an ABI change shows up in the dump's diff anyway.
4. **Docs.** Leave `creator/CLAUDE.md` alone: its Library side section points to the root `CLAUDE.md` for library rules, and 61aec7629 keeps each such rule in one place.
   - Root `CLAUDE.md`, Tools → `creator-api` line: after "Members are only ever added with default implementations.", add "Its binary API is checked against the committed dump in `tools/creator-api/api/`; `:tools:creator-api:<update task>` rewrites it when it changes on purpose, and that diff is reviewed."
   - `tools/creator-api/README.md`: one sentence after "New members always come with default implementations…": "The module's binary API is checked against `api/` on every pull request; a change to it updates that file in the same commit."
   - `.claude/skills/code-style/SKILL.md`, in the "Published modules keep their binary API" rule: append "`tool-creator-api`'s is checked by ABI validation (its `api/` dump)." Keep `check_doc_names.py` passing.
5. No change to `CreatorEntryPoint` or any published API. This plan adds a check only. It does not touch `tools/creator-session`.

## Tests

None in Kotlin. To verify, run the check task, which passes on the committed dump. Then add `fun extra(): Int` without a default to `CreatorEntryPoint` locally: the check task fails and names the change. Revert it. `python3 .github/scripts/check_doc_names.py` passes.
