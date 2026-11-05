# Lane 8 knowledge: On-TV checklist for the owner

## How to use this file
- Read this whole file before every review, then the brief (`../8-on-tv-checklist.md`). The brief says what to write. This file is the reference behind it.
- If the PR touches something this file doesn't cover, research it online first, cite what you opened, and add it here. Examples: a new remote key, media keys, PiP, the screensaver, a new permission.
- After every review, send the PR's thread short, dated additions for **Past findings**, **Lessons learned**, **Source library** and **Open questions**, so they get committed in the same PR. Note which checklist steps the owner reported as confusing or failed.
- Keep the file under about 400 lines. If it grows past that, condense the oldest Past findings into one line per PR.
- Cite only pages you actually opened. Mark general QA practice that has no source "(practice)".

## Core expertise

### 1. Writing steps a beginner can follow (practice)
- **One action per step, then what he should see.** For example: "Press Down twice, then OK. *You should see* the PIN pad." A step that has no expected result can't fail, so it tests nothing.
- **Give exact presses and on-screen words.** Use the remote's real button names (OK, Back, Home, the arrows, Netflix, Prime Video, YouTube, Apps). Copy labels from the strings in the diff, for example "Play the adhan now (test)".
- **Say what broken looks like:** for example "the screen goes black for more than 5 seconds", "nothing happens", or "it opens Google TV instead".
- **State the setup first** ("Start on the family home screen"), and say where each step ends, so steps can be repeated.
- **Number every step,** so he can reply "step 4 failed". Keep each step to one line. Put any warning *before* the step it is about.
- **No developer words.** Write "the box", "the remote", "the PC". Never write APK, intent, activity or permission, except as an exact line to paste into Update TV.
- **Keep it short.** About 10 minutes in all: 2 to 6 new steps plus the smoke test. A long list gets skipped.
- **Order:** the risky or new things first while he is fresh, then the smoke test.

### 2. The Google TV quality checklist, turned into owner checks
Every item below is from the Android TV quality criteria [tv-quality]:

| ID | What it requires | Plain check for the owner |
|---|---|---|
| TV-DP | everything reachable with the D-pad | "You can reach X with the arrows alone" |
| TV-DB | Back returns toward home and never traps you | "Press Back: the panel closes, and again: you are home" |
| TV-DM | nothing depends on a Menu button | the remote has none, so never use one in a step |
| TV-OV | nothing cut off at the screen edges (overscan) | "Look at all four edges: no text is cut off" (also compare `C:\TVWallpapers\home.png`) |
| TV-LO | landscape, no black bars except on video | "The picture fills the screen" |
| TV-TR | the app doesn't partly cover other apps unasked | the adhan card should appear only at prayer time, and disappear afterwards |
| TV-LM / TV-LB / TV-BN | the icon and banner appear in the launcher with the app's name | "In All apps you see Adhan and Kids Corner with their pictures" |
| TV-LS | no errors while installing, loading or using | "Update TV said done with no red text" |
| TV-PC / TV-PP | OK pauses and resumes; play/pause keys work | only if the PR touches video players (kids PlayerActivity) |
| TV-NP | audio that keeps playing in the background shows controls; video pauses when you leave | leave a cartoon and check it stops |
| TV-BU / TV-BY | video blocks the screensaver; a still screen lets it start | if the PR touches the wallpaper or the screensaver, wait for the screensaver time |
| TV-ME | memory limits on low-RAM devices | the 10-minute "is it getting slow" check on the 2 GB box |

