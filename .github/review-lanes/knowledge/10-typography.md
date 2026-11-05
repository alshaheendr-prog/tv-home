# Lane 10 knowledge: Typography

## How to use this file
- Read this whole file before every review, then the brief `../10-typography.md`. Lane 11's knowledge file has contrast maths, lane 4's the Centre stage spec; lane 6's has Arabic glyph coverage and digit locale facts. Don't repeat them here; link to them.
- Research online whatever the PR touches that this file does not cover (a new font, weight, feature, API). Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. Past about 400 lines, condense the oldest Past findings and Lessons. Never delete a source that is still cited.
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

## Past findings
- 2026-11-05, PR #21 (docs only): first lane 10 review. No Must fix. Should fix: Android 15 text defaults were described as if they apply on the Android 14 box; the Plex Arabic-Indic digit width claim was wrong; the tracking inventory was missing 0.22/0.14/0.1. Nice: Chivo character count, unused fonts, Fraunces width note, SF wording, missing size 50.

## Lessons learned
- 2026-11-05: Shape the text to learn what a feature does: Cormorant's `tnum` gives old-style tabular digits, which reading the feature list alone would not reveal.
- 2026-11-05: Check the cmap of every face a TextView uses: Inter has no Arabic, so mixed kickers use the system Arabic face; Amiri here has no Latin.
- 2026-11-05: Version strings matter: the app mixes Inter 3.019 and 4.001, and Amiri 1.002 is one release behind.
- 2026-11-05: Android "behavior changes for apps targeting N" also need the device to run Android N. Check the box's OS (Android TV 14) before applying a target-35 default.
- 2026-11-05: Check digit widths for every numeral system a face has. Plex's Western digits are tabular, its Arabic-Indic digits aren't.
- 2026-11-05: To find every tracking value, grep `setLetterSpacing` across all three apps and Canvas views (`IntroView` uses `Paint.setLetterSpacing`).
- 2026-11-05: Apple's tracking table is in pt. Convert it to em before comparing (+0.40 pt at 29 pt = 0.014 em).

