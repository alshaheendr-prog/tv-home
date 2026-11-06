# Lane 4 knowledge: Visual direction (overall premium look; shared design reference)

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../4-look-and-feel.md`.
- History lives in [archive/4-look-and-feel.md](archive/4-look-and-feel.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code or seen it on the TV or in a screenshot.
- **2026-11-05: lane 4 was split.** Lane 4 is now the art-director lane: it judges the whole screen as one picture (Centre stage fidelity, premium feel, hierarchy at a glance, consistency across home, Kids and adhan, scope creep, before/after screenshots) and hands specifics to the new design lanes: 9 Layout and spacing, 10 Typography, 11 Colour and contrast, 12 Motion and animation, 13 App icons and artwork, 14 Wallpaper and backgrounds, 15 UI components and states. This file keeps all its earlier content because those lanes reuse it as the shared design reference (type scale, contrast maths, motion tokens, glass, focus, safe areas, recipes); each new lane has its own knowledge file for its deeper detail. Lane 4 adds here only whole-screen findings and lessons; detailed findings go to the owning lane's file.

## Core expertise

### 1. Units: turning guidelines into pixels on our 1920x1080 stage
Authority: lane 9's [knowledge file](9-layout.md).
- Google designs TV at **960x540 dp** ("At MDPI 1px = 1dp") and asks for assets aimed at 1080p. Our stage is 1920x1080 px, so **1 dp = 1 sp = 2 px on the stage**. [tv-layouts]
- Apple's tvOS points are at 1920x1080 @1x, so **1 tvOS pt = 1 px on the stage**. [hig-typography]
- The app sizes all text in px (`COMPLEX_UNIT_PX` in `text()`), so the system font-scale setting does not change it. That is acceptable for a fixed stage, but sizes must already be generous. Google asks TV apps to respect text scaling in general (`adb shell settings put system font_scale 1.2` to test). [tv-accessibility]

### 2. Type at 10 feet
Authority: lane 10's [knowledge file](10-typography.md); if this section disagrees, lane 10 wins.
- Viewing distance is about **3 m (10 ft)**; keep text large and limit the amount of reading. [design-for-tv]
- Google TV type scale (tv-material tokens, sp, then stage px):
  Display L/M/S 57/45/36 sp (114/90/72 px); Headline L/M/S 32/28/24 sp (64/56/48 px); Title L/M/S 22/16/14 sp (44/32/28 px); Body L/M/S 16/14/12 sp (32/28/24 px), line heights 24/20/16 sp; Label L/M/S 14/12/11 sp (28/24/22 px), weight Medium, tracking 0.1 to 0.5 sp. [tv-type-tokens]
- So the **smallest Google style is 22 px** (Label Small) and the smallest body is 24 px. tvOS: default body **29 pt**, **minimum 23 pt**; tvOS styles run Caption 2 23 pt to Title 1 76 pt, mostly Medium weight. [tv-type-tokens] [hig-typography]
- Display styles are for "short, important text passages, or numerals" (our clock); don't use them for section headings. Body text: no decorative faces. [tv-typography]
- **Avoid light weights**: Apple says prefer Regular, Medium, Semibold or Bold, avoid Ultralight, Thin and Light "especially when text is small"; with a thin custom font "aim for larger than the recommended sizes". [hig-typography]
- Kickers (small all-caps labels) are Label styles: Medium weight with positive tracking. At 20 px ours sit below both the Google (22 px) and Apple (23 pt) minimums. Treat 22 px as the floor for any new small text, and 24 px+ for anything the family must read.
- Use clear readable fonts, larger sizes and extra line spacing; never rely on colour alone. [color-on-tv]
- Smashing (2025): start TV body text at about **24 px**, keep a small scale of **5 to 6 sizes**, "when in doubt, go larger"; avoid pure `#FFFFFF` (eye strain in dim rooms), tint bright elements warm; reserve the highest contrast for focus. Our `WHITE 0xF5F5F7` avoids pure white but is slightly cool, not warm (lane 11); the gold accent is warm. [smashing-tv]