### 3. Exploratory heuristics for a TV and a remote
SFDPO is a memory aid for test ideas [sfdpo]: Structure, Function, Data, Platform, Operations. Applied here:
- **Structure:** three apps that talk to each other (home, adhan, kids). A change in one can break another: the kids stats on the home screen, or the adhan card over Kids Corner.
- **Function:** each visible change, plus its error path. What if the app isn't installed? What if there's no internet, so no weather?
- **Data:** an empty wallpaper folder, very long app names, Arabic text, PIN edge cases (wrong, right, wrong 5 times), a first run with no settings yet.
- **Platform:** Android TV 14 on the Xiaomi box. A grant can go missing after an update (run Update TV again). A reboot. No internet (unplug the router or turn off Wi-Fi).
- **Operations:** what a family really does. Kids pressing Home or YouTube over and over, fast double presses, holding OK, pressing Back during the intro.
- Remote-specific interruptions (practice): press Home in the middle of a flow. Press Back twice quickly. Press OK during the intro animation. Hold an arrow down for 3 seconds. Press the app buttons (YouTube, Prime, Apps) while a PIN pad is open. Leave a panel open for 5 minutes.
- Time-related checks: around prayer time, bedtime, the daily limit, midnight (new day, new world), after a reboot, and after the box sleeps and wakes.

### 4. Smoke test (regression) design (practice)
- A smoke test is a short, fixed list that touches every major area once. It is run after *every* update, so it must be short and stable.
- Here it covers: home loads, clock and prayer times, wallpaper, launching apps, the YouTube PIN, the kids lock, the adhan test, the remote buttons, and Back/Home. This is the 10-step list in the brief.
- Only change the smoke test when the app changes in a way that makes a step wrong. Then rewrite that step in the same PR.
- After any PR that touches startup or services, add the reboot test. Some failures only show up then (BootReceiver, the FGS start rules [a15]).

### 5. Testing the adhan and other timed features safely
- Use the built-in test first: Adhan, then **"Play the adhan now (test)"**. It sends the `test` action to `AdhanService`, which calls `fireAdhan(3)` (`AdhanService.java:134`, `SettingsActivity.java:187`).
- The adhan's corner card only shows **over another app**, not over the family home. So test it from inside Netflix or another app. (A PR #20 lesson.)
- The sound needs audio focus. On Android 15, focus is refused unless the app is on top or running an FGS [a15]. The adhan runs an FGS, so the sound should pause Netflix or YouTube. Check that the other app pauses and then resumes.
- Don't change the TV's clock unless there is no other way. If he must:
  - Write the steps to set it back to automatic time afterwards.
  - Warn him that kids stats, the daily limit and today's world may count wrongly that day.
- Bedtime and the daily limit: lower the setting in Parent settings (for example a 1-minute limit), test, then **put it back** as an explicit last step.

### 6. Parental-control test cases (kids lock and PINs)
- **Before any wrong-PIN test, enter the right PIN once,** so the counter starts at zero.
- 5 wrong tries make the pad rest: 30 seconds first, growing to at most 5 minutes. Wrong tries are forgotten after 1 hour (`PinLock.java:13-14`, FREE_TRIES = 5, FIRST_WAIT 30 s, MAX_WAIT 5 min, FORGET_AFTER 1 h). Warn him before he tries it.
- Kids mode escape checks, each of which must bring Kids Corner back:
  - Press Home.
  - Press the YouTube button.
  - Press Settings.
  - Press the Apps button.
  - Press Back several times.
  - Reboot with kids mode on.
- Only the parent PIN ends kids mode, through the lock at the end of the row or "Grown-ups".
- The PIN must still be asked for: the YouTube tile, the YouTube remote button, the Parent menu (Up, Up, Down, Down within 2.5 s, `MainActivity.java:3009-3026`), and hold-OK arranging, if it needs the PIN.
- Never put `pm clear` in a checklist. It wipes the PINs, settings and today's time [adb-gist].

### 7. Capturing evidence
- **Photo:** the easiest way. Ask for a phone photo of the whole screen and the step number.
- **Screenshot from the PC** (if he is comfortable), with `adb shell screencap -p /sdcard/s.png` then `adb pull /sdcard/s.png` [adb-gist]. Update TV already saves `C:\TVWallpapers\home.png`.
- **Short video:** `adb shell screenrecord /sdcard/demo.mp4`, stopped with Ctrl+C [adb-gist]. Use it for animation or stutter problems.
- **Crash log** (for the developer, not the owner): `adb logcat -d > log.txt` [adb-gist]. Only ask for it if he already runs adb commands.
- **Remote key problems:** `logcat ... | grep -i keycode` shows which code a button really sends. Xiaomi's Home can arrive as `KEYCODE_ALL_APPS` [tvlauncher-noroot].

