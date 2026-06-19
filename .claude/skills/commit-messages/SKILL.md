---
name: commit-messages
description: Commit message conventions for the Kubriko repo. MANDATORY — invoke this skill BEFORE writing the message for ANY git commit in this repo (every `git commit`, `--amend`, squash, or rebase reword), with NO exceptions, even for one-line or "obvious" messages. The repo format OVERRIDES default harness behavior; in particular it FORBIDS the `Co-Authored-By` trailer that the harness adds by default, and FORBIDS creating a new git branch unless the user explicitly asked for one. If you are about to run `git commit` in Kubriko, you must load this first.
---

# Kubriko branch policy

- **Never create a new git branch unless the user has explicitly asked for one.** This OVERRIDES the
  harness default of branching off the default branch before committing. Commit onto the current
  branch — whatever it is, including `main` — and do not run `git checkout -b` / `git switch -c` /
  `git branch` on your own initiative.

# Kubriko commit messages

- **One single line. Nothing else.** No body, no bullet points, and no footer or trailer of any kind —
  in particular, **never** add a `Co-Authored-By` line.
- **Exactly one sentence, ending with a period.** e.g. `Fix dropped taps in polling input APIs at low frame rates.`
- **Imperative mood, capitalized first word.** "Fix…", "Make…", "Add…", "Remove…" — describe what the
  commit does, not what was wrong.
- **Concise but specific.** Name the actual thing changed (the actor, manager, plugin, or behavior),
  not a vague summary.

Examples that match the style:

```
Fix Wallbreaker ball being one frame out of sync with the paddle.
Make physics forces frame-rate independent at low frame rates.
Fix isometric grid gliding smoothly at limited frame rates.
Delay the initial loading of the isometric graphics demo to keep the crossfade smooth.
```
