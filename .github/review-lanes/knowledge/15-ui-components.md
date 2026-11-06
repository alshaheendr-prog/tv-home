# Lane 15 knowledge: UI components and states

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../15-ui-components.md`.
- History lives in [archive/15-ui-components.md](archive/15-ui-components.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
- Keep the component inventory (in "This box and this code") up to date: when a PR adds or changes a component, update its row. That inventory is this lane's main tool.
- Mark anything you could not verify with **(unverified)**. Move it to verified only after reading the code or seeing it on the TV or in a screenshot.
- Units: 1 dp in Google's TV specs = 2 px on our 1920x1080 stage; 1 tvOS pt = 1 px (see lane 4 knowledge, Core 1).
- Lane 4's knowledge file still holds the shared type, contrast, motion and glass reference; reuse it instead of copying it here.

## Core expertise

### 1. The state model every control needs
- Google TV: one focused element at a time. Per-element states are **default, focused, pressed**, plus **enabled, disabled, selected**. "When an element is selected, it enters a 'pressed' state until the remote button is released." Disabled: "background color and opacity display a lower prominence to express that the object is not clickable." [tv-focus]
- Google's use-case matrix: cards have enabled x default/focused/pressed; buttons add disabled; chips have enabled, enabled+selected, disabled, disabled+selected, each x default/focused/pressed. So a selectable control can need up to 9 looks. [tv-focus]
- tvOS: up to **five** visually distinct states: unfocused ("less prominent"), focused ("elevation to the foreground, illumination, and animation"), highlighted/pressed ("instant visual feedback ... a button might briefly invert its colors and animate"), selected (chosen or activated, for example a filled heart), unavailable ("appears inactive"). Because focus grows an item, make sure "the larger item doesn't crowd the surrounding interface". [hig-focus]
- Material 3 state layers: a translucent overlay of the content colour signals the state. Compose M3 tokens: **hover 0.08, focus 0.10, pressed 0.10, dragged 0.16**. Disabled filled button: container **0.10** (older spec 0.12), label and icon **0.38**. [m3-state-tokens] [m3-filled-button-tokens] (m3.material.io itself needs JavaScript; these numbers come from the generated token files.)
- Rule for reviews: list every state the control can be in, then check each has its own look, and that combined states (focused + selected) keep both cues.

### 2. What Compose for TV actually ships (tv-material defaults)
These are the defaults Google's own TV components use; they are the best "house style" reference for Android TV. [tvm-button] [tvm-surface] [tvm-listitem] [tvm-card] [tvm-iconbutton]
- **Button:** rest container `surfaceVariant` at 80%, content `onSurface` at 80%. **Focused: container becomes `onSurface` (near white) and content `inverseOnSurface` (dark)**, so focus inverts the button. Pressed = focused colours. Disabled: container and content at **40%**. Focused scale **1.1**; pressed scale = rest scale (1.0), so a press visibly "sinks" back. Focused-disabled gets a **1.5 dp** border at 20%. Min size 58x40 dp (116x80 px), padding 16/10 dp.
- **Icon button** sizes: small 28 dp, medium 40 dp, large 56 dp (56/80/112 px); focused scale 1.1.
- **Clickable/selectable Surface:** focused scale 1.1, pressed = rest; disabled container alpha 0.4; selected container `inverseSurface` at **0.5**; focused border 2 dp.
- **List item:** focused scale **1.05** (smaller than buttons and cards), shape 8 dp corners, selected container opacity 0.4, min heights 48/64/80 dp for 1/2/3 lines.
- **Card:** focused scale 1.1, focused border **3 dp**, shape 8 dp; subtitle alpha 0.6, description 0.8; glow none by default.
- Design guidance pages: text and icon buttons are "fully rounded"; wide and image buttons use 12 dp corners; label in sentence case on one line; "Don't overuse buttons"; "keep one primary action per screen"; "the first button in a group gets focus first and acts as the primary action"; use only one button variant in a column. [tv-buttons]
- Focus indicators can mix **scale, border, glow (2 to 32 dp), outline, colour**; default scales 1.025, 1.05, 1.1; "scaling values ... can vary based on their size". [tv-focus]
- Lists: "List items are not buttons. The items don't have containers"; use a container background only when necessary; show selection with an icon, "avoid relying solely on the background color to indicate selection"; don't put buttons inside list items. [tv-lists]
- Cards: one topic per card; content block as wide as the image; compact cards need a scrim over the image; 20 dp spacing in rows. [tv-cards]

### 3. Focus indicators that read from the sofa
- Android TV: "use color, size, animation, or a combination ... Use a **uniform scheme** for indicating focus across your application"; give "sufficient padding ... so that the highlights around them are clearly visible"; always have an item in focus. [tv-navigation]
- Smashing (TV, 2025): three basic focus styles are **scaling, colour inversion, borders**, and they combine; scaling suits image-only items; use highlight colours sparingly but "in a highly contrasting way". [smashing-tv]
- tvOS: rely on the system effect where possible; custom focus only if needed; never change focus without the person acting, except to a nearby item when the focused one disappears. Use a highlight for whole rows in lists, a ring for fields. [hig-focus]
- WCAG 2.4.13 (AAA, but a good bar): the focus indicator area must be at least a **2 px perimeter** of the item and have **3:1 contrast between the same pixels focused vs unfocused**; an inset indicator needs to be thicker (3 px example); "dark grey on black fails, white on black passes". [wcag-focus-appearance]
- WCAG 1.4.11: visual information that identifies a component's **state (focused, selected)** needs 3:1 against adjacent colours; inactive (disabled) controls are exempt. [wcag-non-text]
- Our application: a 3 px gold `0xE6F1D58A` edge over a dark scrim easily passes; the same edge over a bright clip may not. A brighter fill alone (`0x26FFFFFF` over dark violet in Kids parent rows) is a weak, single cue: pair it with a scale or edge.

### 4. Press feedback
- Apple: "**Always include a press state** for a custom button. Without a press state, a button can feel unresponsive." [hig-buttons]
- Google TV: "TV UI must provide **instant and distinct feedback** when buttons are pressed." [design-for-tv]
- NN/g: feedback for each action is "perhaps the most basic guideline of user-interface design"; "Whenever users interact with a system, they need to know whether the interaction was successful"; feedback also stops repeat presses. [nng-visibility]
- Response limits: **0.1 s** feels instant (a highlight should appear within it), **1 s** keeps flow (show that something is happening beyond it), **10 s** needs progress and a way out. [nng-response-times]
- On Android, `View.onKeyDown` calls `setPressed(true)` on a confirm key (DPAD_CENTER, ENTER) for a **clickable or long-clickable** view, then starts the long-press timer. So a `StateListDrawable` foreground with `state_pressed`, or a `ColorStateList`, gets the press for free on OK down; a view that is only `setFocusable` (not clickable) never gets it. [aosp-view] [tv-navigation]
- Good press patterns for a calm premium look: a short dip (scale 0.94 to 0.97, about 70 to 100 ms) and back, or a brief brighter flash of the focus fill; tv-material simply drops focused scale back to 1.0 while pressed.

### 5. Selected, on/off and value rows
- Selection must be visible without colour alone: a check mark, filled icon or words ("On", "Shown", "✓ allowed"). [tv-lists] [wcag-non-text]
- tvOS distinguishes focus from selection: in tvOS "selection requires a separate gesture" (OK), so a focused item must not look chosen. [hig-focus]
- Rows whose OK cycles a value (`parentRow`) should show the value as the thing that changes, and change it at once on OK so the press is answered.

### 6. Empty, loading and error states
- NN/g empty states: "Do not default to totally empty states"; communicate system status; don't show "No records" while still loading; use the empty state to teach ("Star your favorites to list them here"); give a direct pathway to fill it. [nng-empty]
- Apple loading: "Show something as soon as possible" (placeholders); let people do other things while waiting; use determinate progress when you know how long. [hig-loading]
- Apple feedback: match the delivery to the importance (passive status for status, interrupt only for real risk); "Show people when a command can't be carried out and help them understand why"; confirm only important completions. [hig-feedback]
- NN/g error messages: near the source; more than colour or animation; plain words; specific; give the next step; "Avoid words like invalid, illegal, incorrect" (don't blame); no humour in routine errors; preserve what the user entered. [nng-errors]
- NN/g notifications: passive notifications "can easily be missed", so don't rely on them for things the user must understand; "Toasts that disappear quickly are a poor choice for errors". [nng-ivn]
- Material snackbar: brief, doesn't interrupt, disappears on timeout; **only one at a time**, a new one dismisses the old; can carry one action. [mdc-snackbar]
- Apple alerts: use sparingly, only for essential, actionable information; not merely informational; avoid alerts at app start; don't alert for common undoable actions. [hig-alerts]

### 7. Discoverability and signifiers
- NN/g: people must be able to tell what is interactive "without hovering or hunting"; don't style static content like buttons (and the reverse: something that looks like a button must do something); icons need labels unless standard. [nng-clickable]
- Hidden actions (hold OK) need a signifier where the action is. Smashing notes press-and-hold "opens a contextual menu with additional actions" on TV, but nothing tells people it exists, so show a short "Hold OK ..." hint in context. [smashing-tv]
- Labels: sentence case, one line, the same verbs everywhere. [tv-buttons]

### 8. Overlays, dialogs and stacking (the look)
- NN/g: modal dialogs interrupt and cost memory; use them for critical warnings, required input or multi-step flows; overuse trains people to ignore them; every dialog needs a clear way out. [nng-modal]
- Android TV: Back goes to the previous destination; no on-screen back button needed; when the only actions are confirm, destructive or purchase, add a **Cancel** button. [tv-navigation]
- Smashing: a side menu or overlay dims the background to keep focus on it; let the interface "get out of the way" on a single task. [smashing-tv]
- Our rule: one layer looks active at a time (the layer under it is dimmed by a scrim), open and close are mirror images, and focus visibly returns to where the family was.

### 9. Consistency as a design system
- NN/g: internal consistency means "use the same patterns everywhere inside the system", including across a product family (same function, same icon, same dialog layout); external consistency means matching platform conventions; break a convention only when it clearly helps. [nng-consistency]
- Brad Frost's **interface inventory**: screenshot every distinct treatment of each component, group them side by side; "Displaying a plethora of similar-but-still-different treatments next to each other exposes redundancy"; tell intentional from unintentional deviation. [frost-inventory]
- Apple buttons: use style, not size, to mark the preferred choice; limit prominent buttons to one or two per view; give the primary role to non-destructive actions. [hig-buttons]
- Apple collections: keep padding around items so focus effects stay visible and items never overlap; animate inserts, deletes and reorders. [hig-collections]

### 10. Review recipes
Interface inventory for a PR (do this for every control the PR touches, plus its siblings):
| Control | Rest | Focused | Pressed | Selected | Disabled | Empty/loading/error |
|---|---|---|---|---|---|---|
Fill each cell with the drawable, colours, scale and shadow from the code, then a screenshot. Empty cells are findings unless the state cannot happen.

Focus contrast on a screenshot: sample the same pixel of the focus edge focused and unfocused (and over the brightest clip); use lane 4's `ratio()` recipe; aim for 3:1 or more.

Press check on the box: press and hold OK on the control for half a second, then release; something must change on DOWN or within 100 ms of UP.

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep).

### Shared building blocks (home, `app/.../MainActivity.java` unless noted)
- `focusGlass(r)` (~`:1939`): vertical gradient `0x47FFFFFF` to `0x1FFFFFFF`, 3 px stroke `0xE6F1D58A`. "The focused look everywhere."
- `goldLift(v, has, z)` (~`:1946`): gold ambient and spot shadow via `translationZ` (PIN keys z 16, All apps z 22). Not used on rows or player buttons.
- `glass(r, fill, edge)` (~`:804`): plain `GradientDrawable`, 2 px edge. Used for rest states of pills, player buttons, surah cells, reciter tiles.
- `goldEdge()` (~`:2730`): the "selected" look for reciter tiles.
- `Glass.java`: tint + frost + sheen + top-bright 2 px edge (`0xB3FFFFFF/0x26FFFFFF/0x40FFFFFF`), leaning to `Accent` colour; no blur. `GlassFrame`: glow rings (7 fading outlines) + optional blurred backdrop + `Glass`. `PanelBg.java`: night-blue `0xE61B2140` to `0xEB0A0D1C`, warm gold radial glow, gold hairline on top, 2 px edge.
- `overlay()` (~`:1871`): full-screen scrim `0xCC050810/0x8C050810/0xD9050810`, elevation 30. `show()` (~`:1898`): 260 ms fade with `EASE`, children rise 28 px over 420 ms, 40 ms stagger. `closePanels()` (~`:2002`): 180 ms fade out, focus returns 200 ms later to the dock tile that opened the panel.
- `panelCard()` (~`:1953`, `PanelBg`, 44 radius), `listCard()` (~`:1969`, kicker + 46 px title + scrolling list with 110 px fading edge), `panelHint()` (~`:1995`, 22 px `SOFT` pill top right).
- Message pill (`pillText` ~`:1136`, `toast()` ~`:844`, `updatePill()` ~`:1551`): 28 px Inter Medium white on `Glass(40, 0, 0, 0x4D000000)`, top left at (80, 56), fades in 200 ms, stays 3.5 s, fades out 300 ms; hides the "now playing" widget while shown; a new message replaces the old one (one at a time). Also carries the 10-minute reminder, the Jumu'ah reminder and "Paused for the adhan".

### Home component inventory
| Component | Rest | Focused | Press | Selected / other |
|---|---|---|---|---|
| Dock tile (`addTile` ~`:1211`, `magnify` ~`:1363`) | icon, 2 px `0x4DFFFFFF` ring | 1.2, lift -20, neighbours 1.06 and pushed aside, `BrandGlow`, name label 22 px | none (launches) | arrange: 4 px gold ring + `ARRANGE_HINT` label |
| PIN key (~`:2087`) | round 96 px, `0x1FFFFFFF` fill | 1.12, `0x59/0x26FFFFFF` fill, 3 px gold, `goldLift` 16 | dip 0.94 (70 ms) then 1.12 | wrong: damped shake 420 ms + red `0xFFFF6B6B` dots; right: green `0xFF5BE38A` dots; **resting** (~`:2189`): keys look normal, every press = wrong feedback, `pinSub` counts down `PinLock.waitText` |
| All apps item (~`:2307`) | no box, label `SOFT` | `focusGlass(30)`, 1.1, label white, `goldLift` 22 | none | Kids pick: "⭐ " prefix; hold OK: `appMenu` |
| App menu button (`menuButton` ~`:2394`) | no box, `SOFT` | `focusGlass(22)`, white, **no scale** | none | |
| Parent row (`parentRow` ~`:3324`) | no box, value `DIM` 26 px | `focusGlass(22)`, 1.03 | value changes (list redrawn) | value words ("On", "Shown", "100%") |
| Player button (`playerButton` ~`:600`) | pill `glass(99, 0x1AFFFFFF)` | `focusGlass(99)`, 1.08 | none (icon flips play/pause) | |
| Surah cell (~`:2598`) | `glass(24, SURAH_FILL, SURAH_EDGE)` | `focusGlass(24)`, 1.07, English `DIM` to `SOFT` | opens player | |
| Reciter tile (~`:2690`) | `glass(22, 0x0F/0x24FFFFFF)` | `focusGlass(22)`, 1.03 | saves, closes list | chosen: `goldEdge()`, gold text, "✓" |
| Quran panel button (`quranButton` ~`:2619`, e.g. "Reciter: Any") | pill `glass(99, 0x1AFFFFFF)` | `focusGlass(99)`, 1.06 | opens or acts | |
| Calendar row (~`:3284`) | no box | `focusGlass(20)`, 1.02 | **nothing happens** | "Today/Tomorrow" gold within 7 days |
| Panel hint (`panelHint`) | 22 px `SOFT` pill | not focusable | | text swaps ("Added to Kids ⭐") |

### Kids inventory (`kids/.../KidsActivity.java`)
| Component | Rest | Focused | Press | Other |
|---|---|---|---|---|
| Dock tile (~`:770`-`:845`) | icon or 16:9 banner, 2 px `0x4DFFFFFF` edge | magnify + label (same idea as home) | none | hold OK: PIN, then arrange |
| Lock tile (~`:856`) | `Glass(44, 0x40000000)`, "🔒", red setup dot | magnify, "Grown-ups" label | opens PIN | |
| `button()` (~`:1932`) | `0x26FFFFFF` pill | **inverts**: white fill, dark ink, no scale | none | |
| `goldButton()` (~`:1977`) | `0x2EFFC233` pill, gold ink | gold fill `0xFFFFD45A`, dark ink, 1.06 | none | |
| PIN key (~`:1472`) | 130x96 rounded rect `0x1AFFFFFF` | white fill, dark ink, 1.06 (160 ms, default interpolator) | none | wrong: 3-step shake 180 ms, red `0xFFFF5A5A`; no success flash; **resting** (~`:1533`): keys look normal, every press = wrong feedback, countdown via `PinLock.waitText` |
| Cartoon card (~`:1388`) | `0x1AFFFFFF` | white card, dark title, 1.08 (220 ms, default interpolator) | plays | empty state panel `cartoonsEmpty` |
| Parent value row (~`:1687`) | value pill "◀ x ▶" | background `0x26FFFFFF` only | Left/Right change value | |
| App permission cell (~`:1828`) | icon (dimmed to 115 when not allowed), "✓ allowed" / "not allowed" | background `0x24FFFFFF` only | toggles | words + colour + icon alpha (good) |
| Rest screen (`buildRest` ~`:1112`) | full screen `0xF20B0820`, 200 px moon/star, 88 px title | star buttons or "Grown-ups" focused | | 600 ms fade in, 400 ms out |
- Kids `launch()` (~`:1058`) returns silently if the app is missing or Kids is resting.
- Kids `WHITE` is pure `0xFFFFFFFF` (home uses `0xF5F5F7`); intentional brightness for kids or not is an open question for lane 11.

### Adhan overlay (`adhan/.../AdhanService.java`)
- Own colours: `GOLD 0xFFFFC24A`, `TEXT 0xFFF6F3FF`, `MUTED 0xB8F6F3FF` (~`:59`); fonts are **Baloo** (the Kids face) for the panel, reminder and widget card, but **Inter + Plex Arabic** for the banner.
- Banner (`buildAlert` ~`:286`): full-width band 116 px + 70 px fade, near-solid `0xE6/0xF5080B14`, gold line drawing out from the centre, 46 px English + 50 px Arabic. Rises 750 ms, line 900 ms, text 700 ms (all decelerate 2.2). Hides after the adhan ends or 60 s. Takes no keys.
- Reminder (`buildReminder` ~`:378`): top-right card, `0xD90A0C16` with 2 px `0x99FFC24A` gold edge, 36 px bold gold title + 28 px sub, 15 s, 500 ms fades (default interpolator). Shown only when the launcher is **not** in front; the launcher shows its own pill instead.
- Widget card (`buildCard` ~`:698`): `0xE00A0C16`, 2 px `0x33FFFFFF` edge, 20 px gold kicker, Amiri verse, corner chosen in settings, 56/44 px from the corner.

### Facts that matter for every review
- No `StateListDrawable`, `state_pressed`, `setEnabled(false)` or disabled look exists anywhere in the three apps. Only home PIN keys animate a press.
- PIN rest (both pads, PR #20): `PinLock.waitText` (`PinLock.java:69`) says "Too many tries. Wait N seconds" below 60 s, then rounds up to whole minutes. The pad stays live and each press runs the wrong-PIN shake; nothing is dimmed. Android's lock screen instead turns entry off (`handleAttemptLockout` → `setPasswordEntryEnabled(false)`) and counts seconds ("Try again in # seconds."). [aosp-keyguard]
- Every focus change is done by `setOnFocusChangeListener` swapping drawables and animating scale, so each component re-implements focus. A PR that adds a control must copy the right sibling exactly.
- Loading copy in use: "Forecast loading…", "Loading · جارٍ التحميل…" (Quran). No clips: a calm gradient, retried every 3 s for 2 minutes, with no message.
- Missing-app copy in use: "X isn't installed yet. Opening the Play Store…" (then opens it after 1.2 s), "Kids Corner isn't installed yet. Run Update TV on the PC.", "YouTube isn't installed.", "Couldn't open X".
- Hold-OK hints in use: All apps `panelHint` ("Hold OK to add an app to Kids ⭐", then "Hold OK for options: home row or Kids ⭐"), the dock's `ARRANGE_HINT` while arranging, Kids parent menu line. The home dock has no hint before arranging.

Added by the whole-app audit (2026-11-05):
- Adhan Settings row (`SettingsActivity.row` ~`:78`): rest none; focused `0x26FFFFFF` fill + 3 px `0xFFFFC24A` edge, radius 18, **no scale**; press none (OK cycles the value, or "▶" acts silently); Baloo 32/30; background flat `0xFF0B0D14`. It shows in home All apps.
- Kids action row (`actionRow` ~`:1712`): focused background `0x26FFFFFF` only. Kids `toastIn(b, msg)` (~`:1727`) swaps a button's text to "msg ✓" for 1.2 s, which is the family's only in-place confirmation pattern and the model for answering presses.
- Layer stack (home): pill 0 < panels (`overlay()`) 30 < player 35 < reciters 40 < splash 60. Panels with equal elevation stack by build order: pin (`:238`) < apps/calendar < quran < parent (`:241`).

Added by the PR #22 review (2026-11-05):
- PIN key, resting (home + Kids): digit keys at alpha 0.4 (200 ms), no shake; the countdown is the only words.
- Kids PIN key press: dip 0.96 for 70 ms, then back to 1.06 or 1.0 over 140 ms.
- Home Parent learn rows while RemoteKeys is off: title `DIM`, value "Run Update TV once first"; OK gives "Run Update TV on the PC first" + `nudge()` (320 ms, ±10 px).
- Pill: elevation 50, above every panel; bottom centre (0, 64) over panels, top left (80, 56) on home; 2 s + 330 ms per word, 3.5 to 7 s.
- Kids value row: OK steps forward and wraps; the end arrow dims at the first and last value.
- Reciter list: "Loading reciters · جارٍ التحميل…" (26 px SOFT) after 300 ms, then filled in place without a new fade.

Added by the PR #23 review (2026-11-06):
- Press dip, both apps: `pressDip(e, getCurrentFocus())` runs from `dispatchKeyEvent` (home `MainActivity.java:495`, after the player, ambient and arrange handlers; Kids `KidsActivity.java:267`, after arrange). Only clickable views, OK/Enter/NumPad Enter, DOWN, no repeats: scale x0.96 over 70 ms, then back over 140 ms (EASE at home, the default curve in Kids). Views tagged `"dip"` (PIN keys) keep their own 0.94 dip. Not clickable, so no dip: home calendar rows, Kids value rows (their answer is the value change).
- Calendar row: now a "reading guide": `glass(20, 0x14FFFFFF, 0)` only, with no edge, no scale and no press.
- App menu (hold OK in All apps): `panelCard` (PanelBg) 640 wide, 40/30 padding, scrim `0x8C000000`, 200 ms fade in with EASE, 180 ms EXIT fade out, focus returned before the fade.
- `menuButton`: `focusGlass(22)` + 1.03 (180 ms EASE), like `parentRow`.
- Kids `focusRow(v, has, scale, r)`: `solid(0x26FFFFFF, r, 0xE6)` (3 px white edge at 90%) + scale. Used by parent value rows and action rows (1.03, r 22) and app cells (1.05, r 30). Kids `button()` now inverts **and** scales 1.06.
- Kids PIN key press: dip 0.94 (was 0.96). Kids wrong-PIN shake: the home's damped 420 ms ValueAnimator.
- Adhan Settings row: focused `0x26FFFFFF` + 3 px `0xE6F1D58A` + 1.03 (180 ms). The test row answers "Playing ✓" for 1.2 s. No press dip (OK rebuilds the list, so the new row grows in again).
- Adhan reminder card: `0xE6080B14`, 2 px `0x33FFFFFF` edge, gold hairline 140 px, Inter 34 title white, Plex Arabic 30 gold, time 26 Inter tnum, 8 s. The adhan GOLD is now `0xFFF1D58A` (one gold). The home reminder is still a one-line pill with no time, ~5 s by word count.
- Quran: the reciter button always says "Choose reciter". The saved choice shows only inside the reciter list.
- Home parent list: 30 px inner side padding and -30 scroller margins, so a 1.03 row keeps its edge. (Kids parent list has not had this yet.)

Added by the PR #25 review (2026-11-06):
- Home `parentRow` inner text width is **728 px** (900 card − 112 padding − 60 row padding; the ±30 list padding and scroller margins cancel). Label 28 px Inter Medium in weight 1, value 26 px Semibold with **no gap** between them. A label + value over 728 px wraps the label inside the fixed 96 px row.
- New rows (PR #25): home "Hijri date …" (As calculated / +1 day / −1 day) and "Sources under verses and hadith" (Shown / Hidden); Adhan "Show sources …" (On / Off) under WIDGET CARD and a HIJRI DATE section; Kids "Hadith source" (Show / Hide) under "Name of Allah or hadith".
- Baloo (adhan and kids) has no ✓ (U+2713); it falls back to the system font wherever it's used (e.g. "Playing ✓", `toastIn`).

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/15-ui-components.md](archive/15-ui-components.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Read the long-press handler, not the hint copy: hints can promise an action the code no longer does.
- 2026-11-05: Each component sets its own focus look in a listener, so "consistent" must be checked by diffing the values (radius, stroke, scale, shadow), not by eye.
- 2026-11-05: Android gives a free pressed state on OK down only to clickable views (`View.onKeyDown`); a focusable-only view gets no press at all.
- 2026-11-05: A merged PR can add a state (the PIN rest) the inventory doesn't list yet. Re-check the inventory against `main` every review.
- 2026-11-05: "Matches tv-material" must cover scale as well as colour.
- 2026-11-05: m3.material.io pages need JavaScript; read the generated token files in androidx (`StateTokens.kt`, `FilledButtonTokens.kt`) and the tv-material `*Defaults.kt` files with curl instead. Apple HIG pages read well from `developer.apple.com/tutorials/data/design/human-interface-guidelines/<page>.json`.
- 2026-11-05: Check elevations and build order whenever one panel opens another. Equal elevation means the later-built panel wins, whatever opened last.
- 2026-11-05: A message component has to sit above every layer it can be fired from. List the places that call `toast()` and check each one is visible.
- 2026-11-05: Audit the "secondary" screens too (the adhan Settings activity is reachable from All apps but wasn't in the inventory).

- 2026-11-05: Dimming a whole view also dims its focus drawable. When the view's background is the focus cue, dim the content (text colour), not the view.
- 2026-11-05: A component placed once (the pill) must be placed again when the layer under it changes. Check every open and close path while it's showing.
- 2026-11-05: Check crossfades between sibling panels both ways. `bringToFront` at alpha 1 is a cut, even if the layer below fades.

- 2026-11-06: A press effect that reads `getScaleX()` and goes back to it is only right once the focus animation has finished. Restore to the focus look's *target* scale, never to the current value.
- 2026-11-06: When one app gets a clipping fix (padding inside a scroller for a scaled row), check its sibling app's same component in the same PR.
- 2026-11-06: Taking a cue away ("not a button") must not take focus visibility away. A non-actionable focusable row still needs a visible, non-gold marker (fill + soft edge).
- 2026-11-06: A code comment that says "the same in both apps" is a claim to check against both apps' code.

- 2026-11-06: Measure every new row at its **longest value**, not its current one: a label can fit with "+1 day" and wrap with "As calculated", so the row changes shape on each OK.
- 2026-11-06: A setting that only works while another is on should sit directly under that one, in every app (Kids and Adhan did, home didn't).
