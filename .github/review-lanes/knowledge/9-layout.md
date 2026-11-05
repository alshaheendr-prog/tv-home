# Lane 9 knowledge: Layout and spacing

## How to use this file
- Read this whole file before every review, then the brief `../9-layout.md`. Lane 4's knowledge file holds the Centre stage spec and the colour, glass and motion facts; this file holds geometry.
- Research online whatever the PR touches that this file does not cover. Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. Past about 400 lines, condense the oldest Past findings and Lessons into one-line summaries. Never delete a source that is still cited.
- Mark anything not verified on a screenshot or in code with **(unverified)**. Computed geometry is marked **(computed)**: it comes from font metrics and code constants, and the screenshot wins if they disagree.

## Core expertise

### 1. Units on our stage
- Google designs TV at **960x540 dp** ("At MDPI 1px = 1dp"), with assets aimed at 1080p. Our stage is 1920x1080 px, so **1 dp = 2 px**. [tv-layouts]
- tvOS points are 1920x1080 @1x, so **1 pt = 1 px** on our stage. [hig-layout]
- Android spacing uses an **8 dp grid** for layout, components and spacing, and a **4 dp grid** for small elements such as icons and type; "If using a baseline grid, stick to measurements of 4 and 8." On our stage: **16 px** main grid, **8 px** fine grid. [grids-units]
- Everything here is in px on the stage, so the system font scale does not change layout. Google asks TV apps to respect text scaling and to "Ensure that components still fit on the screen at larger text scales" (`adb shell settings put system font_scale 1.2`). Our fixed stage is a deliberate exception, so sizes must already be generous. [tv-a11y]

### 2. Safe areas and overscan
- Google: "position the elements with a 5% margin of **48dp** on the left and right sides, and **27dp** on the top and bottom". The same page also gives "58dp on the sides and 28dp on the top and bottom", and rounds 27 to 24 dp. On our stage: **96 to 116 px sides, 48 to 56 px top/bottom**. [tv-layouts]
- "Most modern TVs no longer have overscan issues." Still: "Don't adjust or clip background screen elements to the overscan safe area. Instead, allow partial display of offscreen elements." That is what `bleed()` does. [tv-layouts]
- Apple: "Inset primary content **60 points** from the top and bottom of the screen, and **80 points** from the sides ... regardless of TV compatibility settings or overscan cropping." = 60/80 px on our stage. [hig-layout]
- Working rule for this repo: text and focusable items at least **96 px from the sides and 54 px from top/bottom** at Screen size 100%; Apple's 80/60 px is the hard floor. Background layers bleed.
- Screen size maths: at fit f the stage is scaled by s = f/100 (on a 1920x1080 window) and centred, so the border added on each side is `(1 - s) * 1920 / 2` px horizontally and `(1 - s) * 1080 / 2` px vertically (in window px). 97% adds 29/16 px, 94% 58/32, 91% 86/49, **88% 115/65**. A stage element at x from the edge ends up at `border + x * s` from the real screen edge. (computed from `fitStage()`)

### 3. Grid and columns
- Google TV grid: **12 columns of 52 dp with 20 dp gutters**, 58 dp side margins (104 px / 40 px / 116 px on our stage); "4dp of vertical spacing between lines". [tv-layouts]
- Card layouts on that grid: 1 card 844 dp, 2 cards 412 dp, 3 cards 268 dp, 4 cards 196 dp, 5 cards 124 dp, with **20 dp** (40 px) spacing so the next card "peeks". [tv-cards]
- tvOS grids: unfocused widths from 860 pt (2 across) down to 160 pt (9 across), always **40 pt horizontal spacing and at least 100 pt vertical spacing**; "Use consistent spacing. When content isn't consistently spaced, it no longer looks like a grid". Make partly hidden off-screen content the same width on each side. [hig-layout]
- Our home screen is not a card grid: it is one right-aligned column plus a centred dock. The grid that matters is the right edge (110 px) and the vertical slots (34, 494, 700, dock).

