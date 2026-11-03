# Challenge of the fifth review's follow-up plans, in CI

You run headless in GitHub Actions on the branch `{{BRANCH}}`, in a fresh checkout at its latest commit. Nobody is watching,
so decide yourself and finish.

**What to challenge:** the plans `issues/F01-*.md` to `issues/F06-*.md`, indexed by `issues/README.md`, before later jobs
carry them out one per job. Read `creator/CLAUDE.md`, `.claude/skills/codebase-review/SKILL.md` (section 1, step 7, and
section 2), and `.github/scripts/check_sizes.py`, whose constructor counting is the limit: 12 parameters, data classes not
counted.

**The goals:**
- UI parts outside the entry files take only what they use, enforced by a new layering test.
- Five Creator constructors past 12 parameters come down to 12 or fewer, by grouping parameters that genuinely belong
  together, never by a data-class bag.
- Behaviour is unchanged.

**For each plan, try to break it:**
- Check every fact it states against the code: each API, file, signature, module, and line count, including that a planned
  type lives in a module its callers can depend on.
- Check it against the plans before and after it.
- Trace each change through every caller and test.
- Check the threading, ordering, and lifecycle it introduces.
- Check that every grouping is cohesive, and that the counter really drops to 12 or fewer.
- Check the size limits on every touched file and function: 500 lines per file, 120 per function, and no growth past a limit.
- Check that the plan's tests can fail.

**Edit the plans in place:** rewrite what is wrong, and add `**Challenged:** amended — <what changed>` or `**Challenged:**
sound` under each header. Drop a plan only if its goal can't be met safely, saying why in `issues/README.md`.

**Commit:** load `commit-messages`, and commit as `Amend the fifth review's follow-up plans after challenging them.` Then
`git push origin HEAD:{{BRANCH}}`. Append one line per plan's verdict to `issues/EXECUTION-LOG.md` in the same commit.

**Rules:**
- Only plain shell commands are allowed: no variable-assignment prefixes, no inline Python or heredocs. Use the Read, Edit,
  and Write tools for files.
- Scratch files go to `build/scratch/`.
- Never edit `.github/workflows/` or `issues/pipeline/`. Never branch, merge, rebase, or force-push.
