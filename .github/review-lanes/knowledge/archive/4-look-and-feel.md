# Lane 4 knowledge: Visual direction (overall premium look; shared design reference): archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #25 (2026-11-06, update 3: Islamic content)
- Must: Sayyid al-Istighfar (Bukhari 6306) in the band = 2 Arabic + 2 English lines, bottom ~y 766, over the strip. Should: English size by `length() > 150` lets 143–150 characters wrap at 32 px (bottom 720–736). Nice: 2-line English at 28 px is now routine (10 items, bottom ~710); kicker with "FROM … SAHIH (AL-ALBANI)" about 1110 px of caps. Not scope creep: Hijri offset and sources rows (owner-approved).

### PR #23 (2026-11-06, update 2: visual polish)
- Must: the boxless gold event line was added as the last row of the clock column (`:1219`). The column already ends at about y 477, so the line covers y 477..524 and prints over the verse heading at y 500. It shows on about 1/3 of the year, and `stressTest()` forces it on. Fix: no extra column height. Put it on the verse heading row as a steady gold item outside `lineBox` (recommended), or on the date row.
- Should: the new verse fade (`lineHolder`, `:1260-1271`) is a 1920-wide 40% black band, which puts a dark stripe across the empty picture side. Feather it horizontally toward the text (ComposeShader DST_IN), as JetStream does.
- Should: the same band wraps its content, so its height snaps by 38 to 67 px at each 30 s verse change. Give it a fixed height.
- Nice: verse heading SOFT vs gold elsewhere; DIM 72% vs SOFT 85% too close (lane 11); Home banner's cartoon sun (lane 13).
- Good: the audit's direction breaks are all fixed (chip, next-prayer box, emoji, two golds in adhan, Baloo reminder). One family across the three apps, no scope creep.
- No TV screenshots. Used a code-built mock on two clip frames (`/mnt/project-files/tv-box/reviews/pr23-lane4/`, `mock.py` reusable).

### PR #22 (2026-11-05, audit update 1)
- Must: none. The Centre stage layout is untouched.
- Should: the message pill overlaps the Quran player's buttons (bottom 64 vs controls at 74). If a panel closes while a message shows, the pill stays bottom-centre over the dock. Fix: a `placePill()` called from `toast()` and from the close paths. Over the player it goes top-centre at 56.
- Nice: the disabled look differs (0.4 / 65% / 70%); make it one 40% value. PIN press dip 0.94 vs 0.96. Check the adhan band's Arabic fit on the TV (80 px area vs about 86 px line; if clipped, use BAND 142).
- Scope creep: none. The audit's lane 4 findings (eventChip, next-prayer pill, drive colours, forecast emoji, adhan reminder style) are deferred to the visual-polish PR.
- No screenshots: C:\TVWallpapers wasn't connected. The review used code geometry only.

### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- Must: eventChip is a Layout-0 leftover: a glass chip alone at (80, 402) on the left, shown ~100+ days a year (Ramadan, Shawwal, Dhul Hijjah 1-13, Ashura, White Days and eves); it does not follow the ambient drop. Fix: boxless gold line in centerGroup under the dates.
- Should: next-prayer cell gets a 30% white glass pill (`:1600`) inside the boxless strip; use a gold time instead.
- Should: drive status mint `0xCC9BE7B4` / coral `0xFFFF9A8F` in the hero column; use SOFT and a gold dot.
- Should: colour emoji (☀ ⛅ 🌫 🌧 ⛈) in the forecast line above the clock (`skyIcon`).
- Should: adhan reminder over other apps is Baloo + gold `0xFFC24A`; the adhan banner already uses home's Inter/Plex/`0xF1D58A`. Restyle the reminder to the banner recipe.
- Nice: verse kicker DIM white while all other kickers are gold; stale comments `:865`, `:1362`, `:1672-1673`, `:2911`; adhan white `0xF6F3FF`; three dark-surface recipes; notice pill top-left; dock dominates the 20 px blur while browsing (fine: it fades after 2 min).
- No TV screenshots were reachable (C:\TVWallpapers not connected); used a code-built mock (reviews/audit-2026-11/lane-4-mock-squint.png).

