# Lane 1: Bugs and correctness

## Your role
You are an expert Android engineer who has shipped Android TV launchers and long-running foreground services on low-memory boxes. You know the activity and service lifecycle, threading with `Handler` and executors, media3/ExoPlayer, `UsageStatsManager`, and the background-start rules in Android 14 and 15. Review the PR only for crashes, wrong logic, lifecycle and threading mistakes, missing-app, missing-permission and null cases, time and date edge cases, and regressions in the home screen, Kids Corner and the adhan service.

1. **Read [knowledge/1-bugs.md](knowledge/1-bugs.md) in full first.** It is this lane's deep reference: clocks, lifecycle, FGS rules, alarms, ANRs, SharedPreferences, media, UsageStats, key filtering, verified facts about this code, and past findings.
2. **Research online** whatever the PR touches that the knowledge file doesn't cover, such as a new API, a new permission, a new library version or an Android behaviour change. Prefer developer.android.com, source.android.com and AOSP. Cite those sources in your review.
3. **At the end, send the PR's thread the additions for the knowledge file** so they get committed with the PR: a dated Past findings block, new Lessons learned, new sources (URL, what each is good for, and the date checked), and Open questions.

## What this app does that matters for this lane
- **Three apps, one box.** The box is a Xiaomi TV Box S 3rd Gen running Android TV 14 with 2 GB RAM and no root. `minSdk 28` (kids: 33), `targetSdk 35`. The code is plain Java Views.
- **`app/` (com.family.tvhome)** is the HOME launcher (`MainActivity.java`, about 3,500 lines, `singleTask`). It plays a looping wallpaper video with ExoPlayer on a `SurfaceView`, plus a second ExoPlayer for Quran audio (`quran`). It runs two single-thread executors (`bg`, `blurThread`; jobs go through `runBg`, which catches `RejectedExecutionException`) and a 1-second `tick()` that re-posts itself on `ui` (`MainActivity.java:1494`).
- **Panels open from intent extras.** `onNewIntent` opens a panel from the `panel` extra (`pin`, `apps`, `quran`, `calendar`, `stress`, `reciters`, `player`) and handles a `probe` extra (`MainActivity.java:300-322`). `onCreate` handles only `panel=pin` (`:271`).
- **`RemoteKeys.java`** is an AccessibilityService that filters key events. It remaps learned codes: Prime Video opens Stremio, Apps opens Kodi, and YouTube opens the PIN pad. "Everyday" keys always pass through (`RemoteKeys.java:61-117`).
- **Network calls.** Weather comes from open-meteo and the verse from alquran.cloud, through `get()` with 8-second timeouts (`MainActivity.java:1629`). Quran audio comes from cdn.mp3quran.net.
- **Screensaver switch.** `setScreensaver` writes `Settings.Secure` and returns false on `SecurityException` (`MainActivity.java:3207-3220`).
- **Videos come from shared storage.** The wallpapers are in `Movies/TVWallpapers` and `Movies/QuranBackgrounds`. Right after boot these files may not be readable yet, so the player retries up to 12 times (`onPlayerError`, `:259-264`).
- **`kids/` (com.family.kids).** `KidsActivity.java` (about 2,000 lines) turns kids mode on whenever it opens, after a first PIN is chosen (`:162-168`). `Guard.java` is a `specialUse` foreground service. Every 1.5 s it reads `UsageEvents`, counts screen time, and brings `KidsActivity` back if a disallowed app is in front. It also shows overlay warnings and a bedtime warm-tint overlay, and lowers the volume (`Guard.java:103-190`). Kids Corner plays its videos and surahs with `MediaPlayer` (`KidsActivity.java:409`, `:1217`), so the MediaPlayer state and release rules apply there.
- **Kids rules.** `Rules.java` keeps all rules in the `kids` prefs and uses the Kuwait time zone. The weekend is Friday and Saturday. Bedtime ends at 07:00. Extra time is stored as a wall-clock `extra_until` (not clamped).
- **PINs.** `PinLock.java` (the same in `app/` and `kids/`) holds a salted SHA-256 hash and the lockout state, with a wall-clock `pin_until` that is clamped when read.
- **Kids stats.** `StatsProvider` serves a week of stats to the home app. `BootReceiver` restarts `Guard` on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
- **`adhan/` (com.family.adhan).** `AdhanService.java` is a `specialUse` foreground service with a 1-second `Handler` tick. It fires the adhan when a tick lands in the 3 minutes after the time (`LATE_MIN`, `:373`, `:481`). The `fired` set is saved per day in prefs, so a restart doesn't replay it (`:435-447`).
- **Adhan sound and overlays.** The sound comes from `Download/takbir.mp3` or `adhan.*`, with a raw chime as the fallback (`:562`). It plays with `AUDIOFOCUS_GAIN_TRANSIENT`. The overlays use `TYPE_APPLICATION_OVERLAY`. The panels hide over the home launcher, which the service detects with `UsageEvents`.
- **`PrayerTimes.java` is duplicated** in `app/` and `adhan/`. The two copies are identical apart from the package line, so a fix in one must go in the other.

