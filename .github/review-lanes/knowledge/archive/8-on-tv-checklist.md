# Lane 8 knowledge: On-TV checklist for the owner: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- **2026-11-06, PR #23** (update 2: visual and UI polish):
  - No Must.
  - Should: the reminder is now 8 s, not 15 s (brief and knowledge), and the code facts above.
  - Nice:
    - "Every control dips" doesn't cover Kids settings rows or the Adhan settings app.
    - `pressDip` restores the scale it read at key-down, so an OK pressed during a focus zoom settles slightly small.
  - Checklist (13 new steps, 4 can't-test-now items, plus the smoke test): link above.

### PR #22 (2026-11-05, audit update 1)
- **2026-11-05, PR #22** (audit update 1: focus trap, Back one layer at a time, idle close, PIN notes, wallpaper, Quran, kids player and adhan fixes):
  - No Must.
  - Should: the stale adhan-card lesson; stale line references (secret menu, PIN keys) and the kids OK rule.
  - Nice:
    - Kids OK wraps the limit from 240 min to "No limit".
    - Guard's urgent bring-back has no minimum gap.
    - The message pill sits 64 px from the bottom over panels.
    - Play/Pause can't be tested on this remote.
  - Checklist (14 new steps plus the smoke test): https://github.com/alshaheendr-prog/tv-home/pull/22#issuecomment-6672557289

- **2026-11-05, PR #20** (PIN limits, crash after restart, missed adhan, Quran text):
  - Should fix: `pin_fails` was never reset over time. Fixed: wrong tries are forgotten after 1 h, and the rest is capped at 5 min.
  - Should fix: the kids lock warning toast was easy to miss. Fixed: it moved into the Parent menu.
  - Tips added to the brief: enter the right PIN before wrong-PIN tests; ~~the adhan card only shows over another app~~ (corrected: only over Google TV's own home, see section 5); "Don't keep activities" for restart crashes.
  - Checklist comment (8 new steps plus the smoke test): https://github.com/alshaheendr-prog/tv-home/pull/20#issuecomment-6669904149
- **2026-11-05, PR #21** (docs only, lane knowledge files): no TV steps. Should fix: the Home/RemoteKeys claim was wrong (RemoteKeys passes Home through), and the ALL_APPS claim conflicts with the `rk_apps` default. Comment: https://github.com/alshaheendr-prog/tv-home/pull/21#issuecomment-6670685789
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit:
  - Must: Parent settings focus is off by one after OK (`drawParent(n)`, `:3090-3145`).
  - Should: the PIN set-up text "Hold OK on YouTube to change it later" is stale.
  - Should: the Adhan settings still show the Projectivy note and rows that never act over the family home.
  - Should: Back on the stats page closes the whole menu.
  - Should: brief smoke steps 7 and 8, the corner-card lesson, the dock list, built-in worlds, and the missing drive step were all wrong.
  - Nice: Back from the calendar, the long walk down to "Leave Kids Corner", and raw adb lines in the Adhan set-up box.
  - Whole-app checklist: `/mnt/project-files/tv-box/reviews/audit-2026-11/lane-8.md`.


## Source library
All checked 2026-11-05 unless noted.
- [tv-quality] https://developer.android.com/docs/quality-guidelines/tv-app-quality: all the TV-xx criteria (D-pad, Back, overscan, playback, Ambient Mode, memory). It has no T-xx procedures.
- [sfdpo] https://www.stickyminds.com/article/how-do-you-spell-testing: James Bach's SFDPO heuristic for test ideas.
- [a15] https://developer.android.com/about/versions/15/behavior-changes-15: BOOT_COMPLETED FGS rules, SAW FGS visibility, audio focus. These explain why the reboot and adhan tests matter.
- [install-errors] https://bayton.org/android/resolve-app-install-errors: VERSION_DOWNGRADE, UPDATE_INCOMPATIBLE and other errors he might see in Update TV.
- [adb-gist] https://gist.github.com/Pulimet/5013acf2cd5b28e55036c82c91bd56d8: screencap, screenrecord, logcat, keyevents, pm clear.
- [tvlauncher-noroot] https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/: set-home-activity and how to verify it, and undo. Its Xiaomi `KEYCODE_ALL_APPS` Home claim is not verified on this box and conflicts with our `rk_apps` default.
- [a11y-gist] https://gist.github.com/mrk-han/67a98616e43f86f8482c5ee6dd3faabe: how accessibility services are enabled and overwritten through settings.
- [downgrade-forum] https://forum.level1techs.com/t/downgrading-app-on-android-12/179457: a user's report that `-r -d` cleared the downgrade error but hit a permission-model failure. Downgrades are fragile.
- [tv-checklist] https://developer.android.com/training/tv/publishing/checklist: reopened 2026-11-05: requirements only, no test procedures (Ethernet and Wi-Fi, a clean exit).
- From the brief (checked by the first reviewer, not reopened in this session): https://developer.android.com/about/versions/14/changes/fgs-types-required.
- [uat-scripts] https://www.virtuosoqa.com/post/uat-test-scripts: one action per step, an explicit pass condition, preconditions, exact values. Checked 2026-11-05.
- [screenrecord] https://essential-books.onrender.com/essential/android/recording-the-display-d649a1cd7b414b78a95d35d3a54d9eaa.html: the 180 s cap, `--size`, `--bit-rate`. Checked 2026-11-05.
- [aerialviews] https://github.com/theothernt/aerialviews: `screensaver_components`, the 6-minute timeout floor, TestActivity. Checked 2026-11-05.
- [somnambulator] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/55405715/adb-command-to-launch-screensaver-activity: starting the screensaver from adb (results vary). Checked 2026-11-05.
- [tv-quality] https://developer.android.com/docs/quality-guidelines/tv-app-quality: reopened.
  - TV-DB means Back leads back toward the home screen.
  - TV-BY means don't block Ambient Mode when there is no video or animation.
  - TV-NP now applies to audio only (May 2024), and video must pause when you leave.
  - TV-ME points to the memory page.
- [tv-memory] https://developer.android.com/training/tv/playback/memory: hard limits only for 1 GB low-RAM devices.
  - At most 280 MB total (Anon+Swap + Graphics + File), with 200 MB recommended.
  - Per category: Anon under 160, Graphics 30-40, File 60-80.
  - Media buffer for a 2 GB device at 2160p: 100-120 MB.
  - It gives no limit for 2 GB devices. Measure with the Android Studio profiler, or `dumpsys meminfo` (the developer's job).
- [hendrickson] https://www.ministryoftesting.com/articles/ab1cd85c: Test Heuristics Cheat Sheet.
  - Interruptions: shut down, reboot, kill, disconnect, timeout, cancel.
  - Count: 0, 1, many.
  - Starvation: CPU, memory, network.
  - Time and date: timeouts, reset clock.
- [keep-screen-on] https://developer.android.com/develop/background-work/background-tasks/awake/screen-on: FLAG_KEEP_SCREEN_ON keeps the screen on only while the window is visible. Clear it when it's not needed. On TV, use it during video playback.
- [mibox-off] https://reddit.birdcat.cafe/r/MiBox/comments/1je6blo/how_do_i_switch_it_off: 2nd-gen box. Users disagree on how the power button behaves. HDMI-CEC and an auto-off after 15 min are mentioned. Weak source, not verified on the 3rd gen.
- [dev-options] https://www.dignited.com/95778/how-to-enable-google-tv-developer-options: it gives Settings, Device Preferences, About, Build for Developer options. That's the older Android TV path. On this Google TV 14 box the owner uses Settings, System. Not verified on the box.

- [remote-manual] https://manuals.plus/asin/B0F3JWFL56.pdf: the 3rd-gen box remote has Power, Assistant, D-pad, Home, Back and app buttons, and no Play/Pause. Checked 2026-11-05.
- [key-repeat] https://gerrit.omnirom.org/plugins/gitiles/android_frameworks_base/+/5128949634c682b630a9a94517f4a6df0b5dfc21%5E%21: `DEFAULT_KEY_REPEAT_DELAY_MS = 50`, so a held key repeats every 50 ms. Checked 2026-11-05.
- [aosp-cec] https://source.android.com/devices/tv/hdmi-cec?hl=en: names Remote Control Passthrough, but doesn't say how keys reach apps. Checked 2026-11-05.

- [tv-icons] https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines: the banner is mandatory at 16:9 (320x180 xhdpi, 640x360 xxxhdpi, in mipmap folders) and is used in the TV launcher. The launcher icon shows in Settings, the Now Playing card and the Google TV "Your apps" row. Themed icons aren't supported on TV. It gives no test procedure. Checked 2026-11-06.
- [adaptive-icons] https://medium.com/google-design/515af294c783: 108 dp layers, masked to at most 72 dp, a 66 dp safe circle in the centre. Launchers may add parallax or scale. Checked 2026-11-06.
- [adaptive-clip] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/49661571/adaptive-icon-not-displaying-properly: art outside the safe zone gets clipped. The fix is scaling it into the centre. Checked 2026-11-06.
- [tv-update-fix] https://www.guidingtech.com/best-ways-to-fix-apps-not-updating-on-android-tv/: Play Store update fixes only. It says nothing about launcher icon caches. Checked 2026-11-06.

## Open questions to research next
(The web-fetch budget ran out on 2026-11-05 before these could be opened.)
- Answered 2026-11-05: the publishing checklist lists requirements only, no test procedures.
- Answered 2026-11-05: `screenrecord` stops at 180 s, the default and the maximum [screenrecord]. Still open: `install -d` rules for release apps on Android 14.
- Which key codes do Home and Apps really send on this remote? Check with `adb logcat | findstr /i keycode` while pressing each one, then reconcile with the `rk_apps` default.
- Sources on writing test cases for non-technical users (TestLodge, Marker.io) and on UAT scripts: confirm or extend section 1.
- Elisabeth Hendrickson's Test Heuristics Cheat Sheet: interruption and timing heuristics for section 3.
- The TV-ME memory limits for low-RAM devices (the exact MB values) and how the owner could see memory (Settings, then Storage/Memory on Xiaomi).
- Does the Xiaomi Android TV 14 screensaver (Ambient Mode) start over the video wallpaper? How do you trigger it quickly for testing?
- How do you make the box go to sleep and wake for a test (power button behaviour on this remote)?
- The exact path to Wireless debugging on this box (Settings, System, Developer options?). Ask the owner to confirm in one line.
- What the remote's power button does on the 3rd-gen box: short press and long press, sleep or off.
- Is there a safe way for the owner to test the 10-minute reminder without changing the clock? Maybe a test row in the Adhan settings: that's a feature, so it's only for the owner to ask for.
- The memory budget for 2 GB devices: the docs give none. Lane 3 may know a measured baseline.

- Does the TV's own remote send Play/Pause to the box over HDMI-CEC? Ask the owner whether the TV remote has those keys.
- Does the Xiaomi Bluetooth remote send its own key repeats, or the framework's 50 ms? It changes the cartoon seek pace.
- Replace the remaining line numbers in the brief and knowledge file with method names.

- Does Google TV's own launcher ("Your apps", Settings > Apps) refresh a sideloaded app's icon or banner straight after `install -r`, or only after a reboot? No source found on 2026-11-06. Ask the owner what they see after this update.
- Adaptive Icon Playground ([adaptive-icons]) could preview masks on the box. It's for the developer only, if lane 13 wants it.
