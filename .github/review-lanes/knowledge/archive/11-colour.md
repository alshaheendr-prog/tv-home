# Lane 11 knowledge: Colour and contrast (palette, text over video, gold, TV panels): archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- Must: the event line went boxless gold into an unprotected zone. Over sky it is 1.44:1 / Lc 19, over snow 1.04:1 / Lc 0 (the old chip gave Lc 41 over sky). Fix: `WHITE` words with a gold `dotLine` dot.
- Should:
  - The verse fade's 4 even stops give only about 25% black under the kicker and the last line (Lc 57 sky / 48 snow). Use 6 stops.
  - Extend the top scrim to 480 px `{0x80,0x4D,0}`.
  - `NIGHT_TEXT 0.55` gives the verse Lc 31-38. Use 0.7.
  - The next prayer is marked by colour only (1.04-1.32:1), and its gold time is weaker than white (Lc 42 vs 61 over sky). Keep it `WHITE` with a 3x36 px gold underline.
  - Calendar focus fill 1.26:1: add a gold start tick and a `SOFT` date line.
- Nice: reciter Lc 55 over snow (shade mid stops `0x66`); `SOFT`/`DIM` step 1.41:1; `Glass` and Kids panel are still undithered.
- Fine: one gold, kickers, `DIM` raise, drive dot, Kids focus and pause disc, Kids player text, adhan reminder card.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05 PR #22: no new palette colours or gradients. The new disabled/resting states use view alpha and `DIM`:
  - Should: the focused PIN key at 0.4 alpha while the pad rests has a gold ring of 2.6:1 / Lc 19 (it is 9.3:1 / Lc 69 normally).
  - Should: the wrong-PIN note is `DIM` 24 px, Lc 58.
  - Should: the Kids end arrow, `DIM 0xB3` vs `WHITE`, is only 1.87:1.
  - Nice: the dimmed learn-button rows are Lc 52 on the focused row.
  - Fine: the toast at bottom centre over panels; "Loading reciters" in `SOFT` (Lc 78 over snow); a wrong PIN now has words as well as the red dots.

