# Lane 11 knowledge: Colour and contrast (palette, text over video, gold, TV panels)

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../11-colour.md`.
- History lives in [archive/11-colour.md](archive/11-colour.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
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
  - **Lc 90** preferred for body text (18 px/300 or 14 px/400 and up); suggested *maximum* for "very large and bold fonts (>36px bold) and large areas of color" (APCA in a Nutshell doesn't say "dark mode" there).
  - **Lc 75** minimum for body text at 24 px/300, 18 px/400, 16 px/500, 14 px/700.
  - **Lc 60** minimum for "content text that is not body": 48 px/200, 36 px/300, 24 px/400, 21 px/500, 18 px/600, 16 px/700.
  - **Lc 45** large heavier text (36 px/400 or 24 px bold), fine-detail icons. **Lc 30** absolute minimum for spot-readable text and solid icons. **Lc 15** minimum for non-text that must be seen (5 px solid or more); below Lc 15 counts as invisible.
  - For an AAA-like level add Lc 15. Adjust size by x-height (a 0.45 x-height font needs about 16% more size).
- A reference px is **1.278 arcmin** of visual angle. On our stage (computed): 55-inch TV at 3 m, 1 stage px = 0.73 arcmin = **0.57 CSS px**; 65-inch at 3 m 0.67; 43-inch at 3 m 0.44; 75-inch at 2.5 m 0.93. So a 20 px kicker reads like ~11 px, 24 px like ~14 px, the 32 px English verse like ~18 px (38 px Arabic ~22 px; these are the shown sizes, the build-time 30/36 are always replaced), the 340 px clock like ~190 px. [apca-nutshell] (arithmetic)
- APCA constants (apca-w3 0.1.9 source): simple 2.4 exponent; coefficients 0.2126729/0.7151522/0.0721750; normal polarity exponents 0.56 (bg) and 0.57 (text); reverse (light text on dark) **0.65 (bg) and 0.62 (text)**; soft black clamp at Y 0.022 with exponent 1.414; scale 1.14; offset 0.027; output clipped below 0.1. [apca-w3-src]
- Max backdrop grey for `WHITE` text: **Lc 90: 68; Lc 75: 107; Lc 60: 140; Lc 45: 168** (computed).
- APCA is still a candidate, not law. Report both numbers; treat WCAG 2 as the floor and APCA as the better predictor for our light-on-dark UI.

### 3. Text over moving video
- NN/g: test the **worst-case** background and placement; techniques are an overlay/gradient ("floor fade"), a semi-opaque box (REI moved from 30% to 50% opacity), blur, a text shadow or outline, placing text where the image is calm, or changing text colour; "Light-colored or faded backgrounds are not well-suited to white text". [nngroup-text-over-images]
- Android's own caption styles offer edge types none, outline, drop shadow, raised, depressed: the platform's accepted ways to keep white text readable over arbitrary video. [captionstyle]
- Our stack, top to bottom at rest: text shadow, then (only at the top and bottom) a scrim, then the 8% dim, then the clip. The verse band (stage y 494 to ~600) is in neither scrim. A drop shadow darkens a few px around each stroke: it rescues edges but cannot fix a large bright area behind a light weight. (reasoning)
- Apple: in bright surroundings colours look darker and muted, in dark rooms brighter and more saturated; test tvOS on several HD and 4K TV brands and display settings. [hig-color]

### 4. Colour on TV panels
- Google TV: **Standard** picture mode is the most common; most TVs support sRGB; design basic UI in sRGB and test in Standard mode. Vivid raises saturation, Dynamic raises contrast, Movie, Sports, Game, Custom also exist. A panel the page labels only "Low" contrast is **562:1** (it doesn't say "low-end LCD"); the same colour looks washed out on low-contrast panels. Avoid relying on colour alone; optimise for SDR and HDR; darker colours save power; avoid white backgrounds. [color-on-tv]
- Build TV UI from **dark themes**; use content-based colour (seed from hero image) since TV has no wallpaper Material You. [tv-color-system]
- **Banding** causes: low colour depth, compression, panel or GPU. Advice: high-bit-depth gradients, avoid extreme transitions, test on many devices, **dither**, or use solid colours/subtle patterns. [color-on-tv]
- Banding visibility (Netflix CAMBI): a luminance step is visible when **ΔL > 0.019 x L_mean** (BT.1886 display model); CAMBI's window is ~1 degree; banding gets more visible on brighter displays in dimmer rooms; a CAMBI score around 5 starts to annoy, 24 is unwatchable. [cambi]
  Applied to 8-bit sRGB (computed): one code step is about a 7% luminance step near code 20 (6.9%) and still about 2% near code 100 (2.1%), so **every 1-code step below about code 100 can show**. Banding comes from few 8-bit steps spread wide. Keep dark ramps short or give them more steps (raise the darkest stop), dither, or let the video show through. (Same line in lane 4.)
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
- Computed luminances: Kids ring red `FF6B5B` L 0.33, bonus green `7CFC6A` L 0.75, gold `FFC233` L 0.60. Red vs the Kids yellows is 1.7 to 2.1:1 (1.73 vs `FFC233`; below 3:1), but the ring's length and the "left/bonus" words also carry the state.
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
- Dithering inventory (grep `setDither`): dithered: home overlay scrim (`:1878`), Kids `grad()` (`KidsActivity.java:1947-1950`, used at `:932, :1375, :1955, :1963`), adhan band (`AdhanService.java:301`). Not dithered: `PanelBg`, `Glass`, home `topScrim`/`bottomScrim`, Kids `panel()` and shades (`:920-922`, `:1863`).
- Verse band text: `lineKicker` `DIM` 20 px (`:1101`, not through `kicker()`): 1.64:1 / Lc 28 over light sky `#9EC9F0`, 1.31:1 / Lc 16 over snow, after the 8% dim. `lineEn` is `SOFT` at 32 px shown (built at 30): 1.88:1 / Lc 38 over sky.
- `Glass.java`: body gradient = tint, then accent wash `(0x24 x strength)` alpha of `Accent.color`, then frost `0x29`->`0x0D` white, then sheen `0x1C`->0 over the top `min(h/2, 140)` px; edge stroke 2 px `0xB3/0x26/0x40` at 0/.55/1. No dither.
- `PanelBg.java`: fill `0xE61B2140/0xE0121731/0xEB0A0D1C` (blue channel 64->49->28: ~36 steps), warm radial `0x33F1D58A`, sheen `0x1FFFFFFF`, edge `0x8C/0x1F/0x33` white, hairline gold up to `0xE6`. No dither.
- `GlassFrame.Glow`: 7 rings, colour leans 70% toward `Accent.color` and alpha up to +35% with strength. `BrandGlow`: brand colour mixed 30% x strength toward accent; Quran tile `0xFF2FA58C`, Apps `0xFFDDE6FF` (`MainActivity.java` ~`:1379`).
- `Accent.java`: default colour `0xFFF1D58A`, `pick()` on a 64x36 frame; `glideTo()` 1,600 ms, `DecelerateInterpolator(1.5f)`, `ArgbEvaluator` (linear light). Parent setting `accent` can switch it off.
- No `setColorMode` anywhere in `app`, `kids`, `adhan` (grep), so no HWUI auto-dither.
- **Kids** (`KidsActivity.java`): `WHITE 0xFFFFFFFF, SOFT 0xE6FFFFFF, DIM 0xB3FFFFFF` (`:64`) differ from home. Shades `withDim(0x73)` top, `withDim(0xA6)` bottom, dim default 8 (`:915-923`). Panels `0xF20A0718`->`0xFC070512` (`:1863`, comment "full screen: no dithering": the R/G/B ramps are only 3/2/6 steps). Ring: bonus `0xFF7CFC6A`, normal world accent, `0xFFFF6B5B` at 25% or less (`:1068-1084`). Focused buttons go solid white with dark ink `0xFF161028`; gold buttons `0x2EFFC233`->`0xFFFFD45A` (`:1979-1986`). `ink()` (`:1920`) shadows text whose luma > 0.45.
- `Worlds.java` accents: Power `FFC233`, Mushroom `FFD84A`, Speed `FFD21F`, Dream `FFE066`, Block `7CFC6A` (same as bonus green). `sky` arrays are defined but not read by `KidsActivity` (grep `.sky`: none).
- **Adhan** (`AdhanService.java:59`): `GOLD 0xFFFFC24A` (hue 40, sat 71%), `TEXT 0xFFF6F3FF`, `MUTED 0xB8F6F3FF`, `INK 0xFF0B0D14`. Alert band `0x00080B14`->`0xE6080B14`->`0xF5080B14` with `setDither(true)` (`:294-302`); gold line `F1D58A` (`:310`); title `0xF2FFFFFF`, Arabic sub `0xFFF1D58A` (`:322`), dot `0xCCF1D58A` (`:326`); `GOLD FFC24A` on the Hijri, countdown, reminder title, next-prayer row and card kicker (`:203/209/386/496/706`); shadows 10 px `0x80000000`. Cards `0xE00A0C16`/`0xD90A0C16`. Computed: `MUTED` on the card is about Lc 61, 9.4:1.

