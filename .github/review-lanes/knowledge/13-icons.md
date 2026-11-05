# Lane 13 knowledge: App icons and artwork

## How to use this file
- Read this whole file before every review, then read the brief `../13-icons.md`.
- Research online whatever the PR touches that this file does not cover. Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for: **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. If the file grows past about 400 lines, condense the oldest Past findings and Lessons into one-line summaries. Never delete a source that is still cited.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code, measured the file, or seen it on the TV or in a screenshot.
- Neighbouring lanes: 15 (UI components and focus states), 5 (focus and D-pad), 3 (bitmap memory, decode cost), 4 (whole-screen look), 2 (anything published). Hand over findings that are really theirs.

## Core expertise

### 1. Android TV launcher art: what the system asks for
- A TV app must provide **both** a launcher icon (`android:icon`) and a banner (`android:banner`); depending on the device, either one is the launch point in the apps rows. [tv-start] [tv-icon]
- **Banner**: 16:9, "use an xhdpi resource with a size of **320 x 180 px**. Text must be included in the image", with one banner per language if the app is localised. [tv-start] The density table: mdpi 160x90, hdpi 240x135, **xhdpi 320x180**, xxhdpi 480x270, **xxxhdpi 640x360**, in `mipmap-*`. An adaptive 16:9 banner (two layers) is recommended; plain xhdpi 320x180 is for API 25 or lower. [tv-icon]
- **Banner do's and don'ts**: keep the logo in the safe area; don't add text or graphics with extra information or anything misleading; don't spill the logo out of the safe area; "Avoid adding any border around the logo as they get cropped and create unpolished visuals"; don't crop the logo; show the full logo, icon + text. [tv-icon]
- **Launcher icon**: 1:1, mdpi 80, hdpi 120, **xhdpi 160**, xxhdpi 240, **xxxhdpi 320** px; used in Settings, the Now Playing card and the Your apps row on Google TV. Adaptive icons are "highly recommended"; Android TV does **not** support themed (monochrome) icons; `android:roundIcon` is deprecated. [tv-icon]
- Quality checklist: **TV-LB** a 320x180 banner and at least a 160x160 (xhdpi) icon; **TV-BN** the banner contains the app's name; **TV-4K** "compatible with 4K by providing high-resolution assets within the app, including the app icon, banner, content cards". [tv-quality]
- The old Android TV design patterns page gave the same banner rule (320 x 180 px, xhdpi, text in the image, a version per language) and asked for full-colour **1920 x 1080** background images, scaled to fit otherwise. [tv-patterns-2014]

### 2. Adaptive icons (how other apps' icons are built)
- Two layers, foreground and background, each **108 x 108 dp**. The inner **72 x 72 dp** appears within the masked viewport; the outer **18 dp** on each side is reserved for system effects (parallax, pulsing). [aid-ref]
- The design guide: logo at least 48x48 dp and not more than **66x66 dp**, because the inner 66 dp is never clipped by any OEM mask; layers must not carry their own masks or background shadows. [adaptive] The TV guide speaks of a **72 x 72 safe zone** instead; treat 66 dp as the hard safe zone and 72 dp as the visible square. [tv-icon]
- `AdaptiveIconDrawable.getExtraInsetFraction()` is the padding on each side (the layer extends that fraction of the bounds beyond them), which is **1/4**: 108 = 72 x (1 + 2 x 1/4). [aid-ref] (the 1/4 value: arithmetic from 72 and 108 dp)
- Consequence for us: drawing both layers into bounds grown by size/4 on every side shows exactly the 72 dp viewport, edge to edge, with no OEM mask. That is what `fullBleed()` does (verified in code).

