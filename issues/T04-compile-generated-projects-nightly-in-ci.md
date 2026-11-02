<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Compile every generated project template, with every Block, against the just-published libraries in a nightly CI step

**Kind:** process  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** TST-3 (ACT-11)  ·  **Depends on:** none
**Files:** `.github/workflows/tests.yml`, `creator/CLAUDE.md` (Running and testing → the `KUBRIKO_CREATOR_INTEGRATION` bullet), root `CLAUDE.md` (Build configuration flags → the **Testing** paragraph that describes `Tests`)

## Problem

Nothing in CI ever compiles the Kotlin that Kubriko Creator generates. `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/template/ProjectTemplateIntegrationTest.kt` generates every template, adds Actor and Manager types that use every Event and Block, and compiles them for desktop and web. It is gated:

```kotlin
@Test
fun everyTemplateGeneratesAProjectThatCompilesForEveryTarget() {
    assumeTrue(System.getenv("KUBRIKO_CREATOR_INTEGRATION") == "true")
    val bundled = CreatorConfiguration.load()
    val configuration = System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
```

`.github/workflows/tests.yml` never sets `KUBRIKO_CREATOR_INTEGRATION`. The only test environment variable in it is `KUBRIKO_STRESS: '1'`, in the `stress-tests` job. The generator unit tests compare indented text exactly (e.g. `ActorTypeCodeGeneratorTest`'s `assertContains(…, "            Key.F5 -> {\n")`). So generated code that doesn't compile passes CI as long as its text matches, and it only fails for users, in their own project. The ungated `theIntegrationProjectUsesEveryBlockAndCondition` (TST-14, landed) keeps the integration project complete, but nothing runs the compile.

The libraries have to come from Maven Local at the commit's version, since generated code uses the current `main` API. The workflow's `publish-check` job already publishes every artifact to Maven Local under a throwaway version, on macOS:

```yaml
  publish-check:
    name: Build every published artifact
    ...
    runs-on: macOS-latest
    timeout-minutes: 60
    steps:
      ...
      - name: Publish every artifact to Maven Local
        run: ./gradlew publishToMavenLocal -Plibrary.version=0.0.0-check --no-configuration-cache
```

Decision (`decisions.md`): "Nightly CI job compiling generated code: yes."

## Fix

1. In `tests.yml`, add two steps to the `publish-check` job, after "Publish every artifact to Maven Local". They reuse that job's Maven Local publish rather than publishing again in a new job (a Linux job couldn't build the iOS artifacts the publish includes). Both run only nightly or by hand, never on pull requests, pushes, or a release's `workflow_call`, using the same condition `stress-tests` uses:
   ```yaml
      # Generates every project template with an Actor type and Manager types using every Event and Block, and
      # compiles it for desktop and web against the libraries just published, which the generator tests' exact text
      # can't prove. It downloads every dependency of a generated project, so it runs nightly, not on every change.
      - name: Compile Kubriko Creator's generated projects
        if: (github.event_name == 'schedule' || github.event_name == 'workflow_dispatch') && !inputs.unit_tests_only
        run: ./gradlew :creator:test --tests '*ProjectTemplateIntegrationTest*' --rerun --no-configuration-cache
        env:
          KUBRIKO_CREATOR_INTEGRATION: 'true'
          KUBRIKO_CREATOR_VERSION: '0.0.0-check'
      - name: Upload Kubriko Creator's integration test reports
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: creator-integration-test-reports
          path: 'creator/build/reports/tests/'
   ```
   - Raise `publish-check`'s `timeout-minutes` from 60 to 120. Each template is compiled twice (as created, then with every Block) for two targets, each in a fresh Gradle project.
   - Check two things before committing:
     - `--rerun` (Gradle 7.6+) is accepted by the repository's Gradle wrapper. If it isn't, use `./gradlew :creator:cleanTest :creator:test --tests …` instead.
     - `inputs.unit_tests_only` is false on `schedule`. Its `default: true` applies to `workflow_call` only, and `stress-tests` relies on the same behaviour, so match it exactly.
   - Creator itself builds against the libraries as projects (`projects.tools.creatorApi` and so on). Only the generated projects resolve `0.0.0-check`, from Maven Local, because `KUBRIKO_CREATOR_VERSION` sets `useMavenLocal = true`. Nothing else is needed.
2. `creator/CLAUDE.md` → the `KUBRIKO_CREATOR_INTEGRATION=true …` bullet: add "`Tests` runs it nightly (and on a manual dispatch) in its `publish-check` job, against that job's `0.0.0-check` publish." This file is edited concurrently by the docs fixer during the review, so find the bullet by its text.
3. Root `CLAUDE.md`: in the **Testing** paragraph, the sentence "…and nightly with `KUBRIKO_STRESS=1`; it also builds every published artifact for every platform on macOS (`publishToMavenLocal` under a throwaway version, …)" gains: "and, nightly, compiles every Kubriko Creator project template against that publish (`ProjectTemplateIntegrationTest`)". Keep `check_doc_names.py` passing: every name quoted must exist.

No Kotlin changes. The whitespace-exact generator assertions stay as they are. Loosening them is a separate, optional follow-up (TST-3's note) and is not part of this plan.

## Tests

None in Kotlin. Verify with a manual `workflow_dispatch` of `Tests` on the branch (`gh workflow run tests.yml --ref main` once it is pushed, which needs the user's go-ahead to push). The new step runs and the integration test reports as passed, not skipped: the test report lists `everyTemplateGeneratesAProjectThatCompilesForEveryTarget` with a duration of minutes. Then break a generated line locally (e.g. drop a `}` in one Block's emitted code), run `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<a version published locally> ./gradlew :creator:test --tests '*ProjectTemplateIntegrationTest*'`, see it fail, and revert. `python3 .github/scripts/check_license_headers.py` still passes for the workflow.

## Manual check

The first nightly run after landing: confirm in the Actions tab that "Compile Kubriko Creator's generated projects" ran, rather than being skipped, and passed.