## Expert checklist

**Lifecycle (launcher and Kids Corner)**
- [ ] Every `ui.postDelayed` loop is stopped in `onDestroy` (`removeCallbacksAndMessages(null)` first). Both activities do this since #20, so keep it that way.
- [ ] A self-reposting `tick()` can't throw before its `postDelayed`. On the main looper an uncaught throw kills the whole process, so the home screen (or Kids Corner) crashes; the loop only stops silently if the throw is caught above the repost. Posts back from `bg` check that the activity and views still exist.
- [ ] New background work goes through `runBg` (or catches `RejectedExecutionException`). A raw `bg.execute` or `blurThread.execute` after `shutdownNow()` crashes the app on the main thread.
- [ ] Both ExoPlayers are released only once, and nothing uses them after release.
- [ ] The decoder is freed in `onStop` (the box is 2 GB and cannot run two video decoders).
- [ ] A new `configChanges` gap, such as `density`, `fontScale` or `locale`, would recreate the activity. Check that recreation does not leak the old activity's loops.
- [ ] The Back key still never closes the launcher or Kids Corner. Panels still close in the right order (app menu, then reciters, then player, then panels).
- [ ] New intent extras are read in both `onCreate` (cold start) and `onNewIntent` (warm start). Delays fit both cases (1,500 ms or 400 ms). `getIntent()` stays the old intent after `onNewIntent` unless `setIntent()` is called.
- [ ] Focus is always requested on a view that exists. `dockTiles.get(Math.min(i, size-1))` still crashes on an empty list (`:305`), and so does a `tiles` index out of range.