Added by the whole-app audit (2026-11-05):
- Quran player: `playerShade` `{0x66,0x1A,0x1A,0x99}` black (`:505`), shown only with a Quran video. `playerSurah` `D9` white 168 px with shadow (8, 0, 4, `0x80000000`); `playerReciter` `B3` white 44 px with shadow (5, 0, 2, `0x80000000`). `StarField` is the default background: bg `0B1124/0A0F1F/070A15`, glow `0x30F1D58A` r 900, fade `0xCC/0/0/0xB3` `070A15`. No dither.
- `Quran Backgrounds.ps1` does no brightness or contrast change: scale, crop and crossfade only.
- `driveText` is 20 px, moved into the day row (`:1069`), green `0xCC9BE7B4`, red `0xFFFF9A8F`.
- `eventText` is `GOLD` 22 px with no shadow (`:901`).
- Kids `PlayerActivity`: pause "❚❚" `E6` white 120 px with no shadow; bottom bar `{0xCC000000, 0}`; title white 48 px with no shadow; progress `FFC233` on `0x40FFFFFF`; time `B3` white 30 px.
- Kids `Guard` warm tint `FF8A2A` up to alpha `0x3A`. Kids rules marks: `8CFF7A` on `0x2E7CFC6A`, or `DIM`, with words.
- Adhan `SettingsActivity`: bg `0B0D14`, label `F6F3FF`, value `FFC24A`, muted `B8F6F3FF`, warning `FF8F8F`, focus `0x26` white + 3 px `FFC24A` stroke.
- `IntroView`: own `GOLD`/`GOLD_DEEP B8893A`, title gold gradient with a 36 px glow, kicker `B3` white 22 px (Lc 64 on `101A36`). Backdrops are radial gradients on an undithered Paint.

