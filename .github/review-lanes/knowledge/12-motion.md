# Lane 12 knowledge: Motion and animation (easing, timing, choreography, interruption)

## How to use this file
- Read this whole file before every review, then read the brief `../12-motion.md`.
- Research online whatever the PR touches that this file does not cover. Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for: **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. If the file grows past about 400 lines, condense the oldest Past findings and Lessons into one-line summaries. Never delete a source that is still cited.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code or AOSP source, or seen it in a screen recording.
- Lane 3 owns frame cost (knowledge/3-smoothness.md); lane 4 keeps the overall look. Don't duplicate their notes; link to them.

## Core expertise

### 1. Material 3 easing and duration tokens
- Easing (cubic-bezier) [mdc-motion] [m3-motion-tokens]:
  | Token | Curve | Use |
  |---|---|---|
  | Standard | (0.2, 0, 0, 1) | utility motion that begins and ends on screen |
  | Standard decelerate | (0, 0, 0, 1) | utility motion entering |
  | Standard accelerate | (0.3, 0, 1, 1) | utility motion exiting |
  | Emphasized | path M0,0 C0.05,0 0.133,0.06 0.167,0.4 C0.208,0.82 0.25,1 1,1 | M3 hero motion on screen |
  | Emphasized decelerate | (0.05, 0.7, 0.1, 1) | hero entering |
  | Emphasized accelerate | (0.3, 0, 0.8, 0.15) | hero exiting |
  | Legacy (FastOutSlowIn) | (0.4, 0, 0.2, 1) | Material 2 default |
- Durations (ms) [m3-motion-tokens]: Short1-4 **50/100/150/200**; Medium1-4 **250/300/350/400**; Long1-4 **450/500/550/600**; ExtraLong1-4 **700/800/900/1000**.
- Springs (MDC, damping/stiffness): fast spatial 0.9/1400, fast effects 1/3800, default spatial 0.9/700, default effects 1/1600, slow spatial 0.9/300, slow effects 1/800. Pick speed by size or distance (small = fast, full screen = slow); "effects" springs (alpha, colour) are critically damped so they never overshoot. [mdc-motion]
- Rule of thumb from both: duration grows with the area or distance moved; exits are shorter than entrances.

### 2. Android interpolators, by the numbers (AOSP android14 source)
- `DecelerateInterpolator(f)`: y = 1 - (1 - t)^(2f); f = 1 is y = 1 - (1 - t)^2. `AccelerateDecelerateInterpolator`: y = cos((t + 1)π)/2 + 0.5. `AccelerateInterpolator(f)`: y = t^(2f). [aosp-decelerate] [aosp-acceldecel]
- Computed shape (fraction done at 10%, 25%, 50% of the time; time at which 90% and 99% are done):
  | Curve | f(.1) | f(.25) | f(.5) | t@90% | t@99% |
  |---|---|---|---|---|---|
  | Decelerate 1.0 | .19 | .44 | .75 | .68 | .90 |
  | **EASE = Decelerate 1.6** | .29 | .60 | .89 | .51 | .76 |
  | Kids EASE 1.8 | .32 | .65 | .92 | .47 | .72 |
  | Decelerate 2.0 | .34 | .68 | .94 | .44 | .68 |
  | Decelerate 2.2 | .37 | .72 | .95 | .41 | .65 |
  | AccelerateDecelerate (default) | .02 | .15 | .50 | .80 | .94 |
  | M3 standard | .16 | .61 | .88 | .54 | .84 |
  | M3 standard decelerate | .45 | .69 | .89 | .52 | .83 |
  | M3 emphasized decelerate | .62 | .83 | .95 | .36 | .74 |
  | M3 standard accelerate | .03 | .13 | .37 | .93 | .99 |
  | FastOutSlowIn | .03 | .24 | .78 | .63 | .87 |
- Reading the table: `EASE` behaves close to M3 Standard decelerate (both ~90% done at half time), so it is a sound house curve. AccelerateDecelerate has barely moved at 10% of its time: for focus or short fades it **feels late**. Perceived duration of a decelerate motion is about half its nominal duration, so a 1,500 ms `EASE` sink looks like ~750 ms of motion plus a long settle.
- `PathInterpolator(x1, y1, x2, y2)` (API 21) gives any M3 cubic-bezier exactly, with no dependency. [pathinterpolator-ref]
- `ValueAnimator` default: **300 ms** and AccelerateDecelerate. [valueanimator-ref]