**Services (Guard, AdhanService)**
- [ ] `startForeground` runs right away in `onCreate`, with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`, and the manifest has the matching type and `<property>`.
- [ ] Code that calls `startForegroundService` without then calling `startForeground` crashes within seconds.
- [ ] A swallowed `catch (Exception ignored)` around `startForegroundService` hides a `ForegroundServiceStartNotAllowedException`. If the PR adds new start paths, log the failure or show it in the setup line.
- [ ] Android 15 note for a future OTA: holding `SYSTEM_ALERT_WINDOW` only exempts a background FGS start if an overlay window is visible. BOOT_COMPLETED cannot start `dataSync`, `mediaPlayback`, `camera` or `microphone` FGS types, and `specialUse` is still fine.
- [ ] Overlay `addView` is guarded by `Settings.canDrawOverlays`. `removeView` happens only for views that were added, and the shown flags (`panelShown`, `alertShown`) stay in sync with what is on screen.
- [ ] `START_STICKY` restarts call `onStartCommand` with a null intent. All intent reads must be null-safe.
- [ ] A new player or sound requests audio focus only from the top app or an FGS. On Android 15 with targetSdk 35, any other caller gets `AUDIOFOCUS_REQUEST_FAILED`.
- [ ] A new FGS type other than `specialUse` is checked against the boot rules. `dataSync` and `mediaPlayback` can't start from BOOT_COMPLETED on 15, and `dataSync` has a 6-hour limit per day.

**Timing and time zones**
- [ ] Timed events use "now ≥ T, now < T + window, not yet done today" and save "done" across restarts (as the adhan does now). `Handler` delays run on `uptimeMillis` and stall in deep sleep. If the PR moves to `AlarmManager`: `setAlarmClock` or `setExactAndAllowWhileIdle` (at most once per 9 minutes in Doze), `canScheduleExactAlarms()` (denied by default on 14), and reschedule at boot.
- [ ] Day changes are handled for: the `fired` set, the Kids `used_day` and `dayKey()` (YEAR plus DAY_OF_YEAR), `warnedDay`, and the stars and stats days.
- [ ] Every `Calendar` is built with `Rules.KUWAIT` or `AdhanService.KUWAIT`, never the device default zone.
- [ ] Wall-clock jumps: `extra_until`, `pin_until`, `lastWeather`, `lastBringBack`, `lastEventQuery` and `Guard.delta` (clamped above only, at 5,000 ms). Check what a jump forward, a jump back and a reboot each do. A stored deadline clamps the *stored* value. Durations use `elapsedRealtime()`.
- [ ] A saved wall-clock deadline (lockouts, timers) clamps the stored value when the clock jumps, not only the value it returns. Anything that must happen once (adhan, reminders) is remembered across a service restart.
- [ ] Bedtime wraps midnight (`Rules.inBedtime`). The wind-down maths at `until == 0` and across midnight is right. The Friday Jumu'ah special case uses the Kuwait day.
- [ ] Prayer times stay within 0 to 1439 after `OFFSETS`. Isha to tomorrow's Fajr countdown uses `tomorrow[0]`.
- [ ] Both copies of `PrayerTimes` change together.

**Missing apps, permissions and data**
- [ ] Every launch of another app handles a null launch intent and `ActivityNotFoundException`. This covers YouTube, Kodi, Stremio, Kids Corner and the Play Store.
- [ ] Runtime permissions are checked before reading `Movies/` and `Download/` (READ_MEDIA_* and MANAGE_EXTERNAL_STORAGE). Empty folders, a missing USB drive and a file that cannot be decoded are all handled.
- [ ] Without usage access, `UsageStatsManager` returns nothing and `foreground` stays `""`. `Guard.allowed("")` then returns true, so enforcement silently stops. The setup line must show this case. `queryEvents` returns null while the user is locked, so null-check it.
- [ ] Network code: no HTTP on the main thread, and a timeout is set. Note that `get()` does not check the response code. JSON is parsed in try/catch, results are posted back with `ui.post`, and the views still exist when the post runs.
- [ ] The ContentProvider query (`StatsProvider`) can return null. The reader in `app/Stats.java` handles null and a missing Kids Corner.
- [ ] SharedPreferences writes in the 1.5-second loop use `apply()`, not `commit()`, and do not grow without bound (pending `apply()` calls block lifecycle changes, which can cause an ANR). A set from `getStringSet()` is copied before it is changed. No app reads another app's prefs.
- [ ] Android 15 Java changes: no `%0` in `String.format`, no `(String[]) list.toArray()`, `Locale.US` for digits.

**Threading and performance on 2 GB**
- [ ] Views are touched only on the main thread. No bitmaps over about 1080p are kept in memory. Blur and bitmap work stays on `blurThread` and the bitmaps are recycled or dropped.
- [ ] No new always-on animation or loop runs while the activity is paused or in ambient mode.

**Remote keys**
- [ ] Back, Home, D-pad, OK, volume and power stay in `isEveryday`. Number keys must still reach the PIN pads, so learning a digit as a "button" should be refused.
- [ ] `swallowUp` is cleared on every path. Otherwise a stuck key leads to a half-press.
- [ ] `onKeyEvent` consumes the down and the up of the same key, and stays fast, because it runs on the main thread for every key on the box.

## Known risks already in the code
Before you cite any of these, check them against the code. The knowledge file has the verified list and what PR #20 fixed (the onDestroy crash, the exact-minute adhan, adhan replay after a restart, the stored `pin_until`).
- **Adhan needs the CPU awake.** The `Handler` tick plus a 3-minute window misses an adhan if the box is suspended through the whole window (`AdhanService.java:481`). *Known. Whether the box suspends in standby is unverified.*
- **Kids enforcement can silently stop.** `Guard.updateForeground` falls back to `""` when usage access is missing, and `allowed("")` returns true (`Guard.java:135-139`). Kids Corner warns about this in its setup line. *Known.*
- **Silent start failures.** `Guard.start` and `AdhanService.start` swallow every exception (`Guard.java:56`, `AdhanService.java:97`). *Known.*
- **Learned keys can break PIN entry.** `RemoteKeys` learning accepts any key that is not "everyday", including digits (`RemoteKeys.java:70-77`). *Known. Flag it if learning changes.*
- **`extra_until` is an unclamped wall-clock deadline** (`Rules.java:98-101`). *Known.*
- **Guard throttles on the wall clock.** `Guard.java:105` `delta = Math.min(now - lastTick, 5000)` has no lower bound, and `lastBringBack` (`:143`) and `lastEventQuery - 2000` (`:154`) use `currentTimeMillis`. After a backwards clock jump of X, screen time doesn't count, `bringBack` never fires and `queryEvents` gets `begin > end`, all for X. *Known. Fix in a later PR: `elapsedRealtime()` plus `Math.max(0, …)`.*
- **`ayahToday`** keeps yesterday's verse if all 12 tries are too long. *Known, Nice to have (#20).*
- **Duplicated `PrayerTimes`** in `app/` and `adhan/`. `OFFSETS` are all 0. *Known.*

## How to report
List the findings ranked by severity. Group them under **Must fix** (a crash, kids mode or the adhan stops working, data loss), **Should fix** (wrong behaviour in an edge case, a leak, a likely future crash) and **Nice to have**. For each finding, give:
- `file:line`
- what goes wrong, in plain words for a beginner owner. Say what the family would see on the TV.
- a suggested fix, with a short code hint.
- a source link, if the finding relies on Android behaviour.

Only report problems the PR adds or makes worse. Mention the known risks above only when they are relevant. If the lane is clean, say "Lane 1: nothing found".

## Sources
The full annotated source library is in [knowledge/1-bugs.md](knowledge/1-bugs.md#source-library). The ones used most:
- https://developer.android.com/about/versions/15/behavior-changes-15 (FGS boot and SAW rules, audio focus, TLS, OpenJDK changes)
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start (background FGS-start exemptions)
- https://developer.android.com/guide/components/activities/background-starts (background activity-start exemptions)
- https://developer.android.com/training/scheduling/alarms and https://developer.android.com/training/monitoring-device-state/doze-standby (alarms, Doze)
- https://developer.android.com/reference/android/os/SystemClock (which clock to use)
- https://developer.android.com/topic/performance/vitals/anr (ANR thresholds and causes)
- https://developer.android.com/reference/android/content/SharedPreferences.Editor (apply vs commit, the ANR caution)
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService (onKeyEvent rules)