### PR #21 (docs, lane 4 re-review as the art director, 2026-11-05, at 8b9f60c)
- Must: none.
- Should: the brief gave no picture source a cloud reviewer can use. Default to Update TV's saved screenshots.
- Should: `stressTest()` shows `eventChip` (`:394`), so `stress.png` has a top-left glass chip. (Correction on commit: the chip is not always GONE; `drawEventChip()` shows it on Islamic event days and their eves.)
- Should: the adhan app mixes two golds: `0xF1D58A` on the banner line, Arabic sub and dot (`AdhanService.java:310/322/326`), `0xFFC24A` elsewhere.
- Should: area-map gaps (Kids Corner lacked 5, 9, 10, 11, 12; adhan lacked 10, 11; splash intro and StarField had no owner). Fixed in the README.
- Should: the shared Core sections drifted from lanes 11 and 15 (row scale 1.02, sky tint warm up to 13%). Each Core section now names its owning lane.
- Nice: concrete blur test; mirroring item named adhan files that don't exist; type-size count (about 16 vs NN/g's 3 and Smashing's 5 to 6, for lane 10); Liquid Glass notes.

### PR #21 (docs, first pass, 2026-11-05)
- Should: Android 15 target-35 text defaults added (elegantTextHeight on, includeFontPadding off in `text()`, start-overhang shift off). Fraunces name table is stale; stem:hairline 1.7:1 points to a text cut. Stale comment `:1093` ("one centred column") added.
- Nice: Inter mixes v3.019 and v4.001; kickers are 8 call sites, not 7; "Cormorant" is Cormorant Garamond v4.001.

### PR #20 (merged 2026-11-05)
- Must: none.
- Should: kids lock warning was an off-brand Toast the children could see. Fixed: moved into the Parent menu.
- Should: PIN wait text wrapped in the kids pad. Fixed: "Too many tries. Wait 30 seconds".
- Should: adhan card English capped at 4 lines. Fixed: uncapped.
- Nice: Amiri needs font padding for its marks. Done.
- Nice: home `pinSub` colour while resting (`DIM`). Not done.
- Research notes from the original brief: Screen size default 100% is deliberate (6aa9cc2), not a bug; old comments mention sunrise and the sun card; kickers are only 20 px.

## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [design-for-tv] https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv: 3 m viewing distance, limit text, instant D-pad feedback.
- [tv-typography] https://developer.android.com/design/ui/tv/guides/styles/typography: roles of display/headline/title/body/label (numbers are only in an image).
- [tv-type-tokens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/tokens/TypeScaleTokens.kt: the actual TV type scale numbers.
- [tv-layouts] https://developer.android.com/design/ui/tv/guides/styles/layouts: 960x540 dp canvas, 5% safe margins, grid, full-bleed backgrounds.
- [tv-focus] https://developer.android.com/design/ui/tv/guides/styles/focus-system: scale 1.025/1.05/1.1, glow 2 to 32 dp, focus states, one focus.
- [color-on-tv] https://developer.android.com/design/ui/tv/guides/foundations/color-on-tv: Standard picture mode, sRGB, contrast range, banding tips, dark UI.
- [tv-color-system] https://developer.android.com/design/ui/tv/guides/styles/color-system: dark themes, content-based colour from hero images.
- [tv-accessibility] https://developer.android.com/training/tv/accessibility: text scaling on TV, font_scale adb command.
- [wcag-143] https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html: 4.5:1 / 3:1, large-text definition, luminance formula, F83.
- [nngroup-glass] https://www.nngroup.com/articles/glassmorphism/: glass legibility: blur, opacity, contrast, use sparingly.
- [hig-typography] https://developer.apple.com/design/human-interface-guidelines/typography (page is JS; read via https://developer.apple.com/tutorials/data/design/human-interface-guidelines/typography.json): tvOS sizes (default 29, minimum 23 pt), avoid light weights.
- [hig-layout] https://developer.apple.com/design/human-interface-guidelines/layout (read via .../tutorials/data/design/human-interface-guidelines/layout.json): tvOS safe area 60/80 pt, focus growth must not overlap.
- [hig-materials] https://developer.apple.com/design/human-interface-guidelines/materials (read via .../tutorials/data/design/human-interface-guidelines/materials.json): 35% dimming under clear glass over bright content. Liquid Glass regular vs clear: clear only over visually rich media, with a 35% dim if the background is bright; on tvOS buttons and image views adopt glass on focus; standard materials ultraThin, thin, regular, thick. Re-checked 2026-11-05.
- [nng-hierarchy] https://www.nngroup.com/articles/visual-hierarchy-ux-definition/: hierarchy from contrast, scale and grouping; at most 3 sizes; blur test (5 to 10 px shows the groups, 20 px exposes stray focal points). Checked 2026-11-05.
- [polypane-squint] https://polypane.app/blog/debug-your-visual-hierarchy-with-the-squint-test/: what to check in a squint test (primary focus, grouping, emphasis, interactivity). Checked 2026-11-05.
- [appleinsider-tvos26] https://appleinsider.com/articles/25/06/18/tvos-26-hands-on-sleek-liquid-glass-redesign-new-control-center-and-more: translucent top bar over video, glassy icon edges. Checked 2026-11-05.
- [macrumors-tvos26] https://www.macrumors.com/2025/06/09/tvos-26-liquid-glass-redesign-older-models/: Liquid Glass runs only on Apple TV 4K 2nd gen and later because it renders in real time; supports no real blur on our 2 GB box. Checked 2026-11-05.
- [mobilesyrup-glass] https://mobilesyrup.com/2025/06/10/how-apple-is-making-sure-liquid-glass-as-legible-as-possible/: adaptive shadow, tint and light/dark switching; use glass sparingly. Checked 2026-11-05.
- [hig-motion] https://developer.apple.com/design/human-interface-guidelines/motion (read via .../tutorials/data/design/human-interface-guidelines/motion.json): purposeful, brief, cancellable motion.
- [mdc-motion] https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Motion.md: M3 easing curves and duration tokens.
- [valueanimator-ref] https://developer.android.com/reference/android/animation/ValueAnimator: default AccelerateDecelerate, 300 ms.
- [decelerate-ref] https://developer.android.com/reference/android/view/animation/DecelerateInterpolator: what the factor does.
- [vpa-ref] https://developer.android.com/reference/android/view/ViewPropertyAnimator: `animate()` uses ValueAnimator's default interpolator unless set.
- [textview-ref] https://developer.android.com/reference/android/widget/TextView: includeFontPadding, fallbackLineSpacing, elegantTextHeight, useBoundsForWidth (target-35 default on Android 15+ devices only), setLineHeight.
- [fraunces-readme] https://raw.githubusercontent.com/undercasetype/Fraunces/master/README.md: Fraunces axes and optical-size behaviour.
- [inter-readme] https://raw.githubusercontent.com/rsms/inter/master/README.md: Inter design intent and OpenType features.
- [plex-arabic-readme] https://raw.githubusercontent.com/IBM/plex/master/packages/plex-sans-arabic/README.md: Plex Sans Arabic weights.
- [amiri-readme] https://raw.githubusercontent.com/aliftype/amiri/main/README.md: Amiri's Bulaq/Quran heritage, development status.
- [adb] https://developer.android.com/tools/adb: `exec-out screencap -p`, `screenrecord` limits.
- [alreq] https://w3c.github.io/alreq/: W3C Arabic and Persian Layout Requirements: vertical extents, mixed-script alignment, kashida, digits, bidi.
- [smashing-tv] https://www.smashingmagazine.com/2025/09/designing-tv-principles-patterns-practical-guidance/: practical TV type (24 px body, 5 to 6 sizes), no pure white, focus contrast, safe zones.
- [a15-behavior] https://developer.android.com/about/versions/15/behavior-changes-15: target-35 text defaults (elegantTextHeight, useBoundsForWidth). Checked 2026-11-05.
- [inter-v4] https://github.com/rsms/inter/releases/tag/v4.0: Inter v4 changes (UPM 2048). Checked 2026-11-05.
- [hig-tvos] https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos (read via .../tutorials/data/design/human-interface-guidelines/designing-for-tvos.json): tvOS principles: focus that gently highlights and expands, edge-to-edge artwork, subtle fluid animation, cinematic and legible from across the room. Checked 2026-11-05.
- To open next: Material 3 motion pages (need a JS-capable reader), Google TV home-screen design references.
- [hig-color] https://developer.apple.com/design/human-interface-guidelines/color (read via .../tutorials/data/design/human-interface-guidelines/color.json): tvOS "limited color palette that coordinates with your app logo. Subtle use of color ... while deferring to the content"; use colour consistently, never the same colour for different meanings; colour sparingly on glass, reserve it for status or primary actions; don't use colour alone for focus. Checked 2026-11-05.
- [xda-gtv-ambient] https://www.xda-developers.com/google-tv-personalized-cards-rolling-out/: Google TV's ambient redesign moved to small cards at the bottom so they don't obscure the background. Supports keeping home overlays few and small. Checked 2026-11-05.
- [macrumors-aerials] https://www.macrumors.com/2025/08/01/tvos-26-custom-aerial-screen-saver-apple-tv/: tvOS 26 aerial playlists; says nothing about clock styling over aerials (dead end for that question). Checked 2026-11-05.

- [mdc-snackbar] https://github.com/material-components/material-components-android/blob/master/docs/components/Snackbar.md: snackbars sit towards the bottom; use `setAnchorView` to place them above bottom controls and navigation, never over them. Checked 2026-11-05.
- [flutter-white38] https://api.flutter.dev/flutter/material/Colors/white38-constant.html: white at 38% is the Material disabled colour in dark themes (disabled text and radios; `ThemeData.disabledColor`). Checked 2026-11-05.

- [tv-immersive-list] https://developer.android.com/design/ui/tv/guides/components/immersive-list: the anatomy names a "cinematic scrim", with no values in the text (they're in the Figma kit). Checked 2026-11-06.
- [jetstream-moviedetails] https://raw.githubusercontent.com/android/tv-samples/main/JetStreamCompose/jetstream/src/main/java/com/google/jetstream/presentation/screens/movies/MovieDetails.kt: Google's TV sample protects text with a horizontal gradient (x 300 to 1000) plus a vertical one (from y 600) toward the text, and the text column is 55% wide. Checked 2026-11-06.
- [smashing-text-over-images] https://smashingmagazine.com/2023/08/designing-accessible-text-over-images-part1: scrims should be subtle, overlays make the image "stand out less", and the Optimal Overlay Finder calculates the opacity needed for WCAG. Checked 2026-11-06.
- [sky-hero] https://sky-ui.cf.sky.com/sections/hero/usage: the hero scrim is optional, used only when the image gives too little contrast. Checked 2026-11-06.

From PR #25 (2026-11-06, update 3: Islamic content):
- [staticlayout-ref] https://developer.android.com/reference/android/text/StaticLayout: Builder.obtain / getLineCount for measuring wraps before showing text. The fetch returned only the index page; API 23 for Builder is from general knowledge *(unverified on the page)*.

## Open questions to research next
- Is `fraunces_soft_light.ttf` cut at opsz 9 or 144? Stem:hairline 1.7:1 suggests a text cut (2026-11-05). Confirming needs a fresh Fraunces opsz-144 instance (font downloads were blocked); consider re-instancing at opsz 144, SOFT 100, wght 300 with `tnum` kept (fontTools `instancer`).
- Do the Arabic parts of the kickers lose marks on the TV (`includeFontPadding` off; `elegantTextHeight`'s target-35 default doesn't apply on Android 14)?
- Should the adhan app settle on one gold, and which one: the home `0xF1D58A` or `0xFFC24A`?
- Remove `eventChip` from `stressTest()` so `stress.png` matches a normal day (later polish PR, lanes 9 and 15)?
- Do a blur pass at 5 px and 20 px on a current `home.png`. Is the clock the only focal point at 20 px?
- Should Inter Light/ExtraLight be re-bundled from v4?
- What do the scrims cover if the window is 3840x2160 (UI composed at 4K)? They are sized in px on `root`. Check `adb shell wm size` and a screenshot.
- Find a typography source on Arabic/Latin size matching (x-height ratio) and line height for Naskh with full tashkeel (alreq gives no size rule).
- Measure real contrast behind the verse band and strip on the brightest clip in `Movies/TVWallpapers`.
- Find Google TV's own home-screen visual spec (not opened this time).
- Does Android TV 14 "reduce motion"/remove animations setting need honouring beyond `fullLengthAnimations()` respecting 0?
- Should kickers move to 22 px (Google Label Small) and SemiBold, or stay 20 px for the look? Mock both at 3 m.
- Does `skyIcon` emoji render as colour emoji, tofu or a square on the box? Needs a screenshot of a day with a forecast.
- With screenshots: re-run the 5 px / 20 px blur on the real home.png (bright and dark clips) and replace the mock.
- Should the adhan app's floating panel and corner card (seen only over a non-family launcher) be restyled too, or retired visually once Google TV home stays disabled?
- Is there a published Google TV / Android TV ambient-mode visual spec (clock size and placement over photos)? Not found this time.

- Does a 2 to 3 px trim of Plex Arabic's padding in the adhan band clip any marks on the TV? Needs `Screens\adhan.png`.
- Should the home note pill move to top-centre everywhere (one place) rather than top-left on home and bottom-centre in panels? Decide with lane 15 when the eventChip leftover is removed (both use the top-left).

- What are the Google TV design kit's "cinematic scrim" values? They're in the Figma file at goo.gle/tv-desing-kit, which can't be opened here.
- If the event line moves to the verse heading row, should the verse heading turn gold on non-event days, or stay SOFT so the event stands out?
- Does 40% black behind the verse still give 4.5:1 on the brightest clip once the band is feathered? Lane 11 to measure.

From PR #25 (2026-11-06, update 3: Islamic content):
- Where does the strip's ink really start on the TV (computed ~y 725)? Measure it on `stress.png`.
