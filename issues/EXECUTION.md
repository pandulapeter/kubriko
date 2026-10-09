# Executing the third review's remaining plans

Brief for the orchestrating agent. Start: *"Follow `issues/EXECUTION.md`."* The process is the `codebase-review`
skill's section 4; this file fills it in for what is left.

**State:** the 179 Now plans landed as `92b16c51..70de96c6`, 44 of the 48 Planned plans as `ac016029..b31bb3f7`. Four
remain (README → Remaining plans), each blocked on something outside the code. Run a plan only when the user starts it
and its blocker is resolved:

- **A53** — the user reports the macOS live-theme result. If the elevations already follow a theme switch, delete the
  plan with no code change (commit `Drop the elevation plan after the live-theme check showed no defect.`).
- **D53** — the user reports the device check. Remove the padding only if it showed no change; otherwise delete the plan.
- **D56, D57** — after the next Tesselar sync has made the same change in Tesselar.

## Preconditions

Stop and tell the user if any fails: the tree is clean apart from `issues/`; `./gradlew desktopTest` passes on the main
checkout; the plan's quoted snippets still match `HEAD` (re-locate by text).

## Procedure

Four plans in disjoint files need no parallel lanes: run them in the main checkout, one commit per plan, following the
skill's per-plan procedure (load `code-style` before editing and `commit-messages` before committing; `git rm` the plan
in the same commit; check the commit is one line with no attribution). Verify with the plan's Verify commands — for
A53 the Showcase on every platform (`:app:shared:build`, `:app:desktop:compileKotlin`, `:app:web:compileKotlinWasmJs`,
`:app:android:assembleDebug`) and `:tools:ui-components:build`; for D53/D56/D57 the touched example's `build` and
`desktopTest`. A KGP "FqNames can't be derived from DirtyData" error is a stale incremental cache — rerun.

## Finish

- `git log --format=%B <start>..HEAD | grep -ciE 'co-authored|generated with|claude code|anthropic\.com'` prints 0.
- Update the `third-review` memory note. **Do not push.**
- When no plan file is left, delete `issues/` and commit `Remove the review plans.`
