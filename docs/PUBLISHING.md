# Publishing checklist (run once, before the first GitHub push)

Tags and the public history are created **last**, on settled history — not during active
development. Rationale: history gets rewritten while we build (e.g. the project was rebuilt so it
is "born" named `spring-ai-agent-by-example`, with no rename commit), and **tags pin commit
SHAs** — any rewrite after tagging would leave tags dangling. So we track the *plan* here by
phase/intent, and lay the actual tags in one pass at the end.

## Tag policy

- One **annotated** tag per completed phase: `phase-0`, `phase-1`, … (annotated, so each carries a
  message).
- A phase tag points at the commit where **that phase's code AND its book chapter(s) are
  complete**, so a learner who checks out the tag gets runnable code plus the chapter to read.
- Going forward, commit each phase's book chapter **with** (or right after) that phase's code, so
  the tag naturally includes both. (Phases 0–1 code landed before the book existed; their chapters
  arrive in the book/model-swap commit — the phase-0/1 tags will point at the latest commit that
  completes each, since no later-phase code sits in between yet.)

## Tag → phase → book chapter map (intent, not SHAs)

| Tag | Phase / code state | Book chapters to read |
|-----|--------------------|-----------------------|
| `phase-0` | Phase 0 — sync loop, 1 MCP, step visibility | Ch 1–2 |
| `phase-1` | Phase 1 — 2nd MCP + dependent loop (+ model-swap experiment, config only) | Ch 3–4 |
| `phase-2` | Phase 2 — failure & recovery | Ch 5 |
| `phase-3` | Phase 3 — memory / multi-turn | Ch 6 |
| `phase-3.5` | Phase 3.5 — cost/usage observable | Ch 7 |
| `phase-4` | Phase 4 — async (202 + runId + poll) | Ch 8 |
| `phase-5` | Phase 5 — streaming the loop live (SSE) *(pending)* | *(pending)* |

Keep this table current as phases land — it is the source of truth for what gets tagged.

## Pre-push checklist

1. [ ] All intended phases committed; history settled (no more planned rewrites).
2. [ ] **Delete pre-rename safety branch** so the old name is never published:
       `git branch -D backup-pre-rename`
3. [ ] Confirm no earlier working name remains in any ref:
       `git grep -n <earlier-name> $(git rev-list --all)` → expect **no output**.
4. [ ] (Optional, for full parity) rename the working folder to `spring-ai-agent-by-example`
       so folder = artifactId = repo name.
5. [ ] Add a **"How to follow along"** section to [`book/README.md`](../book/README.md) mapping
       each tag to its chapters (mirror the table above) + the `git checkout <tag>` instructions.
6. [ ] Create the annotated tags on the final commits, e.g.:
       ```
       git tag -a phase-0 <sha> -m "Phase 0 — sync loop, one MCP, step visibility"
       git tag -a phase-1 <sha> -m "Phase 1 — second MCP + dependent multi-step loop"
       ```
7. [ ] Create the GitHub repo named `spring-ai-agent-by-example`, add the remote, then:
       `git push -u origin main --tags`
8. [ ] Verify on GitHub: tags resolve, `git checkout phase-0` works, no old name in history.

## Notes

- The default model is config (`AGENT_MODEL`); the book/NOTES recommend `qwen2.5:14b` for reliable
  multi-step runs. No secrets in the repo, so nothing to scrub before publishing.
