# Executing the fourth review sweep

You are the orchestrating agent. The user started you with *"Follow `issues/EXECUTION.md`."* Read `issues/README.md`
first and the `codebase-review` skill's section 4. Repository: `/Users/pandulapeter/Projects/Kubriko`, branch `main`.
Never create a branch, merge, rebase, push, bump `library.version`, publish, or dispatch a workflow.

## 1. Preconditions — stop and tell the user if any fails

1. `git status --short` shows nothing outside `issues/`.
2. Every plan carries a `**Challenged:**` line and none says `DROP`.
3. Every row of the README's **Decisions** table has an answer recorded. Carry out each plan with the answered
   option; where the answer is not the plan's recommended option, the plan's Fix/Decision section describes it.
4. `./gradlew desktopTest` passes.

## 2. Before the lanes

If `issues/` is uncommitted, load `commit-messages` and commit it: `Add the fourth review plans.` Then
`START=$(git rev-parse HEAD)`.

## 3. Lanes

Seven lanes, each in its own **detached** worktree next to the checkout, cut from `START`:

```
git worktree add --detach ../Kubriko-lane-<x> $START
```

| Lane | Plans, in order | Tests | Compile checks |
|---|---|---|---|
| E | 01, 02, 03, 04, 05, 06, 07 | `:engine:desktopTest` | `:engine:build` (all four platforms: 05 changes an internal expect) |
| U | 10, 11, 12, 13, 14, 15, 16 | `:plugins:audio-playback:desktopTest` | `:plugins:audio-playback:build` |
| I | 20, 21, 22, 23, 24, 25, 26, 27, 28 | `:plugins:gamepad-input:desktopTest :plugins:keyboard-input:desktopTest :plugins:pointer-input:desktopTest` | `:plugins:gamepad-input:build :plugins:keyboard-input:build :plugins:pointer-input:build` |
| R | 35, 36, 37, 38 | `:plugins:persistence:desktopTest :plugins:sprites:desktopTest :plugins:shaders:desktopTest` | the same modules' `:build` |
| T | 40, 41, 42, 43, 44, 45 | `:tools:scene-editor:desktopTest :tools:debug-menu:desktopTest` | `:tools:scene-editor:build :tools:debug-menu:build`, plus `:tools:debug-menu-api:build :tools:debug-menu-noop:build` if 43's answer is the overload |
| X | 60, 61, 62, 63, 64, 65 | `desktopTest` of `:examples:shared`, `:examples:game-wallbreaker`, `:examples:demo-physics`, `:examples:game-space-squadron`, `:examples:game-annoyed-penguins`, `:examples:demo-shader-animations`, `:examples:demo-performance` | the same modules' `:build` |
| S | 50, 51, 52, 53, 54 | — | `:app:android:assembleDebug`, `:app:desktop:compileKotlin`, `:app:web:wasmJsBrowserDistribution`; 53: `:app:ios:compileKotlinIosSimulatorArm64` and, if Xcode is available, `xcodebuild -project app/ios/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug build CODE_SIGNING_ALLOWED=NO` |

Spawn one `general-purpose` subagent per lane, in one message, but **at most two Gradle builds run at once**
repo-wide: give every lane the semaphore script below and start S last (it is lightest and builds the app). A KGP
"FqNames can't be derived from DirtyData" error is a stale incremental cache — rerun before calling a lane red.

Semaphore (write it to `<scratchpad>/gradle-slot.sh` if it is not there; lanes call it instead of `./gradlew`):

```bash
#!/bin/bash
# gradle-slot.sh <dir> <gradle args...> — runs ./gradlew in <dir> holding one of two global slots.
D=<scratchpad>/gradle-slots; mkdir -p $D; dir=$1; shift
while true; do for s in 1 2; do if mkdir $D/slot$s 2>/dev/null; then
  trap "rmdir $D/slot$s" EXIT; cd "$dir" && ./gradlew "$@"; exit $?; fi; done; sleep 10; done
```

### Lane subagent prompt (fill in `<x>`, `<plans>`, `<tests>`, `<compile>`, `<worktree>`)

> You carry out lane `<x>` of Kubriko's fourth review sweep, plans `<plans>` in that order, in the detached worktree
> `<worktree>` (absolute path). Read `<worktree>/issues/README.md` (Decisions — the answered option for each of your
> plans), then per plan:
> 1. Read the plan and every file it names. Load the `code-style` skill before your first edit.
> 2. Implement exactly the plan with the answered option. Re-locate moved code by the quoted snippet. If the plan is
>    wrong, impossible, or its own "drop this plan if" condition holds, do not improvise another fix: revert the
>    working tree (`git -C <worktree> checkout -- . && git -C <worktree> clean -fd -- ':!issues'`), leave the plan
>    file, note why, and move on.
> 3. Add the tests it asks for; update the CLAUDE.md / README / documentation files it names.
> 4. Verify: `<scratchpad>/gradle-slot.sh <worktree> <tests>` and `<scratchpad>/gradle-slot.sh <worktree> <compile>`.
>    Manual checks that need a device, a browser, a store or a second monitor are skipped and reported.
> 5. `git -C <worktree> rm issues/NN-*.md` in the same commit as the fix.
> 6. Load the `commit-messages` skill; commit with one `-m` sentence — one plan, one commit. Then:
>    ```
>    test "$(git -C <worktree> cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
>    git -C <worktree> log -1 --format=%B | grep -qiE 'co-authored|generated with|claude code|anthropic\.com' && echo "ATTRIBUTION"
>    git -C <worktree> show --stat HEAD
>    ```
>    and amend on the spot if a check fails or the commit touched files outside this plan's list.
>
> Isolation: every command starts with `cd <worktree> &&` or uses `git -C <worktree>`; scratch files go only to
> `<scratchpad>/lane-<x>/`; never run a script you did not write (except `gradle-slot.sh`); never `git stash`; never
> branch, merge, rebase, push or bump versions.
>
> Report: one line per plan, `NN: <hash> "<message>" — verified: <commands>; skipped manual: <what, why>`, then
> `Not done: NN — <why>`, then the final test result.

## 4. Merging — in the order E → U → I → R → T → X → S, as lanes report

For each lane:
1. `git -C ../Kubriko-lane-<x> log --stat --format=%s $START..HEAD` — stop on any path outside the lane's files
   (README → Lanes) other than its own plan files.
2. In the main checkout: `git cherry-pick $START..<lane HEAD>`. Conflicts are not expected (no shared files outside
   lane E's root `CLAUDE.md`); if one occurs in a `CLAUDE.md`, merge word by word keeping every sentence of both sides.
3. `./gradlew desktopTest`. A break is fixed with one more commit, never by rewriting landed commits.
4. `git worktree remove ../Kubriko-lane-<x>`.

## 5. Finish

1. `./gradlew build` and `node engine/src/webMain/checkTriangleBridge.mjs`. A break gets one more commit
   (`Fix the iOS build after the keyboard changes.`).
2. Final checks: `git log --format=%B $START..HEAD | grep -ciE 'co-authored|generated with|claude code|anthropic\.com'`
   prints `0`; `git log --oneline $START..HEAD` is one line per plan (plus any fix-up commits); `git status` is clean;
   `git worktree list` shows only the checkout.
3. Plan files still present are the skipped ones. If none are left, `git rm -r issues` and commit
   `Remove the review plans.` Otherwise trim the README to the skipped plans and ask the user.
4. Report the commit count, skipped plans with reasons, and the **Manual checks owed** (copy them from the README
   before deleting it); update the `fourth-review` memory with the commit range. **Do not push.**