### 3. Fonts in this app (verified with fontTools on the repo's files, 2026-11-05)
Authority: lane 10's [knowledge file](10-typography.md).
- **Fraunces** is a display "Old Style" soft serif with axes opsz 9 to 144, wght 100 to 900, SOFT 0 to 100, WONK. As opsz goes down, x-height rises, spacing opens and letters widen; "wonky" forms switch off at 18 px and below. [fraunces-readme]
- Our `fraunces_soft_light.ttf` is a static instance (no `fvar`), weight class 300. Its name table is stale: it says **"Fraunces 9pt Black"**, but nameID2 is Regular and the weight is 300, so name tables can't be trusted after instancing. It has **no `tnum` feature** and proportional digits (advance of "1" is 687 vs about 1000 for others, upm 2000). So `setFontFeatureSettings("tnum")` on the clock does nothing for face 0, and the clock's width changes as the minutes change. The measured stem:hairline ratio is about **1.7:1**, which points to a low-opsz text cut rather than the display cut (opsz 144): strong evidence, not proof. A text cut is wider and lower-contrast than the display cut the spec imagines.
- Inter (all bundled weights), Jost, Outfit, Cormorant, Chivo Mono and Baloo **do** have `tnum`. The bundled Inter mixes **v3.019** (Light, ExtraLight; UPM 2816) with **v4.001** (Medium, SemiBold, Bold; UPM 2048); proportional metrics are the same. "Cormorant" is Cormorant Garamond v4.001. [inter-v4] Inter has a tall x-height and features such as tabular numbers and slashed zero. [inter-readme]
- IBM Plex Sans Arabic (Medium, SemiBold bundled): no `tnum`; its Western digits are equal width (600 units) but its Arabic-Indic digits are proportional (271 to 632; lane 10). [plex-arabic-readme]
- Amiri is a classical Naskh revival of the Bulaq (Amiria) press face, "one of the few metal typefaces that were used in typesetting the Quran"; mature since v1 (2022), no further development planned. Its vertical extents are very large (winAscent 1850, winDescent 910 on upm 1000), so it needs `includeFontPadding(true)` or explicit line height for marks. [amiri-readme]
- Android text APIs that matter: `setIncludeFontPadding` (default true; adds room for accents above ascent/descent); `setFallbackLineSpacing` (API 28, default true; respects taller fallback fonts); `setElegantTextHeight` (uses uncompacted variants and taller bounds, useful for Arabic); `setUseBoundsForWidth` (API 35; uses glyph bounds so italic overhangs such as Cormorant are not clipped; its target-35 default applies only on Android 15+ devices, so not on the Android TV 14 box); `setLineHeight(unit, value)` (API 34). [textview-ref]
- **Target-35 (Android 15) defaults:** `elegantTextHeight` is now **true by default** (Arabic is named for testing), and `useBoundsForWidth` is on. These apply only on Android 15+ devices: the box runs Android TV 14, so today line heights come from hhea ascent/descent plus `includeFontPadding` (lane 10). `shiftDrawingOffsetForStartOverhang` is **not** on by default, so a first glyph can still clip at the start. [a15-behavior] No code calls `setElegantTextHeight`, `setUseBoundsForWidth`, `setShiftDrawingOffsetForStartOverhang` or `setFallbackLineSpacing` (grep). `text()` (`:799`) turns `includeFontPadding` off everywhere, including the Inter+Arabic kickers.