### 4. Alignment
- "Align elements to make them easier to scan, and use indentation to convey hierarchy ... People assume that aligned items are related." [hig-layout]
- "Provide consistent alignment between similar content and UI elements", and "Establish consistent spacing between" like elements; inconsistent spacing makes "designs appear haphazard". [layout-basics]
- Right alignment for the column is the chosen spec (commits 147dfa9, e29d5f1). Apple's RTL guidance: one- and two-line blocks can follow the context's alignment, but a **paragraph of three or more lines should be aligned to its own language**; and "Use a consistent alignment for all text items in a list". So a right-aligned English verse is fine at 1 to 2 lines, and should not grow to 3+. [hig-rtl]
- W3C alreq: Arabic lines are normally right-aligned. [alreq]
- Optical vs geometric edges: TextView bounds include side bearings, italic overhang (Cormorant) and shadow room. Judge the visible ink edge on a screenshot. Big type (the 340 px clock) can sit several px inside its bounds.

### 5. Spacing, proximity and grouping
- Proximity "can overpower competing visual cues such as similarity of color or shape". "Using varying amounts of whitespace to either unite or separate elements is key"; space between a heading and its own content must be tighter than space to the previous section; "grouping together unrelated elements may camouflage them"; "groupings may change as they adapt to varying screen sizes" (recheck at 88%). [nng-proximity]
- Google TV on panels: "Avoid using too many panels to group contents"; "Place related contents together in one panel"; "Group contents based on user reading path" and don't send attention back and forth between panels. [tv-layouts]
- Practical ratio: inside-group gap at most about half the between-group gap. Our strip separates its parts with space only (lines and boxes removed); that only works if the between-part gap (36 px after the names, plus divider margins) is clearly larger than gaps inside a part.
- Google TV list items: content padding **16x12 dp** (32x24 px), min height **48 dp** (96 px) one line, 64 dp two lines, 80 dp three lines; dense 40 dp. Use as the floor for focusable rows. [tvm-listitem]

### 6. Hierarchy and balance
- Hierarchy tools: contrast in value and saturation, scale ("limit large elements to two"), grouping. "Use no more than 3 sizes" for header, subheader and body (web advice; TV allows a few more). **Squint/blur test**: blur the screenshot by 5, 10 and 20 px and check the intended focal point stays first; test with real content because a strong image can dominate. [nng-hierarchy]
- Apple: "Order content by relative importance" (top and leading side first); "Use progressive disclosure". [hig-layout]
- Google: "A single-pane layout can help drive attention to primary content"; make designs "scannable, uncluttered". Text over a hero image needs a scrim so the background does not distract; don't place text over images that contain text. [tv-layouts] [tv-carousel]
- For us: the clock is the hero; the verse band second; the strip third; the dock only when focused. The video's left half is the "image" side and should stay empty.

### 7. Focus growth needs room
- Google: "To prevent overlapping, it's important to consider the padding between items and the size increase of focused states." Default focus scales 1.025, 1.05, 1.1. [tv-layouts] [tv-focus]
- Apple: "an element gets bigger when it comes into focus ... make sure you don't let them overlap important information." [hig-layout]
- Room needed for a tile of size T at scale s, pivot at bottom centre, lift L: grows `(s-1)*T/2` each side and `(s-1)*T + L` upward. Dock: T 132, s 1.2, L 20 → **13 px each side, 46 px up**. (computed)

### 8. Density and reading at 10 feet
- Average viewing distance **3 m (10 ft)**; "make sure that text and other elements are large enough to be seen from a distance or limit the amount of text and reading". [design-for-tv]
- A French survey of 1,054 households found real viewing distances make many TV UIs "impossible or difficult to read", and questions the usual 10-foot sizes (abstract). [bertolus-2017]
- Legibility needs a character height of about **20 to 22 arcmin** (16 to 18 may be acceptable) [wiklund-2019]; televised characters of **10 arcmin** were read "with almost complete accuracy" in a 1959 lab test [seibert-1959]. On a 55" TV at 3 m, 1 stage px is about 0.63 mm, so 20 arcmin = **about 28 px of cap height** (65": about 23 px). (computed)
- Line length: **45 to 90 characters** including spaces; longer lines make it hard to find the next line. [butterick-ll] The verse band is one long line on purpose (no line change to find), but a two-line English verse at 140+ characters per line is hard to read; keep wrapped lines shorter.
- A preferred subtitle viewing distance of about 4.3 TV widths was found in one HDTV study; bigger characters increased preferred distance (abstract). [yeh-2012]

