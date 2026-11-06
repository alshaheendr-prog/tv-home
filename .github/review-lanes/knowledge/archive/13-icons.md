# Lane 13 knowledge: App icons and artwork: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- 2026-11-06, PR #23 (update 2, new launcher art): no Must. Audit S1–S8 confirmed fixed. S1: notification small icons are opaque PNGs and will show as white squares; use white vector `ic_notify.xml`. S2: the Adhan crescent is 82% of the tile and its visual centre is 10 dp low-left; scale it to 0.85 around (54,54) and move it +5 right, -5 up. Nice: draw `❚❚` with a Path inside its new disc; a Kids adaptive icon; one banner left inset (~28 px xhdpi) and mark height; `⭐`→`★` in All apps.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (no asset changes): no Must. S1: the Kids settings end arrows dim to the hint colour 0xB3, so use 0x66FFFFFF. N1: ⭐ now means "shown first in All apps", but the stats panel still uses ⭐ for Kids stars. N2: ❚❚ now also shows on onPause. N3: OK wraps past a dimmed ▶.

- 2026-11-05, PR #21 (docs only): no art changes. Fixed four fact errors in the lane's own files: ⏮⏭⏸ are drawn by `PlayerIcon`, not text; "❚❚" is in the Kids player, not the home; bare `drawable/` means runtime ×2 upscaling, not just "minimum size"; the Kids no-banner panel is rounded (26), only the icon inside is raw.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (main `d772bcc`): no Must fix. Should fix: launcher art in bare `drawable/` (S1); the 2-colour Adhan icon with notched tips (S2); a ghost disc in the Adhan banner from a flat "cut" circle over a gradient (S3); three logos with three typefaces and no shared system (S4); "⋯" and "✕" not in Inter (S5); the inner tile in the Kids banner (S6); the raw-icon Kids no-banner fallback (S7); All apps double rounding of 28.2 vs 30 px (S8). Nice: FE0F on the home `skyIcon` (N1), calendar corner 18→23 (N2), 4K `fullBleed` size (N3).


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
- [tv-icon] re-checked 2026-11-05: the density tables put banners and icons in `mipmap-*`, and the manifest sample uses `@mipmap`. It says "Avoid adding any border around the logo". There are still no numeric banner safe-area or layer sizes.
- [emoji-pres] https://unicodefyi.com/glossary/emoji-presentation/ (opened 2026-11-05): Emoji_Presentation=No means text by default; VS16 U+FE0F forces emoji, VS15 U+FE0E forces text.
- [emojicompat-default] https://review.blissroms.org/plugins/gitiles/platform_frameworks_support/+/ca429f4bd64320e424b2e984c2e9d615679fa9cd (opened 2026-11-05): an AndroidX EmojiCompat commit. Characters like U+2639 and U+26F9 default to text unless followed by U+FE0F, and TR51 leaves the default to the app.

- [disabled-38] https://api.flutter.dev/flutter/material/ButtonThemeData/getDisabledTextColor.html : Material disabled text is at 0.38 opacity. Opened 2026-11-05.
- [cp-275a] https://codepoints.net/U+275A : not an emoji; the glyph comes from Noto Sans Symbols 2. Opened 2026-11-05.

- [notif-icon] https://documentation.onesignal.com/docs/en/./notification-icons (opened 2026-11-06): small notification icons use only the alpha channel; opaque art shows as a filled square; 24 dp, 24/36/48/72/96 px densities; tinted from Android 12.
- Tried: the VectorDrawable reference (developer.android.com, ?hl=it) for the order of group transforms. The fetched part had no attribute text, so the "scale and rotate around the pivot, then translate" order is **(unverified)**.

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
- Does the box draw text-default weather glyphs without FE0F (☀ ☁ ⛈ 🌫 🌧) in colour or in monochrome? Screenshot the "Tomorrow" line on a clear day and on a cloudy one.
- Does the Google TV "Your apps" row on this box show our icon or our banner, and with which mask? (still open)
- Unicode's `emoji-data.txt` was blocked by the proxy (unicode.org 403) on 2026-11-05. The text-default status of ☀ ☁ ⛈ 🌫 🌧 is from memory of the Unicode data, so it is **(unverified)**.

- Does the box's fallback font have U+275A? android.googlesource.com is blocked by the proxy, so check on the box, or draw it with a Path.

- Does Google TV on this box show foreground-service notifications (Adhan) anywhere a family member will see them? It decides how visible S1 is.
- What is the exact order of VectorDrawable `<group>` transforms? Check the attribute docs, or render one on the box.
- Does `getApplicationIcon` for Kids return the 160 px (xhdpi) or the 320 px PNG on this box? Check with `adb shell wm density`.
