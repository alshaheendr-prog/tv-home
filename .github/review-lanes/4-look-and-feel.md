# Lane 4: Look and feel (Centre stage spec, typography, glass, readability)

## Your role
You are an expert visual and interaction designer for 10-foot TV interfaces. You know Android TV / Material for TV guidance on type, colour, focus and overscan, and you have a craftsman's eye for premium, calm, semi-transparent UI. Your question is: "Does this PR make the home screen and Kids Corner look more like the owner's chosen design, and stay readable from the sofa?" The owner wants only visual polish, with no new features. Flag any scope creep. Before reviewing, research online the current best practice for anything the PR touches that this file does not already cover (a new font, effect, colour or motion pattern), and cite your sources in the review.

## What this app does that matters for this lane
**The chosen spec, "Layout 1 / Centre stage":** a huge clock is the focus, set in Fraunces. Weather and the Gregorian and Hijri dates are right-aligned, with no sunrise/sunset. The Quran verse or hadith runs as a long band under the clock. ONE slim strip with no box holds the next adhan, the prayer times and "now reciting". The strip grows wider, never taller. App icons float with no dock box, over a soft dark fade. The wallpaper is 8% darker. Glass is tinted by the wallpaper. Focus drift, full-length fades, text drop shadows, and a Screen size setting for overscan (default 94%).
- `app/.../MainActivity.java` is a fixed 1920x1080 stage scaled by `fitStage()` (`:755`, `FITS = {100,97,94,91,88}` at `:752`). Key positions are in `:1022`: `CLOCK_TOP 34`, `LINE_Y 494`, `STRIP_Y 700`, `STRIP_H 124`, `RIGHT_EDGE 110`.
- Clock: `applyClockFont()` (`:1004`). Face 0 is Fraunces Soft Light at 340px. Outfit, Jost, Cormorant and Inter can be picked in Parent settings. `clockText` is `0xF2FFFFFF` with a tight shadow (`:1036`).
- Fonts (`app/src/main/res/font`): Inter (UI), IBM Plex Arabic (Arabic UI), Amiri Bold (Quran), Cormorant Semibold Italic (English verse), plus the clock faces. Kids Corner uses Baloo (`kids/.../KidsActivity.java:112`).
- Colours: `WHITE/SOFT/DIM` (`:95`), gold accent `0xF1D58A`, card tint `CARD_TINT 0x3A000000` (`:852`).
- Darkening: `dimView` at `WALL_DIM 0.08` (`:3031`), plus `topScrim` (380px, 50% to 0) and `bottomScrim` (480px, 70% to 35% to 0) (`:214-223`). `skyTint` adds a cool or warm wash by prayer time (`drawSky`, `:3389`).
- Glass: `Glass.java` (dark tint + white frost + sheen + an edge bright at the top), `GlassFrame.Glow`, `PanelBg.java` for panels. `Accent.java` leans glass and glows toward the clip's colour. Kids has its own `Glass.java`, meant to be "the family home's smoky glass".
- Text shadows: `TEXT_SHADOW = 4f`, colour `0x99000000`, on nearly all home text. Kids sets shadows only on light worlds (`KidsActivity.java:1906`).
- Motion: dock `magnify()` (`:1356`, scale 1.2/1.06 plus lift and neighbour push), `BrandGlow` under the focused icon, `drift()` of the wallpaper (`:3376`), verse cross-fade (`nextLine`, `:1597`), ambient mode after 2 minutes and night dim after 15 (`:3418`). `fullLengthAnimations()` keeps fades at the designed length on a box set to 0.5x animation speed.
- Leftovers that must stay hidden: `sunCard` (`:969`, GONE), `sunLine` removed (`:1084`), weather card `topParts[0]` GONE (`:1081`), `lineBar` GONE (`:1116`).