### 3. Play Store and Apple: what "a good icon" means elsewhere
- Google Play icon: **512 x 512 px, 32-bit PNG, max 1024 KB, full square**; Play applies the mask with a radius "equivalent to **30%** of icon size" and adds the shadow, so omit rounded corners and shadows from the asset. Use keylines as a guide, not a hard rule. Illustrated art works well full-bleed; don't force a logo to full bleed. No ranking, deal or programme badges, nothing misleading. [play-icon]
- Apple tvOS app icons are **rectangular, layered (2 to 5 layers), 800x480 px layout**, masked by the system to a rounded rectangle; provide unmasked layers ("Providing layers with pre-defined masking … makes edges look jagged"); don't bake in highlights, drop shadows, bevels, blurs or glows, the system adds them. [hig-app-icons]
- Apple: "Embrace simplicity" (one core idea, minimal shapes, simple solid or gradient background); include text only when essential; keep text in a top layer so parallax doesn't crop it; prefer clearly defined edges on foreground shapes. [hig-app-icons]
- tvOS images: provide @1x and @2x; flat icons as PNG (8-bit palette if 24-bit isn't needed), photos as JPEG/HEIC; leave a safe zone round foreground layers because focus scaling crops. Top Shelf wide art is 2320x720 pt (4640x1440 @2x); text in Top Shelf art must be in the image. [hig-images] [hig-top-shelf]
- Shared lesson from all three: **the platform owns the mask, corners, shadow and focus lighting; the art owns a simple full-bleed picture.** Our `fullBleed()` + `ring()` + `BrandGlow` is that same split.

### 4. Densities, mipmaps and sharpness
- `res/drawable/` with no qualifier is treated as **mdpi (160 dpi)** and scaled for other densities; `nodpi` resources are never scaled; tvdpi is about 213 dpi (1.33 x mdpi). [densities]
- Put launcher icons in `mipmap-*`: all mipmap folders stay in the APK and "some app launchers display your app icon as much as **25% larger**" than the density bucket asks, so a missing larger size makes the launcher upscale it and it "appear[s] less crisp". [densities]
- Apple's version of the same rule: high-resolution displays demand more pixels; an image that looks fine at design time can look pixelated or stretched on a device. [hig-images]
- Vectors (`VectorDrawable`) scale cleanly and keep the APK small; our `ic_calendar.xml` is one (96 dp viewport). [vector]
- On our stage: tile 132 px x focus 1.2 = **158 px** at a 1080p UI; if the UI is composed at 4K (lane 3 open question) the same tile is **317 px** on the panel. `fullBleed` makes 168 px bitmaps (Kids 192), so they are sharp at 1080p and upscaled about 1.9x at 4K.

### 5. Corners, edges and optical balance
- Our corner family (verified): dock 30 on 132 (**22.7%**), Kids icons **24%**, All apps 30 on 124 (24%), PIN icon 24 on 96 (25%), calendar 18 on 96 (18.75%), Play Store 30%. One family is roughly 22 to 25%; the calendar is the odd one out.
- Rounding twice (a pre-rounded bitmap then an outline clip of a slightly different radius) leaves a faint lighter or darker sliver at the corners. Round once.
- Optical balance (design judgement): white or light icons look bigger and closer than dark ones at the same size; thin-logo icons on transparent backgrounds look smaller. The fixed `ring(30, 2, 0x4DFFFFFF)` edge on every dock tile is what makes mixed brands read as one set; keep it.
- Shadows and glows: system-style shadow belongs to the container, not the art (Play, Apple). Our dock uses `BrandGlow` (a coloured oval behind) and no elevation; the All apps grid uses elevation 6. Don't add both to one tile.

### 6. Bitmaps: decode, scale, filter
- Decode no larger than the view: read bounds with `inJustDecodeBounds`, then `inSampleSize` (powers of two) to a lower-resolution copy that matches the UI component. [load-bitmap]
- Filtering: always `Paint.FILTER_BITMAP_FLAG` (or a `BitmapDrawable`, which filters by default) when scaling; mipmaps help below 50% scale (lane 3, romain-guy deck).
- A drawn Canvas icon needs `ANTI_ALIAS_FLAG` for shapes; `lockFace()` has it (verified).
- Memory: a 168 px ARGB_8888 icon is 168 x 168 x 4 = **113 KB**; 11 dock + about 40 grid icons is about 5.7 MB (arithmetic; lane 3 owns the budget).

### 7. Emoji and symbols as icons
- Android's Noto Color Emoji uses the **CBDT/CBLC** bitmap colour-font format. [noto-emoji] A bitmap emoji enlarged far beyond its strike size goes soft. Noto Color Emoji 2.047 has one strike at **109 ppem (136x128 px bitmaps)**, so emoji stay sharp to about 109 px text; our largest is 72 px (Kids Good Words, `KidsActivity.java:582`). Verified with fontTools 2026-11-05.
- Emoji newer than the device's font show as **tofu (☐)**; Android 11 and lower can't update the emoji font. [emoji2] Android TV 14 is fine for what we use: the newest, "🫧" (Emoji 14.0), is in Noto Emoji 2.047, and Android got Emoji 15 in the December 2022 feature drop [emoji15] (not seen on the box).
- Symbol glyphs not in the bundled font fall back to a system font with a different weight, width and baseline. Verified with fontTools (2026-11-05): Inter has ▶ ◀ ▼ ✓ › ● ⌫ ★ ∞ ✗ but **not** ⭐ ✕ ⋯; Inter Light/ExtraLight only have ›; Baloo has ▶ ◀ ▼ › ∞ but **not** ★ ✓ ✗ ⌫ ● ❚ (the Kids "❚❚" pause mark falls back). The Quran player's ⏮ ⏭ ⏸ are only keys: `PlayerIcon` draws them as anti-aliased vectors (`MainActivity.java:601-607`).
- Rule: a symbol that acts as an icon (keypad delete, transport controls, All apps) should be a vector drawable or a glyph we ship; emoji only where colour and playfulness are the point (Kids Good Words, weather).

### 8. Kids art and copyright
- US Copyright Office Circular 14: a derivative work is "based on or derived from one or more already existing works"; only the copyright owner may prepare or authorise one; and protection does not extend to material used unlawfully. Fan art of Goku, Mario or Kirby is a derivative work. [circ14]
- So: our published repo and the GitHub release APKs must contain **only original art** (and no look-alikes of a franchise mascot). Character names in text (`Worlds.java`) are a much smaller risk than pictures, but flag any PR that adds logos, screenshots or drawings of them.
- Character pictures the parent drops into `Pictures/KidsCorner` stay on the box and are never committed, bundled or downloaded by our code.
- Good original kids icons (design judgement): one big friendly shape, two to four flat colours, a soft gradient background, a face or a star, strong silhouette at 48 px, no text.

### 9. Review recipes
Zoom an asset to see aliasing and count colours (a 2-colour PNG cannot be anti-aliased):
```python
from PIL import Image
im = Image.open("adhan/src/main/res/drawable/icon.png").convert("RGB")
print(im.size, len(im.getcolors(1 << 20)))
im.crop((40, 40, 120, 120)).resize((320, 320), Image.NEAREST).save("zoom.png")
```
Does the bundled font have a symbol?
```python
from fontTools.ttLib import TTFont
cm = TTFont("app/src/main/res/font/inter_medium.ttf").getBestCmap()
print({c: ord(c) in cm for c in "⋯✕⏮⏭⏸⭐★✓"})
```
Dock contact sheet on the box: `adb exec-out screencap -p > home.png`, crop the bottom 260 px, view at 100% and at 25% (sofa distance). Check one adaptive icon (YouTube), one legacy icon, one missing app (letter tile), and the Quran and All apps tiles side by side.
Check the APK's launcher art: `aapt2 dump badging app-release.apk | grep -E "icon|banner"` (unverified command output on this repo).

### 10. Numbers to remember
| What | Number | Source |
|---|---|---|
| TV banner | 320x180 xhdpi; 640x360 xxxhdpi | [tv-start] [tv-icon] |
| TV launcher icon | 160 xhdpi; 320 xxxhdpi | [tv-icon] |
| Adaptive layers / visible / safe | 108 / 72 / 66 dp | [aid-ref] [adaptive] |
| Extra inset each side | 1/4 of bounds | [aid-ref] |
| Play icon | 512x512, 1024 KB, radius 30% | [play-icon] |
| tvOS icon layout | 800x480 px, 2 to 5 layers | [hig-app-icons] |
| Launcher upscales up to | 25% over the bucket | [densities] |
| Our dock tile at focus | 158 px (1080p UI), 317 px (4K UI) | arithmetic |
| fullBleed bitmap | 168 px (home), 192 px (Kids) | code |

## This box and this code
Verified by reading the code and measuring files on 2026-11-05 (line numbers drift; re-grep).
- **Our launcher art** (all in bare `res/drawable/`, so mdpi: Android scales it ×2 at xhdpi, ×1.33 at tvdpi [densities]; 192 px icons become blurry 384 px, the 512 px Kids icon is decoded at 1024 px, about 4 MB, banners are upscaled to 640x360): home `icon.png` 192x192 RGB, 7.8 KB, 141 colours, orange-to-crimson gradient with a yellow round face and two eyes, edges not anti-aliased; home `banner.png` 320x180, "Family TV Home" in a white serif beside the face. Kids `icon.png` 512x512 RGB = `C-happy-star-square.png` (byte-identical), a smiling yellow star on a teal-to-blue gradient; Kids `banner.png` 320x180, the star inside a rounded-square tile plus "Kids Corner" in a rounded bold face. Adhan `icon.png` 192x192 with **2 colours** (gold crescent on navy, hard stair-stepped edges); Adhan `banner.png` 320x180, crescent plus "Adhan / أذان".
- The three icons share no common shape or corner logic: two are flat full squares, the Kids one is a soft illustrated gradient. Readable as a family only through warm gold/yellow.
- Kids icon options on the network share (read-only): Night house, Rocket, Happy star, each as full art (512 RGBA with rounded corners), `-square` (512 RGB full-bleed) and `-banner` (320x180). `icon-choices.png` shows the three; Happy star was chosen. The bundled Kids banner (31,039 bytes) differs from `C-happy-star-banner.png` (36,809 bytes).
- Manifests: `android:icon="@drawable/icon"`, `android:banner="@drawable/banner"` in all three apps (`app` `:21-22`, `kids`, `adhan`). No adaptive icon, no `mipmap-*`.
- Dock catalogue (`DOCK`, `MainActivity.java` ~`:103`): Netflix, Crunchyroll, Stremio, Prime Video, Shahid, Movies & Shows (Kodi or VLC), Browser, Quran, Kids Corner, All apps, YouTube, each with a brand colour used for the letter fallback and `BrandGlow` (Quran glows `0xFF2FA58C`, All apps `0xFFDDE6FF`).
- `loadDockIcons()` runs on `bg`, tries each package in `pkgs`, then `findByName()` (label contains the name, e.g. "browse"), then gives up (letter tile). Icons reload 600 ms after every `onResume` (`:292`).
- `fullBleed()` (`:1802`) and `Icons.fullBleed()` are the same algorithm at 168 and 192 px; corner `30*168/132 = 38.2 px` (shows as 30 px at 132) vs Kids `0.24 * 192 = 46 px`.
- All apps grid: `CENTER_CROP` + `setClipToOutline` radius 30 at 124 px over a bitmap already rounded to 38.2 x 124/168 = **28.2 px**: double rounding (`:2310-2321`). Grid entries come from `r.loadIcon(pm)` (activity icon), Settings from `getApplicationIcon("com.android.tv.settings")`.
- `lockFace()` (`:2110`): 168 px, gradient `0xFF1E2A50` to `0xFF0B1022`, gold `0xFFF1D58A` shackle (12 px stroke arc) and body (radius 16), keyhole in navy; anti-aliased; no rounded corners in the bitmap (the 96 px `pinIcon` clips at 24).
- Kids app row (`KidsActivity.java` ~`:760-830`): icons `min(136, 1340/n - 44)` px, corner `w * 0.24`; banners `min(250, 1460/n - 52)` wide at 16:9, radius 26, `CENTER_CROP`; placeholder `roundGrad(appColor, 0xFF141A2A)` while the icon loads; "My cartoons" tile is the 🎬 emoji at `w * 0.46` px on the world's body colours, or "▶ Cartoons" in Baloo ExtraBold for banners. `banner(pkg)` runs on the UI thread.
- `characterPhoto()` (`KidsActivity.java` ~`:338`): matches `Pictures/KidsCorner/<name>.*`, decodes with `inSampleSize = max/600` (so a 4000 px photo is decoded at 1000 px; not a power of two).
- Built-in "world" sample clips on the share (`/mnt/project-files/tv-box/wallpapers/`) contain drawn look-alikes of Kirby, Mario hills and pipes, Sonic loops and dragon balls. They are not in the repo and the setup script deletes the old ones from the TV (`Kids Corner setup.ps1:61`). Keep it that way.

## Past findings
- 2026-11-05, PR #21 (docs only): no art changes. Fixed four fact errors in the lane's own files: ⏮⏭⏸ are drawn by `PlayerIcon`, not text; "❚❚" is in the Kids player, not the home; bare `drawable/` means runtime ×2 upscaling, not just "minimum size"; the Kids no-banner panel is rounded (26), only the icon inside is raw.

## Lessons learned
- 2026-11-05: Measure assets, don't trust their look in a thumbnail: counting colours and zooming showed the adhan icon has no anti-aliasing at all.
- 2026-11-05: "Drawn from each app's own icon/banner" was only half true: the home dock uses icons only; banners appear only in Kids "Big banners". Read the loader, not the comment.
- 2026-11-05: A symbol in a string is an icon with an unknown font. Check the bundled font's cmap before approving it.
- 2026-11-05: Check whether a symbol string is drawn or only used as a key (`PlayerIcon` uses "⏮" only to pick a shape).
- 2026-11-05: A bare `drawable/` asset is mdpi, so its problem is runtime scaling, not just its pixel size.

## Open questions to research next
- Is the box UI composed at 1080p or 4K (`adb shell wm size`, `dumpsys SurfaceFlinger`)? If 4K, raise `fullBleed` to about 320 px (lane 3 to weigh the memory).
- What density does the box report (`adb shell wm density`: 320 xhdpi or 213 tvdpi)? It decides whether our bare-`drawable/` art is upscaled ×2 or ×1.33.
- How do Google TV's Your apps row and Settings actually show our icon vs banner on this box, and with what mask and corner radius? Screenshot it.
- Answered 2026-11-05: Noto Color Emoji 2.047 has one strike at 109 ppem (136x128 px). A device copy of the font could still differ.
- Answered 2026-11-05: 🫧 (Emoji 14.0) is in Noto 2.047 and Android has had Emoji 15 since Dec 2022 [emoji15]. Still to see on the box.
- Should the home draw other apps' banners (as Google TV does) or stay with square icons (as tvOS and the chosen spec do)? Design question for lane 4, not a feature.
- Adaptive banner XML format for TV: partly answered 2026-11-05. [tv-icon] says "two layers" like adaptive launcher icons, but gives no layer sizes and no XML sample. Sizes still unknown.
- Material 3 icon and shape guidance (m3.material.io was blocked by the proxy on 2026-11-05).
- Nintendo, Sega, Toei and Mojang fan-content policies: what exactly do they allow for private, non-commercial use?

## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [tv-icon] https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines: banner and icon density tables, adaptive banner (two layers; no sizes or XML sample), no themed icons, banner do's and don'ts, launcher icon used in Google TV's Your apps row (re-checked 2026-11-05).
- [tv-start] https://developer.android.com/training/tv/start/start: icon and banner both required, 320x180 xhdpi, text in the image, per language.
- [tv-quality] https://developer.android.com/docs/quality-guidelines/tv-app-quality: TV-LB, TV-BN, TV-4K.
- [tv-patterns-2014] https://spot.pcc.edu/~mgoodman/developer.android.com/preview/tv/design/patterns.html: archived 2014 Android TV patterns page (banner rule, 1920x1080 backgrounds). Old; use only for history.
- [adaptive] https://developer.android.com/develop/ui/views/launch/icon_design_adaptive: 108 dp layers, 66 dp safe zone, 48 dp minimum logo, 18 dp reserved, no masks in layers.
- [aid-ref] https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable: 108/72/18 dp, extra inset fraction, `getIconMask`.
- [play-icon] https://developer.android.com/distribute/google-play/resources/icon-design-specifications: 512 px, 1024 KB, full square, 30% radius, no baked shadows, keylines, forbidden badges.
- [densities] https://developer.android.com/training/multiscreen/screendensities: density buckets, tvdpi 1.33x, nodpi, mipmap, launchers upscale up to 25%. Quote: "default resources ... designed for the baseline pixel density (mdpi) and resizes those bitmaps" (re-checked 2026-11-05).
- [emoji15] https://www.androidcentral.com/apps-software/emoji-15-android-colors-customization: Android got Emoji 15 in the December 2022 feature drop. Checked 2026-11-05.
- [vector] https://developer.android.com/develop/ui/views/graphics/vector-drawable-resources: vector drawables and why they suit icons.
- [load-bitmap] https://developer.android.com/topic/performance/graphics/load-bitmap: `inJustDecodeBounds`, `inSampleSize`, decode to the view's size.
- [pm-ref] https://developer.android.com/reference/android/content/pm/PackageManager: `getApplicationIcon`, `getApplicationBanner`, `getActivityBanner(Intent)`.
- [emoji2] https://developer.android.com/develop/ui/views/text-and-emoji/emoji2: tofu for unsupported emoji, emoji versions, font not updatable on Android 11 and lower.
- [noto-emoji] https://raw.githubusercontent.com/googlefonts/noto-emoji/main/README.md: Noto Color Emoji is CBDT/CBLC; SVG and PNG sources.
- [hig-app-icons] https://developer.apple.com/design/human-interface-guidelines/app-icons (read via https://developer.apple.com/tutorials/data/design/human-interface-guidelines/app-icons.json): tvOS 800x480 layered icons, unmasked layers, no baked effects, simplicity, text.
- [hig-images] https://developer.apple.com/design/human-interface-guidelines/images (read via .../images.json): scale factors, formats, layered images and parallax safe zones.
- [hig-top-shelf] https://developer.apple.com/design/human-interface-guidelines/top-shelf (read via .../top-shelf.json): Top Shelf sizes, text inside images.
- [color-on-tv] https://developer.android.com/design/ui/tv/guides/foundations/color-on-tv: banding on TVs, sRGB, Standard picture mode (also lane 4/11).
- [circ14] https://wpclipart.com/USC_circ14_as_html.html: HTML copy of US Copyright Office Circular 14 (derivative works; only the owner may authorise; unlawful material unprotected).
- Also relied on (opened by lane 3/4, same day): lane 3 [romain-guy-deck] for mipmaps and filtering; lane 4 [tv-focus] for focus scale 1.025 to 1.1 and glow 2 to 32 dp.
- To open next: m3.material.io icon and shape pages, Google TV launcher screenshots/teardowns, a franchise fan-content policy page, AOSP `config_icon_mask` for TV.