### 3. `ViewPropertyAnimator` (VPA): rules that bite (AOSP android14 source)
- `View.animate()` returns **one VPA per view**, created once and reused forever. [aosp-view-animate]
- `setDuration`, `setStartDelay` and `setInterpolator` set flags (`mDurationSet`, `mStartDelaySet`, `mInterpolatorSet`) that are **never cleared**; the javadoc calls them settings for "ensuing property animations". So a delay or curve set once applies to **every later `animate()` on that view** until changed. [aosp-vpa]
- Starting a property cancels only that property in the running animation; the old animator is cancelled only when **all** its properties have been taken over. If the old animation moved alpha + translationY and the new one moves alpha only, the old keeps running translationY and its `withEndAction` still fires. [aosp-vpa]
- `withEndAction` runs only if the animation ends normally; on cancel it is dropped. [aosp-vpa]
- Each property animates from its value **at the time `animate()` is called**, so an interrupted move continues without a jump, but its velocity restarts from the curve's beginning (a decelerate curve restarts fast, which reads as continuous; AccelerateDecelerate restarts slow, which reads as a hiccup). [aosp-vpa] (reasoning for the second half)
- VPA runs one `ValueAnimator` per batch on the UI thread's Choreographer. [propanim] [choreographer-ref]

### 4. Duration scale, reduced motion and accessibility
- `ValueAnimator.getDurationScale()` (API 33) is the system scale; it affects **duration and start delay**; 0 makes animations end immediately; default 1.0. `registerDurationScaleChangeListener` (API 33) reports changes on the main thread. `areAnimatorsEnabled()` (API 26) is false when the scale is 0 or battery saver disables animations. [valueanimator-ref]
- Android has no separate "reduce motion" flag: "Remove animations" (Accessibility) writes 0 to the window, transition and animator scales (AOSP phone Settings source; TvSettings unverified). [aosp-disable-anim] Treat 0 as "the person asked for no motion" and make every end state correct without animation. (unverified wording of the TV settings menu)
- Window animation scale affects window and activity transitions (launching Netflix etc.), not our `ValueAnimator`s; the animator scale is what our code sees. (the split of the three Developer-options scales is general Android knowledge, not re-read this time)
- Apple: when **Reduce Motion** is on, reduce automatic and repetitive animation, including zooming, scaling and peripheral motion; tighten springs; avoid depth (z) animation; **replace x/y/z transitions with fades**; avoid animating into and out of blurs. [hig-accessibility]
- WCAG 2.3.3 (AAA): motion triggered by interaction can be disabled unless essential. "Motion animation" adds steps to create the illusion of movement; **changes of colour or opacity are not covered** (blur may be). Vestibular reactions include dizziness, nausea and headaches; parallax is the classic trigger. [wcag-233] Our wallpaper drift is a parallax-like effect: keep it slow and small.
- WCAG 2.2.2: moving content that starts automatically and lasts **more than 5 s**, shown alongside other content, needs a way to pause or hide it unless essential; auto-updating content (our verse every 30 s) needs a way to pause, hide or control frequency. The Parent menu's "Verse and hadith card" on/off is that control. [wcag-222]
- WCAG 2.3.1: no more than **3 flashes per second**; saturated-red flashes are stricter. [wcag-231]

### 5. Timing research and premium-motion principles
- NN/g: most UI animation **100 to 500 ms**; simple feedback ~100 ms; substantial screen changes 200 to 300 ms; up to 400 ms for big moves on large screens; at 500 ms animations become "a real drag". "It is far more common for animations to be too long than too short." The more often seen, the shorter. Entering a little longer than exiting (300 in, 200 to 250 out). Ease-out is the usual choice; ease-in can feel unresponsive. [nngroup-duration]
- NN/g: animation is for feedback, state change, spatial orientation and signifiers; gratuitous or simultaneous animations distract; "down-time animations" frustrate; motion used to create urgency is a dark pattern. [nngroup-purpose]
- Apple HIG: motion purposeful, brief and precise; avoid motion on frequent interactions; **let people cancel motion**, never make them wait; don't use motion as the only carrier of information. [hig-motion]
- Apple WWDC18 "Designing Fluid Interfaces": people are "really, really sensitive to latency"; interfaces must allow "constant redirection and interruption"; start with **100% damping (no overshoot)**, "a spring doesn't need to overshoot"; bounce only when the gesture has momentum (Music uses 80% damping only for a swipe dismiss). On a D-pad there is no momentum, so no bounce. [wwdc18-fluid]
- Android spring animation: velocity is continuous through interruptions, but the default damping is `DAMPING_RATIO_MEDIUM_BOUNCY` and stiffness `STIFFNESS_MEDIUM`: always set `DAMPING_RATIO_NO_BOUNCY` for this app. Needs the `androidx.dynamicanimation` dependency, which is a build change (ask lane 7). [spring-anim]
- Google Design: nav transitions about 300 ms, small components about 100 ms, tune in 25 ms steps, exits subtler than entrances, containers enter from 95% scale. [gd-motion-not-hard]
- Premium motion, distilled for this app: restraint (few things move, small distances), one curve family, choreography (the thing you acted on moves first, context follows 40 to 120 ms later), exits quicker than entrances, colour/light changes slower than spatial ones, and immediate response to every key.

