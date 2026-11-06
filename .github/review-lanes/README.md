# Review lanes

Every pull request on this repo is reviewed by expert Claude lanes. There are fifteen lanes. Each has a brief in this folder (its checklist), a core knowledge file in [knowledge/](knowledge/) (its expertise: deep reference, facts about this box and code, lessons) and an archive in [knowledge/archive/](knowledge/archive/) (past findings, open questions, the full source library). The owner wants every reviewer to be a super expert in its field and to get better with every PR.

To keep usage down without lowering the bar (owner's choice, 2026-11-06), each PR gets the lanes that own what it changes, not always all fifteen. See "Which lanes review a PR" below.

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
| Verse and hadith band | 6, 10, 11, 9, 12 |
| Prayer strip, next adhan, countdown | 6, 1, 9, 10, 11 |
| Adhan popup, 10-minute reminder, corner card (adhan app) | 6, 1, 15, 5, 12, 10, 11 |
| Dock and app icons, All apps grid | 13, 15, 5, 12 |
| Remote buttons (RemoteKeys) | 5, 2, 1 |
| Wallpaper video, dimming, scrims, glass tint | 14, 11, 3, 12 |
| Quran player panel and recitation line (and its `StarField` background) | 6, 15, 1, 3, 12, 14 |
| Calendar and Islamic days | 6, 15, 9, 11 |
| PIN pads, YouTube lock, Parent settings | 2, 15, 5, 11, 12 |
| Screen size and overscan | 9, 5 |
| Kids Corner: home, worlds, apps, cartoons, rest and bedtime, kids lock | 2, 4, 5, 9, 10, 11, 12, 13, 14, 15, 6 (Good Words) |
| Splash intro (`IntroView`) | 12, 10, 4 |
| Ambient mode and night dim | 12, 14, 3 |
| Screensaver (Aerial Views option) | 14, 1 |
| Build, signing, install as an update, CI | 7 |
| What the owner checks on the TV | 8 |
| The whole screen as one picture, scope creep | 4 |

## Which lanes review a PR

1. **Always:** lanes 1 (bugs), 2 (safety), 3 (smoothness), 7 (build) and 8 (on-TV checklist), whatever the PR touches.
2. **Plus the owners** of every area the PR changes, from the "Who covers what" table above. Lane 4 joins any PR that changes how a screen looks.
3. **All fifteen** when the owner asks, for a whole-app audit, or when a PR changes Kids Corner broadly or touches most areas.
4. **Paperwork-only PRs** (only notes, briefs, knowledge files, README, CLAUDE.md or skills; no app code, resources, build files or CI) get **one** quick check by a single reviewer (lane 1, or the lane whose files changed), not a full round.
5. When unsure whether an area is touched, include its lane. The PR description lists the lanes chosen and why.

## How a review runs

1. The Claude thread that opens a PR picks the lanes (above) and asks the project coordinator to start one review thread per chosen lane, giving each the PR number and its brief.
2. Each reviewer reads its brief and **its core knowledge file** in full, and searches its archive only for history on what the PR touches. It **researches online** only what the PR touches that its knowledge doesn't already cover, and cites the sources it used.
3. Each reviewer posts one comment on the PR headed `Review lane N: <name>`, with findings ranked **Must fix**, **Should fix**, **Nice to have** (or "nothing found"), and sends the same findings to the PR's thread.
4. The PR's thread fixes every Must fix and Should fix, and pushes. Lane 8's checklist goes to the owner as-is.
5. **Re-checks are narrow:** after the fixes, only the lanes that raised a Must fix or Should fix confirm their own items, looking only at the fix. No lane does a second full review.
6. The PR is ready to merge only after every chosen lane has reported and every Must fix and Should fix is done.

If review threads can't be started, the PR's thread runs the chosen lanes itself as separate subagents, one per brief, with the same rules.

## Quality watch

The owner asked to be told straight away if quality slips. The PR's thread (and the coordinator) tells the owner, and puts the lane back on for that area, whenever:
- a problem shows up on the TV, or in a later review, that a lane skipped for that PR owns;
- a reviewer only got something right by digging in its archive, which means that lesson belongs in its core file; or
- a lane reports it lacked the knowledge to judge something and had to guess.

## Getting more expert with every PR

At the end of every review, each reviewer sends the PR's thread its additions: dated past findings (what it found and what was fixed), new sources with what each is good for, and open questions go to `knowledge/archive/N-*.md`; lessons learned and new facts every review needs go to the core `knowledge/N-*.md`, merged with older lessons on the same point so the core stays lean (about 25 KB). The PR's thread commits them in the same PR, so the next review starts from everything learned so far. Checklist items that should apply to every PR go into the brief too.

Every so often (or when the owner asks), a lane can spend a review-free session working through the "Open questions" in its archive with fresh online research and update its knowledge file.

## Whole-app audits

Besides PR reviews, the owner can ask for a whole-app audit: every lane reviews the current app on `main` (not a diff), records its findings in its archive (and lessons in its core file), and the fixes go through normal PRs.
