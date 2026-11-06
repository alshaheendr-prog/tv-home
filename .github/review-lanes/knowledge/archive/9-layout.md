# Lane 9 knowledge: Layout and spacing: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (audit update 1): 2 Must, 1 Should, 5 Nice.
  - The message pill is placed once in `toast()` and goes stale when layers change, so it can cover the dock or a panel header.
  - Over the Quran player, the pill covers the controls (buttons at y 906..990).
  - Adhan `LIFT 56` cuts the padded Arabic view to 80 of 84.75 px and leaves the ink 16 px under the gold line. `LIFT 32` is suggested.
  - All of this is (computed).
  - Outcome: the pill now moves with each layer (`placePill`), and over the player it sits top centre. The adhan was fixed with BAND 148 rather than LIFT 32, so the words keep their 56 px overscan margin and get 92 px of room.

- 2026-11-05, PR #21 (docs): self-check. Fixed the false "end tiles past 96 px" claim; added the neighbour 1.06 / lift 5 tiles and the stale `magnify()` comment; recorded the build-time verse sizes, the 4 px tile-to-label gap, the Kids 30 px gutter and the broadcast safe areas.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (main d772bcc): 3 Must, 5 Should, 4 Nice, all in panels and Kids grids. Quran top-right buttons overlap the now-playing block once a reciter is named (button 1043 px for Al-Minshawi, starts x 767; block x 430..1070); Parent rows and the Kids cartoon grid clip the focused item's sides; All apps (1722 in 1720) and the surah grid clip the gold shadow; reciter list and Kids apps grid sit 20 px off-centre; Parent rows 76 px < 96. (computed)


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
- [itu-bt1848] https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.1848-1-201510-I!!PDF-E.pdf: graphics-safe 5% and action-safe 3.5% areas. Checked 2026-11-05.
- [ebu-r95] https://tech.ebu.ch/docs/r/r095.pdf: the same safe areas for 16:9 HD. Checked 2026-11-05.
- [bbc-subs-mirror] https://bbcbreakingnews.pages.dev/accessibility/forproducts/guides/subtitles: unverified mirror of the BBC subtitle guidelines (online subtitles at most 68% of the width, inside the central 75% x 90%, at most 2 lines). Checked 2026-11-05.
- To open next: m3.material.io spacing and layout pages (JavaScript only), Google TV home-screen references, the official bbc.co.uk subtitle guidelines.
- [tv-layouts] re-opened 2026-11-05: "To prevent overlapping, it's important to consider the padding between items and the size increase of focused states"; points to Cards for component spacing.
- [cliptopadding] https://codemia.io/knowledge-hub/path/android_what_does_the_cliptopadding_attribute_do: with `clipToPadding false`, children "may draw into the padded area"; "`clipChildren` controls clipping to parent outer bounds" (a secondary source; the official ViewGroup reference page did not load the method text). Checked 2026-11-05.
- [tv-compose-lists] https://developer.android.com/training/tv/playback/compose/lists: no padding guidance for focused items (checked 2026-11-05; not useful, don't re-open for this).

- [m-snackbar] https://zyun.gitbook.io/temporary-views/official/material-design/original/snackbars (a mirror of Material's snackbar page): don't place one in front of frequently used targets or navigation, show it above persistent bottom navigation, and keep it in the same spot. Checked 2026-11-05.
- [webos-notif] https://webostv.developer.lge.com/develop/guides/notifications: TV toasts are a non-modal overlay at the bottom of the screen. Checked 2026-11-05.

## Open questions to research next
- Real screenshots at 100% and 88% with the stress test: do the computed y values above match (column end about 477, verse end about 685, focused tile top about 838)?
- Does `adb exec-out screencap` capture the SurfaceView video on this box (needed to judge balance)?
- Should the dock label and the Kids dock move up to the 54 px bottom line, or is 36 to 53 px fine on this TV (it doesn't crop at 100%)?
- Should the adhan overlay follow the home Screen size (it lives in another app and ignores `fit`)?
- Propose a named spacing scale (8/16/24/32/48/64 px) only as a comment-level guide, or leave the ad hoc values? (Polish only, no refactor without the owner.)
- Find Material 3's own spacing tokens (m3.material.io needs JavaScript; not opened) and Google TV's home-screen spec.
- Read the official bbc.co.uk subtitle guidelines (only an unverified mirror [bbc-subs-mirror] was opened).
- What is the family's TV size and sofa distance? It sets the real arcmin of every size (Core 8).
- Confirm items 1 to 3 on box screenshots: the Quran panel with Al-Minshawi chosen and playing; Parent settings with a row focused; Kids cartoons with the first card focused.
- Material 3 spacing tokens: m3.material.io and the pub.dev M3 Expressive spacing page both failed to load on 2026-11-05.

- On a box screenshot: does the adhan Arabic line clip at `LIFT 56` (a padded view of 80 of 84.75 px)? Does the pill over the player cover Play?

