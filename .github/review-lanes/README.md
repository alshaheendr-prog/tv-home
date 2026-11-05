# Review lanes

Every pull request on this repo gets fifteen Claude reviews, one per lane. Each lane has a brief in this folder (its checklist) and a growing knowledge file in [knowledge/](knowledge/) (its expertise: deep reference with sources, facts about this box and code, past findings and lessons). The owner wants every reviewer to be a super expert in its field and to get better with every PR.

| # | Lane | Brief |
|---|------|-------|
| 1 | Bugs and correctness | [1-bugs.md](1-bugs.md) |
| 2 | Safety and privacy (PINs, kids lock, secrets) | [2-safety.md](2-safety.md) |
| 3 | Smoothness (60fps, memory on the 2GB box) | [3-smoothness.md](3-smoothness.md) |
| 4 | Visual direction (overall premium look, Centre stage) | [4-look-and-feel.md](4-look-and-feel.md) |
| 5 | Remote and TV fit (D-pad, focus, overscan) | [5-remote-and-tv-fit.md](5-remote-and-tv-fit.md) |
| 6 | Arabic and Islamic content | [6-arabic-and-islamic.md](6-arabic-and-islamic.md) |
| 7 | Build check (builds, installs as an update) | [7-build-check.md](7-build-check.md) |
| 8 | On-TV checklist for the owner | [8-on-tv-checklist.md](8-on-tv-checklist.md) |
| 9 | Layout and spacing | [9-layout.md](9-layout.md) |
| 10 | Typography | [10-typography.md](10-typography.md) |
| 11 | Colour and contrast | [11-colour.md](11-colour.md) |
| 12 | Motion and animation | [12-motion.md](12-motion.md) |
| 13 | App icons and artwork | [13-icons.md](13-icons.md) |
| 14 | Wallpaper and backgrounds | [14-wallpaper.md](14-wallpaper.md) |
| 15 | UI components and states | [15-ui-components.md](15-ui-components.md) |

## Who covers what

Every part of the three apps has owners. A PR that touches an area gets all its lanes' attention; lanes 1, 2, 3, 7 and 8 look at every PR whatever it touches.

| Area | Main lanes |
|------|-----------|
| Clock | 4, 9, 10, 11, 12 |
| Weather and the Gregorian / Hijri dates | 1, 6, 9, 10 |
| Verse and hadith band | 6, 10, 11, 9 |
| Prayer strip, next adhan, countdown | 6, 1, 9, 10 |
| Adhan popup, 10-minute reminder, corner card (adhan app) | 6, 1, 15, 5, 12 |
| Dock and app icons, All apps grid | 13, 15, 5, 12 |
| Remote buttons (RemoteKeys) | 5, 2, 1 |
| Wallpaper video, dimming, scrims, glass tint | 14, 11, 3 |
| Quran player panel and recitation line | 6, 15, 1, 3 |
| Calendar and Islamic days | 6, 15, 9 |
| PIN pads, YouTube lock, Parent settings | 2, 15, 5 |
| Screen size and overscan | 9, 5 |
| Kids Corner: home, worlds, apps, cartoons, rest and bedtime, kids lock | 2, 4, 13, 14, 15, 6 (Good Words) |
| Screensaver (Aerial Views option) | 14, 1 |
| Build, signing, install as an update, CI | 7 |
| What the owner checks on the TV | 8 |
| The whole screen as one picture, scope creep | 4 |

## How a review runs

1. The Claude thread that opens a PR asks the project coordinator to start one review thread per lane (all 15), giving each the PR number and its brief.
2. Each reviewer reads its brief and **its whole knowledge file first**, then **researches online** whatever the PR touches that the knowledge file doesn't already cover, before it reviews. It cites the sources it used.
3. Each reviewer posts one comment on the PR headed `Review lane N: <name>`, with findings ranked **Must fix**, **Should fix**, **Nice to have** (or "nothing found"), and sends the same findings to the PR's thread.
4. The PR's thread fixes every Must fix and Should fix, and pushes. Lane 8's checklist goes to the owner as-is.
5. The PR is ready to merge only after all fifteen lanes have reported and every Must fix and Should fix is done.

If review threads can't be started, the PR's thread runs the fifteen lanes itself as separate subagents, one per brief, with the same rules.

## Getting more expert with every PR

At the end of every review, each reviewer sends the PR's thread its additions for `knowledge/N-*.md`: dated past findings (what it found and what was fixed), lessons learned, new sources with what each is good for, and open questions to research next. The PR's thread commits them in the same PR, so the next review starts from everything learned so far. Checklist items that should apply to every PR go into the brief too.

Every so often (or when the owner asks), a lane can spend a review-free session working through its "Open questions" list with fresh online research and update its knowledge file.

## Whole-app audits

Besides PR reviews, the owner can ask for a whole-app audit: every lane reviews the current app on `main` (not a diff), records its findings in its knowledge file, and the fixes go through normal PRs.