### 6. TV specifics: D-pad, key repeat, 60 Hz
- Key repeat: first repeat after `getKeyRepeatTimeout()` = the long-press timeout (**400 ms** default), then every **50 ms** (`KEY_REPEAT_DELAY`). [aosp-viewconfiguration] [viewconfiguration-ref] Remotes may generate their own repeats (unverified for the Xiaomi remote; measure with `adb shell getevent -lt`). `InputManagerService.getKeyRepeatTimeout/Delay` call `ViewConfiguration`, so the real input dispatcher uses these 400 / 50 ms values; the timeout follows `Settings.Secure` `long_press_timeout`. [aosp-ims]
- So a held key moves focus 20 times a second: any focus animation longer than 50 ms is restarted before it ends. Good TV motion stays readable in that state: short focus zooms, a glow that keeps up, no large secondary motion per step.
- Leanback's long-proven values: focus zoom factors **1.06 / 1.10 / 1.14 / 1.18**, focus animation **150 ms** with AccelerateDecelerate, run as a continuous "focus level" 0 to 1 so an interruption resumes from the current level; card "selected" animation delay 400 ms, duration 150 ms; search orb zoom 1.2 over 150 ms; guided-step background fade 350 ms. [leanback-focus] [leanback-dimens] [leanback-integers]
- tv-material Carousel: an item shows **5,000 ms**, the cross-fade is **100 ms out + 100 ms in**, and auto-scroll stops while the carousel is focused or accessibility is on. A useful comparison for our 30 s verse rotation with its 1 s cross-fade. [tv-carousel]
- tv-material Surface focus scale is a tween: 300 ms on focus, 500 ms on unfocus, 120 ms on press, 300 ms on release, with `CubicBezierEasing(0, 0, 0.2, 1)` (f(.1)=.30, f(.25)=.58, f(.5)=.84, t@90%=.60, t@99%=.86); default `focusedScale` 1.1. Unfocus is slower than focus, so the item you leave settles gently. [tv-surface-scale]
- Google TV focus guidance (scales 1.025/1.05/1.1, one focus) is in lane 4's file; it gives no durations (checked: the TV design pages have no motion page; `/design/ui/tv/guides/styles/motion` is 404). [tv-focus]
- 60 Hz: one frame is 16.7 ms; a 150 ms animation is 9 frames, 240 ms is 14. Durations below ~100 ms are only 5 or 6 frames: they read as a pop. Use frame counts when stepping through a screen recording.

### 7. Audio fades (the soft Quran)
- Media3 `Player.setVolume(v)`: 0 silence to 1 unity gain (linear gain on the signal). [media3-player]
- Perceived loudness follows decibels, so a linear gain ramp sounds front-loaded. Our `AccelerateInterpolator(1.4f)` gives gain = 0.22 x t^2.8: relative to the target it is **-56 dB at 10%, -34 dB at 25%, -17 dB at 50%, -7 dB at 75%, -2.6 dB at 90%** (computed). So the first ~11 s of the 45 s fade are effectively silent and the rise is perceived over roughly the last 30 s: gentle, as intended.
- Loop fades: in, 3 s default curve (`fadeLoopIn`); out, linear by `playerTick` over the last 4 s (`LOOP_FADE_MS`), stepped every 250 ms (16 steps; audible zipper is unlikely at those levels, unverified).