### 8. When something fails: recovery
1. **Run Update TV again.** It re-applies every grant: overlay, usage access, the accessibility service, the home activity. A missing grant fails silently (no popup, buttons not remapped, Home goes to Google TV).
2. **If the remote buttons stopped working,** relearn them: Parent settings, then learn the buttons again. They are stored per box (`RemoteKeys.java:14,38`).
3. **If Home opens Google TV,** Update TV sets the home activity again. The manual line is `adb shell cmd package set-home-activity --user 0 com.family.tvhome/.MainActivity` [tvlauncher-noroot].
4. **Rollback.** Installing an *older* APK over a newer one fails with `INSTALL_FAILED_VERSION_DOWNGRADE` [install-errors]. Forcing it would mean uninstalling, which **deletes the PINs, settings and kids stats**. So the safe rollback is: reply on the PR, the developer reverts it, CI builds a new (higher-numbered) release with the old code, and he runs Update TV. Don't tell him to download an old APK.
5. **If the box seems stuck,** unplug it for 10 seconds. The services restart by themselves through the BootReceivers.
6. **Report:** step number, a photo, and what he expected.

### 9. TV behaviour that explains odd results
- The FGS can't start from a BOOT receiver for some types on Android 15. Starting an FGS with the overlay permission also needs a visible overlay [a15]. That is why "unplug and replug" is a real test, not a formality.
- Setting HOME alone doesn't capture the Xiaomi Home button. The accessibility service does that, so if RemoteKeys is off, Home goes to Google TV [tvlauncher-noroot].
- Accessibility services are stored as one list separated by colons. Some tools overwrite it [a11y-gist]. If another accessibility app was set up, it may switch RemoteKeys off.

## This box and this code (verified 2026-11-05)
- **Box:** Xiaomi TV Box S 3rd Gen, Android TV 14, 2 GB RAM.
- **Remote:** arrows, OK, Back, Home, volume, plus Netflix, Prime Video, YouTube and Apps. **No Menu button and no number keys.** PIN entry is on screen, but number keys work if a remote has them (`MainActivity.java:464`).
- **Updates:** the owner runs "Update TV" (PowerShell on Windows, wireless ADB). It installs with `-r`, grants permissions, enables RemoteKeys, sets the home activity and saves a screenshot to `C:\TVWallpapers\home.png`.
- **UI:** native Java, not the WebView the README describes. The screens are `app/.../MainActivity.java`, `kids/.../KidsActivity.java` and `adhan/.../SettingsActivity.java`.
- **Back handling:** `MainActivity.java:453` (plus arranging at `:1271`), `KidsActivity.java:258` (plus arranging at `:989`).
- **Secret Parent menu:** Up, Up, Down, Down within 2500 ms, only when no panel is open (`MainActivity.java:462, 3009-3026`).
- **PinLock** (app and kids): 5 free tries, a 30 s rest that grows to at most 5 min, and wrong tries forgotten after 1 h (`PinLock.java:13-14,58-60`).
- **Adhan test row:** "Play the adhan now (test)" (`SettingsActivity.java:187`). It triggers `fireAdhan(3)` (`AdhanService.java:134`).
- **Overlays:** `AdhanService.java:174` and `Guard.java:198,245`. Both need the overlay grant from Update TV.
- **Kids:** asks for READ_MEDIA_VIDEO/IMAGES/AUDIO at `KidsActivity.java:170`. Reads folders `Movies`, `Pictures/KidsCorner`, `Download/Kids` and `Music/Quran`.
- **Wallpaper clips:** `Movies/TVWallpapers` (`MainActivity.java:1461`).
- **Services restart after a reboot and after an update** (BootReceiver: BOOT_COMPLETED, MY_PACKAGE_REPLACED).