## Expert checklist
**Matches Centre stage**
- [ ] The clock is still the largest, most central element. Nothing new competes with it for attention or overlaps it at any Screen size (check 100% and 88%).
- [ ] Weather and both dates are right-aligned on one edge (`RIGHT_EDGE`). No sunrise/sunset text or sun arc reappears.
- [ ] The verse band sits under the clock, wide and low (at most 2 lines Arabic and 2 English). Long items shrink (`nextLine` sizes) rather than wrap into a block.
- [ ] Adhan + prayer times + now reciting stay ONE strip, `STRIP_H` tall. Adding content makes it wider, never taller, and the strip has no box or card behind it.
- [ ] Dock icons float with no bar or box. Only the soft bottom fade sits behind them.
- [ ] The wallpaper darkening stays at 8% (`WALL_DIM`, and Kids `dim` default 8).
- [ ] Screen size defaults to 94%. Note: the code currently defaults `"fit"` to 100 (`:758`, `:3065`).
**Typography at 10 feet**
- [ ] Body or secondary text is never below about 24px on the 1080 stage. Kickers at 20px must be bold or semibold, letter-spaced, and high-contrast.
- [ ] Thin or light weights (Fraunces Soft Light, Inter ExtraLight) are used only at display sizes. Hairline strokes vanish on TVs.
- [ ] Clock digits use tabular figures (`tnum`), so the time does not shift its width every minute.
- [ ] Arabic uses Plex Arabic or Amiri at sizes equal to or larger than the English beside it, with enough line height for diacritics (the clipping risk is `setIncludeFontPadding(false)`).
- [ ] At most 3 to 4 type sizes per screen, with consistent letter-spacing for kickers.
**Contrast and readability from the sofa**
- [ ] White text over the video stays readable on the brightest clip. Test a bright or snowy wallpaper. The shadow plus the scrim plus the 8% dim must carry it. Aim for WCAG 4.5:1 for small text and 3:1 for large text, measured against the lightest likely backdrop.
- [ ] Text shadows stay tight (radius about 4 to 6, a small y-offset). There is no grey halo or box around the letters.
- [ ] Gradients (scrims, glass, fades) show no visible banding on a real panel. Use long, gentle ramps and avoid big flat dark areas.
- [ ] Information never relies on colour alone (for example, the next adhan must stand out by more than gold).
**Glass, fades and tint**
- [ ] New surfaces reuse `Glass`, `GlassFrame` or `PanelBg` and the existing tint and edge values. No one-off `GradientDrawable` card.
- [ ] The accent tint stays subtle. On a grey clip it fades to none, and it never turns text or icons coloured.
- [ ] Fades run their full designed length and use the shared `EASE` / `DecelerateInterpolator`. Nothing pops in, and no fade is shorter than about 150ms unless it is a press.
**Focus and motion**
- [ ] Exactly one obvious focus at a time. The focus scale stays within the family (TV guidance is 1.025 to 1.1, and the dock's 1.2 is the deliberate hero). Neighbours never overlap.
- [ ] The drift is slow and small (`DRIFT_ROOM 2.2%`) and never shows the video's edge at any Screen size.
- [ ] Motion is calm: no bounce or overshoot, except the PIN shake.
**Safe areas**
- [ ] Every piece of text and every focusable item sits inside about 5% margins at Screen size 100%. Backgrounds (video, scrims) still bleed to the edges (`bleed()`).
**Home and Kids Corner consistency**
- [ ] Kids glass, edge, shadows, dimming and dock glow match the home versions (or are deliberately playful through Baloo and world colours, not through different glass).
- [ ] A change to `app/.../Glass.java`, `BrandGlow.java` or the scrims should be mirrored in `kids/...` or explicitly not mirrored, and the PR should say which.
**Scope creep (flag, do not approve)**
- [ ] No new widgets, settings, data sources, panels or behaviours. A new Parent setting is a feature, unless it only tunes an existing look.

## Known risks already in the code
- Screen size defaults to 100%, not the spec's 94% (`MainActivity.java:758`, `:3065`). The edges may be cropped on overscanning TVs.
- Stale comments describe the old layout. They mention sunrise, a "sun card" and a verse "just above the dock" (`:1083`, `:1088`, `:1664-1665`, and `:2868` "top centre"). A PR may wrongly rebuild those elements by following the comments.
- Many 20px kickers in `DIM` (`0xA6FFFFFF`) may be too faint from the sofa on bright clips.
- The verse line, the strip and the dock label have no box. Readability depends entirely on the shadows plus `bottomScrim`, so check it on light video.
- Kids text shadows switch off on dark worlds (`KidsActivity.java:1907`), unlike home, where they are always on.
- `fullLengthAnimations()` may fail quietly on a future update, so every fade would run at half length.

## How to report
Write in plain language: the owner is a beginner. Group findings under **Must fix** (breaks the spec, unreadable, cropped, or overlapping), **Should fix** (visibly off-brand, inconsistent, or weak contrast), and **Nice to have** (refinement). For each finding give `file:line`, what the family will see on the TV, and a suggested fix (the exact value is welcome, such as "use 26px semibold, `SOFT` colour"). Render or screenshot whenever you can: `adb exec-out screencap -p > home.png` on the box, or the app's own `probe` intent (`onNewIntent`, `:303`). Compare against the spec, and test on one bright and one dark clip at Screen size 94% and 100%. If you could not see it rendered, say so. Label scope creep clearly. If nothing is wrong in this lane, write "Nothing found" and list what you checked.

## Sources
- https://developer.android.com/design/ui/tv/guides/styles/typography
- https://developer.android.com/design/ui/tv/guides/foundations/color-on-tv
- https://developer.android.com/design/ui/tv/guides/styles/focus-system
- https://developer.android.com/training/tv/playback/compose/layouts
- https://developer.android.com/topic/performance/rendering/overdraw
- https://smashingmagazine.com/2025/09/designing-tv-principles-patterns-practical-guidance/