### 9. Overlay and panel placement patterns (Google TV templates)
- Right overlay: items you act on independent of the background; left overlay: navigation or actions relevant to the background; centre overlay: urgent information or a decision; bottom overlay: bottom sheets; alert: full-screen message. [tv-layouts]
- The adhan alert band is a bottom-overlay pattern; the adhan card is a corner card; home panels (apps, Quran, PIN, calendar) are centre or full-screen overlays.

### 10. Review recipes
Safe-area overlay on a screenshot (draw lines, then measure ink edges):
```python
from PIL import Image, ImageDraw
im = Image.open("home.png").convert("RGB"); W, H = im.size; d = ImageDraw.Draw(im)
sx = W / 1920
for x in (96 * sx, W - 96 * sx, W - 110 * sx): d.line([(x, 0), (x, H)], fill=(255, 0, 0))
for y in (54 * sx, H - 54 * sx): d.line([(0, y), (W, y)], fill=(255, 0, 0))
im.save("home-safe.png")
```
Find the visible right edge of text in a band (brightest columns): crop the band, threshold luminance above about 200, take the largest x with any lit pixel; compare with `W - 110 * sx`.
Measure a text width before it ships (shapes with HarfBuzz, so Arabic joins and kerning count):
```python
import uharfbuzz as hb  # pip install uharfbuzz in a venv
def width(path, text, px, feats=None):
    face = hb.Face(hb.Blob.from_file_path(path)); font = hb.Font(face)
    b = hb.Buffer(); b.add_str(text); b.guess_segment_properties(); hb.shape(font, b, feats or {})
    return sum(p.x_advance for p in b.glyph_positions) * px / face.upem
```
Height of a TextView line: with `includeFontPadding(false)` one line is `(hhea ascent + |descent|) / upm * size`; with padding on, add `(yMax - ascent) + (|yMin| - |descent|)` from `head` once for the block (top and bottom). Read the numbers with fontTools.
Screenshot set for a layout review (say what you could not capture):
1. Home at rest, stress test on, Screen size 100% and 88%, with the safe-area overlay.
2. Dock focused on the first and last tile with the most tiles the family has (and `DOCK_MAX` if possible).
3. Strip with a recitation playing (widest state) and the longest adhan name.
4. Quiet mode (clock and verse slid down `QUIET_DROP`) and any panel the PR touches.
5. Kids Corner home and the adhan card + alert band if their code changed.

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep). Geometry marked (computed) comes from font metrics (fontTools) and constants, not a screenshot.
- Stage: `fitStage()` `MainActivity.java:763`, `FITS :760`, `BLEED 200 :783`. Kids `fitStage()` `KidsActivity.java:222`, same `FITS`. The video is `DRIFT_ROOM 2.2%` larger than the window on each side (`:3419`).
- `at()` (`:837`): END gravity turns x into a right margin, BOTTOM turns y into a bottom margin.
- **Right column** (`buildCenter()`, `:1037`): `c` is 1920 wide at y `CLOCK_TOP 34`, `Gravity.END`, right padding `RIGHT_EDGE 110`. Children top to bottom: weather row (temp 58 px Inter Light + sky 28 px), day row (24 px), clock row (340 px Fraunces, margins -62/-66), date row (date 34 px Inter SemiBold · Hijri 32 px Plex Arabic with font padding). Estimated heights (computed): 70 + 29 + (419 - 128) + 53 ≈ 443 px, so the column ends near **y 477**, about 17 px above `LINE_Y 494` (unverified on screen).
- **Verse band** (`:1097-1133`): lineBox padding 56/6/56/10, kicker 20 px, Arabic Amiri Bold 38 px (32 px if over 110 characters) with font padding, English Cormorant italic 32 px (28 px if over 150 characters), max width 1700 each. One line each (computed): 6 + 24 + 6 + 104 + 2 + 39 + 10 ≈ **191 px → y 494..685**, 15 px above the strip. Amiri: hhea 1124/-634 (1.758 em per line) plus head yMax/yMin 1823/-909 padding (+0.974 em once). A second Arabic line at 38 px adds about 67 px and would reach about y 752, into the strip slot (computed).
- Stress verse (Al-Baqarah 2:286, 145 characters) is 888 px wide at 32 px Amiri Bold, so one line; the stress English (146 characters) is 1448 px at 28 px Cormorant, one line under the 1700 max. (computed with HarfBuzz)
- **Strip** (`assembleStrip()`, `:2435`): y 700..824, right margin 110, `Gravity.CENTER_VERTICAL|END`, no padding, no box, no lines; names then countdown (44 px), 36 px gap, divider, prayer times block 520 px wide, then the now-reciting part (hidden until playing). Prayer Arabic names hidden in the strip.
- **Dock** (`buildBottom()`, `:1147`): frame 188 tall, 36 px from the bottom → y 856..1044; tiles 132 px centred → y 884..1016; tile margins 11 px (22 px between), dock padding 48. With 11 tiles the dock is about 1790 px wide, x 65..1855 (computed). Focused tile (1.2, lift 20, pivot bottom centre) tops out near **y 838**, 14 px below the strip slot (computed). Label at y about 1000 (`:1394`), x clamped 60..1860.
- Pill (80, 56) top left; old top cards (80, 56) 560x330; event chip at y 56 + 330 + 16. These sit on the left margin line 80 (below Google's 96).
- **Kids** (`KidsActivity.java`): `place()` `:1896` uses absolute left/top. Welcome card (80, 56) `:457`; middle cards y 430, left 80..860 (`:535`), right 890..1840 (`:589`), `MID_SPLIT 890`, `MID_GAP 28`, `MID_BOTTOM = DOCK_Y - 30` (`:543`); `fitMiddle()` equalises the two heights and keeps them between welcome + 16 and the dock - 30. Dock row (80, `DOCK_Y 836`, 1760 x `DOCK_H 208`) → bottom **y 1044, 36 px from the bottom edge**; label at `DOCK_Y + DOCK_H - DOCK_NAME - 4` = y 988 (`:636`), clamped x 80..1840. `trim()` (`:1928`) gives back Baloo's air with negative padding (-0.2 em top, -0.26 em bottom).
- **Adhan** (`AdhanService.java`): `k = max(w,h)/1920` (`:110`); panel and card at 56 px side / 44 px top or bottom (`:254`, `:738`), card 560 px wide; alert band full width, 116 + 70 px fade, row centred (`:282-334`). Not scaled by Screen size: at 88% on a TV that really crops, the card's 44 px edge gap is what is left.
- Vertical map of the home stage at rest (computed; confirm on a screenshot):

| Slot | Stage y | Gap to next | Notes |
|---|---|---|---|
| Top safe line | 54 | | Google 5% |
| Column (weather, day, clock, dates) | 34..~477 | ~17 | `CLOCK_TOP` is a view top; glyph ink starts lower |
| Verse band (1 line each) | 494..~685 | ~15 | 2 Arabic lines would reach ~752 |
| Strip | 700..824 | ~14 to focused tile | fixed `STRIP_H 124`, text centred inside |
| Dock tiles at rest | 884..1016 | | focused tile top ~838 |
| Dock label | ~1000..~1027 | | ~53 px to the bottom edge |
| Bottom safe line | 1026 | | 1080 - 54 |

- Spacing values: `setPadding` in `MainActivity.java` uses 27 distinct values (0 to 90, mostly steps of 2); margins add 3, 11, -30, -62, -66. Kids margins: 80 sides, 16/28/30 gaps. There is no spacing token set.
- Clock width per minute (computed, 340 px Fraunces, spacing -0.01): "11:11" 504 px, "8:08" 574, "12:58" 659, "23:58" 701, "10:00" 731. Other faces with `tnum` are constant (Outfit 784, Jost 776, Cormorant 700, Inter 775 px). Right alignment keeps the right edge still.

## Past findings
None yet; lane created 2026-11-05 (split from lane 4).

## Lessons learned
- 2026-11-05: Measure before you judge: font metrics plus HarfBuzz widths predict most overlaps (Amiri's padded line is 2.7 em tall). Confirm on a stress-test screenshot.
- 2026-11-05: View bounds are not ink edges. Alignment is judged on the visible glyphs.
- 2026-11-05: Check commit history before calling a layout a bug: the right-aligned column and the 100% default were deliberate.

## Open questions to research next
- Real screenshots at 100% and 88% with the stress test: do the computed y values above match (column end about 477, verse end about 685, focused tile top about 838)?
- Does `adb exec-out screencap` capture the SurfaceView video on this box (needed to judge balance)?
- Should the dock label and the Kids dock move up to the 54 px bottom line, or is 36 to 53 px fine on this TV (it doesn't crop at 100%)?
- Should the adhan overlay follow the home Screen size (it lives in another app and ignores `fit`)?
- Propose a named spacing scale (8/16/24/32/48/64 px) only as a comment-level guide, or leave the ad hoc values? (Polish only, no refactor without the owner.)
- Find Material 3's own spacing page text (m3.material.io needs JavaScript; not opened) and Google TV's home-screen spec.
- What is the family's TV size and sofa distance? It sets the real arcmin of every size (Core 8).

## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [tv-layouts] https://developer.android.com/design/ui/tv/guides/styles/layouts: 960x540 dp, 5% margins (48/27 and 58/28 dp), 12 columns of 52 dp, 20 dp gutters, focus-growth padding, panel templates.
- [grids-units] https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units: 8 dp layout grid, 4 dp for small items.
- [layout-basics] https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-basics: consistent alignment and spacing.
- [tv-cards] https://developer.android.com/design/ui/tv/guides/components/cards: card widths per column count, 20 dp peek spacing.
- [tv-focus] https://developer.android.com/design/ui/tv/guides/styles/focus-system: focus scale values.
- [tv-carousel] https://developer.android.com/design/ui/tv/guides/components/featured-carousel: scrim behind text over images, no images with text.
- [tv-lists] https://developer.android.com/design/ui/tv/guides/components/lists: list anatomy (overline, title, subtitle).
- [tvm-listitem] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/ListItemDefaults.kt: list padding 16x12 dp, min heights 48/64/80 dp.
- [design-for-tv] https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv: 3 m viewing distance, limit text.
- [tv-a11y] https://developer.android.com/training/tv/accessibility: text scaling on TV, font_scale test.
- [hig-layout] https://developer.apple.com/design/human-interface-guidelines/layout (read via https://developer.apple.com/tutorials/data/design/human-interface-guidelines/layout.json): 60/80 pt safe area, tvOS grid widths, 40/100 pt spacing, focus growth, alignment.
- [hig-tvos] https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos (via .../designing-for-tvos.json): 8 ft+ distance, edge-to-edge artwork.
- [hig-rtl] https://developer.apple.com/design/human-interface-guidelines/right-to-left (via .../right-to-left.json): paragraph alignment by language, list alignment.
- [alreq] https://w3c.github.io/alreq/ (read via https://raw.githubusercontent.com/w3c/alreq/gh-pages/index.html): Arabic lines right-aligned, tall extents.
- [nng-proximity] https://www.nngroup.com/articles/gestalt-proximity/: proximity beats colour and shape; whitespace for grouping.
- [nng-hierarchy] https://www.nngroup.com/articles/visual-hierarchy-ux-definition/: scale, contrast, grouping, at most two large elements, blur test.
- [butterick-ll] https://practicaltypography.com/line-length.html: 45 to 90 characters per line.
- [bertolus-2017] https://consensus.app/papers/details/83b177020ed25ffaaf9cae70fc30193b/ (Bertolus et al. 2017, IHM): survey of real TV viewing distances (abstract only).
- [seibert-1959] https://consensus.app/papers/details/54cbdc2057c858d99016252f988fadd6/ (Seibert et al. 1959, J. SMPTE): 10 arcmin televised characters (abstract only).
- [wiklund-2019] https://consensus.app/papers/details/6a523653b68b5be49d4c2f7f922d2f12/ (Wiklund et al. 2019): 20 to 22 arcmin character height (abstract only).
- [yeh-2012] https://consensus.app/papers/details/4098900e23755a5c96d65f7504e5e6a8/ (Yeh et al. 2012, CISP): subtitle size and preferred viewing distance (abstract only).
- [autosize] https://developer.android.com/develop/ui/views/text-and-emoji/autosizing-textview: autosize needs a bounded view (not wrap_content), defaults 12 to 112 sp.
- To open next: m3.material.io spacing and layout pages (JavaScript only), Google TV home-screen references, BBC subtitle guidelines (blocked this session).
