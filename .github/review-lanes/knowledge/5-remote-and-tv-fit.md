# Lane 5 knowledge: Remote and TV fit

## How to use this file
- Read this whole file before every review, then the brief (`../5-remote-and-tv-fit.md`). This file is what the lane has already learned; the brief is the checklist.
- Research online whatever the PR touches that this file doesn't cover yet (a new key code, a new window type, a new Android API, a new panel pattern). Prefer developer.android.com and AOSP source (raw.githubusercontent.com/aosp-mirror/...) over blogs.
- After every review, send the PR thread the additions for this file so they get committed with the PR: append a dated, short entry to **Past findings**, **Lessons learned**, **Source library** and **Open questions**. Move anything you verified out of Open questions into Core expertise or "This box and this code", with its source.
- Mark anything you could not check as **unverified**. Say which Android branch a source-code fact comes from (the box runs Android TV 14, so `android14-release` wins over `main`).
- Keep the file under about 400 lines. When it grows past that, condense the oldest Past findings into one line per PR and merge duplicate lessons.

## Core expertise

### 1. What Google requires of a TV app (the rules reviewers quote)
- Every TV remote has a D-pad, Select, Home and Back. The app must work fully with only up, down, left, right, Select, Back and Home. Don't depend on Menu, Search or Start buttons, and don't use touch words ("Tap here"). [nav], [ctrl]
- Every visible control must be reachable with the D-pad. Movement must be predictable. Only override the default focus order when it doesn't work. Avoid controls in hard-to-reach places or overlapping other controls. Categories go vertical, items in a category horizontal. [nav]
- Something must have focus at all times, including right after launch and when idle, and focus must be clearly visible (colour, size, animation), with one focus style across the app and enough padding for the highlight. [nav]
- `nextFocus*` chains should loop so the last control returns to the first (when you set them at all). [nav]
- Back: returns to the previous destination; never a toggle (don't use Back to both open and close a menu); never gated by a confirmation; never an infinite loop; repeated Back must eventually reach the TV home screen; no on-screen Back button; show Cancel when the only actions are confirm or destructive; no splash screen in the back stack. [nav], [ctrl]
- Quality checklist IDs worth quoting (TV app quality, Tier 3 "TV Ready"): TV-DP (five-way D-pad navigable), TV-DM (no Menu-button dependence), TV-DB (Back leads back to the Android TV home screen), TV-OV (no text or function cut off by the screen edge), TV-TR (fills the screen, opaque background, doesn't partly cover other apps), TV-LB (320x180 banner plus icon at least 160x160 at xhdpi), TV-BN (banner contains the app name), TV-ML (`ACTION_MAIN` + `LEANBACK_LAUNCHER`), TV-MT (touchscreen not required), TV-PC (during playback OK pauses/resumes, Left/Right rewind/fast-forward), TV-PP (play/pause key toggles playback), TV-NP (audio that keeps playing after leaving the app needs system media controls). Tier 1 TV-TO is about pointer remotes, not D-pad. [tvq]
- Selection keys: `DPAD_CENTER`, `ENTER`, `NUMPAD_ENTER`, `BUTTON_A`, `BUTTON_SELECT`. Back keys: `BACK`, `BUTTON_B`. In a media context the selection keys act as play/pause, Left/Right as rewind/fast-forward. Don't override `MEDIA_PLAY`/`MEDIA_PAUSE` when a `MediaSession` exists. [ctrl]
- Declare `android.hardware.touchscreen` (and any other unsupported feature: faketouch, telephony, camera, nfc, gps, microphone, sensor, portrait) as `required="false"`, or the app won't install or appear on TV. `RECORD_AUDIO`/`CAMERA`/location permissions imply hardware features. Check TV at runtime with `FEATURE_LEANBACK`. `configChanges="keyboard|keyboardHidden|navigation"` stops a restart when a controller connects or disconnects. [hw], [ctrl]

### 2. How a key press travels (AOSP, verified in `android14-release` unless noted)
1. **Queueing (PhoneWindowManager `interceptKeyBeforeQueueing`)** decides whether the key is passed to the user at all. On Android 14 it drops `KEYCODE_VIDEO_APP_1..8`, `FEATURED_APP_1..4` and `DEMO_APP_1..4` here ("Just drop if keys are not intercepted for direct key"). [pwm14]
2. **Accessibility key filter.** `AccessibilityInputFilter.onInputEvent` skips any event without `FLAG_PASS_TO_USER`, so keys dropped at step 1 never reach `onKeyEvent`. A remote button that Android maps to `VIDEO_APP_*`/`FEATURED_APP_*` can't be learned or remapped by `RemoteKeys`. [aif14]
   - Volume keys may be delayed here so the system's own volume handling settles first (`KeyboardInterceptor.getEventDelay`). [kbi]
   - Each key is held until every filtering service answers. If a service doesn't answer within **500 ms** (`ON_KEY_EVENT_TIMEOUT_MILLIS`), the event is passed on as if not handled (`removeReferenceToPendingEventLocked` re-sends it with `FLAG_PASS_TO_USER`). A slow `onKeyEvent` therefore makes every key on the box lag, then leaks the key anyway. [ked]
   - The service gets copies. Changing them does nothing. Return true to consume. The down/up stream that reaches apps must stay well-formed: consume a DOWN, consume its UP. [a11y]
3. **Dispatching (`interceptKeyBeforeDispatching`)** handles HOME, `ALL_APPS`, `NOTIFICATION`, `SETTINGS`, `SEARCH` and friends before any app sees them. On Android 14 `KEYCODE_ALL_APPS` opens All apps **on the UP** (`if (!down)`); on `main` it moved to the first DOWN. `SETTINGS` opens the settings or the notification panel depending on config. So a service that swallows a DOWN but lets the UP through still triggers the system action on 14. [pwm14], [pwm-main]
   - `GlobalKeyManager` also runs in `interceptKeyBeforeDispatching` (pwm14 `:3281`), after the accessibility filter, and sends vendor "global" buttons to their receiver as `ACTION_GLOBAL_BUTTON`. So global vendor buttons still reach `onKeyEvent` and can be learned. [gkm14]
4. **The app window.** `Activity.dispatchKeyEvent` → focused view `onKeyDown`/`onKeyUp`. Only if nothing consumed it, and only on **ACTION_DOWN**, `ViewRootImpl.performFocusNavigation` runs: it calls `focused.focusSearch(direction)` and `requestFocus`; if nothing moves, `dispatchUnhandledMove` is the last chance; if nothing has focus at all, it calls `restoreDefaultFocus()` (honours `focusedByDefault`). A held arrow plays the "fast scroll" sound for `getRepeatCount() > 0`. [vri14]
   - Consequence: if your own code consumes an arrow DOWN, the framework never moves focus for it. Consuming only the UP does nothing for focus.

### 3. Key codes (AOSP `KeyEvent.java`, `main`)
`HOME 3`, `BACK 4`, `DPAD_UP 19`..`DPAD_RIGHT 22`, `DPAD_CENTER 23`, `ENTER 66`, `MENU 82`, `NOTIFICATION 83`, `SEARCH 84`, `MEDIA_PLAY_PAUSE 85`, `BUTTON_A 96`, `NUMPAD_ENTER 160`, `INFO 165`, `CHANNEL_UP 166`, `TV 170`, `GUIDE 172`, `DVR 173`, `BOOKMARK 174`, `CAPTIONS 175`, `SETTINGS 176`, `TV_POWER 177`, `TV_INPUT 178`, `PROG_RED 183`, `ASSIST 219`, `LAST_CHANNEL 229`, `VOICE_ASSIST 231`, `ALL_APPS 284`, `PROFILE_SWITCH 288`, `VIDEO_APP_1..8 289..296`, `FEATURED_APP_1..4 297..300`, `DEMO_APP_1..4 301..304`, `RECENT_APPS 312`, `SCREENSHOT 318` (Android 15+). Confirm keys for `View` clicks (`KeyEvent.isConfirmKey`): `DPAD_CENTER`, `ENTER`, `SPACE`, `NUMPAD_ENTER` (not `BUTTON_A`). [ke]
- Warning: an AI summary of the developer.android.com KeyEvent page gave several of these values wrongly from memory. Always read the numbers from the AOSP source.
- Vendor buttons: a community post reports a Xiaomi PatchWall button sending "0x0244 (580)", which is above any AOSP key code, so it is probably a scan code or a vendor code (**unverified**, other Xiaomi model). [rd-patchwall]

### 4. Long press, repeats and clicks (AOSP `View.java`, `ViewConfiguration.java`, `KeyEvent.java`, `main`)
- `DEFAULT_LONG_PRESS_TIMEOUT = 400` ms. On Android 14 only `long_press_timeout` can be changed (Secure settings). The repeat timeout equals the long-press timeout, and the repeat delay is fixed at **50 ms**. [vc14] The `KEY_REPEAT_*` Secure settings exist only on `main`. [vc]
- `View.onKeyDown` for a confirm key with repeat count 0: if the view is clickable or long-clickable it sets pressed and schedules the long-click check after the long-press timeout. `View.onKeyUp`: performs the click only if the view is clickable, pressed, the long press has **not** been performed, and the event isn't cancelled. [view]
- `mHasPerformedLongPress` is the return value of the long-click listener, so a `setOnLongClickListener` that returns **false** lets the normal click fire on release too. All long-click listeners must return true. [view]
- Flags: `FLAG_LONG_PRESS` (0x80) is on the first repeat after the timeout (`isLongPress()`); `FLAG_CANCELED` (`isCanceled()`), `FLAG_CANCELED_LONG_PRESS` (0x100) when a long-press action ran, `FLAG_TRACKING` (0x200) on an UP still tracked from `startTracking()`. `onKeyLongPress` only fires for tracked keys. [ke]
- If `Activity.dispatchKeyEvent` consumes the OK DOWN, the view never starts its long-press timer, so long-press silently stops working for that view.
- Disabled views consume confirm keys (`onKeyDown`/`onKeyUp` return true when DISABLED), so OK on a disabled button does nothing and nothing else gets it. [view]

### 5. Focus internals (AOSP `ViewGroup.java`, `View.java`, `FocusFinder.java`, `main`)
- `ViewGroup.addFocusables` only collects children whose visibility is VISIBLE. GONE and INVISIBLE subtrees are skipped; **alpha 0 and off-screen (translated) views are not**. `FOCUS_BLOCK_DESCENDANTS` stops the whole subtree. [vg]
- `canTakeFocus()`: VISIBLE, FOCUSABLE, ENABLED, and (for targetSdk ≥ 28) non-zero size once laid out. `requestFocus` also fails if an ancestor blocks descendants. If layout isn't valid yet, the view is marked "wants focus" and gets it after layout, so `requestFocus` on a just-added view usually works, but on a zero-size view it doesn't. [view]
- When a focused view (or its parent) becomes GONE or INVISIBLE, `clearFocus()` runs and the root re-focuses its **first** focusable view (`rootViewRequestFocus`). That is why focus "jumps to the top-left" after a panel hides: restore it yourself. [view]
- `nextFocusUp/Down/Left/Right`: `FocusFinder.findNextUserSpecifiedFocus` uses the target only if it is focusable, VISIBLE (its own flag), and not blocked by touch mode. Otherwise it follows the target's own `nextFocus` chain, detecting loops, and if nothing qualifies falls back to geometric search. [ff]
- Geometric search: candidates in the direction, "beam" overlap wins first, then weighted distance `13 × major² + minor²`. A slightly misaligned item straight ahead beats a closer one diagonally. Short last rows in grids send Down to the nearest item by this rule, or nowhere. [ff]
- `isKeyboardNavigationCluster` and `focusedByDefault` exist (API 26+). `restoreDefaultFocus` is what the framework uses when nothing is focused. [ff], [vri14]
- Defaults worth remembering:
  - Every View starts as `FOCUSABLE_AUTO`: it becomes focusable as soon as it is clickable (`setOnClickListener` sets clickable), and stops being focusable if clickable is turned off. So a decorative view with a click listener is a focus stop. [view]
  - A new ViewGroup has `FOCUS_BEFORE_DESCENDANTS`, `clipChildren=true` and `clipToPadding=true`. A focus scale-up is clipped unless every ancestor up to one with room has `clipChildren(false)`. [vg]
- ScrollView (Android 14): on an arrow DOWN it calls `arrowScroll`, which moves focus to the next focusable inside it or scrolls by a step if there is none in reach. When a child takes focus, `requestChildFocus` scrolls it into view (if `revealOnFocusHint`, default true), deferred until layout if needed. If the content can't scroll, a focused ScrollView passes focus to its first child. [sv14]
- Typical focus bugs and their cause:
  - "Focus vanishes after closing a panel": the focused view went GONE; Android picked the first focusable in the window.
  - "Down from the dock reaches a hidden panel": the panel was alpha 0, not GONE.
  - "OK opens and also does the long-press action": the long-click listener returned false.
  - "Arrow moves two places": both custom code and the framework handled the DOWN.
  - "A tile in a scaled row looks cut": a parent still clips children or padding.

### 6. Back and Home for a home-screen app
- A HOME app is the root, so Back at the root consuming the key is correct (the TV home is the app itself). TV-DB's "Back leads to the home screen" is met by Back never leaving the launcher. [tvq]
- Back is a layer-closer, never a toggle: one press closes one layer, top-most first. [ctrl]
- targetSdk is 35 and no `enableOnBackInvokedCallback` is declared, so `KEYCODE_BACK` still reaches `dispatchKeyEvent` (legacy back). [nav] At targetSdk 36 on Android 16, `onBackPressed` and `KEYCODE_BACK` stop arriving unless `android:enableOnBackInvokedCallback="false"` is set. [b16]
- Google TV keeps its own launcher (`com.google.android.apps.tv.launcherx`) as the resolved HOME on many sets even after you pick another. Owners use `cmd package set-home-activity`, and a small accessibility service that catches Home. On one Xiaomi set the author tested, the Home button sent **`KEYCODE_ALL_APPS`**, not `KEYCODE_HOME`, so the service had to catch that too; on a Hisense nothing worked. That is on some Xiaomi remotes, **not verified on this box**, and it conflicts with our code: `RemoteKeys.code()` (`:41-43`) assumes the Apps button sends `KEYCODE_ALL_APPS` (the `rk_apps` default), and `isEveryday()` passes `KEYCODE_HOME` through. `input keyevent KEYCODE_HOME` can behave differently from the real button. [mustafa]
- A March 2026 Google TV update reportedly pushed users of replacement launchers back to the stock home. This comes from user reports, not Google. [gh-block]
- Workarounds seen in the wild: Apps Only mode; disabling `launcherx` over ADB (risky: if the replacement doesn't own Home, Home goes nowhere). [ark], [mustafa]

### 7. Accessibility services as key remappers
- `flagRequestFilterKeyEvents` plus `canRequestFilterKeyEvents="true"` gives `onKeyEvent`. Events arrive before the policy (beyond queueing), the IME and apps. [a11y]
- Android 14 binds accessibility services with `BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS` (`AccessibilityServiceConnection`), so `startActivity` from the service is allowed even when the app has no window showing. The general background-start rules list no accessibility exemption by name, so this comes from the source code. [asc14], [bal]
- A blocked background start doesn't throw. It logs "Background activity launch blocked!" under `ActivityTaskManager`. Check logcat when a remote button seems dead. [bal]
- Button Mapper (the commercial remapper) can't disable the YouTube button on Chromecast with Google TV and some media actions don't work on Google TV, which shows vendor buttons are often handled outside the normal path. [ac-bm]

### 8. HDMI-CEC (the TV's own remote driving the box)
- CEC user-control codes are turned into Android keys: SELECT→`DPAD_CENTER`, UP/DOWN/LEFT/RIGHT→D-pad, EXIT→`BACK`, ROOT_MENU→`HOME`, SETUP_MENU→`SETTINGS`, INPUT_SELECT→`TV_INPUT`, DISPLAY_INFORMATION→`INFO`, PLAY→`MEDIA_PLAY`, PAUSE→`MEDIA_PAUSE`, EPG→`GUIDE`, F2 red→`PROG_RED`. Diagonal CEC keys are unsupported. [cec]
- The table is many-to-one and conversion takes the **first match** (android14 `:309-316`), so PAUSE arrives as `MEDIA_PAUSE` and PLAY as `MEDIA_PLAY`, not `MEDIA_PLAY_PAUSE`, and ROOT_MENU maps only to `HOME`. [cec]
- CEC keys are **injected** with source `SOURCE_HDMI`. If the TV doesn't send "released", the box sends the UP itself after **550 ms** (`FOLLOWER_SAFETY_TIMEOUT`). Long-press over CEC depends on the TV repeating "pressed", so "hold OK" may not work from a TV remote (**unverified** on this family's TV). [cecdev]

### 9. Toasts on TV
- Apps targeting Android 12+ get text Toasts capped at **two lines** with the app icon. [toast-guide], [toast-src]
- `LENGTH_LONG` = 3.5 s, `LENGTH_SHORT` = 2 s. Max **5** queued Toasts per package. Background Toasts are rate-limited. `setGravity`/`setMargin` are ignored for text Toasts (they only log an error), so a Toast can't be moved into the safe area. Custom-view Toasts are deprecated since API 30 and blocked from the background. [toast-src], [nms]
- So nothing the family must read is a Toast. Use an on-stage message (scaled by Screen size) instead.

### 10. Overscan and 10-foot layout
- Design canvas 960x540 dp (1 dp = 1 px at mdpi). Overscan-safe margins **48 dp left/right, 27 dp top/bottom** (5%). The page also mentions 58/28 dp for a "5% margin layout" and rounds 27 to 24 dp. 12-column grid, 52 dp columns, 20 dp gutters. Backgrounds may bleed to the edge. [layouts]
- On the app's 1920x1080 stage that is 96 px sides, 54 px top/bottom (or 116/56 px for the 58/28 variant). Focus scale-ups need room: leave padding for the focused size so it doesn't overlap or clip. [layouts]
- TV-OV: no text or function partly cut off by the screen edges. [tvq]
- Text scale: TV users can change font scale (Android 12+). Use flexible sizes and check layouts at larger scales; test with `adb shell settings put system font_scale 1.2`. This app sizes text in px on a fixed stage, so font scale doesn't reach it (by design); say so when a PR adds `sp` text. [a11y-tv]

### 11. How to check things on the real box (for lane 8's checklist)
These are standard ADB commands; the first four are from [mustafa], the others are AOSP shell tools (**unverified** on this box's firmware).
- Which app is HOME: `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME`.
- Make this app HOME: `adb shell cmd package set-home-activity --user 0 com.family.tvhome/.MainActivity`.
- Enable the accessibility service without wiping others: append to `settings get secure enabled_accessibility_services` with a colon, then `settings put secure accessibility_enabled 1`.
- Which code a button sends: `adb logcat | grep -i keycode` while pressing it (or the app's own learn row). `input keyevent KEYCODE_HOME` is not the same as the real Home button.
- Raw events: `adb shell getevent -l` (scan codes, before key layout mapping).
- What the filter sees: `adb shell dumpsys accessibility` (enabled services, filter flags).
- Long-press timeout in use: `adb shell settings get secure long_press_timeout` (empty means the 400 ms default).
- Background start blocked: `adb logcat | grep "Background activity launch blocked"`. [bal]

### Added by the whole-app audit (2026-11-05)
- Focus search from a view goes up to the window root unless a parent overrides `focusSearch` (android14 `ViewGroup.java:1074-1084`). So a panel that doesn't handle arrows lets focus leave it for any VISIBLE focusable on screen, including alpha-0 views. The usual guards are `FOCUS_BLOCK_DESCENDANTS` on the layers below (see Kids `syncFocus()`), or overriding `focusSearch` on the panel to search only inside itself. [vg14], [ff14]
- `FocusFinder.isCandidate` (android14 `:549-566`): Down needs `(src.top < dst.top || src.bottom <= dst.top) && src.bottom < dst.bottom`. Overlapping rects still count, so a hidden row just below a panel's last row qualifies even when it overlaps. [ff14]
- `removeAllViews()` on a group that holds focus calls `rootViewRequestFocus()` (android14 `ViewGroup.java:5785-5835`), and so does a focused view going GONE (`View.java:17513-17519`). Both put focus on the window's first focusable view. Rebuilding a focused list causes the same flash as hiding a panel. [vg14], [view14]
- Drawing order with elevation: children are sorted by Z, and equal Z keeps add order (android14 `ViewGroup.buildOrderedChildList` `:4468-4475`). Two panels at the same elevation stack by build order, not by which opened last, unless `bringToFront()` is called. [vg14]
- Volume keys on Android TV never reach the foreground app: `interceptKeyBeforeDispatching` consumes `VOLUME_UP/DOWN/MUTE` when `mUseTvRouting` (platform type TELEVISION = has `FEATURE_LEANBACK`) or `config_handleVolumeKeysInWindowManager` (android14 `PhoneWindowManager.java:3138-3145`, `AudioSystem.java:2464-2473`). An app that "eats every key" doesn't break volume. [pwm14], [audiosys14]

Added by the PR #23 review (2026-11-06):
- 2026-11-06: How clipping works (android14). A child is clipped to its own bounds when its parent has clipChildren (`View.setDisplayListProperties` `:23483`, `renderNode.setClipToBounds`). That clip is applied before the child's scale, so it doesn't cut a scaled child. Two things do cut a scaled child:
  - (a) the parent's **clipToPadding**. It is on by default and only takes effect when the padding is non-zero (`ViewGroup.dispatchDraw` `:4277` clips to the padded area).
  - (b) the parent's own bounds clip, which comes from the **grandparent's** clipChildren.

  So room for a focus grow must be padding on a parent that has `setClipToPadding(false)`, or space inside a parent whose own parent has `clipChildren(false)`. Padding on a ScrollView does nothing for a grid inside it unless that ScrollView has `clipChildren(false)` (an outline clip on the ScrollView still clips the scrolled content). [vg14], [view14], [cliptopad]

- 2026-11-06: A ViewPropertyAnimator that starts on a property that is already animating takes that property away from the running animator. If the running animator has no properties left, it is cancelled (`:943-960`). A cancelled animator's `withEndAction` is dropped (`:1088-1093`). `setStartDelay` and the interpolator stay set on that view for later `animate()` calls. [vpa14]

## This box and this code
- **Box:** Xiaomi TV Box S (3rd Gen), Google TV, 2 GB RAM, 32 GB storage, quad-core up to 2.5 GHz, Mali-G310 V2 GPU, 4K output, one HDMI 2.1 port. The spec page doesn't describe the remote buttons. [mi]
- **SDK:** all three modules `compileSdk 35`, `targetSdk 35`; `minSdk 28` (app, adhan), `33` (kids). No `enableOnBackInvokedCallback`. (`*/build.gradle.kts`)
- **Manifest:** `MainActivity` has HOME+DEFAULT and LAUNCHER+LEANBACK_LAUNCHER filters, `singleTask`, landscape, `configChanges` including keyboard/navigation; `android:banner` set; leanback and touchscreen `required="false"` (`app/src/main/AndroidManifest.xml:15-43`).
- **Key dispatch order** (`MainActivity.dispatchKeyEvent`, `:444-468`): splash → stress → player (`playerKey`, `:576-598`) → ambient → arrange (`arrangeKey`, `:1266`) → Back (acts on UP, always consumed) → secret Up-Up-Down-Down (`secretKey`, `:3020-3038`, sequence at `:3011`, DOWN with repeat 0, only when `anyPanelOpen()` is false, 2.5 s window) → PIN digits and DEL.
- **Back chain** (`:453-460`): app menu → reciter list → player → `closePanels()`. `anyPanelOpen()` (`:3015`) lists pin, apps, quran, parent, calendar and player panels.
- **onNewIntent** (`:300-320`) closes panels, ends arrange, re-focuses `lastDock`, then opens `panel=` extras (apps, quran, pin, calendar, stress, reciters, player).
- **Long-press listeners** return true: dock tile (`:1230`), All apps item (`:2345`, not for Settings/Calendar).
- **Player** (`playerKey`): consumes arrows on DOWN and UP; first press with faded controls only shows them (`swallowUp`); OK with controls visible falls through to the normal click. It doesn't handle `MEDIA_PLAY_PAUSE` (TV-PP), nor `MEDIA_PLAY`/`MEDIA_PAUSE`, which the TV's own remote sends over CEC; the Kids `PlayerActivity` handles media keys (`kids/.../PlayerActivity.java:159-170`).
- **RemoteKeys** (`app/.../RemoteKeys.java`): learnable `rk_prime` (Stremio `com.stremio.one`), `rk_yt` (PIN pad via `MainActivity` `panel=pin`, `NEW_TASK|SINGLE_TOP`), `rk_apps` (Kodi `org.xbmc.kodi`, default `KEYCODE_ALL_APPS`). Learning stores the next non-everyday DOWN. `isEveryday()` (`:105-117`) passes arrows, center, ENTER, BACK, HOME, volume, power, sleep, wakeup. Note: `NUMPAD_ENTER`, `MENU`, `SETTINGS`, `MEDIA_*` are **not** in it, so they can be learned (and then eaten everywhere). `run()` returns silently when the target app is missing (`:100`). `:67` also eats repeats of a swallowed key (correct).
- `remote_keys.xml` subscribes to `typeWindowStateChanged` with `notificationTimeout 1000`, but `onAccessibilityEvent` is empty.
- Because Android 14 opens All apps on the **UP** of `KEYCODE_ALL_APPS`, the `swallowUp` logic is what stops the system All apps screen from opening over Kodi. Keep it.
- **Overscan:** `FITS = {100,97,94,91,88}` (`MainActivity.java:760`, `KidsActivity.java:220`); `fitStage()` (`:763-770`) scales the 1920x1080 stage; the default is 100 (`:766`, `:3110`, Kids `:225`, `:1606`).
- **Adhan overlay** (`adhan/.../AdhanService.java`): `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE` (`:171-178`), offset 56/44 px from the corner (`:254-255`, card `:738-739`), not scaled by Screen size.
- `hideBars()` uses deprecated `setSystemUiVisibility` (`MainActivity.java:440-442`, `KidsActivity.java:251`).
- Toasts: none in Java code since PR #20 (`grep Toast` finds none).

Added by the whole-app audit (2026-11-05):
- TV Home panels (`overlay()`) are all elevation 30 (player 35, reciter 40, splash 60). `show()` doesn't call `bringToFront()` and doesn't block the home row, which stays focusable at alpha 0. Kids Corner's `show()` does both (`syncFocus()`).
- Parent settings focus indexes after a toggle are hard-coded (`drawParent(n)`), and they drifted by one when a row was added at the top.

Added by the PR #22 review (2026-11-05):
- Known risks updated by PR #22: the home player media-key risk is closed. The adhan words now sit at least 56 px above the bottom (BAND 136, LIFT 56); the corner card is still at 56/44 px.

Added by the PR #23 review (2026-11-06):
- 2026-11-06 (PR #23):
  - `pressDip` (`MainActivity` ~`:3421`, Kids ~`:1677`) dips any clickable focused view on OK DOWN (repeat 0). It never consumes the key, so long-press still works. Views tagged "dip" (the PIN keys) are skipped.
  - All apps reuses its tiles when `gridKey` matches, and focuses tile 0.
  - `closeAppMenu` refocuses the tile first, then blocks and fades the menu for 180 ms.
  - Kids `refreshTiles(true)` keeps the row on resume.
  - Calendar rows are focusable but not clickable (a reading guide).
  - The adhan Settings rows now scale 1.03.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- 2026-11-06, PR #23 (update 2, visual polish): no Must.
  - Should: the new focus room is undone by clipToPadding or clipChildren in Parent settings, the hold-OK menu, the reciter list, the All apps and surah glow, Adhan settings, and Kids settings and the app picker. `pressDip` springs back to a scale read mid-animation, so a dock tile can stay at about 1.1 instead of 1.2.
  - Nice: the adhan test row renders twice (focus flash and a double bounce); the Kids dip has no `startDelay(0)`; the adhan reminder sits 60/50 px from the corner (known); `pressDip` dips disabled views.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (audit update 1): no Must or Should. TrapFrame plus focusLayer stop focus leaking out of panels. Back goes back one layer per press. MEDIA_PLAY, PAUSE and PLAY_PAUSE are handled on the Quran page and in the player, which closes that known risk. A missing Stremio or Kodi shows a message. The adhan words are lifted 56 px. Nice: closing the reciters focused a hidden button (fixed, it falls back to the surah cell); the tileFor fallback; "Show first" refocuses the first app; media keys on the home with soft Quran playing; the adhan's 80 px box.

- **2026-11-05, PR #20 (merged):** no Must fix. Should fix: kids-lock warning was a system Toast (fixed: moved into the Parent menu); kids PIN dots stayed red during the wait (fixed). Nice to have: parent-settings set-PIN subtitle mentioned only YouTube (fixed). Rule added to the brief: nothing the family must read is a Toast. Research notes: the adhan overlay ignores Screen size and sits about 2.9%/4.1% from the corner; a Prime or Apps press does nothing if Stremio/Kodi isn't installed; Back and focus handling sound.
- **2026-11-05, PR #21 (docs):** no Must fix. Should: §8 CEC PAUSE/PLAY arrive as `MEDIA_PAUSE`/`MEDIA_PLAY`; the key-repeat settings claim was main-only. Nice: predictive back answered, GlobalKeyManager, SCREENSHOT is 15+, secretKey line. All other file:line refs and android14 claims verified.
- **2026-11-05, knowledge build (no PR):** from AOSP source: `VIDEO_APP_*`/`FEATURED_APP_*` keys never reach accessibility services on Android 14; `ALL_APPS` fires on UP on 14; 500 ms accessibility key timeout; Android 14 lets accessibility services start activities from the background; long-click listeners must return true; the Quran player ignores `MEDIA_PLAY_PAUSE`.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- **2026-11-05, whole-app audit (`d772bcc`, no PR):** Must: panels don't trap focus, so it escapes to the hidden dock or layers behind (PIN pad, Parent settings, calendar, All apps last row, reciter list, hold-OK menu); Stop on the Quran page drops focus onto the hidden dock; Parent settings toggles move focus up one row; the PIN pad opens behind Parent settings; Back from the calendar skips All apps. Should: focus flash and `lastDock` reset on close or rebuild; the pill is hidden under panels; wrong "Hold OK on YouTube" hint; Back on stats closes all settings; home player media keys; Kids player OK repeat and Play toggling; adhan banner text in the bottom 5%; home top at y 34; silent Prime/Apps when the app is missing. Nice: label clamp 60, pill x 80, adhan overlay margins, alpha-only Quran page under the player, arrange vs ambient, adhan Settings `configChanges`, unused a11y events.


## Lessons learned
- 2026-11-05: Read key-code numbers and framework behaviour from AOSP source at the right branch, not from summaries. A summary of the KeyEvent page invented several values.
- 2026-11-05: "Consume DOWN and UP together" isn't only hygiene here: on Android 14 the system acts on the UP of some keys (`ALL_APPS`).
- 2026-11-05: Anything about the real remote (which code each button sends, whether Home is `HOME` or `ALL_APPS`) can only be settled on the box. Ask for it in lane 8's checklist instead of guessing.
- 2026-11-05: AOSP many-to-one tables (`HdmiCecKeycode`) convert by first match, so read the order.
- 2026-11-05: Check the branch for every constant: `ViewConfiguration` and `KeyEvent` differ between 14 and `main`.
- 2026-11-05: developer.android.com pages often hold their numbers in images or sub-pages. If a fetch returns no number, say so; don't fill it in.
- 2026-11-05: For every panel, press every arrow at every edge in your head, and run the rects through `isCandidate` against everything still VISIBLE behind it. An "alpha 0" home row is a live target.
- 2026-11-05: When the same project already has the right pattern (Kids `syncFocus()` and `bringToFront()`), point to it as the fix; it is cheaper and safer than inventing one.
- 2026-11-05: Hard-coded focus indexes in rebuilt lists drift silently when a row is added. Look for them whenever a list gains a row.
- 2026-11-05: Check whether a "consumes everything" key handler can block volume before flagging it: on TV it can't (see §2).

- 2026-11-05: On Android 14, setDescendantFocusability(BLOCK) doesn't clear focus that is already inside. Block first, then requestFocus the target straight away.
- 2026-11-05: A focusSearch override that returns the focused view makes ViewRootImpl treat the arrow as having nowhere to go.
- 2026-11-05: With no MediaSession, unhandled media keys go to the last media-button receiver.

- 2026-11-06: When a PR says "added padding so focus isn't clipped", check clipToPadding on that same group and clipChildren on its parent. Padding on its own usually does nothing.
- 2026-11-06: A press effect must return to the target value for the view's state (focused or resting), never to a value read part-way through an animation. TV Material defines `pressedScale` as a fixed state value.

## Open questions to research next
- Which key code does each button on this box's remote send (Home, Apps, Netflix, Prime Video, YouTube, Google TV/Live, the input/settings button)? Do any of them map to `VIDEO_APP_*`/`FEATURED_APP_*` and so can't be learned? (On the box: Parent settings → learn, or `adb shell getevent -l` / logcat.)
- Does this box's Home button send `KEYCODE_HOME` or `KEYCODE_ALL_APPS`? If `ALL_APPS`, the default `rk_apps` mapping would turn Home into "open Kodi". Highest-priority check: `adb logcat | grep -i keycode` while pressing Home and Apps (lane 8 has the same step).
- Is the Netflix button handled by Google's own button service before the filter (like the YouTube button on Chromecast with Google TV)?
- Answered 2026-11-05: at targetSdk 36 on Android 16, `KEYCODE_BACK` stops arriving unless `enableOnBackInvokedCallback="false"` is set (see §6). [b16]
- Does this box's firmware map the Netflix/YouTube/Prime buttons as global keys? (`adb shell dumpsys window`, look for GlobalKeyManager.) **Unverified.**
- Does the "Touch & hold delay" accessibility setting on Google TV change `long_press_timeout` for D-pad holds?
- CEC: does the family's TV send repeated "pressed" so hold-OK works from the TV remote?
- Is `KEYCODE_BUTTON_A` turned into a confirm key by the key-character-map fallback for game pads, or does it need handling?
- Does long-press OK reach the view when the TV's own remote sends SELECT over CEC (does the TV repeat "pressed")? This is still open, and now matters for dock arranging and All apps hold-OK.
- Does `View.requestFocus()` on a dock tile from `onNewIntent` (Home pressed while a panel is fading) ever race with the 180 ms fade? Watch on the box (checklist item 6).

- Should the home get a MediaSession so the TV remote's Pause works on the soft Quran? (That may count as a feature; ask first.)

- Is the elevation shadow (`goldLift` translationZ) of an outer-column tile clipped at the grid's bounds on this box's HWUI? This is inferred from the source; confirm it with lane 8's photo.

## Source library
Tags used above. All were opened on 2026-11-05 unless marked otherwise.
- [nav] https://developer.android.com/training/tv/start/navigation : D-pad, focus visibility, `nextFocus*` loops, Back rules, no splash in back stack, Live-tab rules.
- [ctrl] https://developer.android.com/training/tv/start/controllers : minimum controls, selection/back/media key tables, "Back is never a toggle", pointer remotes, `configChanges`.
- [tvq] https://developer.android.com/docs/quality-guidelines/tv-app-quality : all TV-xx criteria (TV-DP, TV-DM, TV-DB, TV-OV, TV-TR, TV-LB, TV-ML, TV-MT, TV-PC, TV-PP, TV-NP, TV-G6 16 KB pages from Aug 2026).
- [hw] https://developer.android.com/training/tv/start/hardware : unsupported features, implied features, `FEATURE_LEANBACK`, `leanback.supports_touch`.
- [layouts] https://developer.android.com/design/ui/tv/guides/styles/layouts : 960x540 dp canvas, 48/27 dp safe margins (also 58/28), 12-column grid.
- [toast-guide] https://developer.android.com/guide/topics/ui/notifiers/toasts : 2-line limit for target 31+.
- [bal] https://developer.android.com/guide/components/activities/background-starts : background activity start exemptions, Android 14 PendingIntent opt-ins, the "blocked" log line.
- [a11y-tv] https://developer.android.com/training/tv/accessibility : text scaling (`font_scale`), audio description; links to TalkBack pages (not yet read).
- [ke] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/KeyEvent.java : key-code values, flags, `isConfirmKey`.
- [view] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/View.java : `onKeyDown/Up` click and long-press, `canTakeFocus`, focus clearing on GONE/INVISIBLE, `findUserSetNextFocus`.
- [vg] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/ViewGroup.java : `addFocusables` visibility rule, `FOCUS_BLOCK_DESCENDANTS`.
- [ff] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/FocusFinder.java : user-specified focus rules, beam and weighted distance.
- [vc] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/ViewConfiguration.java : 400 ms long press, 50 ms repeat, `KEY_REPEAT_*` settings (main only).
- [vc14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewConfiguration.java : fixed 50 ms repeat, repeat timeout = long-press timeout. Checked 2026-11-05.
- [gkm14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/core/java/com/android/server/policy/GlobalKeyManager.java : global vendor buttons, `ACTION_GLOBAL_BUTTON`. Checked 2026-11-05.
- [b16] https://developer.android.com/about/versions/16/behavior-changes-16 : no `KEYCODE_BACK` at target 36, and the opt-out. Checked 2026-11-05.
- [mx] mundoxiaomi.com, Button Mapper article: remaps the Netflix button on Xiaomi Google TVs through accessibility. Weak source, other device. Checked 2026-11-05.
- [vri14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewRootImpl.java : `performFocusNavigation` on DOWN, `restoreDefaultFocus`.
- [sv14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/widget/ScrollView.java : `executeKeyEvent`, `arrowScroll`, `requestChildFocus` reveal.
- [toast-src] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/widget/Toast.java : 2-line note, durations, ignored gravity/margins for text toasts.
- [nms] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/services/core/java/com/android/server/notification/NotificationManagerService.java : `MAX_PACKAGE_TOASTS = 5`, background custom toast block.
- [pwm14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/core/java/com/android/server/policy/PhoneWindowManager.java : app keys dropped at queueing, `ALL_APPS` on UP.
- [pwm-main] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/services/core/java/com/android/server/policy/PhoneWindowManager.java : newer `ALL_APPS` on first DOWN, `SETTINGS` behaviour.
- [aif14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/accessibility/java/com/android/server/accessibility/AccessibilityInputFilter.java : skips events without `FLAG_PASS_TO_USER`.
- [kbi] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/services/accessibility/java/com/android/server/accessibility/KeyboardInterceptor.java : volume-key delay.
- [ked] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/services/accessibility/java/com/android/server/accessibility/KeyEventDispatcher.java : 500 ms per-key timeout, unhandled keys passed on.
- [a11y] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/accessibilityservice/AccessibilityService.java : `onKeyEvent` contract (also mirrored at learn.microsoft.com, see the brief).
- [asc14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/accessibility/java/com/android/server/accessibility/AccessibilityServiceConnection.java : `BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS`.
- [cec] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/core/java/com/android/server/hdmi/HdmiCecKeycode.java : CEC to Android key map.
- [cecdev] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/core/java/com/android/server/hdmi/HdmiCecLocalDevice.java : injected keys, 550 ms safety UP.
- [mi] https://www.mi.com/sg/product/xiaomi-tv-box-s-3rd-gen/specs : box hardware.
- [mustafa] https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/ : Google TV keeps `launcherx` as HOME; Xiaomi Home button sent `ALL_APPS`; ADB commands. Blog, tested on three TVs.
- [gh-block] https://android.gadgethacks.com/news/google-tv-update-blocks-third-party-launcher-workaround/ : March 2026 update reports. User reports only.
- [ark] https://til.arktronic.com/2025/04/05/google-tv-customization.html : Projectivy, disabling `launcherx`, Apps Only mode.
- [ac-bm] https://www.androidcentral.com/how-remap-youtube-and-netflix-buttons-chromecast-google-tv : Button Mapper limits on Google TV.
- [rd-patchwall] https://reddit.zackpollard.pro/r/AndroidTV/comments/1myx4k3/creative_ways_to_remap_the_patchwall_button_on : PatchWall button "580". Community post, weak.
- Earlier (opened by the first review, listed in the brief): learn.microsoft.com mirror of `AccessibilityService.onKeyEvent`; developer.android.com `AccessibilityServiceInfo`; `build-adaptive-apps-for-tv`.
- Not reachable on 2026-11-05: androidauthority.com (Projectivy block article) and hexnode forum (fetch budget ran out); retry next time.
- [ff14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/FocusFinder.java : `isCandidate`, `beamsOverlap`.
- [vg14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewGroup.java : `focusSearch` up to the root, `removeAllViewsInLayout` refocus, `buildOrderedChildList` Z order.
- [view14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/View.java : `setFlags` GONE → `clearFocus`, `rootViewRequestFocus`, refocus when not in touch mode.
- [audiosys14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/media/java/android/media/AudioSystem.java : `getPlatformType` (leanback → TELEVISION).
- [pwm14] (already listed) now also cited for volume routing `:3138-3145`.
- [focus-restore] https://developer.android.com/develop/ui/compose/touch-input/focus/focus-restoration : focus should return to the last-focused child on re-entry, and to the item acted on after Back (the `saveFocusedChild`/`restoreFocusedChild` pattern). This is the Compose wording of our "return focus to where the user was" rule.
- [adaptive-tv] https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv : "Ensure every UI element is reachable and has a clear focused state." It has no guidance on overlays or focus traps.
- Not useful: the `device/google/atv` overlay config URL returned 404 (moved). `config_handleVolumeKeysInWindowManager` defaults to false in core `config.xml:4612`; TV routing comes from `mUseTvRouting` instead.

- android14-release ViewGroup.java (lines 826-838, descendant focusability), ViewRootImpl.java performFocusNavigation, MediaSessionService.java dispatchMediaKeyEventLocked, and TextView.java onDraw clipRect. Read 2026-11-05.

- [cliptopad] https://learn.microsoft.com/dotnet/api/android.views.viewgroup.setcliptopadding : by default, children are clipped to the parent's padding, only when the padding is non-zero.
- [tvm-scale] https://composables.com/jetpack-compose/androidx.tv/tv-material/objects/ClickableSurfaceDefaults/api.md : `scale()` defaults are `scale` 1f, `focusedScale` 1.1f and `pressedScale = scale`. The developer.android.com page was too long to fetch, so this mirror was used.
- [vpa14] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewPropertyAnimator.java : how a new animation takes over a property and cancels the old one; the end action is dropped on cancel.
- [vg14] and [view14] (already listed) are now also cited for `dispatchDraw` `:4277`, `setClipChildren` `:4603` and `setDisplayListProperties` `:23483`.
