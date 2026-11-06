# Lane 10 knowledge: Typography: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
- 2026-11-06 PR #25 re-check c031f6c: M1 to M2 and S1 to S4 fixed; S5 deferred (Adhan card max 560 px); 26 px English floor on 8 items (Nice: Taha 20:132, Ibrahim 14:34, An-Nisa 4:86, Al-Fajr 89:27-30, Al-Baqarah 2:201 and 2:127, Abu Dawud 5067).
### PR #25 (2026-11-06, update 3: Islamic content)
- **2026-11-06, PR #25 (Update 3, Islamic content):**
  - Must: `amiri_bold.ttf` has `(` but no `)` (U+0029). The «(٥)» verse numbers in 16 of 110 verses mirror wrongly in RTL, so the fix is ۝ (U+06DD) plus Arabic-Indic digits.
  - Must: the band now overruns the strip: the Istighfar is 270 px tall, and 12 items have 2-line English, all in a 206 px gap.
  - Should: 37 verses go to 32 px because `length()` counts the marks; break strategy BALANCED; Kids `wordArabic` 0.8 spacing gives a −4 px ink gap on 2-line hadith; the stress test is stale; the Adhan card's 500 px column fits about 40 characters and grows to 631 px tall.
  - Nice: brackets in caps kickers (no `case` in Inter); three middle dots in one kicker; `:` missing from Amiri (Muslim 1631); ﷺ in Baloo on the Adhan card; the Ramadan strip label is +107 px.
  - Scripts: `/mnt/project-files/tv-box/reviews/pr25-lane10/`.

### PR #23 (2026-11-06, update 2: visual polish)
- 2026-11-06, PR #23: no Must. Audit S1 to S10 done (MixText Plex spans, tnum on Kids/adhan clocks, 22 px floor, CAPS_TRACK 0.12, Arabic-Indic digits in Amiri lines, ••• and ×, Amiri surah names). Should: `quranNow`/`nowSurah` in Amiri draw their English half in system Roboto (Amiri has no Latin, `-`, `'`, `·`); two splash lines (`IntroView.java:476, 557`) bypass `drawWelcome()`; surah medallion "100" to "114" at 22 px tnum is 39.5 px of ink in a 46 px ring (use pnum, 52 px); `playerSurah` 168 px Amiri with font padding is 459 px tall for 283 px of ink (padding off); verse reference "Al-Baqarah" letterspaced (uppercase it); Cormorant clock still `tnum` without `lnum`. Nice: Arabic 1.1× in splash vs 1.0× in kickers, kicker call sites say 20, Kids `wordArabic` top has 0.4 px spare (shadow cut), `fitPair` ignores spans.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22: no Must. S1: on the reciter loading line, the "…" sat on the wrong side of the Arabic, and the Arabic is in an Inter view. Fixed by moving the "…" to the English side; the separate fAr view waits for the visual batch. Nice: the home PIN message orphaned "again." (fixed with a line break), ⭐ comes from the emoji font (use ★), and the adhan row is 80 px for an 84.8 px Plex line (the ink fits).

- 2026-11-05, PR #21 (docs only): first lane 10 review. No Must fix. Should fix: Android 15 text defaults were described as if they apply on the Android 14 box; the Plex Arabic-Indic digit width claim was wrong; the tracking inventory was missing 0.22/0.14/0.1. Nice: Chivo character count, unused fonts, Fraunces width note, SF wording, missing size 50.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (main `d772bcc`): no confirmed Must fix. Should fix: Arabic in Inter views drawn by the system Naskh (kickers, weather words, toasts, splash), Kids Arabic marks likely clipped by `trim()` (computed: marks reach 1.15 em vs a 1.08 em line top minus 0.2 em trim), Kids/adhan Baloo clocks without `tnum` (AM/PM jumps about 70 px), 20 px and 18 px text (kickers, Arabic prayer names, surah numbers, calendar Arabic, adhan panel), strip Arabic 26 vs English 32, 8 caps tracking values, adhan `cardEn` 1.88 em leading, Western digits in Amiri lines, ⋯ and ✕ missing from Inter, surah names in two faces. Nice: Fraunces has no `tnum` upstream, Inter v3 Light, 440 KB unused fonts, Kids symbols missing from Baloo, Hijri weight, size count, weather emoji presentation, toast digits.


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
- [aosp-fonts-xml] https://android.googlesource.com/platform/frameworks/base/+/2a0135d19cab/data/fonts/fonts.xml: fallback order; `und-Arab` elegant NotoNaskhArabic first, then compact NotoNaskhArabicUI; symbols in NotoSansSymbols, emoji in NotoColorEmoji (older revision; the box's own list is unverified).
- [typefacespan-28] https://developer.android.com/sdk/api_diff/28/changes/android.text.style.TypefaceSpan: `TypefaceSpan(Typeface)` added in API 28.
- [fraunces-gh] https://github.com/arrowtype/Fraunces: axes (opsz 9 to 144, wght, SOFT, WONK), static instance naming (9pt/72pt/144pt); no figure-set features documented.
- [gf-fraunces-vf] Google Fonts `ofl/fraunces/Fraunces[SOFT,WONK,opsz,wght].ttf` (raw.githubusercontent.com/google/fonts/main/…): v1.000, GSUB `case liga rvrn ss01`, fvar default opsz 9 / wght 900 / SOFT 0.
- [inter-site] https://rsms.me/inter/: opsz text-to-display, Display "cleaner lines … for large text", `tnum` same width across weights, `zero`, `case`.

