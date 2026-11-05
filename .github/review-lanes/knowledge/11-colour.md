# Lane 11 knowledge: Colour and contrast (palette, text over video, gold, TV panels)

## How to use this file
- Read this whole file before every review, then read the brief `../11-colour.md`.
- Research online whatever the PR touches that this file does not cover. Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for: **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. If the file grows past about 400 lines, condense the oldest Past findings and Lessons into one-line summaries. Never delete a source that is still cited.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code, computed it, or seen it on the TV or a screenshot.
- Lane 4 (look and feel) keeps the type scale, layout and glass-look notes; do not duplicate them here. Its WCAG 1.4.3 numbers are reused below.

## Core expertise

### 1. WCAG 2 contrast: the maths and what it means for white over video
- WCAG 1.4.3: 4.5:1 normal text, 3:1 large (24 px regular or ~18.5 px bold, CSS px). Ratio = (L1+0.05)/(L2+0.05) with sRGB relative luminance, piecewise linearisation. Background images that don't give enough contrast fail (F83). [wcag-143, from lane 4's library]
- WCAG 1.4.11 non-text contrast: **3:1** against adjacent colours for the visual information that identifies UI components and their **states, including focus**, and for parts of graphics needed to understand content. Test "the least contrasting area" on a gradient. Decorative graphics, logos and photos are exempt. [wcag-1411]
- WCAG 1.4.1 use of colour: colour must not be the only visual means of conveying information. Hue plus a lightness difference counts as a second cue only at **3:1 or more**; if users must know *which* colour (green valid, red invalid) an extra non-colour cue is needed regardless. [wcag-141]
- Worked numbers for our `WHITE 0xF5F5F7` (L 0.914), computed with the formula (recipe in section 8):
  | Backdrop after dimming (grey) | WCAG | APCA Lc |
  |---|---|---|
  | black | 19.3 | 101 |
  | grey 68 | 8.95 | 90 |
  | grey 94 (a grey-40% clip under the 8% dim) | 5.96 | 81 |
  | grey 107 | 4.89 | 75 |
  | grey 112 (WCAG 4.5:1 limit, L 0.162) | 4.5 | ~73 |
  | grey 140 (APCA Lc 60 limit, L 0.262) | 3.09 | 60 |
  | light sky `#9EC9F0` under 8% dim | 1.89 | 38 |
  | snow `#E0E6EE` under 8% dim | 1.37 | 20 |
  So **with only the 8% dim the clip itself must be darker than about sRGB grey 120 (#787878, it becomes 112 after the dim) for small text**; anything brighter is carried by the shadow or a scrim.
- How much darkening a snowy backdrop needs (computed): black at 35% over snow gives 2.7:1 / Lc 55; at 50% gives 4.4:1 / Lc 72; at 70% (our bottom scrim's base) gives 8.8:1 / Lc 90.

### 2. APCA (the candidate for WCAG 3): better for dark UI and light-on-dark
- APCA reports **lightness contrast Lc** from 0 to about 105 and is perceptually uniform: Lc 60 means the same perceived contrast whether the colours are light or dark. WCAG 2 "overstates contrast for dark colors to the point that 4.5:1 can be functionally unreadable when one of the colors in a pair is near black" and "cannot be used for guidance designing dark mode". [apca-nutshell] [apca-why]
- Bronze levels (CSS reference px, Helvetica-like font, x-height 0.52) [apca-nutshell]:
  - **Lc 90** preferred for body text (18 px/300 or 14 px/400 and up); suggested *maximum* for very large bold text (>36 px bold) in dark mode.
  - **Lc 75** minimum for body text at 24 px/300, 18 px/400, 16 px/500, 14 px/700.
  - **Lc 60** minimum for "content text that is not body": 48 px/200, 36 px/300, 24 px/400, 21 px/500, 18 px/600, 16 px/700.
  - **Lc 45** large heavier text (36 px/400 or 24 px bold), fine-detail icons. **Lc 30** absolute minimum for spot-readable text and solid icons. **Lc 15** invisible for many users.
  - For an AAA-like level add Lc 15. Adjust size by x-height (a 0.45 x-height font needs about 16% more size).
- A reference px is **1.278 arcmin** of visual angle. On our stage (computed): 55-inch TV at 3 m, 1 stage px = 0.73 arcmin = **0.57 CSS px**; 65-inch at 3 m 0.67; 43-inch at 3 m 0.44; 75-inch at 2.5 m 0.93. So a 20 px kicker reads like ~11 px, 24 px like ~14 px, a 32 px verse like ~18 px, the 340 px clock like ~190 px. [apca-nutshell] (arithmetic)
- APCA constants (apca-w3 0.1.9 source): simple 2.4 exponent; coefficients 0.2126729/0.7151522/0.0721750; normal polarity exponents 0.56 (bg) and 0.57 (text); reverse (light text on dark) **0.65 (bg) and 0.62 (text)**; soft black clamp at Y 0.022 with exponent 1.414; scale 1.14; offset 0.027; output clipped below 0.1. [apca-w3-src]
- Max backdrop grey for `WHITE` text: **Lc 90: 68; Lc 75: 107; Lc 60: 140; Lc 45: 168** (computed).
- APCA is still a candidate, not law. Report both numbers; treat WCAG 2 as the floor and APCA as the better predictor for our light-on-dark UI.

### 3. Text over moving video
- NN/g: test the **worst-case** background and placement; techniques are an overlay/gradient ("floor fade"), a semi-opaque box (REI moved from 30% to 50% opacity), blur, a text shadow or outline, placing text where the image is calm, or changing text colour; "Light-colored or faded backgrounds are not well-suited to white text". [nngroup-text-over-images]
- Android's own caption styles offer edge types none, outline, drop shadow, raised, depressed: the platform's accepted ways to keep white text readable over arbitrary video. [captionstyle]
- Our stack, top to bottom at rest: text shadow, then (only at the top and bottom) a scrim, then the 8% dim, then the clip. The verse band (stage y 494 to ~600) is in neither scrim. A drop shadow darkens a few px around each stroke: it rescues edges but cannot fix a large bright area behind a light weight. (reasoning)
- Apple: in bright surroundings colours look darker and muted, in dark rooms brighter and more saturated; test tvOS on several HD and 4K TV brands and display settings. [hig-color]

### 4. Colour on TV panels
- Google TV: **Standard** picture mode is the most common; most TVs support sRGB; design basic UI in sRGB and test in Standard mode. Vivid raises saturation, Dynamic raises contrast, Movie, Sports, Game, Custom also exist. A low-end LCD can be **562:1** contrast; the same colour looks washed out on low-contrast panels. Avoid relying on colour alone; optimise for SDR and HDR; darker colours save power; avoid white backgrounds. [color-on-tv]
- Build TV UI from **dark themes**; use content-based colour (seed from hero image) since TV has no wallpaper Material You. [tv-color-system]
- **Banding** causes: low colour depth, compression, panel or GPU. Advice: high-bit-depth gradients, avoid extreme transitions, test on many devices, **dither**, or use solid colours/subtle patterns. [color-on-tv]
- Banding visibility (Netflix CAMBI): a luminance step is visible when **ΔL > 0.019 x L_mean** (BT.1886 display model); CAMBI's window is ~1 degree; banding gets more visible on brighter displays in dimmer rooms; a CAMBI score around 5 starts to annoy, 24 is unwatchable. [cambi]
  Applied to 8-bit sRGB (computed): one code step is a 5 to 6% luminance step near code 20 and still about 2% near code 100, so **every 1-code step below about code 100 can show**. Long ramps between dark navies and blacks are where banding lives.
- BT.1886 lifts near-black shadow brightness based on the display's real black level; footage graded on one display can look **crushed** on another. Near-black detail is not reliable across TVs. [lightillusion-bt1886]
- **Mixed SDR/HDR (Android 13+):** while HDR content is on screen the device can dim SDR layers so their perceived brightness stays the same; dimming can cause **black crush** and banding, which dithering reduces; dimming may fall back to the GPU. [aosp-mixed-sdr-hdr] Our UI is SDR; it only meets this if HDR video plays under it (wallpapers are SDR clips: unverified for every file).
- Limited (16 to 235) vs full (0 to 255) range mismatches between box and TV make blacks grey or crush shadows. **(unverified here: no source opened this session; check the box's HDMI colour-range setting before blaming the code.)**

### 5. Dithering and blending on Android (verified in AOSP android14-release source)
- `GradientDrawable` keeps `mDither = false` by default and only applies it if `setDither(true)` (or `android:dither`). [aosp-gradientdrawable]
- `Paint` has no dither in its default flags (`HIDDEN_DEFAULT_PAINT_FLAGS` = kerning, embedded bitmap text, filter bitmap). [aosp-paint]
- HWUI forces dithering (`SkSurfaceProps::kAlwaysDither_Flag`) only when the window colour mode is **not** `Default` (wide gamut/HDR); `CanvasContext::shouldDither()` returns `mColorMode != Default`. Our windows use Default, so **our gradients are not auto-dithered on Android 14**. [aosp-hwui-dither] Romain Guy has said informally that gradients "should be dithered by default" and that an older version regressed (Slack, not docs). [kotlinlang-dither]
- `Window.setColorMode(COLOR_MODE_WIDE_COLOR_GAMUT | COLOR_MODE_HDR)` exists; wide gamut may be refused by the device. We never call it. [window-ref]
- `ArgbEvaluator` interpolates in **linear light** (pow 2.2, lerp, pow 1/2.2), so a colour glide passes through brighter midpoints than a gamma-space lerp. `Accent.mix()` lerps in gamma space. [aosp-argbevaluator]

### 6. Palette systems: what "consistent" means
- Material colour utilities work in **HCT** (hue, chroma, tone = L*): tone drives contrast, chroma drives colourfulness, hue is independent. [mcu-readme] [mcu-colorspaces]
- `Blend.harmonize(design, source)` rotates the design colour's hue toward the source by **half the difference, at most 15 degrees**, keeping chroma and tone. That is the principled way to let a brand/gold colour lean toward the wallpaper without changing its lightness (our `Accent.mix()` lerps RGB, which also shifts tone). [mcu-blend]
- Material's image scorer ignores colours below chroma 5 or under 1% of the image and targets chroma 48. Our `Accent.pick()` (24 hue bins, ignores V or S under 0.12, clamps S 0.35 to 0.7, V 0.95, strength by colourful share / 0.15) is a simpler cousin. [mcu-score]
- Apple: don't use the same colour to mean different things; supply increased-contrast variants; colour on glass sparingly; with colourful backgrounds prefer monochrome controls. tvOS: a **limited palette** that coordinates with the logo, and "avoid using only color to indicate focus" (scale and motion are primary). [hig-color]
- Apple dark mode: dimmer "base" backgrounds recede, brighter "elevated" ones come forward. That maps onto our panel (`PanelBg` navy) over dimmed video. [hig-dark-mode]

### 7. Colour vision deficiency (CVD)
- Red-green CVD affects up to **8% of males and 0.5% of females** of Northern European descent, less in some other populations. [cvd-review-2025]
- Arab boys in Riyadh: **2.93%** red-green CVD (deutan 1.95%, protan 0.49%). [cvd-riyadh-1996] Palestinian male teenagers: **8.0%** (deutan 5.4%, protan 2.3%). [cvd-nablus-2021] For a family, assume one male viewer may be deutan.
- Practical rule: any pair that must be told apart differs in **lightness** (3:1, WCAG 1.4.1) and in words/shape. Deutans lose red vs green, not light vs dark.
- Computed luminances: Kids ring red `FF6B5B` L 0.33, bonus green `7CFC6A` L 0.75, gold `FFC233` L 0.60. Red vs the yellow accents is about 1.9:1 (below 3:1), but the ring's length and the "left/bonus" words also carry the state.
- Home `GOLD` vs `WHITE` is only **1.32:1**: gold-vs-white text is a hue cue, so it must never be the only signal (it isn't for the next prayer: it also gets a pill).

### 8. Review recipes
WCAG and APCA for a text colour over a sampled backdrop (blend alpha text first) [wcag-143] [apca-w3-src]:
```python
def lin(c): c/=255; return c/12.92 if c<=0.04045 else ((c+0.055)/1.055)**2.4
def L(p): r,g,b=map(lin,p); return .2126*r+.7152*g+.0722*b
def wcag(a,b): h,l=max(L(a),L(b)),min(L(a),L(b)); return (h+.05)/(l+.05)
def Y(p): r,g,b=[(c/255)**2.4 for c in p]; return .2126729*r+.7151522*g+.0721750*b
def clamp(y): return y if y>.022 else y+(.022-y)**1.414
def apca(txt,bg):
    t,b=clamp(Y(txt)),clamp(Y(bg))
    if abs(b-t)<.0005: return 0
    if b>t: s=(b**.56-t**.57)*1.14; return 0 if s<.1 else (s-.027)*100
    s=(b**.65-t**.62)*1.14; return 0 if s>-.1 else (s+.027)*100   # negative = light on dark
def over(fg,a,bg): return tuple(round(f*a+x*(1-a)) for f,x in zip(fg,bg))
bg = over((0,0,0), 0.08, sampled_rgb)          # the 8% wall dim
txt = over((255,255,255), 0xD9/255, bg)         # e.g. SOFT
print(wcag(txt,bg), apca(txt,bg))
```
Screenshot workflow: `adb exec-out screencap -p > home.png`; sample the brightest pixels within ~10 px behind each text run (the screenshot already includes dim, scrim and shadow); test the brightest and darkest clip in `Movies/TVWallpapers`; record clip name and numbers. Whether `screencap` captures the SurfaceView video on this box is **unverified** (lane 4 open question).

Banding check: show the panel or scrim over a dark, flat clip in a dark room, Standard mode, at 3 m; then at 1 m. Bands visible at 3 m are a Should fix. Fix order: shorten the ramp or raise its darkest stop; `setDither(true)` on the `GradientDrawable`/`Paint`; let the video show through (it dithers naturally); a tiny noise texture (lane 3 must agree on cost).

Colour-blind check: run a screenshot through a Brettel-model simulator (Myndex links one at myndex.com/CVD, not opened) or grey it out (`ImageMagick -colorspace Gray`): every state must still be distinguishable.

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep).
- `MainActivity.java:95-96`: `WHITE 0xFFF5F5F7` (R=G=245, B=247: a very slightly cool neutral; L 0.914), `SOFT 0xD9FFFFFF`, `DIM 0xA6FFFFFF`, `GLASS 0x29FFFFFF`, `GLASS_EDGE 0x47FFFFFF`, `GLASS_HI 0x4DFFFFFF`, `SHADE 0x8C000000`. `CARD_TINT 0x3A000000` (`:860`). `GOLD 0xFFF1D58A` (`:1936`, hue 43.7, HSV sat 43%, L 0.68).
- `kicker()` (`:863`) sets `0xD9E8CB86` (hue 42, sat 42%) at 85% alpha; panel kickers pass `0xCCF1D58A` but are overridden. Computed: Lc 59 / 9.6:1 on black, Lc 54 / 5.0:1 on grey 68, Lc 42 / 2.9:1 on grey 107.
- `DIM` on black: Lc 54, 8.6:1. `SOFT` on black: Lc 84, 14.9:1. `GOLD` on black: Lc 83, 14.6:1.
- Darkening: `videoCover` TL-BR `0xFF1B2A4A/0xFF0E1426/0xFF241A3A` until the first frame (`:209`); `skyTint` alpha 0 to 0.11 cool `SKY_COOL 0xFF5B86E5` / to 0.13 warm `SKY_WARM 0xFFFF8C42` (`:3430-3449`, 2 s fades); `dimView` `baseDim()` = 1 - 0.92 x brightness/100 (8%, 22%, 36% for 100/85/70) (`:3076-3077`); `topScrim` 380 px `0x80000000`->0, `bottomScrim` 480 px `0xB3000000`/`0x59000000`/0 (`:215-227`). Night dim 0.6 over 4 s (`:3483`).
- Text shadows: `TEXT_SHADOW 4f`, `0x99000000`, dy 2 (`:1029`, `:1061-1167`); strip `0xB3000000` (`:2493`); clock (6, 0, 3, `0x73000000`); `text()` adds (5, 0, 2, `0x66000000`) at 100 px+.
- Colour-only cues: `adhanCountdown` gold when 15 min or less (`:1572`); calendar "Today/Tomorrow/in N days" gold within 7 days (`:3298`); player Shuffle/Repeat/Sleep gold when on (`:690-708`, text also changes). Next prayer cell gets `glass(14, GLASS_HI, 0)` and passed ones alpha 0.55 (`:1597-1601`).
- `focusGlass()` (`:1939`): `0x47FFFFFF`->`0x1FFFFFFF` with a 3 px `0xE6F1D58A` stroke. `goldLift()` sets ambient and spot shadow colours to `GOLD`. `panelHeader()` gold bar `0xFFF1D58A`->0 (`:1926`). `SURAH_FILL 0xB30D1428`, `SURAH_EDGE 0x33F1D58A` (`:2509`).
- `Glass.java`: body gradient = tint, then accent wash `(0x24 x strength)` alpha of `Accent.color`, then frost `0x29`->`0x0D` white, then sheen `0x1C`->0 over the top `min(h/2, 140)` px; edge stroke 2 px `0xB3/0x26/0x40` at 0/.55/1. No dither.
- `PanelBg.java`: fill `0xE61B2140/0xE0121731/0xEB0A0D1C` (blue channel 64->49->28: ~36 steps), warm radial `0x33F1D58A`, sheen `0x1FFFFFFF`, edge `0x8C/0x1F/0x33` white, hairline gold up to `0xE6`. No dither.
- `GlassFrame.Glow`: 7 rings, colour leans 70% toward `Accent.color` and alpha up to +35% with strength. `BrandGlow`: brand colour mixed 30% x strength toward accent; Quran tile `0xFF2FA58C`, Apps `0xFFDDE6FF` (`MainActivity.java` ~`:1379`).
- `Accent.java`: default colour `0xFFF1D58A`, `pick()` on a 64x36 frame; `glideTo()` 1,600 ms, `DecelerateInterpolator(1.5f)`, `ArgbEvaluator` (linear light). Parent setting `accent` can switch it off.
- No `setColorMode` anywhere in `app`, `kids`, `adhan` (grep), so no HWUI auto-dither.
- **Kids** (`KidsActivity.java`): `WHITE 0xFFFFFFFF, SOFT 0xE6FFFFFF, DIM 0xB3FFFFFF` (`:64`) differ from home. Shades `withDim(0x73)` top, `withDim(0xA6)` bottom, dim default 8 (`:915-923`). Panels `0xF20A0718`->`0xFC070512` (`:1863`, comment "full screen: no dithering": the R/G/B ramps are only 3/2/6 steps). Ring: bonus `0xFF7CFC6A`, normal world accent, `0xFFFF6B5B` at 25% or less (`:1068-1084`). Focused buttons go solid white with dark ink `0xFF161028`; gold buttons `0x2EFFC233`->`0xFFFFD45A` (`:1979-1986`). `ink()` (`:1920`) shadows text whose luma > 0.45.
- `Worlds.java` accents: Power `FFC233`, Mushroom `FFD84A`, Speed `FFD21F`, Dream `FFE066`, Block `7CFC6A` (same as bonus green). `sky` arrays are defined but not read by `KidsActivity` (grep `.sky`: none).
- **Adhan** (`AdhanService.java:59`): `GOLD 0xFFFFC24A` (hue 40, sat 71%), `TEXT 0xFFF6F3FF`, `MUTED 0xB8F6F3FF`, `INK 0xFF0B0D14`. Alert band `0x00080B14`->`0xE6080B14`->`0xF5080B14` with `setDither(true)` (`:294-302`); gold line `F1D58A` (`:310`); title `0xF2FFFFFF`, Arabic `0xFFF1D58A`, shadows 10 px `0x80000000`. Cards `0xE00A0C16`/`0xD90A0C16`. Computed: `MUTED` on the card is about Lc 61, 9.4:1.

## Past findings
None yet; lane created 2026-11-05 (split from lane 4).
- Baseline audit at creation (not a PR review): kicker gold is APCA-weak (Lc 59 max); verse band relies on the shadow on bright clips; three golds across apps; no gradient dithering in `app`/`kids`; Block World accent equals the bonus green; lane 4's "warm off-white" note for `WHITE` is inaccurate (it is slightly cool).

## Lessons learned
- 2026-11-05: Blend alpha colours before measuring: `SOFT`, `DIM` and the 85% kicker gold are much weaker than their hex suggests on a light backdrop.
- 2026-11-05: WCAG 2 passes light-on-dark text that APCA flags (kicker gold 9.6:1 but Lc 59). For this dark UI, report both.
- 2026-11-05: Convert stage px to visual angle before using any px-based rule (APCA, WCAG large-text): 1 stage px is roughly half a CSS px from the sofa.
- 2026-11-05: Don't assume Android dithers gradients: on Android 14 in Default colour mode it doesn't unless you ask.

## Open questions to research next
- Measure real contrast behind the verse band and strip on the brightest clip in `Movies/TVWallpapers` (shared with lane 4). Does `screencap` include the SurfaceView video on this box?
- Does the Xiaomi box output full or limited RGB range, and does the TV match? (Settings > Display; check for grey blacks.)
- Is HDR ever on while our UI shows (HDR wallpaper clip, or "match content dynamic range")? If so, does SDR dimming crush `PanelBg`?
- Should the three golds converge (home `F1D58A` vs adhan `FFC24A` vs Kids `FFD45A`)? Mock side by side on the TV; decide which differences are deliberate (Kids playful).
- Is `GradientDrawable.setDither(true)` visibly better on `PanelBg`/scrims on this Mali GPU, and does it cost anything (ask lane 3)?
- Would HCT `harmonize()` (≤15° hue shift, tone kept) look better than RGB `Accent.mix()` for glow colours?
- Find a primary source on TV limited/full range (ITU-R BT.709 / CTA-861) and on picture-mode colour temperature defaults.
- Open BBC or Netflix subtitle style guides (blocked this session) for text-over-video edge/shadow norms.

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