## Open questions to research next
- Which system Arabic font draws the kicker Arabic on this Android TV 14 box (Noto Naskh Arabic or the UI cut), and does it clash with Plex? Screenshot a kicker.
- On the box (Android 14), screenshot a Quran line and a Plex Hijri line to confirm metrics-based line height (no elegant metrics). Repeat if the box ever gets Android 15.
- Is any Arabic-Indic number that changes on screen set in a Plex face (Hijri date, toasts like "بعد ١٠ دقائق")? If so, does it wobble?
- How long is the longest reciter name at 24 px Plex, and the longest surah name at 26 px Amiri, against the now-reciting box width?
- Type-size count (from lane 4): the home uses about 16 sizes vs NN/g's 3 and Smashing's 5 to 6. Audit in a whole-app review.
- Re-instance Fraunces at opsz 144, SOFT 100, wght 300 with `tnum` (fontTools `instancer` on the variable font) and compare stem contrast and width with the bundled file.
- Would Inter Display (or Inter's opsz axis in v4) suit the 44 to 60 px numbers better than Inter Light v3.019?
- Kickers: 22 px at 0.10 to 0.12 tracking vs today's 20 px at 0.18; mock both at 3 m.
- Should the strip's Arabic adhan name match or exceed its English (26 vs 32 px)?
- Update Amiri to 1.003, and check whether "Amiri Quran" handles pause marks better than Amiri Bold at 32 to 38 px (with lane 6).
- Open the Google Fonts knowledge pages on pairing, x-height and optical size (they need JavaScript; not opened this session).

## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [tv-typography] https://developer.android.com/design/ui/tv/guides/styles/typography: TV type roles, legibility, pairing, optical size, avoid decorative fonts.
- [tv-type-tokens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/tokens/TypeScaleTokens.kt: TV sizes, line heights, tracking, weights.
- [mdc-typography] https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Typography.md: Material 3 baseline and emphasized type scale.
- [hig-typography] https://developer.apple.com/design/human-interface-guidelines/typography (read via https://developer.apple.com/tutorials/data/design/human-interface-guidelines/typography.json): tvOS sizes and leading, 29/23 pt, avoid light weights, SF tracking table. The JSON also has the full SF Pro / SF Compact tracking tables (6 to 96 pt) and the tvOS table with an "Emphasized weight" column (Bold); re-checked 2026-11-05.
- [hig-rtl] https://developer.apple.com/design/human-interface-guidelines/right-to-left (via .../right-to-left.json): Arabic +2 pt beside Latin caps, alignment, numerals.
- [butterick-ls] https://practicaltypography.com/letterspacing.html: 5 to 12% for caps, none for lowercase.
- [butterick-lsp] https://practicaltypography.com/line-spacing.html: 120 to 145% line spacing.
- [butterick-ll] https://practicaltypography.com/line-length.html: 45 to 90 characters.
- [butterick-caps] https://practicaltypography.com/all-caps.html: caps for short labels, letterspaced, kerned.
- [alreq] https://w3c.github.io/alreq/ (read via https://raw.githubusercontent.com/w3c/alreq/gh-pages/index.html): Arabic extents, no letter-spacing, digits, kashida, mixed-script alignment.
- [css-text-3] https://raw.githubusercontent.com/w3c/csswg-drafts/main/css-text-3/Overview.bs: letter-spacing must not break cursive connections; spacing disables optional ligatures.
- [ms-figures] https://learn.microsoft.com/en-ca/typography/develop/character-design-standards/figures: tabular widths, old-style figure heights.
- [glyphs-figures] https://glyphsapp.com/learn/figure-sets/: tnum/pnum/lnum/onum and how figure sets are classified.
- [textview-ref] https://developer.android.com/reference/android/widget/TextView: letter-spacing, line height, font padding, fallback spacing, elegant height, ellipsize, font features.
- [linebreaker-ref] https://developer.android.com/reference/android/graphics/text/LineBreaker: SIMPLE, HIGH_QUALITY, BALANCED.
- [autosize] https://developer.android.com/develop/ui/views/text-and-emoji/autosizing-textview: autosize defaults and wrap_content warning.
- [a15-behavior] https://developer.android.com/about/versions/15/behavior-changes-15: elegantTextHeight default true, useBoundsForWidth, start overhang (Android 15+ devices only).
- [cozens-clipping] https://simoncozens.github.io/android-clipping: how Views clip text by ascent/descent; `includeFontPadding` uses glyph yMax/yMin; `elegantTextHeight` replaces font metrics with hard-coded 1900/-500 (padding off) or 2500/-1000 (padding on) per 2048 UPM; fallbackLineSpacing. Checked 2026-11-05.
- [aosp-elegant] https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/c6d3e0b1a2ff12870acd6aeec47326526b68b781: mirror of the AOSP commit that skips hard-coded metrics for apps with targetSdk >= 35 (Android 15 framework). Checked 2026-11-05.
- [tv-carousel] https://developer.android.com/design/ui/tv/guides/components/featured-carousel: scrim behind text over imagery.
- [fraunces-readme] https://raw.githubusercontent.com/undercasetype/Fraunces/master/README.md: axes and optical size.
- [inter-readme] https://raw.githubusercontent.com/rsms/inter/master/README.md: design intent, features, Display subfamily.
- [inter-v4] https://github.com/rsms/inter/releases/tag/v4.0: v4 changes (listed by lane 4, not re-opened).
- [plex-arabic-readme] https://raw.githubusercontent.com/IBM/plex/master/packages/plex-sans-arabic/README.md: weights and packaging.
- [amiri-readme] https://raw.githubusercontent.com/aliftype/amiri/main/README.md: Bulaq heritage, development status.
- [amiri-news] https://raw.githubusercontent.com/aliftype/amiri/main/NEWS.md: 1.001 to 1.003 changes.
- [cormorant-readme] https://raw.githubusercontent.com/CatharsisFonts/Cormorant/master/README.md: family structure.
- [baloo-readme] https://raw.githubusercontent.com/EkType/Baloo2/master/README.md: Baloo 2 design and weights.
- [wiklund-2019] https://consensus.app/papers/details/6a523653b68b5be49d4c2f7f922d2f12/ (Wiklund et al. 2019): 20 to 22 arcmin character height (abstract only).
- [seibert-1959] https://consensus.app/papers/details/54cbdc2057c858d99016252f988fadd6/ (Seibert et al. 1959, J. SMPTE): 10 arcmin televised characters (abstract only).
- [bertolus-2017] https://consensus.app/papers/details/83b177020ed25ffaaf9cae70fc30193b/ (Bertolus et al. 2017): household TV distances vs UI text size (abstract only).
- [tomioka-2007] https://consensus.app/papers/details/1d637990012f56319bf7cca8b0be5c0d/ (Tomioka 2007, J. Physiol. Anthropol.): thinner characters in negative mode for older viewers (abstract only).
- To open next: Google Fonts knowledge (pairing, optical size, x-height), Amiri documentation page, OpenType feature registry pages on learn.microsoft.com, Noto Naskh Arabic metrics.
