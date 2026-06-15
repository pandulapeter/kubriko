---
name: commit-messages
description: Commit message conventions for the Kubriko repo. Use whenever writing a git commit message in this repo.
---

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
