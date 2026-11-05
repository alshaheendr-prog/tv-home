# Review lanes

Every pull request on this repo gets eight Claude reviews, one per lane. Each lane has a brief in this folder (its checklist) and a growing knowledge file in [knowledge/](knowledge/) (its expertise: deep reference with sources, facts about this box and code, past findings and lessons). The owner wants every reviewer to be a super expert in its field and to get better with every PR.

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
2. Each reviewer reads its brief and **its whole knowledge file first**, then **researches online** whatever the PR touches that the knowledge file doesn't already cover, before it reviews. It cites the sources it used.
3. Each reviewer posts one comment on the PR headed `Review lane N: <name>`, with findings ranked **Must fix**, **Should fix**, **Nice to have** (or "nothing found"), and sends the same findings to the PR's thread.
4. The PR's thread fixes every Must fix and Should fix, and pushes. Lane 8's checklist goes to the owner as-is.
5. The PR is ready to merge only after all eight lanes have reported and the Must fixes are done.

If review threads can't be started, the PR's thread runs the eight lanes itself as separate subagents, one per brief, with the same rules.

## Getting more expert with every PR

At the end of every review, each reviewer sends the PR's thread its additions for `knowledge/N-*.md`: dated past findings (what it found and what was fixed), lessons learned, new sources with what each is good for, and open questions to research next. The PR's thread commits them in the same PR, so the next review starts from everything learned so far. Checklist items that should apply to every PR go into the brief too.

Every so often (or when the owner asks), a lane can spend a review-free session working through its "Open questions" list with fresh online research and update its knowledge file.
