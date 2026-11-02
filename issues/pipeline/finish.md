# Finishing the fifth review's execution, in CI

You run headless in GitHub Actions on macOS, on the branch `{{BRANCH}}`, in a fresh checkout at its latest commit. Nobody is
watching, so decide yourself and finish. Every plan batch and both phase reviews have run before you.

1. **Build:** run `./gradlew build` (the whole repository, Apple targets included) and `./gradlew :creator:test`.
   - Fix whatever breaks, each break in its own commit, in the repository's voice (for example `Fix the iOS build after the
     module split.`).
   - Load `code-style` before editing and `commit-messages` before committing. The pre-commit hook must pass, never
     `--no-verify`.
   - Push after every commit: `git push origin HEAD:{{BRANCH}}`.
2. **Check the range:** the range is `{{START}}..HEAD`.
   - `git log --format=%B {{START}}..HEAD | grep -ciE 'co-authored|generated with|claude code|anthropic\.com'` must print 0.
   - Every commit message must be one line.
3. **The plans folder:**
   - If no plan file is left in `issues/` (only `README.md`, `EXECUTION.md`, `EXECUTION-LOG.md`, `SKIPPED.md`, and
     `pipeline/`), delete `issues/` and the two `.github/workflows/fifth-review-*.yml` files in one commit,
     `Remove the review plans.`
   - Otherwise, trim `issues/README.md` to the plans still present, with why each was skipped, and commit that.
   - Push.
4. **Write the pull request's description** to `build/scratch/pr-body.md` (git-ignored), in plain prose with lists where they help:
   - what the range did, phase by phase;
   - the commit count;
   - every skipped plan and every review finding left for a decision, with its reason;
   - the manual checks worth doing in the real app, only where a mistake would hurt (from `issues/EXECUTION.md` → Finish and
     the logs).
   Take the facts from `issues/EXECUTION-LOG.md` as it was before step 3 (`git show HEAD~1:issues/EXECUTION-LOG.md` if you
   deleted it).

Never branch, merge, rebase, force-push, bump a version, publish, or dispatch a workflow; the workflow opens the pull request
after you. End with a short summary.