### 4. Pairing Arabic and Latin
Authority: lane 10's [knowledge file](10-typography.md) (glyph coverage: lane 6).
- Arabic needs room above and below the line for marks; never set `includeFontPadding(false)` on Arabic or Quran text without an explicit line height (lesson from PR #20: Amiri needed padding).
- Arabic beside Latin at the same px size looks smaller (lower x-height equivalent), so set Arabic equal to or larger than the English next to it (ours: Arabic verse 38 to 44 px vs English 32 px Cormorant). (design judgement; research a source in Open questions)
- W3C Arabic Layout Requirements: Arabic ascenders and descenders "extend much further than those of the Latin script", so mixed lines need more vertical room; diacritics stay attached to their base letter and their height varies by font; "care must be taken to correctly align text in the different scripts when they appear together" (no size rule is given). [alreq]
- Arabic lines are normally right-aligned. Justify with restrained kashida rather than wide word spaces; never several kashidas in one word. Numbers run left to right even in Arabic text; prefer Unicode isolates over old embedding marks for bidi. [alreq]
- Digits: European digits are used in the Maghreb, Arabic-Indic (٠١٢) in Eastern Arabic countries such as Egypt, Saudi Arabia and Iraq. In this app Arabic strings use Arabic-Indic digits (`Widgets.ar()`, see lane 6); look only at the visual side: one digit style per line, and digit sizes that match the words beside them. [alreq]
- Keep one Arabic UI face (Plex Arabic) and one Quran face (Amiri). Plex Arabic was designed as part of the Plex family, so it pairs naturally with a neutral grotesque like Inter. (design judgement)

### 5. Colour, contrast and text over video
Authority: lane 11's [knowledge file](11-colour.md); if this section disagrees, lane 11 wins.
- WCAG 1.4.3: **4.5:1** for normal text, **3:1** for large text; large is **18 pt (24 px) regular or 14 pt (about 18.5 px) bold**. Contrast is (L1+0.05)/(L2+0.05) with sRGB relative luminance L = 0.2126 R + 0.7152 G + 0.0722 B (linearised). Background images that don't give enough contrast are a failure (F83). Do not round the ratio. [wcag-143]
- What that means for our white text (`WHITE 0xF5F5F7`, L about 0.91): the backdrop must have **L at most 0.163 for 4.5:1** and **at most 0.27 for 3:1**. With only the 8% dim, the clip itself must stay below about L 0.18 (roughly sRGB grey #777) for small text. Anything brighter relies on the drop shadow. (arithmetic from [wcag-143])
- Practical check: screenshot (`adb exec-out screencap -p > home.png`), sample the brightest pixels right behind a text run, compute the ratio, and test a bright/snowy and a dark clip. [adb]
- TV colour realities: most TVs are in Standard picture mode and support sRGB, so design and test in sRGB/Standard; contrast ratios vary hugely (the page's 562:1 example is labelled only "Low"); same colour can look washed out on low-contrast panels. Darker UI saves power; avoid white backgrounds. [color-on-tv]
- **Banding**: [color-on-tv] says avoid extreme colour transitions, dither, test on several panels, or use solid colours/subtle patterns. Banding comes from few 8-bit steps spread wide. Keep dark ramps short or give them more steps (raise the darkest stop), dither, or let the video show through. (Same line in lane 11.)
- Build from **dark themes** for a cinematic feel. Content-based colour (extracting a seed colour from the hero image) is the TV-endorsed way to get dynamic colour, since Android TV has no wallpaper-based Material You. That is exactly what `Accent.java` does. [tv-color-system]

### 6. Glass and translucency
Authority: lane 11 (glass colour) and lane 14 (glass over video): [11-colour.md](11-colour.md), [14-wallpaper.md](14-wallpaper.md).
- NN/g: glass works "best when utilized sparingly". Busy backgrounds (video) need **heavier blur**; lower opacity shows more background and hurts readability; text and graphics must still meet contrast everywhere the background can vary; a low-opacity stroke adds depth. Where the background is unpredictable, design for all of it. [nngroup-glass]
- Apple: for clear glass over bright content, "consider adding a dark dimming layer of **35% opacity**"; use the more opaque "regular" material when there is a lot of text. Use vibrant (high-contrast) colours on materials. [hig-materials]
- Our glass cannot blur the video (SurfaceView; PixelCopy returns black on this box). So the tint and the shadows must do all the work: on a bright clip, glass behaves like a 20 to 25% dark tint, not frosted glass. Judge legibility as if there were no blur.
- tvOS 26 Liquid Glass backs these choices: "clear" glass only over visually rich backgrounds, the 35% dim for bright content still stands, buttons and image views adopt glass on focus (like our gold-glass focus), and Apple limits it to Apple TV 4K 2nd gen and later because it renders in real time (so no real blur on our 2 GB box). [hig-materials] [macrumors-tvos26] [appleinsider-tvos26] [mobilesyrup-glass]

### 7. Focus
Authority: lane 15's [knowledge file](15-ui-components.md) for the focus look, lane 5 for focus mechanics.
- Exactly **one** focused element at a time. States: default, focused, pressed (held until the remote button is released), plus enabled, disabled, selected. [tv-focus]
- Default focus scale values **1.025, 1.05, 1.1** (larger elements need less). Glow 2 to 32 dp, coloured by image or brand. Focus can combine scale, border, outline, glow and colour change. [tv-focus]
- tvOS: focused items grow, so leave padding so they never overlap important information. [hig-layout]
- Our family of scales today: dock 1.2 (deliberate hero), PIN keys 1.12, menu rows 1.1/1.08/1.07/1.06/1.03, calendar rows 1.02. A new element should pick from 1.02 to 1.1 unless it is the dock (lane 15 owns this).

### 8. Layout and safe areas
Authority: lane 9's [knowledge file](9-layout.md).
- Google: **5% margins**: 48 dp sides and 27 dp top/bottom (the page also says 58/28 dp in one place, and the grid uses 58 dp sides). On our stage: **96 to 116 px sides, 54 to 56 px top and bottom**. 12-column grid of 52 dp columns with 20 dp gutters. [tv-layouts]
- "Most modern TVs no longer have overscan issues." Backgrounds must still bleed to the edge, not be clipped to the safe area. [tv-layouts]
- Apple: inset primary content **60 pt top/bottom, 80 pt sides** (= 60/80 px on our stage). [hig-layout]
- Our `RIGHT_EDGE 110` sits inside both. `CLOCK_TOP 34` is a view position: the visible digit tops are lower because of font ascent, so check the screenshot, not the number.

### 9. Motion
Authority: lane 12's [knowledge file](12-motion.md); if this section disagrees, lane 12 wins.
- Material 3 easing: Standard `cubic-bezier(0.2, 0, 0, 1)`, Standard decelerate `(0, 0, 0, 1)` for entering, Standard accelerate `(0.3, 0, 1, 1)` for exiting; Emphasized decelerate `(0.05, 0.7, 0.1, 1)`, Emphasized accelerate `(0.3, 0, 0.8, 0.15)`. [mdc-motion]
- M3 durations: Short 50 to 200 ms, Medium 250 to 400 ms, Long 450 to 600 ms, Extra long 700 to 1000 ms. **Duration grows with the area or distance travelled**; exits are a little shorter than entrances (container transform 300 in, 250 out). [mdc-motion]
- Android defaults: `ValueAnimator` (and therefore `animate()` with no interpolator) uses **AccelerateDecelerateInterpolator** and **300 ms**. `DecelerateInterpolator(f)`: 1.0 is an inverted y=x² parabola; higher factors start faster and end slower. Our `EASE` is `DecelerateInterpolator(1.6f)`. [valueanimator-ref] [decelerate-ref] [vpa-ref]
- Apple: add motion purposefully; avoid motion on frequent interactions; keep feedback brief and precise; let people cancel; don't make motion the only carrier of information. [hig-motion]
- Premium calm, in practice: one easing family (decelerate in, quicker accelerate out), no overshoot or bounce except deliberate error feedback (PIN shake), cross-fades rather than pops, and no fade under about 150 ms except press feedback.

### 10. How to judge a screen
- Screenshot: `adb exec-out screencap -p > home.png` (`exec-out`, not `shell`, for raw PNG). Video: `adb shell screenrecord /sdcard/demo.mp4` (max 3 min, no audio; some devices cannot record at native resolution). [adb]
- Whether `screencap` captures the SurfaceView wallpaper on this box is **unverified** (it normally does for non-secure surfaces).
- Look at: hierarchy (is the clock clearly first?), alignment to `RIGHT_EDGE`, text over the brightest part of the clip, banding in scrims, focus visibility from 3 m (view the PNG at about 25% size), and Arabic marks not clipped.

### 11. Review recipes
Contrast of white text against a sampled backdrop pixel (run on a screenshot) [wcag-143]:
```python
def lin(c): c/=255; return c/12.92 if c<=0.04045 else ((c+0.055)/1.055)**2.4
def L(r,g,b): return 0.2126*lin(r)+0.7152*lin(g)+0.0722*lin(b)
def ratio(a,b): hi,lo=max(L(*a),L(*b)),min(L(*a),L(*b)); return (hi+0.05)/(lo+0.05)
# ratio((245,245,247), backdrop_rgb) >= 4.5 small text, >= 3.0 for 24 px+ (or 19 px+ bold)
```
Does a font really have a feature (tabular figures, etc.)?
```python
from fontTools.ttLib import TTFont
t = TTFont("app/src/main/res/font/fraunces_soft_light.ttf")
print({r.FeatureTag for r in t["GSUB"].table.FeatureList.FeatureRecord})
```
Screenshot set for a look-and-feel review (state what you could not capture):
1. Home at rest, bright clip and dark clip, Screen size 100% and 88%.
2. Dock focus on the first, middle and last tile (neighbour push, glow, label).
3. Verse band with the longest Arabic and English items (shrink, not wrap).
4. Strip with the longest adhan and reciter names (wider, never taller).
5. Any panel the PR touches, and Kids Corner if shared code changed.

Mapping our motion to Material tokens (for judging a new animation) [mdc-motion]:
| Our use | Ours now | M3 token range |
|---|---|---|
| Focus scale (dock, rows) | 180 to 240 ms, decelerate | Short4 to Medium1 (200 to 250 ms) |
| Dock label fade in / out | 220 / 150 ms | Short3 to Short4 |
| Panel fade in / out | 240 to 500 / 180 to 300 ms | Medium (in), Short4 to Medium2 (out) |
| Verse cross-fade | 400 out + 600 in | Long range: fine for ambient content |
| Wallpaper drift | 1,400 ms | beyond Extra long: ambient, deliberate |
| Splash/intro staggered rise | 800 ms, 120 ms stagger | Extra long2: fine once per boot |

Type check against the TV minimums (stage px) [tv-type-tokens] [hig-typography]:
| Role | Google TV | tvOS | Ours |
|---|---|---|---|
| Smallest label | 22 px (Label S) | 23 px | kickers 20 px |
| Small body | 24 px (Body S) | 29 px default | 22 to 26 px |
| Title | 28 to 44 px | 38 to 48 px | 30 to 46 px |
| Display / numerals | 72 to 114 px | 57 to 76 px | clock 340 px, Arabic hero 168 px |

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep).
- **Layout is a right-aligned column, not centred**: commits 147dfa9 ("clock, weather and dates aligned right") and e29d5f1 ("strip aligned right with the clock"). `buildCenter()` uses `Gravity.END` with `RIGHT_EDGE` padding; the verse holder (`LINE_Y 494`) and the strip (`STRIP_Y 700`, `STRIP_H 124`) are placed `TOP|END` at `RIGHT_EDGE` (`MainActivity.java:1030`, `:1132`, `:2481`). The left of the screen is left to the video.
- Screen size: `FITS = {100, 97, 94, 91, 88}`, **default 100% on purpose** (commit 6aa9cc2, "screen size defaults to 100%"). Not a bug.
- Clock: face 0 Fraunces Soft Light at 340 px (`applyClockFont`, sizes `{340, 310, 306, 330, 304}` for Fraunces, Outfit, Jost, Cormorant, Inter), colour `0xF2FFFFFF`, shadow (6, 0, 3, `0x73000000`), `tnum` requested (no-op on Fraunces, see Core 3), AM/PM 50 px Inter Light `0xB3FFFFFF`.
- Colours: `WHITE 0xFFF5F5F7`, `SOFT 0xD9FFFFFF`, `DIM 0xA6FFFFFF` (`:95`), `GOLD 0xFFF1D58A`, `CARD_TINT 0x3A000000`.
- **Kickers**: `kicker()` (`:863`) sets letter-spacing 0.18 and colour `0xD9E8CB86` (warm gold at 85%), overriding the `DIM` passed in. All kickers are 20 px Inter SemiBold. (The old note "20px dim white" was wrong about the colour; the size concern stands.)
- Text sizes in use (px): 20 (kickers, 8 call sites), 22, 24, 26, 28, 30, 32, 38, 40, 44, 46, 50, 60, 66, 168 (Arabic), 230/340 (clock). `text()` turns `includeFontPadding` off and adds a (5, 0, 2, `0x66000000`) shadow at 100 px+.
- Home shadows: `TEXT_SHADOW 4f`, dy 2, `0x99000000` on most text; strip text `0xB3000000`.
- Darkening: `dimView` black at `baseDim()` (8% at brightness 100), `topScrim` 380 px `0x80000000` to 0, `bottomScrim` 480 px `0xB3000000`/`0x59000000`/0 (`:215-226`). The scrims are sized in window px on `root`, not on the scaled stage. The verse band (y 494 to ~600 on the stage) sits above the bottom scrim, so only the 8% dim and the shadows protect it.
- `skyTint` (`drawSky`, `:3434`): cool `0xFF5B86E5` around Fajr (up to 11%), warm `0xFFFF8C42` around Maghrib (up to 13%).
- Glass edge gradient `0xB3FFFFFF / 0x26FFFFFF / 0x40FFFFFF` at 0, .55, 1 is identical in `app` and `kids` `Glass.java` (verified).
- Dock: `TILE 132`, `DOCK_H 188`, `DOCK_Y 36`; `magnify()` scale 1.2/1.06, lift -20/-5, neighbours pushed 22/26 px, 240 ms `DecelerateInterpolator(2f)`; label 22 px Inter Medium `0xF2FFFFFF`.
- Motion: `EASE = DecelerateInterpolator(1.6f)`. A view's `animate()` keeps the last interpolator set on it, so calls that set none inherit it: `quranPanel` inherits `EASE` from `show()`, and `playerPanel`'s 300 ms close inherits `DecelerateInterpolator(2f)` from its open (`:662`). Only a view never given one uses AccelerateDecelerate. Details: lane 12's knowledge file. Verse cross-fade: out 400 ms (+8 px), in 600 ms. Wallpaper drift 1,400 ms.
- Kids: `ink()` (`KidsActivity.java:1920`) gives **light-coloured text** a (8, 0, 2, `0xB3000000`) shadow and dark text none (decided by text colour, not by the world). Kids dim default 8 (`:918`); Baloo Bhaijaan 2 Medium/Bold/ExtraBold.
- `pinSub` rests in `DIM` 24 px (`:2050`) (Nice-to-have from PR #20, not done).
- Stale comments still describe the old layout: sun card (`:965`), "dates and sunrise sit under the clock" (`:1091`), "one centred column" on a 1920-wide `Gravity.START` view (`:1093`; the right alignment really comes from `:1039-1040`), verse "low on the screen just above the dock" (`:1096`), weather card sunrise (`:1672-1673`). `sunCard`, `sunLine`, `topParts[0]`, `lineBar` are hidden or removed.

Added by the whole-app audit (verified 2026-11-05):
- eventChip sits at `at(..., 80, 56 + CARD_H + 16)` = (80, 402), outside topParts, so ambient mode neither fades nor drops it.
- Ambient (`enterAmbient`, ~`:3467`): after 2 min the dock fades and drops 40 px; centerGroup, lineFrame and the strip (topParts[1]) drop QUIET_DROP 110 px. Night dim to 0.6 after 15 min.
- Default dock = all 11 DOCK entries (dock_order unset): 11 x (132 + 22) + 96 = 1,790 px wide, nearly edge to edge.
- Next-prayer highlight: `prayerCells[i].setBackground(isNext ? glass(14, GLASS_HI, 0) : null)` (~`:1600`).
- Drive line colours: mint `0xCC9BE7B4` (free space), coral `0xFFFF9A8F` (unplugged), in dayRow beside the forecast, above the clock.
- Adhan app surfaces: banner = Inter Medium + Plex Arabic SemiBold, `0xF1D58A`, dark `0xE6080B14→0xF5080B14` (house style). Reminder, floating panel and corner card = Baloo / Chivo Mono, `GOLD 0xFFC24A`, `TEXT 0xF6F3FF`, fill `0xE00A0C16`/`0xD90A0C16`. Panel and card show only over a non-family launcher; the reminder shows over any app except tvhome.
- Kids grown-up panels use violet `sheet()` `0xFF241B45→0xFF150F2B`; home panels use navy PanelBg.

Added by the PR #22 review (2026-11-05):
- `toast()` (`MainActivity.java:887`) picks the pill's place only when a message starts. Over any open panel, including the player (`anyPanelOpen()` counts `playerPanel`), it goes bottom-centre at 64. On the home screen it goes top-left at (80, 56). Elevation is 50: above panels (30), the player (35) and reciters (40), and under the intro (60). It stays up for 3.5 to 7 s, depending on word count.
- Quran player controls are bottom-centre at 74 (`:609`): a 720x3 bar with a 34 px gap, then the button row with 16 px padding. Together they cover roughly y 860 to 1006.
- List cards are 940 tall and centred, so their bottom edge is at y 1010 (70 px from the bottom).
- Disabled looks: home PIN keys fade to alpha 0.4 while resting. Home learn-button rows use `DIM` (0xA6) when RemoteKeys isn't running. Kids stepper end arrows use Kids `DIM` 0xB3. Press dips: home PIN 0.94, Kids PIN 0.96.
- Adhan banner: `BAND 136`, `BAND_FADE 70`, `LIFT 56` (`AdhanService.java:280`). The words are padded 56 px off the bottom, leaving an 80 px content area. The Arabic is 50 px Plex SemiBold with font padding, about 86 px tall (win 1128/601 on upm 1000).

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/4-look-and-feel.md](archive/4-look-and-feel.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Split into specialist design lanes 9 to 15. Judge the whole picture here, hand details to the owning lane, and say when a detail had no lane to check it.
- 2026-11-05: Check commit history before calling a spec mismatch a bug: the Screen size default and the right-aligned column were both deliberate changes after the spec text was written.
- 2026-11-05: Read the helper, not the call site: `kicker()` overrides the colour passed to `text()`.
- 2026-11-05: Requesting an OpenType feature proves nothing; check the font file has it (`fontTools`, look at GSUB features).
- 2026-11-05: Name tables can be stale after instancing, so measure the glyphs.
- 2026-11-05: Target-SDK changes alter text rendering with no code change. Recheck them when targetSdk moves.
- 2026-11-05: Anything children see must be on-brand (no system Toasts); short copy that fits the pad beats smaller text.
- 2026-11-05: Judge from Update TV's screenshots: `C:\TVWallpapers\home.png`, `intro.png` and `Screens\{apps,quran,reciters,player,pin,calendar,stress,adhan}.png`. Ignore the top-left chip in `stress.png` unless it's an event day.
- 2026-11-05: Grep a colour across all three apps before calling it one app's colour.
- 2026-11-05: Once a specialist lane owns a topic, the shared copy here drifts. Point to the owner's file rather than restating numbers.
- 2026-11-05: When a layout is reorganised, list every view positioned relative to the old parts (eventChip used CARD_H of a hidden card). Leftovers hide in positions, not only in visibility flags.
- 2026-11-05: Count how many days a "rare" state shows before ranking it: the event chip is on screen for about a third of the year.
- 2026-11-05: A highlight that adds a box to a boxless design is a direction break; mark state with the accent colour instead.
- 2026-11-05: Check where the ambient/quiet animations look (topParts, centerGroup, lineFrame); anything outside them stays put and looks stuck.
- 2026-11-05: Without screenshots, a quick PIL mock from the code's real positions, fonts and colours, blurred at 20 px, is a usable squint test. Say plainly that it is a mock.

- 2026-11-05: A floating message placed "when shown" must be re-placed when the layer under it changes. Check every close path, not only the open.
- 2026-11-05: Check a new bottom-centre element against every full-screen layer that already uses bottom-centre (player controls, dock). Two elements anchored to the same edge collide.
- 2026-11-05: Keep one value for "not available now" across apps (about 38 to 40% on dark). A secondary-text grey reads as normal, not disabled.

