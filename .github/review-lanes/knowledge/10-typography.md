# Lane 10 knowledge: Typography

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../10-typography.md`.
- History lives in [archive/10-typography.md](archive/10-typography.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
- Lane 11's knowledge file has contrast maths, lane 4's the Centre stage spec; lane 6's has Arabic glyph coverage and digit locale facts. Don't repeat them here; link to them.
- Mark anything not verified in code, a font file or a screenshot with **(unverified)**. Font facts below were measured with fontTools 4.68 and HarfBuzz (uharfbuzz 0.57) on the repo's files on 2026-11-05.

## Core expertise

### 1. Size at 10 feet
- Units: 1 sp = 2 px on our 1920x1080 stage (Google designs TV at 960x540 dp); 1 tvOS pt = 1 px. [tv-type-tokens] [hig-typography]
- **Google TV type scale** (sp → stage px, line height ratio): Display L/M/S 57/45/36 (114/90/72 px, ~1.12 to 1.22); Headline L/M/S 32/28/24 (64/56/48 px, 1.25 to 1.33); Title L/M/S 22/16/14 (44/32/28 px); Body L/M/S 16/14/12 (32/28/24 px, line height 24/20/16 sp = 1.5 / 1.43 / 1.33); Label L/M/S 14/12/11 (28/24/22 px), Medium weight, tracking 0.1 to 0.5 sp. Display L tracking -0.2 sp. [tv-type-tokens] Phone Material uses the same 15 roles (Display Large Regular 57 sp ... Label Small Medium 11 sp). [mdc-typography]
- Google TV: "Prioritize using larger typography"; "Ensure fonts are readable at a glance, with appropriate width and optical sizing"; "use a sans-serif typeface for body text and labels"; "More personable fonts are best suited to bigger size text"; avoid decorative fonts; "thinner lines are not instantly recognisable". Display styles are "reserved for short, important text passages, or numerals" (our clock); "For display type, consider choosing a more expressive font. If available, set the appropriate optical size". [tv-typography]
- **tvOS text styles** (pt = px, weight, leading): Title 1 76/96 Medium; Title 2 57/66; Title 3 48/56; Headline 38/46; Subtitle 1 38/46 Regular; Callout 31/38; Body 29/36; Caption 1 25/32; Caption 2 23/30 (all Medium unless noted). **Default 29 pt, minimum 23 pt.** "If you use a custom font with a thin weight, aim for larger than the recommended sizes". [hig-typography]
- **Visual angle**: legible character height is about **20 to 22 arcmin** (16 to 18 may be acceptable) [wiklund-2019]; 10 arcmin televised characters were read almost perfectly in a lab [seibert-1959]; real household distances make many TV UIs hard to read [bertolus-2017]. On a 55" TV at 3 m (0.634 mm per stage px): 10 arcmin = 13.8 px, 16 = 22.0, 20 = 27.5 px of **character (cap) height**; on 65": 11.6 / 18.6 / 23.3 px. Inter's cap height is 0.728 em, so 20 arcmin caps need **about 38 px Inter on 55"** (32 px on 65"); our 20 px kickers have 14.6 px caps, about 10.6 arcmin on 55": readable, but at the threshold. (computed)
- Smallest sizes for this repo: 22 px absolute floor (Google Label Small), 24 px for anything the family must read, 28 px+ for body copy they read at length.

### 2. Weight
- Apple: "avoid light font weights ... prefer Regular, Medium, Semibold, or Bold ... avoid Ultralight, Thin, and Light font weights, which can be difficult to see, especially when text is small." [hig-typography]
- Google: legible font width; thin lines are not instantly recognisable. [tv-typography]
- A 2007 study (70 people, aged 46 to 80) found thinner characters in negative mode (light on dark) were judged more legible by older viewers, especially in a dark room (abstract only), so very heavy weights are not automatically better on our dark UI. [tomioka-2007]
- Rule here: Thin/ExtraLight/Light only at about 60 px+; small text Medium or SemiBold; text over video without a box prefers SemiBold.

### 3. Tracking (letter-spacing)
- Butterick: add **5 to 12%** letterspacing (0.05 to 0.12 em) to all caps and small caps, especially at small sizes; "Lowercase letters don't ordinarily need letterspacing"; too much is when another letter would fit between. [butterick-ls]
- Caps: only for short labels "shorter than one line"; always letterspace and kern them; never whole paragraphs. [butterick-caps]
- SF Pro's own tracking (a good model for a UI sans, in pt, mixed-case text): negative from 13 to 23 pt, positive at 11 pt and below; +0.40 pt at 29 pt (= +0.014 em) falling to +0.07 at 76 pt and 0 from 80 pt. Large sizes want less positive tracking. Convert pt to em before comparing with ours. [hig-typography]
- Android `setLetterSpacing` is in em ("Typical values for slight expansion will be around 0.05. Negative values tighten text."). [textview-ref]
- **Arabic is a cursive script**: CSS Text 3 says a renderer that cannot elongate cursively "must not apply spacing between any pair of that script's typographic letter units at all"; also, non-zero spacing should turn off optional ligatures. [css-text-3] alreq: moving joined letters closer or further "creates undesirable results"; stretching (kashida) is not letter-spacing. [alreq] Android's Minikin skips letter-spacing for cursive scripts (see lane 6 knowledge, [minikin] there).
- Our values: kicker 0.18 (above Butterick), verse kicker 0.16, splash kicker 0.22 (`IntroView.java:58`, Paint), adhan card kicker 0.08, adhan Settings 0.1 (`SettingsActivity.java:102`), Kids 0.12 / 0.14 (`section()` `:1972`) / 0.16 / 0.2, dock label 0.02, AM/PM 0.08, clocks -0.03 to 0. That is 8 caps values (0.08 to 0.22); move toward one (about 0.10 to 0.12).

### 4. Line height (leading) and vertical metrics
- Butterick: "the optimal line spacing is between **120% and 145%** of the point size." [butterick-lsp] Google TV body 1.33 to 1.5; tvOS Body 36/29 = 1.24, Title 1 96/76 = 1.26. [tv-type-tokens] [hig-typography]
- Apple: with three or more lines, avoid tight leading even where height is limited. [hig-typography]
- Android: `setLineSpacing(add, mult)` multiplies each line except the last; `setLineHeight(unit, value)` sets baseline-to-baseline distance (API 34 for the unit form); `setFirstBaselineToTopHeight`/`setLastBaselineToBottomHeight` set padding from font metrics and can clip if the view is too short; `includeFontPadding` (default true) adds room "for accents that go above the normal ascent and descent"; `fallbackLineSpacing` (default true) lets a taller fallback font grow the line. [textview-ref]
- Target 35 (Android 15): `elegantTextHeight` defaults to **true** ("replacing the compact font used by default with some scripts that have large vertical metrics"), Arabic is named for testing; `useBoundsForWidth` gives cursive or overhanging glyphs room, but the start-overhang shift is **off** by default. [a15-behavior] Android 15+ devices only: the gate (`targetSdk >= 35`) is in Android 15's framework, and `setUseBoundsForWidth` is API 35. The box is Android TV 14, so line heights come from hhea ascent/descent plus `includeFontPadding` (inferred, unverified on the box). If the box updates to Android 15, recheck every Amiri/Plex line. [aosp-elegant] How the hard-coded metrics clip: [cozens-clipping].
- What a TextView line is on Android: font size x (hhea ascent + |descent|) / upm with padding off; padding on adds the `head` yMax/yMin overshoot once at top and bottom. Values for our fonts are in "This box and this code".

### 5. Line length, wrapping, truncation
- 45 to 90 characters per line including spaces (two to three alphabets). [butterick-ll]
- Android break strategies: `HIGH_QUALITY` (TextView default) does whole-paragraph optimisation; `BALANCED` makes all lines a similar length (good for short centred or right-aligned blocks like the verse); `SIMPLE` is greedy. Hyphenation defaults to NONE since Android Q. [linebreaker-ref] [textview-ref]
- `setEllipsize` with `maxLines` ≥ 2 supports only END and MARQUEE. [textview-ref]
- Autosize (uniform) defaults 12 to 112 sp, granularity 1 px, and should not be used with `wrap_content`. [autosize]
- Apple: keep truncation to a minimum; prefer more lines or a stacked layout to cutting text. [hig-typography]

### 6. Arabic and Latin together
- alreq: "Arabic ascenders and descenders extend much further than those of the Latin script, and care must be taken to correctly align text in the different scripts when they appear together"; it notes "Shapes and proportions (the size issue) in mixed texts" and, for small screens, fonts with "larger loop and teeth height, small descenders" (no numeric rule). [alreq]
- **Size balance (answers lane 4's open question):** Apple: "Arabic and Hebrew text can look too small when next to uppercased Latin text, because Arabic and Hebrew don't include uppercase letters ... it often works well to increase the RTL font size by about **2 points**." [hig-rtl] Our verse already sets Arabic 38 vs English 32 (+19%); the strip Arabic adhan name 26 vs English 32 is the reverse (Arabic smaller).
- Alignment: one- and two-line text can follow the context; paragraphs (3+ lines) align to their own language; keep one alignment within a list. [hig-rtl]
- Digits: European digits (Maghreb), Arabic-Indic (Egypt, Gulf, Iraq), Extended Arabic-Indic (Iran, Afghanistan). Arabic-Indic digits are bidi class AN, European EN, so similar sentences can reorder; the lowest digit is always on the right; percent sign left of Arabic-Indic numbers in Middle-East style. [alreq] "Don't reverse the order of numerals in a specific number." [hig-rtl]
- Justification: Arabic lines normally right-aligned; prefer restrained kashida; excessive kashida gives uneven colour; never several in one word. [alreq]
- Fallback trap: a TextView whose typeface has no Arabic (Inter) draws Arabic with the system fallback font (lane 6 verified cmap). Our kickers and any "English · عربي" line in Inter therefore show the system Arabic face, not Plex (which system face on this box is unverified).

### 7. Figures (numerals)
- Tabular: "all character's advance widths are the same value"; proportional figures vary. Old-style figures: 0, 1, 2 sit at x-height, **3, 4, 5, 7, 9 descend** toward the descender, 6 and 8 ascend; "best suited for text settings". Lining figures share cap-like flat heights. [ms-figures]
- OpenType tags: `tnum` tabular, `pnum` proportional, `lnum` lining, `onum` old-style; a font can combine them (tabular old-style `.tosf`, tabular lining `.tf`). [glyphs-figures]
- Android `setFontFeatureSettings` takes a CSS `font-feature-settings` string, e.g. `"tnum, lnum"`. [textview-ref] Requesting a feature the font lacks does nothing silently.
- For a clock or countdown: tabular lining figures, or a monospaced face (Chivo Mono in the adhan app), or a right-aligned layout that tolerates width changes (our home column).

### 8. The fonts (design intent, from the projects)
- **Fraunces**: display "Old Style" soft serif; axes opsz 9 to 144 (default 144), wght 100 to 900, SOFT 0 to 100, WONK (wonky forms substitute automatically above opsz 18). Lower opsz raises x-height, opens spacing, widens letters. [fraunces-readme]
- **Inter**: designed for screens, tall x-height; features include contextual alternates, slashed zero, tabular numbers; early versions were poor at very large sizes, now fine, and a Display subfamily exists for big sizes. [inter-readme] v4 changed UPM to 2048. [inter-v4]
- **IBM Plex Sans Arabic**: part of the IBM Plex family; its web package imports light, regular and semibold by default and all weights on request. [plex-arabic-readme] It pairs naturally with a neutral grotesque like Inter (design judgement).
- **Amiri**: Naskh revival of the Bulaq (Amiria) press face, "one of the few metal typefaces that were used in typesetting the Quran"; developed 2008 to 2022, considered mature. [amiri-readme] **1.003 (2025-05-15)** restores Latin kerning broken since 1.001; 1.002 fixed sukun/jazm mark positioning and stopped kerning tabular numbers; 1.001 (2024-11-19) uses `rlig` for required contextual forms and fixes double kashida before ain. [amiri-news]
- **Cormorant**: display family by Christian Thalmann, 9 styles including Garamond (larger counters) and Upright, 5 weights, OFL. [cormorant-readme]
- **Baloo 2**: "affable display typeface" by Ek Type, heavy, spurless, five weights; Baloo Bhaijaan 2 is the Arabic member (the README lists the Indic members). [baloo-readme]

### 9. Text over video
- Google TV: use a scrim behind text on images; avoid images with text. [tv-carousel]
- Apple (visionOS context, still useful): for text with no surface behind it, consider making it bold rather than relying on shadows. [hig-typography]
- Our practice (lane 4): tight shadow radius 4 to 8, dy 2 to 3, 40 to 70% black; contrast maths in lane 11 knowledge.

### 10. Review recipes
Font audit (version, metrics, features, coverage) with fontTools:
```python
from fontTools.ttLib import TTFont
t = TTFont(path); n = t["name"]; o = t["OS/2"]; h = t["hhea"]; hd = t["head"]
print(n.getDebugName(4), n.getDebugName(5), hd.unitsPerEm, o.usWeightClass, "fvar" in t)
print(h.ascent, h.descent, hd.yMax, hd.yMin, o.sxHeight, o.sCapHeight)
print(sorted({r.FeatureTag for r in t["GSUB"].table.FeatureList.FeatureRecord}))
cm = t.getBestCmap(); print([c for c in "·°●★٠١٢" if ord(c) not in cm])
```
What a feature really does (glyph names after shaping), with uharfbuzz in a venv:
```python
import uharfbuzz as hb
f = hb.Font(hb.Face(hb.Blob.from_file_path(path))); b = hb.Buffer(); b.add_str("3579")
b.guess_segment_properties(); hb.shape(f, b, {"tnum": True}); print([i.codepoint for i in b.glyph_infos])
```
(map ids to names with `TTFont(path).getGlyphOrder()`). Widths: see the HarfBuzz recipe in lane 9's knowledge file.
Visual angle of a size: cap px = size x capHeight/upm; arcmin = atan(cap px x mm per px / distance mm) x 60 x 180/π; mm per px = TV height mm / 1080 x (Screen size).
On-TV check: stress test (`Update TV`), screenshot, crop each text run at 100%: look for clipped Arabic marks, colliding lines, jumping digits (take two screenshots a minute apart), fallback Arabic glyph shapes in kickers.

## This box and this code
Verified on 2026-11-05 (line numbers drift; re-grep).
- **Bundled fonts** (file: version, upm, weight, hhea asc/desc, head yMax/yMin, GSUB features):
  - `fraunces_soft_light.ttf`: name "Fraunces 9pt Black", "Version 1.000;[b76b70a41]", upm 2000, wght 300, static, 698 glyphs, 1956/-510, 2050/-493; GSUB `case liga ss01` only (no `tnum`, `lnum`); proportional digits (advances 687 to 1133).
  - `inter_medium/semibold/bold.ttf`: Inter 4.001, upm 2048, 2871 glyphs, 1984/-494, yMax about 2275; GSUB `calt ccmp dnom frac locl numr pnum tnum` (no `case`, `zero`, `ss0x`, `cv`). x-height 0.546, cap 0.728.
  - `inter_light/extralight.ttf`: Inter **3.019**, upm 2816, 498 glyphs, 232 characters (no ●, ★), 2728/-680.
  - `plex_arabic_medium/semibold.ttf`: 1.101, upm 1000, 1085/-415, yMax/yMin about 1100/-580; `calt ccmp ... rlig`, no `tnum`; Western digits all 600 wide, **Arabic-Indic digits proportional** (٠288 ١271 ٢495 ٣632 ٤502 ٥543 ٦505 ٧537 ٨537 ٩526).
  - `amiri_bold/regular.ttf`: 1.002, upm 1000, ~5300 glyphs, **Arabic-only cmap (1104 characters: no Latin letters, Western digits or colon)**, hhea 1124/-634 (1.758 em line), yMax/yMin 1823/-909 (2.73 em with font padding); GSUB `ccmp dnom fina init locl medi numr pnum rlig`, GPOS `curs kern mark mkmk`.
  - `cormorant_light.ttf`, `cormorant_semibold_italic.ttf`: Cormorant Garamond 4.001, 373 glyphs, 924/-287, x-height 0.386; default figures **old-style proportional** (3 descends to -275); `tnum` → `three.tosf` (tabular old-style), `tnum, lnum` → `three.tf` (tabular lining). Full name reads "Cormorant Garamond Light SemiBold Italic" (stale name).
  - `outfit_thin.ttf` 1.100 (224 characters) and `jost_extralight.ttf` 3.710 (217): small Latin subsets with `tnum`. `chivo_mono_bold.ttf` 1.008: 642 characters / 800 glyphs, with `tnum`.
  - `baloo_medium/bold/extrabold.ttf` (kids and adhan): Baloo Bhaijaan 2 1.701, 1080/-632 (1.71 em, so Kids' 0.8 line spacing and `trim()`), has Arabic-Indic digits and `tnum`; missing ●, ★ and Quranic marks (lane 6).
- `text()` home (`MainActivity.java:793`): px size, padding off, shadow at 100 px+. About 17 views turn padding back on (`lineAr`, `skyText`, `hijriText`, `adhanAr`, `eventText`, the Quran grid and player titles, `nowSurah`, `nowReciter`, ...; grep `setIncludeFontPadding(true)`). `kicker()` `:863`.
- Clock (`applyClockFont()` `:1012`): sizes `{340, 310, 306, 330, 304}`, tracking `{-0.01, -0.02, 0, -0.01, -0.03}`, AM/PM lift `{58, 50, 50, 56, 48}`, AM/PM Inter Light 50 px for Inter, else the clock face. Width of the time at 340 px Fraunces (HarfBuzz, with -0.01 tracking; without it, 521 to 748 px): "11:11" 504, "8:08" 574, "12:58" 659, "23:58" 701, "10:00" 731 px.
- Verse (`nextLine()` `:1605`): Arabic 38 → 32 px over 110 characters; English 32 → 28 px over 150; max width 1700; English line spacing 1.05 (below Butterick's 1.2 floor, but usually one line). Stress verse: Arabic 888 px at 32 px, English 1448 px (146 characters) at 28 px, one line each.
- Strip: adhan English 32 px SemiBold vs Arabic 26 px Plex SemiBold (Arabic smaller than its English); countdown 44 px Inter Light `tnum` (real tabular in Inter); prayer times 24 px SemiBold `tnum`, names 22 px Medium.
- Home sizes in use (px): 20 (kickers), 22, 24, 26, 28, 30, 32, 34, 38, 44, 50, 58, 60, 66, 168, 340 (plus clock alternates). Kids sizes: mostly 30 to 64, plus 72, 88, 92, 112 (clock), 140, 200. Adhan: 20 to 64.
- Kids `text()` (`KidsActivity.java:1902`): line spacing 0.8, `trim()` -0.2/-0.26 em, `ink()` shadow by luminance. Caps tracking: "WELCOME" 0.2 (`:439`), world chip 0.12, `charFrom` 0.16, `wordKick` 0.2. No `setFontFeatureSettings` anywhere in Kids.
- Unused font files, still packed into the APKs (2026-11-05): `app/.../font/amiri_regular.ttf` (317 KB), the app's `chivo_mono_bold.ttf` (59 KB; only the adhan app uses Chivo) and `adhan/.../font/inter_light.ttf` (62 KB): about 440 KB to drop in a polish PR.
- `nowSurah` / `nowReciter` use `setMaxLines(1)` with no ellipsize (`MainActivity.java:2926-2927`), so long names are cut silently at the edge (against the no-cut rule). Check with the longest reciter name.
- `adhanName` is built with autosize 30..38 (`:917-919`), then switched to a fixed 32 px (`:2449-2450`); `maxLines(1)` stays.
- All 3 apps: compileSdk/targetSdk 35 (`*/build.gradle.kts`). The box runs Android TV 14, so Android 15 text defaults don't apply.
- Adhan (`AdhanService.java`): countdown and times in Chivo Mono Bold (`:209`, `:228`), clock Baloo ExtraBold 64 px, alert 46 px Inter Medium + 50 px Plex SemiBold gold (Arabic +4 px), verse card Amiri Bold 34 px RTL, padding on, line spacing 1.15 (`:710-716`), English Baloo Medium 26 px line spacing 1.1, kicker 20 px Baloo Bold tracking 0.08.

Corrections from the whole-app audit (2026-11-05):
- `fraunces_soft_light.ttf` **is** the right instance: its digit advances match an instance of the upstream variable font at **opsz 144, SOFT 100, wght 300** exactly (opsz 9 or 72 give wider digits). The "9pt Black" name is only a stale name record. No re-instance needed.
- `setLineSpacing(add, mult)` multiplies the font's own line height (hhea ascent + descent), not the text size. Effective leading: `lineEn` Cormorant 1.211 em × 1.05 = **1.27 em** (inside 1.2 to 1.45, so not "below the floor"); Kids Baloo 1.712 × 0.8 = 1.37 em; adhan `cardEn` 1.712 × 1.1 = 1.88 em (too loose); `pinSub` Inter 1.21 × 1.15 = 1.39 em; Amiri `cardAr` 1.758 × 1.15 = 2.02 em. (From framework behaviour in StaticLayout; the TextView docs page could not be read this session.)
- Baloo Bold: Arabic mark stacks reach 1.15 em above the baseline (HarfBuzz, GoodWords strings), above the hhea ascent of 1.08 em. Any negative top padding on Baloo Arabic risks clipping.
- Baloo ExtraBold digits: "11:11" 1848, "12:58" 2280, "10:00" 2469 units by default; 2682 for all with `tnum`.
- Inter (all bundled files): no ✕ (U+2715) or ⋯ (U+22EF); has × • − ★ ✓ ● ⌫. Baloo: no ★ ✓ ✗ ⌫ ● ﷺ. Plex Arabic: has ✓ ∞ × •. Amiri: no symbols, Western digits, colon or °.
- Home Arabic-fallback views (Inter, no Arabic): every `kicker()`, `lineKicker`, `skyText` (via `skyName()`), `eventText`, `pillText` toasts, `IntroView` WELCOME (Paint).
- Western digits inside Amiri lines: `Content.java` character facts (years), `Widgets.java:27` heat alert, `:38` Jumu'ah time.
- Surah names: Amiri in the grid and strip, Plex in `quranNow` and `playerSurah`.

Added by the PR #23 review (2026-11-06):
- `MixText` (`app/.../MixText.java`): `text()` makes every non-Arabic-face view a MixText; whole Arabic runs (incl. spaces, digits, ، ؟) get `TypefaceSpan(Plex Medium or SemiBold)`. Arabic-face views (fAr, fArSemi, fQuran) stay plain TextViews, so Latin inside them still falls back.
- `CAPS_TRACK = 0.12` used everywhere except AM/PM 0.08, dock label 0.02, clocks.
- `kicker()` forces 22 px whatever size the call site passes.
- Amiri Bold surah titles ("سورة " + 114 names): ink +1.019 / −0.664 em, max width 5.34 em.
- Plex Arabic SemiBold vs Inter SemiBold: Plex hhea 1.085/−0.415 vs Inter 0.969/−0.241, so a kicker with an Arabic span is about 6 px taller at 22 px. Plex teeth 0.34 em, alef/lam about 0.88 em; Inter caps 0.728, x 0.546.
- Plex Arabic Medium is 3 to 7% wider than Noto Naskh Arabic 500 on the weather and event strings.
- Inter SemiBold digits at 22 px: tnum "114" 42.7 px advance / 39.7 ink; pnum "114" 33.3 / 31.2, "100" 38.3 / 36.2.

Added by the PR #25 review (2026-11-06):
- `amiri_bold.ttf` (app and adhan) cmap: has `(`, ۝ (U+06DD), ﴾ ﴿; **no `)`, `:`, Latin or Western digits**. ۝ followed by 1 to 3 Arabic-Indic digits renders as the enclosed ayah ornament.
- Band vertical budget: text from `LINE_Y` 494 to `STRIP_Y` 700 = 206 px. The strip's first ink is at about y 732. Block height ≈ 6 + 33 (kicker with Plex) + 6 + Amiri (2.732 em first line, +1.758 em each extra line) + 2 + Cormorant 1.211 em × (1 + 1.05 per extra line) + 10.
- The Adhan card text column is 500 px (card 560, padding 30 + 30).
- Kids `wordCard` text column is 714 px. `wordArabic` is Baloo Bold 42 px with a 1.37 em pitch.

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/10-typography.md](archive/10-typography.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Shape the text to learn what a feature does: Cormorant's `tnum` gives old-style tabular digits, which reading the feature list alone would not reveal.
- 2026-11-05: Check the cmap of every face a TextView uses: Inter has no Arabic, so mixed kickers use the system Arabic face; Amiri here has no Latin.
- 2026-11-05: Version strings matter: the app mixes Inter 3.019 and 4.001, and Amiri 1.002 is one release behind.
- 2026-11-05: Android "behavior changes for apps targeting N" also need the device to run Android N. Check the box's OS (Android TV 14) before applying a target-35 default.
- 2026-11-05: Check digit widths for every numeral system a face has. Plex's Western digits are tabular, its Arabic-Indic digits aren't.
- 2026-11-05: To find every tracking value, grep `setLetterSpacing` across all three apps and Canvas views (`IntroView` uses `Paint.setLetterSpacing`).
- 2026-11-05: Apple's tracking table is in pt. Convert it to em before comparing (+0.40 pt at 29 pt = 0.014 em).
- 2026-11-05: Before saying a font "lacks a feature, re-instance it", check the upstream source. Fraunces has no tabular figures in any instance.
- 2026-11-05: Read `setLineSpacing` multipliers against the font's line height, not the size. Tall fonts (Baloo 1.71 em, Amiri 1.76 em) turn a harmless-looking 1.1 into very loose leading.
- 2026-11-05: Negative padding "trims" must be checked against the tallest shaped string (HarfBuzz extents over the real content), not against the font's ascent.
- 2026-11-05: Grep every non-ASCII character in the code and check it against the cmap of the face that draws it. That finds fallback glyphs (⋯, ✕, ★) that look fine on a PC mock.

- 2026-11-05: Arabic followed by a neutral (… ! . :) in an LTR line needs an RLM after it, or the neutral moved to the Latin side. Check with python-bidi `get_display(s, base_dir='L')`.
- 2026-11-05: A temporary message must keep the line count of the text it replaces, at the real column width (PIN 472 px on the home, 440 px in Kids, parent rows 728 px).
- 2026-11-05: When a row shrinks (adhan LIFT 56 and BAND 136 leave 80 px), check the shaped ink of every string against it. Plex Arabic at 50 px has ink from +39.1 to −13.5 px.

- 2026-11-06: When a fix moves a view to another face, check every string that view shows, including the Latin half of a `fitPair` / `oneLine` pair. Amiri has no Latin.
- 2026-11-06: After a "use the helper" fix, grep for every literal copy of the string (`IntroView` had three WELCOME draws, and only one was converted).
- 2026-11-06: `tnum` is for numbers that change or line up in columns. In a fixed-size badge it makes 3-digit numbers wider (tabular 1).
- 2026-11-06: Switching a big title to Amiri with font padding on adds about 1 em of empty air (head yMax 1.823). Turn padding off for display sizes and check the HarfBuzz ink.
- 2026-11-06: There is no public letter-spacing span before API 34, so a mixed-case tail in a caps kicker needs uppercasing or its own view.

- 2026-11-06: Check every bracket and punctuation mark in RTL text against the cmap *in pairs*: a font with only one of a mirrored pair draws the RTL pair wrong (HarfBuzz won't mirror to a missing glyph).
- 2026-11-06: Never pick Arabic sizes by `String.length()` on vocalised or Uthmani text: the marks inflate the count by about 40%. Measure the width.
- 2026-11-06: When content changes (not code), re-run the band height budget over *all* strings, and make the stress test the tallest real item.