### 8. Review recipes
Reset a view's inherited animator settings (use on any shared view) [aosp-vpa]:
```java
v.animate().cancel();
v.animate().setStartDelay(0).setInterpolator(EASE).setDuration(220).alpha(1f).start();
```
Exact M3 curves without a dependency [pathinterpolator-ref] [m3-motion-tokens]:
```java
static final Interpolator STD_DECEL = new PathInterpolator(0f, 0f, 0f, 1f);      // enter
static final Interpolator STD_ACCEL = new PathInterpolator(0.3f, 0f, 1f, 1f);    // exit
static final Interpolator EMPH_DECEL = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
```
Respect "Remove animations" without hidden APIs (API 26+) [valueanimator-ref]:
```java
if (!ValueAnimator.areAnimatorsEnabled()) { v.setAlpha(1f); v.setTranslationY(0); return; } // jump to the end state
```
Held-key check: hold Right on the dock for 3 s, then Left for 3 s, then tap quickly 5 times; record (`adb shell screenrecord --bit-rate 8000000 /sdcard/m.mp4`, max 3 min) and step frames. Look for: focus target clear in every frame, glow not trailing more than one tile, wallpaper drift not see-sawing, label never shown on the wrong tile.

Timing check from a recording: count frames from key press (first changed frame) to 90% of the move; compare with section 2's t@90%. Over about 8 frames (130 ms) of nothing at the start means a late curve or an inherited delay.

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep).
- Curves: `EASE = DecelerateInterpolator(1.6f)` (`MainActivity.java:157`); `Decelerate(2f)` in `magnify()` (`:1372`), player buttons (`:616`), `openPlayer` (`:662`), `BrandGlow.moveTo`; `Decelerate(2.2f)` in `introAnimation()` (`:1451`), `show()` children (`:1909`), PIN box (`:2138`), Parent box (`:3072`); `Decelerate(1.5f)` in `Accent.glideTo` (`Accent.java:41`); `Decelerate(1.6f)` new instance in `drift()` (`:3427`); `AccelerateInterpolator(1.4f)` in `fadeInQuiet()` (`:2794`).
- Default curve (no interpolator ever set on that view): verse `lineBox` out 400 / in 600 (`:1611`, `:1619`); `videoCover` 700/300 (`:255`, `:653`, `:1484`); `dimView` 300/450/4000 (`:3089`, `:3486`, `:3493`); `skyTint` 2000 (`:3449`); `playerControls` 800 out / 250 in with `withLayer()` (`:684`, `:688`); `playerDim` 1200 (`:709`); `pillText` 200/300 (`:1586-1590`); `reciterPanel` 240/180 (`:2680`, `:2738`); `appMenuBox` 200 (`:2382`); `fadeLoopIn` 3000 (`:731`).
- Inherited curves (not the default): `goldLift` translationZ 200 (`:1950`) inherits `EASE` from every caller (`:2090`, `:2333`, `:2602`, `:2629`); `playerPanel`'s 300 ms close (`:676`) inherits `Decelerate(2f)` from its open (`:662`); `quranPanel` (`:650`, `:671`) inherits `EASE` from `show()`; `enterAmbient()` `topParts` alpha (1,500 ms) inherits `Decelerate(2.2f)` from the intro; the dock label fade-out (150 ms, `:1234`) inherits `EASE` from `showDockLabel`.
- **Start delays set and never reset** (only three `setStartDelay` calls, `:1451`, `:1909`, `:2338`; none sets 0):
  - `introAnimation()` (runs on every `onCreate`: splash then intro on cold start, intro alone otherwise, `:268`): `topParts[0]` 120, `topParts[1]` 240, `centerGroup` 360, `lineFrame` 480, `bottomGroup` 600 ms, all with `Decelerate(2.2f)`. Later calls on these views without a delay: `show()` home fade-out (`:1911`), `closePanels()` fade-in (`:2019`), `enterAmbient()`/`leaveAmbient()` (`:3467-3500`). Result: panel open/close and wake-from-ambient are staggered 120 to 600 ms late, and `topParts` alpha fades use `Decelerate(2.2f)`, not the default.
  - All apps items: `Math.min(index, 28) / 7 * 60 + 80` ms (80, 140, 200, 260, 320) on the item (`:2338`); the same item's focus scale (`:2332`, 220 ms `EASE`) and `goldLift` (`:1950`) inherit it.
  - `show()` children: `i * 40` ms on each panel child (`:1909`); harmless unless a child is animated again.
