# Lane 12 knowledge: Motion and animation (easing, timing, choreography, interruption): archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (audit update 1):
  - Fixed: the audit Should on delayed focus restore. `closePanels`, `closeReciters`, `closePlayer` and `drawQuranState` now move focus at once.
  - Must: moving focus at once widened the inherited-delay gap. After a panel closes, the dock label now shows about 600 ms before the dock (`bottomGroup` keeps the intro's 600 ms delay).
  - Should: the calendar to All apps step is a hard cut both ways. Going back, `bringToFront` plus `setAlpha(1)` cuts it. Going in, All apps goes `GONE` before the calendar has faded in.
  - Nice:
    - The reciters loading line can flash (it appears after 300 ms and has no minimum display time).
    - The toast pill jumps when it changes position.
    - `nudge()` and `pinWrong()` could be one shake helper.
    - Kids' key dip is 0.96 and home's is 0.94.
    - A Parent settings row re-zooms on every toggle.

- 2026-11-05, PR #21 (docs only): corrected the curve labels for `goldLift` (inherits `EASE`), lane 3's 0.5x animator claim (the scripts set 1.0) and lane 4's panel curves; added motion to the coverage map (Verse, Wallpaper, Quran player, PIN pads, Kids rows; new Ambient mode row).
- Baseline audit at creation (not a PR review): inherited start delays from `introAnimation()` and the All apps fade-in (see This box); five decelerate factors plus the default curve in use; held-key restarts of `magnify()`/`BrandGlow`; lane 4's note that undecorated `animate()` calls "get AccelerateDecelerate" is true only for views that never set an interpolator.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit on `main` d772bcc: Must were the intro's inherited delays (label shows over an empty dock after closing a panel or waking, and the clock and dock linger under a new panel) and the All apps focus inheriting an 80 to 320 ms delay (moved up from Should). Should were the delayed focus restore after close (`postDelayed` 120/200 ms), IntroView and the splash timed on the wall clock, the Kids PIN shake (3×60 ms) unlike home's 420 ms keyframes, exits on decelerate curves and short fades on the default curve, and the verse cross-fade (opacity only, 250/400). Nice were the hidden `lineTimer` running all day, the BrandGlow colour snap, the prayer highlight snap, skipping unchanged tiles in `magnify()`, IntroView at scale 0, Kids hide/focus curves, and the magnify Javadoc.


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

- [aosp-animhandler] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/animation/AnimationHandler.java: when no window is visible, Android 14 pauses only infinite-duration animators, after `Animator.getBackgroundPauseDelay()`. Finite fades started in `onStop` still finish and run their end actions. Checked 2026-11-05.
- [spin-delay] https://docsearch.algolia.com/mcp/docs/repo/smeijer/spin-delay: defaults are a 500 ms delay and a 200 ms minimum duration, so a spinner doesn't flash. Checked 2026-11-05.
- [adobe-progress] https://developer.adobe.com/commerce/admin-developer/pattern-library/feedback-to-user/progress-indicator: delay a spinner 500 ms to avoid flicker; skip it for tasks under 500 ms. Checked 2026-11-05.

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

- After the Must 1 fix, record Back from All apps at 60 fps and confirm the dock and its label arrive together.
- Should the toast pill's position follow panel open/close (lane 9), or should it stay fixed at the bottom centre?