## Past findings
- **2026-11-05, PR #20** (PIN limits, crash after restart, missed adhan, Quran text):
  - Should fix: `pin_fails` was never reset over time. Fixed: wrong tries are forgotten after 1 h, and the rest is capped at 5 min.
  - Should fix: the kids lock warning toast was easy to miss. Fixed: it moved into the Parent menu.
  - Tips added to the brief: enter the right PIN before wrong-PIN tests; the adhan card only shows over another app; "Don't keep activities" for restart crashes.
  - Checklist comment (8 new steps plus the smoke test): https://github.com/alshaheendr-prog/tv-home/pull/20#issuecomment-6669904149

## Lessons learned
- 2026-11-05: Test the adhan card from inside another app, never from the family home.
- 2026-11-05: Every wrong-PIN step needs a right-PIN step before it and a warning about the rest time.
- 2026-11-05: A rollback is a revert PR plus Update TV, never an older APK (VERSION_DOWNGRADE, and uninstalling wipes data).
- 2026-11-05: Any step that lowers a limit or changes a setting needs a matching "put it back" step at the end.
- 2026-11-05: The secret menu line numbers moved from 2968 to 3009 after PR #20. Re-check the line numbers in the brief against the code on each review.

## Open questions to research next
(The web-fetch budget ran out on 2026-11-05 before these could be opened.)
- The Android TV publishing checklist (https://developer.android.com/training/tv/publishing/checklist): reopen it and add any test-procedure details (for example how to test overscan).
- The developer.android.com adb page: exact `screenrecord --time-limit` (default 180 s?) and `install -d` rules for release apps on Android 14.
- Sources on writing test cases for non-technical users (TestLodge, Marker.io) and on UAT scripts: confirm or extend section 1.
- Elisabeth Hendrickson's Test Heuristics Cheat Sheet: interruption and timing heuristics for section 3.
- The TV-ME memory limits for low-RAM devices (the exact MB values) and how the owner could see memory (Settings, then Storage/Memory on Xiaomi).
- Does the Xiaomi Android TV 14 screensaver (Ambient Mode) start over the video wallpaper? How do you trigger it quickly for testing?
- How do you make the box go to sleep and wake for a test (power button behaviour on this remote)?

## Source library
All checked 2026-11-05 unless noted.
- [tv-quality] https://developer.android.com/docs/quality-guidelines/tv-app-quality: all the TV-xx criteria (D-pad, Back, overscan, playback, Ambient Mode, memory). It has no T-xx procedures.
- [sfdpo] https://www.stickyminds.com/article/how-do-you-spell-testing: James Bach's SFDPO heuristic for test ideas.
- [a15] https://developer.android.com/about/versions/15/behavior-changes-15: BOOT_COMPLETED FGS rules, SAW FGS visibility, audio focus. These explain why the reboot and adhan tests matter.
- [install-errors] https://bayton.org/android/resolve-app-install-errors: VERSION_DOWNGRADE, UPDATE_INCOMPATIBLE and other errors he might see in Update TV.
- [adb-gist] https://gist.github.com/Pulimet/5013acf2cd5b28e55036c82c91bd56d8: screencap, screenrecord, logcat, keyevents, pm clear.
- [tvlauncher-noroot] https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/: set-home-activity and how to verify it, why the accessibility service is needed for Home, the Xiaomi KEYCODE_ALL_APPS behaviour, and undo.
- [a11y-gist] https://gist.github.com/mrk-han/67a98616e43f86f8482c5ee6dd3faabe: how accessibility services are enabled and overwritten through settings.
- [downgrade-forum] https://forum.level1techs.com/t/downgrading-app-on-android-12/179457: a user's report that `-r -d` cleared the downgrade error but hit a permission-model failure. Downgrades are fragile.
- From the brief (checked by the first reviewer, not reopened in this session): https://developer.android.com/training/tv/publishing/checklist and https://developer.android.com/about/versions/14/changes/fgs-types-required.