- Dock: `magnify()` (`:1363`) animates every tile each move, 240 ms; `showDockLabel()` cancels then rises 10 px, 220 ms `EASE` (`:1392-1397`); label fade out 150 ms (`:1234`); `BrandGlow` 420 ms `Decelerate(2f)`, colour switches at once, position and strength glide; `drift()` 1,400 ms, `DRIFT_ROOM 0.022`, cancelled and restarted each move.
- Panels: `show()` 260 ms `EASE` + children 28 px over 420 ms stagger 40; home parts out 220 ms; `closePanels()` panels out 180 ms, home in 250 ms, focus restored after a 200 ms `postDelayed`; player in 500 ms `Decelerate(2f)`, out 300 ms; `closePlayer` restores focus after 120 ms.
- Ambient (`AMBIENT_AFTER` 2 min): dock alpha 0 + 40 px over 1,200 ms `EASE`; label out 400 ms; top parts to 0.7 over 1,500 ms; clock, verse and `topParts[1]` sink `QUIET_DROP 110` px over 1,500 ms `EASE`. Wake: 450 ms `EASE` (plus the inherited delays above). The first key in ambient only wakes (`:593`). Night dim after 15 min: `dimView` to 0.6 over 4 s.
- PIN: shake `ValueAnimator` keyframes 0,-18,16,-12,8,-4,0, 420 ms, cancelled before restart (`:2221`); PIN key press 70 ms down to 0.94 then 140 ms back (`:2093`).
- `fullLengthAnimations()` (`:3349`) returns early at scale 1 or 0; else reflection on hidden `setDurationScale` (see lane 3).
- Box scales (verified in project files `tv-box/`): Update TV.ps1 `:92-94`, Tune TV.ps1 `:72-73` and the Restore TV.ps1 fallback `:182-184` set window 0.5, transition 0.5 and animator 1.0, so `fullLengthAnimations()` does nothing today. Restore TV with a backup restores the backed-up values instead.
- Kids (`KidsActivity.java`): `GROW = Decelerate(2f)` dock 250 ms (`:695`); `EASE = Decelerate(1.8f)` label 250 ms (`:681`); focus scales 1.06 to 1.08 at 160 to 220 ms with the default curve (`:1389`, `:1475`, `:1987`); `show()` fades panels in 220 ms (`:1870`), `hide()` is instant GONE (`:1879`); rest panel 600 in / 400 out; PIN shake three chained 60 ms moves of 24 px (`:1557`), not cancelled on a repeat (each new `translationX` cancels the old, and the dropped end actions stop the chain early: harmless). No `fullLengthAnimations()`.
- Adhan (`AdhanService.java:337-430`): band translationY in 750 ms, line scaleX from 180 ms over 900 ms, row from 420 ms over 700 ms with `withLayer()`, all `Decelerate(2.2f)`; exit row 0 ms/350, line 150/500, whole 300/550 with `Accelerate(1.5f)`, `setStartDelay(0)` set explicitly. Reminder in/out 500 ms default curve. Small cards 250 in / 150 out.

## Past findings
- 2026-11-05, PR #21 (docs only): corrected the curve labels for `goldLift` (inherits `EASE`), lane 3's 0.5x animator claim (the scripts set 1.0) and lane 4's panel curves; added motion to the coverage map (Verse, Wallpaper, Quran player, PIN pads, Kids rows; new Ambient mode row).
- Baseline audit at creation (not a PR review): inherited start delays from `introAnimation()` and the All apps fade-in (see This box); five decelerate factors plus the default curve in use; held-key restarts of `magnify()`/`BrandGlow`; lane 4's note that undecorated `animate()` calls "get AccelerateDecelerate" is true only for views that never set an interpolator.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit on `main` d772bcc: Must were the intro's inherited delays (label shows over an empty dock after closing a panel or waking, and the clock and dock linger under a new panel) and the All apps focus inheriting an 80 to 320 ms delay (moved up from Should). Should were the delayed focus restore after close (`postDelayed` 120/200 ms), IntroView and the splash timed on the wall clock, the Kids PIN shake (3×60 ms) unlike home's 420 ms keyframes, exits on decelerate curves and short fades on the default curve, and the verse cross-fade (opacity only, 250/400). Nice were the hidden `lineTimer` running all day, the BrandGlow colour snap, the prayer highlight snap, skipping unchanged tiles in `magnify()`, IntroView at scale 0, Kids hide/focus curves, and the magnify Javadoc.


