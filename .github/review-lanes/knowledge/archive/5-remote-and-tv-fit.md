# Lane 5 knowledge: Remote and TV fit: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
- 2026-11-06 PR #25 re-check at c031f6c: Should 1 fixed (kicker redraws at once via lineItem), Nice 2 done (label 338+170 px).
### PR #25 (2026-11-06, update 3: Islamic content)
- 2026-11-06 PR #25 (Update 3: Islamic content): 5 new settings rows in 3 apps, no new keys or panels; focus restored in all three (home indexOfChild, Adhan by index, Kids in place). Should: home Sources toggle showed only at the next band line (up to 30 s), fixed. Nice: home Hijri label wrapped (579+170 px > 728 px row), shortened. Online verse fetch removed.

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

From PR #25 (2026-11-06, update 3: Islamic content):
- [nav] re-opened 2026-11-06: no guidance on settings toggles or value-change feedback, only general focus/selection feedback and the Cancel rule; for toggle patterns see developer.android.com/design/ui/tv.

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

From PR #25 (2026-11-06, update 3: Islamic content):
- should home Parent value rows take Left/Right like Kids? Owner's call, polish only.