Added by the PR #22 review (2026-11-05):
- Over snow, the panel background blends to about `#1B2038` (`PanelBg` mid `0xE0121731` over the overlay scrim `0x8C050810` over the dimmed clip).
- Home PIN keys: off `0x1FFFFFFF` with a `0x2EFFFFFF` ring (1.78:1, so focus is the gold ring plus the 1.12 scale). On: `0x59`->`0x26` white with a 3 px `0xE6F1D58A` ring (9.3:1).
- `WHITE` on the top of `focusGlass` is 5.9:1 / Lc 81; `DIM` there is 3.8:1 / Lc 52.

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/11-colour.md](archive/11-colour.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Blend alpha colours before measuring: `SOFT`, `DIM` and the 85% kicker gold are much weaker than their hex suggests on a light backdrop.
- 2026-11-05: WCAG 2 passes light-on-dark text that APCA flags (kicker gold 9.6:1 but Lc 59). For this dark UI, report both.
- 2026-11-05: Convert stage px to visual angle before using any px-based rule (APCA, WCAG large-text): 1 stage px is roughly half a CSS px from the sofa.
- 2026-11-05: Don't assume Android dithers gradients: on Android 14 in Default colour mode it doesn't unless you ask.
- 2026-11-05: Grep `setDither` before claiming nothing dithers, because helpers like Kids `grad()` hide it.
- 2026-11-05: Check which colour each label really uses. Not every kicker goes through `kicker()` (the verse kicker is plain `DIM`).
- 2026-11-05: Audit every full-screen panel that has its own background (Quran player, Kids player), not just the home screen. They don't get the home dim and scrims, and the PC scripts that make their clips don't darken them.
- 2026-11-05: A colour-coded status line should colour the dot or icon, not the words. Saturated mid-light reds and greens lose about Lc 20 against `SOFT` over bright video.
- 2026-11-05: Count the 8-bit code steps of every dark full-screen ramp. Fewer than about 20 steps over 1,000 px means bands of 50 px or more, which you will see in a dark room.

- 2026-11-05: View alpha dims everything on the view, including the focus ring. Leave the focused view out, or dim only the text colour.
- 2026-11-05: A lightness-only state cue between `WHITE` and `SOFT`/`DIM` is under 3:1 (`WHITE` vs `SOFT` 1.24:1, vs Kids `DIM` 1.87:1). An "off" glyph needs about 35% white (`0x59FFFFFF`: 5.0:1 step, still 3.2:1 on a `0x1A` chip).
- 2026-11-05: Error or status text that temporarily replaces a `DIM` line should switch to `SOFT` while it shows.

