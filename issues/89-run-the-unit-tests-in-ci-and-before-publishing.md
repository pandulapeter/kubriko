# Run the unit tests on every push and pull request, nightly in stress mode, and before publishing the library

**Challenged:** amended — `actions/upload-artifact@v7` (the version `showcase-publish-desktop.yml` already uses) instead of `@v4`; the two jobs upload under different artifact names, because artifact names must be unique within a run and a nightly with both jobs failing would otherwise fail its second upload; upload steps spelled out with `if: failure()`; timeouts raised to 45/90 minutes for a cold build of every module on a hosted runner; `--no-configuration-cache` kept with its reason.

**Kind:** build  ·  **Severity:** high  ·  **Platforms:** CI only
**Files:** `.github/workflows/tests.yml` (new), `.github/workflows/library-publish.yml`, root `CLAUDE.md` (one sentence in the Testing paragraph)

**Part of the testing extension.** It is the last plan of lane E, so it lands after every other lane has merged and
all of the sweep's tests exist. `.github/` is owned by no other lane.

## Problem

After this sweep the repository has roughly a hundred test classes, and none of the workflows runs them. The six
workflows are all publishing jobs (`library-publish.yml`, `showcase-publish-*.yml`) or Claude review jobs.
`library-publish.yml` goes straight from checkout to Maven Central:

```yaml
      - name: Publish to MavenCentral
        run: ./gradlew publishToMavenCentral --no-configuration-cache
```

So a failing test would not stop a release, and a regression only surfaces when someone happens to run
`./gradlew desktopTest` locally. The randomized suites (`29`, and the stress sizes it reads from `KUBRIKO_STRESS`)
catch rare interleavings only if they run often.

## Fix

1. **New `.github/workflows/tests.yml`**, named `'[Library] Tests'` in the naming style of the existing workflows:
   - Triggers: `push` to `main`, `pull_request`, `workflow_dispatch`, and `schedule: - cron: '17 3 * * *'` (nightly,
     off the hour).
   - `permissions: contents: read`.
   - Job `unit-tests`, on `ubuntu-latest`, with `timeout-minutes: 45` (a cold compile of every module's JVM main and
     test sources, the examples and `app/shared` included, on a hosted runner):
     - `actions/checkout@v5`, then `actions/setup-java@v5` with `distribution: 'zulu'`, `java-version: 21` and
       `cache: 'gradle'` (as in `showcase-publish-web.yml`).
     - `./gradlew desktopTest --continue --no-configuration-cache`. `--continue` reports every failing module in one
       run. `--no-configuration-cache` matches the publish command and keeps `KUBRIKO_STRESS` out of any cached
       configuration; the configuration cache is not enabled in `gradle.properties`, so it costs nothing.
     - `node engine/src/webMain/checkTriangleBridge.mjs`. It has no dependencies, and `ubuntu-latest` ships Node.
     - A step with `if: failure()` running `actions/upload-artifact@v7` with `name: test-reports` and
       `path: '**/build/reports/tests/'`.
   - Job `stress-tests`, only `if: github.event_name == 'schedule' || github.event_name == 'workflow_dispatch'`, with
     the same setup and `timeout-minutes: 90`. It runs `./gradlew desktopTest --continue --no-configuration-cache`
     with `env: KUBRIKO_STRESS: '1'` and uploads reports the same way on failure, as `name: stress-test-reports`
     (artifact names must be unique within one workflow run, and both jobs run on the nightly schedule). The runner
     starts from a fresh checkout and the repository enables no Gradle build cache, so the test tasks really run
     rather than being restored from the `unit-tests` job.
2. **`library-publish.yml`:** add a step `Run unit tests` running `./gradlew desktopTest --no-configuration-cache`
   between "Set up JDK 21" and "Publish to MavenCentral". A failing test then stops the release before anything is
   uploaded. The job stays on `macOS-latest` (it needs a Mac for the iOS artifacts; the JVM tests run there as well),
   and its existing `@v4` action versions are left alone, since this plan does not touch the publishing steps.
3. **Root `CLAUDE.md` → Testing:** add one sentence. `[Library] Tests` runs `desktopTest` on every push to `main`
   and every pull request, and nightly with `KUBRIKO_STRESS=1`. `[Library] Publish` refuses to publish when a test
   fails.

## Tests

No unit test. Validate the YAML locally, with `actionlint` if it is installed, otherwise with
`python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1]))" .github/workflows/tests.yml`. Also run the exact
commands from both jobs locally, `./gradlew desktopTest --continue --no-configuration-cache` and the node check,
so the first CI run is not also the first time they run together.

## Manual check

After the push, the user opens the Actions tab and confirms that `[Library] Tests` ran green on `main`. They then
dispatch it once by hand to see `stress-tests` run. If the desktop tests fail on Linux only because
`Dispatchers.Main` (the Swing event dispatch thread) cannot start headless, add
`tasks.withType<Test>().configureEach { systemProperty("java.awt.headless", "true") }` to
`configureKotlinMultiplatform` in a follow-up commit, and re-run. The event queue works in headless mode, so this is
not expected.