- Baseline audit at creation (not a PR review): kicker gold is APCA-weak (Lc 59 max); verse band relies on the shadow on bright clips; three golds across apps; some gradients undithered (corrected by PR #21: the overlay scrim and Kids `grad()` do dither); Block World accent equals the bonus green; lane 4's "warm off-white" note for `WHITE` is inaccurate (it is slightly cool; fixed in PR #21).
- 2026-11-05 PR #21 (docs only): corrected the dither inventory (home overlay scrim `:1878` and Kids `grad()` dither; `PanelBg`, `Glass`, home top/bottom scrims, Kids `panel()`/shades don't). Added the verse-band `lineKicker` (`DIM`, 20 px, `:1101`) as the weakest text on the home screen (1.64:1 / Lc 28 over sky, Lc 16 over snow). Fixed the kicker WCAG claim (2.9:1 on grey 107). Aligned the banding advice with lane 4. Added lane 11 to five rows of the coverage map.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05 whole-app audit (main `d772bcc`):
  - Must: Quran player text over Quran background clips sits on only 10% shade, so the reciter is Lc 20 to 32 over sky or snow (`MainActivity.java:505/519`).
  - Must: the verse band still has no local protection.
  - Should: drive status line in coloured 20 px text (Lc 18 to 37); Kids player pause glyph and title without shadows (Lc 13 over a bright frame); StarField near-black ramp undithered (15/7/4 code steps over 1,110 px); scrims, PanelBg and Kids panel still undithered; kicker gold Lc 59 max; `DIM` Lc 54 on black; event chip text without shadow; adhan gold `FFC24A` vs home `F1D58A`.
  - Fine: PIN dots, Kids allowed marks, Kids buttons, adhan settings, focus edge, flashing.


## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [wcag-143] https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html: 4.5/3:1, luminance formula, F83 (opened by lane 4; reused).
- [wcag-1411] https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html: 3:1 for UI states and focus indicators; test the least contrasting area of a gradient.
- [wcag-141] https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html: colour not the only cue; 3:1 lightness difference counts; red/green needs a non-colour cue.
- [wcag-231] https://www.w3.org/WAI/WCAG22/Understanding/three-flashes-or-below-threshold.html: no more than 3 flashes/s; red-flash definition R/(R+G+B) ≥ 0.8.
- [apca-nutshell] https://raw.githubusercontent.com/Myndex/SAPC-APCA/master/documentation/APCA_in_a_Nutshell.md: Lc levels by use and size, reference px = 1.278 arcmin, x-height adjustment.
- [apca-why] https://raw.githubusercontent.com/Myndex/SAPC-APCA/master/documentation/WhyAPCA.md: why WCAG 2 fails dark mode; keep large dark-mode text below Lc 90.
- [apca-w3-src] https://raw.githubusercontent.com/Myndex/apca-w3/master/src/apca-w3.js: exact APCA constants and polarity maths.
- [color-on-tv] https://developer.android.com/design/ui/tv/guides/foundations/color-on-tv: Standard mode, sRGB, picture modes, 562:1 example, banding tips, dark UI.
- [tv-color-system] https://developer.android.com/design/ui/tv/guides/styles/color-system: dark themes, content-based colour schemes for TV.
- [nngroup-text-over-images] https://www.nngroup.com/articles/text-over-images/: overlays, floor fade, 30%->50% box, shadow/outline, worst-case testing.
- [captionstyle] https://developer.android.com/reference/android/view/accessibility/CaptioningManager.CaptionStyle: system caption edge types (outline, drop shadow, raised, depressed).
- [hig-color] https://developer.apple.com/design/human-interface-guidelines/color (read via .../tutorials/data/design/human-interface-guidelines/color.json): consistent meaning, increased contrast, lighting conditions, tvOS limited palette, focus not by colour alone.
- [hig-dark-mode] https://developer.apple.com/design/human-interface-guidelines/dark-mode (read via the .json): base vs elevated backgrounds, test with Increase Contrast.
- [cambi] https://raw.githubusercontent.com/Netflix/vmaf/master/resource/doc/cambi.md: banding visibility threshold 0.019 x L, ~1 degree window, score 5 annoying.
- [lightillusion-bt1886] https://lightillusion.com/issues_bt1886.html: BT.1886 lifts near-black with display black level; crushed vs washed shadows.
- [aosp-mixed-sdr-hdr] https://source.android.com/docs/core/display/mixed-sdr-hdr: Android 13 SDR dimming under HDR, black crush, dithering, GPU fallback.
- [aosp-hwui-dither] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/libs/hwui/renderthread/CanvasContext.cpp and .../pipeline/skia/SkiaOpenGLPipeline.cpp: forced dither only in non-default colour modes.
- [aosp-gradientdrawable] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/graphics/java/android/graphics/drawable/GradientDrawable.java: `mDither = false` default, `setDither`.
- [aosp-paint] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/graphics/java/android/graphics/Paint.java: default flags, what dither does.
- [aosp-argbevaluator] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/animation/ArgbEvaluator.java: colour interpolation in linear light.
- [aosp-paint-dither-revert] https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/eb5a3553dcae07481b15b93dd7223e29611a36b4: Android S briefly made `DITHER_FLAG` a `Paint` default, then reverted it because Skia also dithered bitmaps and it was slow on some GPUs. Ask lane 3 before dithering large surfaces. Checked 2026-11-05.
- [aosp-dither-2009] https://android.googlesource.com/platform/frameworks/base/+/211db4a%5E%21/: since 2009, NinePatch and Bitmap drawables dither by default (`GradientDrawable` doesn't). Checked 2026-11-05.
- [kotlinlang-dither] https://slack-chats.kotlinlang.org/t/506110/u3v9rcy2d-what-do-you-need-dithering-should-be-automatically: Romain Guy (informal) on default gradient dithering and an API 28-era regression.
- [window-ref] https://developer.android.com/reference/android/view/Window: `setColorMode`, wide gamut / HDR modes.
- [mcu-readme] https://raw.githubusercontent.com/material-foundation/material-color-utilities/main/README.md: library map (HCT, blend, contrast, score, quantize).
- [mcu-colorspaces] https://raw.githubusercontent.com/material-foundation/material-color-utilities/main/concepts/color_spaces.md: why HCT, tone vs chroma vs hue.
- [mcu-blend] https://raw.githubusercontent.com/material-foundation/material-color-utilities/main/java/blend/Blend.java: harmonize = hue shift ≤15°, tone kept.
- [mcu-score] https://raw.githubusercontent.com/material-foundation/material-color-utilities/main/java/score/Score.java: image seed scoring (chroma cut-off 5, target 48).
- [cvd-review-2025] Almustanyir, "A Global Perspective of Color Vision Deficiency", Healthcare 2025, https://consensus.app/papers/details/6ae9f2d2e7bb562381451851260c5d58/: up to 8% of males, 0.5% of females (abstract).
- [cvd-riyadh-1996] Osuobeni, "Prevalence of congenital red-green color vision defects in Arab boys from Riyadh", Ophthalmic Epidemiology 1996, https://consensus.app/papers/details/857ce11d704f5ec5bc5efa2aaca8aab1/: 2.93% (abstract).
- [cvd-nablus-2021] Shehadeh et al., male teenagers in Nablus, 2021, https://consensus.app/papers/details/5044e737c6d8507c845890d26e15f335/: 8.0%, deutan 5.4% (abstract).
- To open next: Smashing "The Realities And Myths Of Contrast And Color" (A. Somers, 2022; fetch budget ran out), BBC subtitle guidelines (blocked), Material 3 colour-contrast page, ITU-R BT.709/BT.1886, rtings picture-mode and gradient tests (blocked by proxy).
- [bbc-w3c-captions-2014] https://lists.w3.org/Archives/Public/public-pfwg-comments/2014OctDec/0003.html: BBC (Nigel Megitt) comments on W3C media accessibility. Outlines and drop shadows "should not be presented as a suitable general alternative to displaying text on a non-transparent background". White text with drop shadows over bright video such as snow can become unreadable. A background box should extend past the text edges.
- [flutter-dither-13868] https://github.com/flutter/engine/pull/13868: Flutter added an opt-in gradient dither flag. Banding was obvious without it and essentially gone with it. Expected cost: "shouldn't have noticeable performance impacts", with a possible slight cost on weak devices, and dithering is a hint the renderer may ignore.
- [blitzcut-captions] https://blitzcutai.com/blog/caption-background-vs-outline-vs-shadow: secondary source from a caption-tool vendor. Ranks box over outline over shadow; a 70 to 80% box keeps most of the contrast; shadow alone is unreliable on light backgrounds. Its numbers aren't sourced, so use it for the ranking only.
- Tried, no colour content: Netflix Timed Text Style Guide, General Requirements (https://partnerhelp.netflixstudios.com/hc/en-us/articles/215758617), covers positioning only.

- [wcag-143-inactive] https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html: inactive components ("visible but not currently operable") have no contrast requirement. Keys that still take focus and answer a press are not inactive.
- [tv-focus-system] https://developer.android.com/design/ui/tv/guides/styles/focus-system: default focus scales 1.025, 1.05 and 1.1x; mix scale and colour cues; disabled shows "lower prominence". It gives no focus-contrast ratio.
- Couldn't read: m3.material.io and m2.material.io need JavaScript. Next time, get Material's disabled opacity (38%?) from the material-components-android source.

- [wcag-2413] https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance: AAA. Focus area at least a 2 CSS px perimeter, and 3:1 between the same pixels focused and unfocused; a background change from black to dark grey fails. Checked 2026-11-06.
- [gd-setcolors] https://learn.microsoft.com/pl-pl/dotnet/api/android.graphics.drawables.gradientdrawable.setcolors?view=net-android-35.0: `setColors(int[], float[])` with offsets is API 29; null offsets space the colours evenly. Checked 2026-11-06.
- Material disabled alpha 0.38: the androidx ContentAlpha page did not show the values when fetched. Still **(unverified)**.

## Open questions to research next
- Measure real contrast behind the verse band and strip on the brightest clip in `Movies/TVWallpapers` (shared with lane 4). Does `screencap` include the SurfaceView video on this box?
- Does the Xiaomi box output full or limited RGB range, and does the TV match? (Settings > Display; check for grey blacks.)
- Is HDR ever on while our UI shows (HDR wallpaper clip, or "match content dynamic range")? If so, does SDR dimming crush `PanelBg`?
- Should the three golds converge (home `F1D58A` vs adhan `FFC24A` vs Kids `FFD45A`)? Mock side by side on the TV; decide which differences are deliberate (Kids playful).
- Is `setDither(true)` visibly better on `PanelBg`/scrims on this Mali GPU, and does it cost frame time on `PanelBg`'s `Paint` (ask lane 3)?
- Should `lineKicker` use `SOFT` or the kicker gold, and should the verse band get a local fade?
- Would HCT `harmonize()` (≤15° hue shift, tone kept) look better than RGB `Accent.mix()` for glow colours?
- Find a primary source on TV limited/full range (ITU-R BT.709 / CTA-861) and on picture-mode colour temperature defaults.
- Open BBC or Netflix subtitle style guides for text-over-video edge/shadow norms. The BBC colour guidance is still unread (the clevercast summary has none).
- Does a 40% local fade behind the verse band look like a box on the TV? Mock it with lane 4 at 0x59, 0x66 and 0x80 on the brightest wallpaper.
- What does `setDither(true)` on `StarField`, `PanelBg` and the scrims cost in frame time on this Mali GPU (lane 3)?
- How bright are the user's actual Quran background clips? Sample frames from `C:\TVWallpapers` sources, or `Movies/QuranBackgrounds` on the box.
- Should Kids `PlayerActivity` reuse `ink()` (move it to a shared helper) so every Kids text gets the same shadow rule?

- Which single alpha should every "disabled" state use across the three apps (home keys 0.4, passed prayers 0.55, Kids `DIM` arrows)? Pick one documented value and find Material's figure in the MDC-Android source.

- Does the extended top scrim (480 px, `0x4D` middle) read as a grey band on dark clips? Mock it with lane 4.
- Is `NIGHT_TEXT 0.7` calm enough in a dark room? Check on the TV at night, at 3 m.
- Does a 3 px gold underline under the next prayer time fit lane 4's boxless strip?