- [w3c-h34] https://www.w3.org/WAI/WCAG21/Techniques/html/H34 : an RLM after punctuation that ends an RTL phrase in LTR text.
- [w3c-inline-bidi] https://w3c.github.io/i18n-drafts/articles/inline-bidi-markup/index.en : neutrals at the edge of a phrase.

- [minikin-ls] https://android.googlesource.com/platform/frameworks/minikin/+/e8721b2%5E%21/ : `isScriptOkForLetterspacing` skips Arabic, N'Ko, Mongolian, Devanagari etc.; spacing applied per script run. Checked 2026-11-06.
- [textappearancespan] https://developer.android.com/reference/android/text/style/TextAppearanceSpan : `getLetterSpacing()` added in API 34. Checked 2026-11-06.
- [noto-naskh-vf] google/fonts `ofl/notonaskharabic/NotoNaskhArabic[wght].ttf` (raw.githubusercontent.com): stand-in for the box's fallback Arabic, for width comparisons.

From PR #25 (2026-11-06, update 3: Islamic content):
- [sargent-enclosures] Murray Sargent, "Arabic and mathematical enclosures", Microsoft blog archive: U+06DD encloses the Arabic-Indic digits that follow it, max 3 with Uniscribe/DWrite. https://learn.microsoft.com/en-us/archive/blogs/murrays/arabic-and-mathematical-enclosures
- [hb-mirror] HarfBuzz commit 7b8b63a, "Don't mirror character if font doesn't support mirrored character" (the diff wasn't shown): https://flutter.googlesource.com/third_party/harfbuzz/+/7b8b63adc5e0389fc4cf2720ef7e5804ab6c29cc

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
- Screenshot the Kids hadith card to confirm or rule out S2 clipping.
- Which Arabic fallback face (Naskh or Naskh UI) does the box really use in kickers? Answered in practice once S1 lands, but record it.
- After S1, do Plex spans in a 22 px Inter kicker need a size bump of +2 px for the Arabic (Apple +2 pt rule beside caps)?
- Inter v4 Display at 44 to 60 px vs Inter Light: mock on the TV.

- Screenshot the reciter loading line on the box after the fix.
- unicode.org (UAX #9) is blocked by the proxy here, so cite rules N1/N2 from a mirror.

- Does the box's StaticLayout use head yMax (1.057) or hhea ascent (1.08) for Baloo's first-line top with font padding on? It decides whether Kids marks have 0.4 or 1.4 px spare. Screenshot the hadith card.
- Kicker Arabic at 1.0× vs 1.1×: mock both on the TV and pick one for MixText and the splash.

From PR #25 (2026-11-06, update 3: Islamic content):
- Whether Minikin itemizes a lone `)` to the fallback font before or after mirroring (it affects which glyph shows). Unverified on the box: take a screenshot of an «(٥)» verse if M1 isn't fixed.
- Whether Amiri's ayah ornament digits (about 0.4 em) read at 32 px from the sofa. Check on the TV.
- Adhan card column is 500 px (about 40 characters a line); tallest card 560 px (Al-Fajr 89:27-30). Widen to about 700 px for long items? (lane 9 or 15)