## Lessons learned
- 2026-11-05: `View.animate()` is stateful. Before calling an animation "default", check every earlier `animate()` on the same view for `setStartDelay` or `setInterpolator`.
- 2026-11-05: Compare curves by when they reach 90%, not by their nominal duration.
- 2026-11-05: On a D-pad, the held-key case is the stress test for focus motion (a step every 50 ms).
- 2026-11-05: Copy the adhan overlay's pattern: set `setStartDelay(0)` explicitly on exit animations of views that had delayed entrances.
- 2026-11-05: Check the owner's scripts in project files before marking a box setting as unverified.
- 2026-11-05: When another lane's file states a motion fact, check it against this file.
- 2026-11-05: Inherited delays show up as wrong order, not just lateness. Look for any view animated with no delay (like `dockLabel`) next to one carrying an old delay (like `bottomGroup`).
- 2026-11-05: Time custom-drawn animations (`onDraw` + `postInvalidateOnAnimation`) with `SystemClock.uptimeMillis()` or `getDrawingTime()`, never `System.currentTimeMillis()`. The home starts at boot, when the wall clock is most likely to jump.
- 2026-11-05: Look for animators that drive hidden views (`GONE` targets). They cost frames and show nothing.
- 2026-11-05: Restoring focus with `postDelayed` after a fade is a responsiveness bug if the target is already focusable. Move focus at once and let the fade play.

## Open questions to research next
- Record the panel open/close and ambient wake on the TV to confirm the inherited delays (expected: dock lingers 600 ms).
- What does the Xiaomi remote send when a key is held: Android-generated repeats (400 ms, then 50 ms) or its own? (`adb shell getevent -lt`)
- Answered 2026-11-05: Update TV sets window 0.5, transition 0.5, animator 1.0 (see This box); AOSP phone Settings' "Remove animations" sets all three to 0 (TvSettings still unverified).
- Answered 2026-11-05: tv-material's Surface focus scale is a tween in `SurfaceScale.kt`/`SurfaceTokens.kt` (300 / 500 ms, see Core 6). Google TV home timings are still unknown.
- Should the dock's unfocus be slower than its focus, as tv-material does?
- For lane 1: if the adhan alert is shown again within 850 ms of `hideAlert()`, `wm.addView` throws on the still-attached view and the banner doesn't appear. Very unlikely.
- Is a single shared `EASE` plus a matching exit curve (`PathInterpolator(0.3, 0, 1, 1)`) enough for every motion here? Mock and compare on the TV.
- Should the verse cross-fade drop its 8 px movement (opacity only is calmer and not "motion" under WCAG 2.3.3)?
- How do premium TV launchers (Apple TV, Google TV) time focus moves? Measure from screen recordings at 60 fps.
- Open Val Head / A List Apart on motion sensitivity and Google's "Making motion meaningful" (fetch budget ran out this session).
- Does the box's wall clock jump at boot? Check with `adb logcat -b events | grep -i time` or by comparing `date` right after a cold boot with a later reading.
- After M1 is fixed, record panel close and ambient wake at 60 fps to confirm that the dock leads and the label never shows alone.
- Is the M1 alternative (a deliberate 0/40/80 ms return cascade) nicer than everything at once? Compare on the TV.
- Ask lane 3: frame cost of N1 (hidden `lineTimer`) and of N4 (skipping unchanged dock tiles).

## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [mdc-motion] https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Motion.md: M3 easing attributes, spring attributes (damping/stiffness), speed by size.
- [m3-motion-tokens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/MotionTokens.kt: exact duration tokens and bezier values.
- [aosp-decelerate] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/animation/DecelerateInterpolator.java: formula 1-(1-t)^(2f).
- [aosp-acceldecel] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/animation/AccelerateDecelerateInterpolator.java: cosine formula.
- [aosp-vpa] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewPropertyAnimator.java: sticky duration/delay/interpolator, per-property cancel, end-action rules.
- [aosp-view-animate] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/View.java: `animate()` creates one VPA per view and reuses it.
- [aosp-viewconfiguration] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewConfiguration.java: long press 400 ms, key repeat 50 ms.
- [viewconfiguration-ref] https://developer.android.com/reference/android/view/ViewConfiguration: `getKeyRepeatTimeout`, `getKeyRepeatDelay`.
- [valueanimator-ref] https://developer.android.com/reference/android/animation/ValueAnimator: 300 ms default, `getDurationScale`, `areAnimatorsEnabled`, scale listener.
- [pathinterpolator-ref] https://developer.android.com/reference/android/view/animation/PathInterpolator: cubic-bezier interpolators.
- [propanim] https://developer.android.com/develop/ui/views/animations/prop-animation: property animation system, ViewPropertyAnimator overview.
- [choreographer-ref] https://developer.android.com/reference/android/view/Choreographer: frame callbacks per Looper thread, vsync.
- [spring-anim] https://developer.android.com/develop/ui/views/animations/spring-animation: spring force, start velocity, default damping MEDIUM_BOUNCY.
- [leanback-focus] https://raw.githubusercontent.com/androidx/androidx/androidx-main/leanback/leanback/src/main/java/androidx/leanback/widget/FocusHighlightHelper.java: 150 ms focus zoom, AccelerateDecelerate, continuous focus level.
- [leanback-dimens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/leanback/leanback/src/main/res/values/dimens.xml: zoom factors 106/110/114/118%.
- [leanback-integers] https://raw.githubusercontent.com/androidx/androidx/androidx-main/leanback/leanback/src/main/res/values/integers.xml: card selected delay 400 / duration 150, guided-step fade 350.
- [tv-carousel] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/Carousel.kt: 5 s per item, 100 ms cross-fade, auto-scroll off when focused or accessibility on.
- [tv-focus] https://developer.android.com/design/ui/tv/guides/styles/focus-system: focus states and scales (no durations).
- [media3-player] https://developer.android.com/reference/androidx/media3/common/Player: `setVolume` 0 to 1 unity gain.
- [nngroup-duration] https://www.nngroup.com/articles/animation-duration/: 100 to 500 ms, frequency, enter vs exit, easing choice.
- [nngroup-purpose] https://www.nngroup.com/articles/animation-purpose-ux/: purposes of animation, distraction, simultaneous motion.
- [hig-motion] https://developer.apple.com/design/human-interface-guidelines/motion (read via .../tutorials/data/design/human-interface-guidelines/motion.json): purposeful, brief, cancellable motion.
- [hig-accessibility] https://developer.apple.com/design/human-interface-guidelines/accessibility (read via the .json): Reduce Motion practices (fades instead of movement, no z or blur animation).
- [wwdc18-fluid] https://developer.apple.com/videos/play/wwdc2018/803/: Designing Fluid Interfaces (latency, interruption, 100% damping by default).
- [wcag-233] https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html: motion animation definition, opacity/colour excluded, vestibular rationale.
- [wcag-222] https://www.w3.org/WAI/WCAG22/Understanding/pause-stop-hide.html: 5 s rule, auto-updating content.
- [wcag-231] https://www.w3.org/WAI/WCAG22/Understanding/three-flashes-or-below-threshold.html: 3 flashes per second.
- [tv-surface-scale] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/SurfaceScale.kt and https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/tokens/SurfaceTokens.kt: TV focus scale durations and easing. Checked 2026-11-05.
- [aosp-ims] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/services/core/java/com/android/server/input/InputManagerService.java: key repeat comes from ViewConfiguration. Checked 2026-11-05.
- [aosp-disable-anim] https://git.replicant.us/replicant-next/packages_apps_Settings/plain/src/com/android/settings/accessibility/DisableAnimationsPreferenceController.java: "Remove animations" sets all three scales. Checked 2026-11-05.
- [gd-motion-not-hard] https://medium.com/google-design/33089196e6c2: Google Design's practical durations and easing. Checked 2026-11-05.
- To open next: A List Apart "Designing Safer Web Animation for Motion Sensitivity" (blocked by proxy), Google Design "Making motion meaningful", m3.material.io motion pages (JS-only), Android TV "Remove animations" help page.
- [systemclock] https://chromium.googlesource.com/android_tools/+/HEAD/sdk/sources/android-25/android/os/SystemClock.java: wall clock can jump; `uptimeMillis` is monotonic, for intervals. Checked 2026-11-05.
- [anim-time] https://learn.microsoft.com/en-us/dotnet/api/android.views.animations.animationutils.currentanimationtimemillis?view=net-android-37.0: the animation clock is not the wall clock. Checked 2026-11-05.
- [gd-meaningful] https://design.google/library/making-motion-meaningful: clear focal point, respond without missing a beat, avoid overlapping paths. Checked 2026-11-05 (no durations given).
