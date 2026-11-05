# Review lanes

Every pull request on this repo gets eight Claude reviews, one per lane. Each lane has a brief in this folder with an expert checklist built from online research.

| # | Lane | Brief |
|---|------|-------|
| 1 | Bugs and correctness | [1-bugs.md](1-bugs.md) |
| 2 | Safety and privacy (PINs, kids lock, secrets) | [2-safety.md](2-safety.md) |
| 3 | Smoothness (60fps, memory on the 2GB box) | [3-smoothness.md](3-smoothness.md) |
| 4 | Look and feel (Centre stage layout) | [4-look-and-feel.md](4-look-and-feel.md) |
| 5 | Remote and TV fit (D-pad, focus, overscan) | [5-remote-and-tv-fit.md](5-remote-and-tv-fit.md) |
| 6 | Arabic and Islamic content | [6-arabic-and-islamic.md](6-arabic-and-islamic.md) |
| 7 | Build check (builds, installs as an update) | [7-build-check.md](7-build-check.md) |
| 8 | On-TV checklist for the owner | [8-on-tv-checklist.md](8-on-tv-checklist.md) |

## How a review runs

1. The Claude thread that opens a PR asks the project coordinator to start one review thread per lane, giving each the PR number and its brief.
2. Each reviewer reads its brief, then **researches online** the current best practice for whatever the PR touches before it reviews. It cites the sources it used.
3. Each reviewer posts one comment on the PR headed `Review lane N: <name>`, with findings ranked **Must fix**, **Should fix**, **Nice to have** (or "nothing found"), and sends the same findings to the PR's thread.
4. The PR's thread fixes every Must fix and Should fix, and pushes. Lane 8's checklist goes to the owner as-is.
5. The PR is ready to merge only after all eight lanes have reported and the Must fixes are done.

If review threads can't be started, the PR's thread runs the eight lanes itself as separate subagents, one per brief, with the same rules.

## Keeping the briefs expert

When a reviewer's research finds something the brief is missing or that has changed, it says so in its comment, and the PR's thread adds it to that brief in the same PR.
