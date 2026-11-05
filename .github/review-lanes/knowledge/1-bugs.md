# Lane 1 knowledge: Bugs and correctness

## How to use this file

- Read this whole file before every review, then read the brief `../1-bugs.md`.
- Research online anything the PR touches that this file does not cover (a new API, permission, library version or Android behaviour). Prefer primary sources: developer.android.com, source.android.com, AOSP code.
- After every review, send the PR's thread the additions for this file so they get committed with the PR:
  - **Past findings:** one dated block for the PR (Must, Should, Nice, and what was fixed).
  - **Lessons learned:** what you would look for sooner next time.
  - **Source library:** each new source with its URL, what it is good for, and the date you checked it.
  - **Open questions:** anything you could not settle.
- Keep entries dated and short. If the file grows past about 400 lines, condense the older entries and keep the rules.
- Cite only sources you actually opened. Mark anything unverified as *(unverified)*.

## Core expertise

### 1. Clocks and time (the most common source of bugs in this repo)
- `System.currentTimeMillis()` is the wall clock. "The wall clock can be set by the user or the phone network ... so the time may jump backwards or forwards unpredictably." Use it only for real dates and times. [SystemClock]
- `SystemClock.uptimeMillis()` stops in deep sleep. It is the time base for `Handler.postDelayed`: "Time spent in deep sleep will add an additional delay to execution." It is also the base for `Thread.sleep` and `Object.wait`. [SystemClock, Handler]
- `SystemClock.elapsedRealtime()` includes deep sleep and is monotonic. It is "the recommend basis for general purpose interval timing". [SystemClock]
- **Rule for deadlines** (PIN lockout, extra time, rest timers): store a wall-clock deadline only if it is clamped on read *and* the clamped value is written back. A tamper-proof option is `elapsedRealtime()` plus `Settings.Global.BOOT_COUNT`. Lesson from PR #20: capping only the returned value still leaves the stored value wrong.
- **Rule for "do X at time T"** (adhan, reminders, bedtime): never test for equality with one minute. Use "now ≥ T, now < T + window, and not yet done today". Persist "done today" so a restart inside the window cannot replay it (PR #20).
- `Handler` loops do not run while the CPU sleeps. A box in standby with its screen off may suspend, so a 1-second tick can come back minutes late. *(How the Xiaomi box behaves in standby is unverified, see Open questions.)*
- The system sends `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` (with `EXTRA_TIMEZONE`) and `ACTION_TIMEZONE_OFFSET_CHANGED`. These are protected broadcasts. Code that caches "today" or a computed schedule should recompute when they arrive, or recompute on every tick as AdhanService does with `cachedDay`. [Intent]
- RTC alarms: "if the user changes the device's time setting, it could cause unexpected behavior in your app." [Alarms]
- Build every `Calendar` with an explicit zone. Kuwait (`Asia/Kuwait`) is UTC+3 with no DST, so DST bugs stay hidden here. Code that is generic about zones must still not assume a day is 24 hours.
- `UsageStatsManager.queryEvents(begin, end)` takes Unix (wall-clock) times. After a backwards clock jump, a window built from a saved "last query" time can be empty or have `begin > end`. The foreground value then goes stale and does not move. [UsageStatsManager]

### 2. Activity lifecycle, launch modes and the HOME launcher
- `onDestroy()` "is not guaranteed to be called when the system kills a process". Don't rely on it to save state. [Process lifecycle]
- A recreated activity leaves the old instance's `Handler` posts running unless `removeCallbacksAndMessages(null)` runs in `onDestroy`. `Handler.sendMessageDelayed` returns false "if the looper processing the message queue is exiting". [Handler]
- `singleTask`: "If an instance exists, it receives the intent via `onNewIntent()`, and activities above it are destroyed." With `singleTop` or `singleTask`, Back doesn't go back to the state before `onNewIntent`. Intent flags override the manifest `launchMode`. [Tasks]
- `getIntent()` still returns the *original* intent after `onNewIntent` unless the code calls `setIntent(intent)`. Code that later reads `getIntent()` extras (for example in `onResume`) sees stale extras. *(Standard API behaviour. Not on the Tasks page.)*
- Extras have to be handled in both paths. A cold start delivers them in `onCreate` through `getIntent()`, and a warm start delivers them in `onNewIntent`.
- From Android 12, Back on a root launcher activity (MAIN + LAUNCHER) moves the task to the background instead of finishing it. [Tasks]
- On Android 15 for targetSdk 35: "If a non-top activity finishes its task, Android goes to the home screen". Apps "can't launch other apps' activities into your task". [B15]
- Our activities declare `configChanges` for keyboard, navigation, orientation, screenSize, screenLayout and uiMode. Any other change (`locale`, `fontScale`, `density`) still recreates the activity, so recreation must be safe.
- A HOME launcher is kept longest in the cached list ("generally keeps the home app and the last-seen activity longest"), but it can still be killed. [Process lifecycle]

### 3. Services and foreground services (FGS)
- After `startForegroundService()` the service has about 5 seconds to call `startForeground()`. If it doesn't, the system throws `ForegroundServiceDidNotStartInTimeException` (shown as an ANR or a crash). [ANR, FGS troubleshooting]
- targetSdk 34+ must declare a type for every FGS. `specialUse` needs `FOREGROUND_SERVICE_SPECIAL_USE` and a `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">`. It has no runtime prerequisites. [FGS types, B14]
- Background FGS starts have been blocked since Android 12. Exemptions include BOOT_COMPLETED, LOCKED_BOOT_COMPLETED and MY_PACKAGE_REPLACED receivers, `ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED` and `ACTION_LOCALE_CHANGED` receivers, a visible activity, user interaction, battery optimisation turned off, exact alarms, and holding SYSTEM_ALERT_WINDOW. On targetSdk 35 the SYSTEM_ALERT_WINDOW exemption also needs a currently visible `TYPE_APPLICATION_OVERLAY` window. [FGS bg-start, B15]
- Android 15 with targetSdk 35: BOOT_COMPLETED receivers cannot start `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` or `microphone` FGS. `specialUse` is not on the list. Testing commands: `am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS <pkg>` and `am compat enable FGS_SAW_RESTRICTIONS <pkg>`. [B15]
- Android 15: `dataSync` and `mediaProcessing` may run for 6 hours per 24 hours, then `onTimeout(int,int)` fires. If `stopSelf()` isn't called it crashes with `RemoteServiceException`. `shortService` gets about 3 minutes. [B15, FGS types]
- `START_STICKY`: after the process dies, `onStartCommand` is called again with a **null** intent. [Service]
- `onStartCommand` runs on the main thread, so no network or heavy disk work goes there. [Service]
- Errors to expect when starting an FGS: `ForegroundServiceStartNotAllowedException` (started from the background with no exemption) and `SecurityException` (missing a type permission). Wrapping the start in `catch (Exception ignored)` hides both, so the family only sees "it stopped working". [FGS troubleshooting]
- A service doing long-running work may be demoted after about 30 minutes or more if it is *not* in the foreground. [Process lifecycle]

### 4. Boot, stopped state and updates
- `BOOT_COMPLETED` is "broadcast once, after the user has finished booting". When it arrives, the user is unlocked and both storage types are readable. `LOCKED_BOOT_COMPLETED` comes earlier, for `directBootAware` components. [Intent, Direct boot]
- Android 15 delivers `BOOT_COMPLETED` again "when it is removed from the Stopped state", for example on the first launch after a force-stop. On Android 15 the system also "cancels all pending intents when the app enters the stopped state". The box runs Android 14, so this matters only after an OTA. [Intent, B15-all]
- Alarms are wiped at shutdown and have to be rescheduled from BOOT_COMPLETED. That only works "if the app has already been launched by the user at least once". [Alarms]
- `MY_PACKAGE_REPLACED` arrives after an update installs. Our two `BootReceiver`s handle it.
- The gap between boot and BOOT_COMPLETED can be tens of seconds on a slow box. Shared-storage media may not be readable yet, which is why `onPlayerError` retries.

### 5. Alarms, Doze, standby
- Doze defers ordinary alarms and jobs, ignores wake locks, and cuts network access. `setExactAndAllowWhileIdle` and `setAndAllowWhileIdle` fire in Doze, but at most "once per nine minutes, per app". `setAlarmClock` always fires: "the system never adjusts their delivery time". [Doze, Alarms]
- Doze starts only when the device is "unplugged, stationary, and the screen is off", and "connecting a charger exits Doze". A mains-powered TV box should rarely enter Doze, but OEM standby can still suspend the CPU *(unverified for this box)*. [Doze]
- On Android 14, `SCHEDULE_EXACT_ALARM` is denied by default for fresh installs of apps targeting 13+. Check `canScheduleExactAlarms()` first, or a `SecurityException` follows. `USE_EXACT_ALARM` is granted automatically but limited to alarm and calendar use cases. If the permission is revoked, all exact alarms are cancelled and the app stops. [B14-all, Alarms]
- An inexact alarm on 12+ can be up to an hour late. A `setWindow` shorter than 10 minutes is typically clipped to 10. [Alarms]
- A second alarm with the same PendingIntent replaces the first, and a `FLAG_ONE_SHOT` alarm can't be cancelled. [Alarms]

### 6. Threading, executors, Handler
- `ThreadPoolExecutor.execute` throws `RejectedExecutionException` (the default `AbortPolicy`) after `shutdown()`, or when a bounded pool is full. Thrown on the UI thread, it crashes the app. Guard it (`runBg` in MainActivity does) or check `isShutdown()`. [TPE]
- `shutdownNow()` interrupts the running task. Blocking I/O inside a task (`HttpURLConnection`) may ignore the interrupt, so the task can still post back to a dead activity. Check that views and the activity are alive (`isDestroyed()`) inside `ui.post`.
- Views are touched only on the main thread. Results from `bg` go back through `ui.post`.
- Fields written on `bg` and read on the UI thread need `volatile` or a post. The repo uses `volatile` for `tempNow`, `appCache` and `hiLo`, so new shared fields should follow the same pattern.
- A non-static inner `Runnable` or `Handler` posted with a long delay holds the Activity until it runs, which leaks it. A self-reposting loop that is never removed leaks it forever.
- If a self-reposting `tick()` throws before reaching its `postDelayed`, the loop stops for good. An uncaught exception in a service's tick crashes the process, and START_STICKY restarts it later. Put the repost first, or wrap the body.
- Android 15: network requests started "outside a valid process lifecycle" (from a cached process) throw `UnknownHostException` or `IOException`. [B15-all]

### 7. ANR
- Input not handled in 5 s, `startForeground` not called in 5 s, a BroadcastReceiver taking too long (10 s if the app has a foreground activity), a service's `onCreate` or `onStartCommand` taking "a few seconds". Common causes are main-thread I/O, binder calls, lock contention and deadlock. [ANR]
- After `goAsync()` the receiver must call `finish()`. Long receiver work goes to WorkManager or a service. Threads started from `onReceive` can be killed once it returns. [ANR, Process lifecycle]
- Background ANRs are shown only with "Show all ANRs" turned on in Developer options, so testing often misses them. [ANR]
- StrictMode catches accidental disk and network work on the main thread, and from Android 15 `detectUnsafeIntentLaunch()` too. [StrictMode, B15]

### 8. SharedPreferences
- `apply()` updates memory at once and writes to disk asynchronously, with no failure report. "Pending apply() calls can block the main thread during component lifecycle transitions, which is a common source of ANR errors". `commit()` blocks until done. When two editors run at the same time, "the last one to call apply wins". [SP.Editor]
- Never modify the set returned by `getStringSet()`: "The consistency of the stored data is not guaranteed". Copy it into `new HashSet<>(...)` first, as AdhanService does. [SP]
- "This class does not support use across multiple processes." The three apps are separate processes, so they must share data through the provider, never by reading another app's prefs. [SP]
- The first read waits until the file has loaded from disk, which blocks the main thread on slow storage. Every `apply()` rewrites the whole XML file *(rewrite cost: from general knowledge, the xckevin article does not confirm it)*. Writing every 1.5 s is fine for a few keys, but per-app stats that grow without bound make each write slower. [xckevin]

### 9. Media: ExoPlayer (media3 1.4.1) and MediaPlayer
- ExoPlayer: `onPlayerError(PlaybackException)` is called "immediately before the playback state transitions to `Player.STATE_IDLE`". To retry, call `prepare()` again. The cause can be `HttpDataSourceException` or `InvalidResponseCodeException`. [Exo events]
- Release each player once. After `release()`, every call is a bug. On a 2 GB box, keeping two video decoders alive fails, so free the decoder in `onStop`.
- MediaPlayer is a state machine, and calls in the wrong state move it to the Error state. "You must call release() once you have finished using an instance", and "if a MediaPlayer instance is garbage collected, release() will be called, causing any ongoing playback to stop", so keep a strong reference. [MediaPlayer]
- Audio focus: on Android 15 with targetSdk 35, "Apps ... must be the top app or run a foreground service to request audio focus", otherwise the call returns `AUDIOFOCUS_REQUEST_FAILED`. AdhanService is an FGS, so this is fine. A new player in a non-FGS background path would fail silently. [B15]

### 10. UsageStatsManager / UsageEvents
- Needs the `PACKAGE_USAGE_STATS` app-op, which the user grants in Settings under Usage access. Without it, queries return empty data or null and do not throw. Check the op with `AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, ...)`, as KidsActivity does. [UsageStatsManager]
- `queryEvents`: "Events are only kept by the system for a few days". From Android 11, it returns **null** while the user is locked (`isUserUnlocked()` false). Null-check it. [UsageStatsManager]
- `ACTIVITY_RESUMED` (= the old `MOVE_TO_FOREGROUND`) "is reported for each activity that moves to foreground". The matching `ACTIVITY_PAUSED` maps to `onPause`. `DEVICE_SHUTDOWN` means every activity stopped, with no separate STOPPED events. A picture-in-picture window or an overlay can play without a new RESUMED event *(PiP case unverified on TV)*. [UsageEvents.Event]
- Events can arrive a few seconds late. Engineering write-ups report 1 to 3 s *(Stack Overflow archive, not primary)*.

### 11. AccessibilityService key filtering
- `onKeyEvent` sees keys "before they are passed to the device policy, the input method, or applications". Returning true consumes the key. [AccessibilityService]
- "Handling the down event but not the up event and vice versa would generate an inconsistent event stream." Consume both down and up, which is the job of `swallowUp`. [AccessibilityService]
- The events are copies. Changing them has no effect, and the method is "intended to perform purely filtering functionality". To remap a key, consume it and start the action. [AccessibilityService]
- `onKeyEvent` runs on the main thread, so a slow handler delays every key press on the box. Static `volatile` state shared with the activity (`learning`, `learnedCb`) must be safe when the service and the activity run in the same process.
- `disableSelf()` exists. If the service crashes repeatedly, the user may have to switch it on again *(crash auto-disable behaviour unverified)*. [AccessibilityService]

### 12. Overlays (WindowManager)
- `addView` with `TYPE_APPLICATION_OVERLAY` needs `Settings.canDrawOverlays`. Calling `removeView` on a view that was never added throws `IllegalArgumentException`. Keep the "shown" flags exactly in step with what is on screen, and set the flag only after `addView` succeeds.
- From Android 12, touches through untrusted overlays are blocked. SYSTEM_ALERT_WINDOW overlays with opacity ≥ 0.8 block touches passed through them. This matters less on a D-pad TV. [B12-all]

### 13. Common Java pitfalls in this codebase
- NPE: `getStringExtra` returns null, `getLaunchIntentForPackage` returns null, and `queryEvents` can return null.
- Index errors: `list.get(Math.min(i, size-1))` still throws when the list is empty (`size-1 = -1`).
- `String.format` with `%0` throws on Android 15. Use `Locale.US` for digits so Arabic locales don't change the output. [B15]
- `Arrays.asList(...).toArray()` is `Object[]` on Android 15, so casting it throws `ClassCastException`. [B15]
- `HttpURLConnection.getInputStream()` throws on 4xx and 5xx. `get()` doesn't check the response code, so an error page becomes a JSON parse error, caught as an exception.
- TLS 1.0 and 1.1 are disabled for targetSdk 35. [B15]

## This box and this code

Verified on 2026-11-05 at commit 47e8c57, unless marked otherwise.
- Box: Xiaomi TV Box S 3rd Gen, Android TV 14, 2 GB RAM, no root *(from the owner)*. Build: `minSdk 28` for app and adhan, `minSdk 33` for kids, `targetSdk 35` for all (`*/build.gradle.kts`). media3-exoplayer 1.4.1 (`app/build.gradle.kts:41`).
- `MainActivity` is `singleTask`, exported, with HOME and LAUNCHER filters. `onDestroy` now runs `ui.removeCallbacksAndMessages(null)` first (`MainActivity.java:428-437`). Fixed in #20.
- `runBg()` catches `RejectedExecutionException` (`MainActivity.java:128-130`). `blurThread` is declared and shut down but no `execute` call was found by grep, so check how it is used before flagging.
- Extras: `onCreate` reads only `panel=pin` (`:271`). `onNewIntent` reads `probe` and then `panel` (`apps`, `quran`, `pin`, `calendar`, `stress`, `reciters`, `player`) after 400 ms (`:300-322`). `onNewIntent` calls `dockTiles.get(Math.min(lastDock, dockTiles.size()-1))`, which throws if `dockTiles` is empty.
- `get()` uses 8 s connect and read timeouts and does not check the response code (`:1629-1641`).
- The wallpaper `onPlayerError` retries up to 12 times (`:259-264`). A successful start resets `clipErrors` (`:255`). The Quran player gives up after 3 errors (`:2860-2864`).
- `PinLock.java` is identical in `app/` and `kids/` apart from the package line. `waitLeft` writes back a clamped `pin_until`. It uses one salted SHA-256 hash and `MessageDigest.isEqual`. Prefs writes use `apply()`.
- `Guard` (kids): `specialUse` FGS, `startForeground` in `onCreate`, `START_STICKY`, a 1.5 s tick. Its first query looks back 6 h, and later queries start at `lastEventQuery - 2000`. `allowed("")` returns true (`Guard.java:135-139`). `delta` is clamped to 5000 ms. `bringBack` is throttled to 2 s. `start()` swallows every exception (`:56`).
- Kids Corner shows the setup line "Without app watching the kids lock can't send him back" when usage access is missing (`KidsActivity.java:1733-1745`).
- `AdhanService`: `fired` is saved per day in prefs (`fired_day`, `fired`) and reloaded on the day change (`:435-447`). The adhan fires when `today[i] ≤ nowMin < today[i]+LATE_MIN` (`LATE_MIN = 3`, `:373`, `:481`). The reminder fires 10 minutes before, also with a 3-minute window. The tick re-aligns to the second (`:514`). Audio focus is `AUDIOFOCUS_GAIN_TRANSIENT` (`:572`).
- No receiver for `TIME_CHANGED` or `TIMEZONE_CHANGED` anywhere. Every tick recomputes from `Calendar.getInstance(KUWAIT)`, so a clock change is picked up on the next tick.
- `Rules.extra_until` is a wall-clock deadline (`Rules.java:98-101`). A backwards jump lengthens extra time and a forward jump ends it early. It is not clamped like `pin_until`.
- Both `BootReceiver`s handle `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`, and the kids one restarts `Guard` only if `Rules.on`.
- `PrayerTimes.java` is duplicated in `app/` and `adhan/`.

## Past findings

### PR #20 (merged 2026-11-05): PIN limits, crash after restart, missed adhan, Quran text
- Must: none.
- Should (both fixed): `PinLock.waitLeft` capped only the value it returned, not the stored `pin_until`, so it now clamps the stored value. The adhan `fired` set lived only in memory, so a restart inside the 3-minute late window replayed the adhan. It is now saved per day in prefs.
- Nice: 20k SHA-256 rounds ran on the UI thread (fixed: now a single salted hash). `ayahToday` keeps yesterday's verse if all 12 tries are too long (not fixed).
- Found by the original research and fixed in #20: the `MainActivity.onDestroy` tick crash (`RejectedExecutionException` after recreation).

## Lessons learned
- 2026-11-05: For every stored deadline, check the stored value, not only the value returned. Ask what a clock jump forward, a jump back, and a reboot each do.
- 2026-11-05: For every "happens once" event, ask what a process restart inside its window does. Memory-only flags replay.
- 2026-11-05: Check that hashing and other crypto work stays off the UI thread or is cheap.
- 2026-11-05: Recheck the brief's "Known risks" against the code before citing them. Several were fixed in #20 (onDestroy, exact-minute adhan, Kids PIN with no limit).

## Open questions to research next
- Does the Xiaomi TV Box S 3rd Gen suspend the CPU in standby (screen off by the remote)? If it does, `Handler` ticks stop and an adhan due while the box is in standby is missed, as the 3-minute window shows. Check with the owner (lane 8) and `dumpsys deviceidle` and `dumpsys power`.
- Does Android TV 14 on this box send BOOT_COMPLETED to a force-stopped app? (On Android 14, stopped apps get no broadcasts until they are launched.)
- Does YouTube or Kodi on Android TV use picture-in-picture, so that it can keep playing with no new `ACTIVITY_RESUMED`?
- media3 1.4.1 compared with the latest release: are there known `SurfaceView` or loop bugs on Amlogic decoders?
- `Rules.extra_until` and a wall-clock jump: is clamping needed, in the same way as `pin_until`?

## Source library

All checked 2026-11-05 unless noted. The short tags in [brackets] are used above.
- [SystemClock] https://developer.android.com/reference/android/os/SystemClock: the definitions of the three clocks and what each is for.
- [Handler] https://developer.android.com/reference/android/os/Handler: `postDelayed` uses uptimeMillis, and deep sleep adds delay. What a false return means.
- [Intent] https://developer.android.com/reference/android/content/Intent: BOOT_COMPLETED semantics (including the Android 15 stopped-state redelivery) and the TIME/TIMEZONE broadcasts.
- [Direct boot] https://developer.android.com/privacy-and-security/direct-boot: LOCKED_BOOT_COMPLETED and `directBootAware`.
- [B15] https://developer.android.com/about/versions/15/behavior-changes-15: Android 15 FGS boot and SAW limits, dataSync timeouts, BAL, audio focus, TLS, the OpenJDK format and toArray changes.
- [B15-all] https://developer.android.com/about/versions/15/behavior-changes-all: stopped state cancels PendingIntents, and background network requests throw.
- [B14] https://developer.android.com/about/versions/14/behavior-changes-14: FGS types required, implicit intents, `RECEIVER_EXPORTED`, BAL opt-in.
- [B14-all] https://developer.android.com/about/versions/14/behavior-changes-all: exact alarms denied by default, cached-app broadcast queueing, killBackgroundProcesses.
- [B12-all] https://developer.android.com/about/versions/12/behavior-changes-all: untrusted touches blocked through overlays.
- [FGS bg-start] https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start: the full list of background-start exemptions.
- [FGS types] https://developer.android.com/develop/background-work/services/fgs/service-types: specialUse, mediaPlayback, shortService.
- [FGS troubleshooting] https://developer.android.com/develop/background-work/services/fgs/troubleshooting: DidNotStartInTime, StartNotAllowed, SecurityException.
- [Service] https://developer.android.com/reference/android/app/Service: null intent on a START_STICKY restart, main thread, onTimeout (API 35).
- [Alarms] https://developer.android.com/training/scheduling/alarms: alarm types, exact-alarm permissions, reboot, Doze, PendingIntent replacement.
- [Doze] https://developer.android.com/training/monitoring-device-state/doze-standby: Doze and standby rules, the 9-minute AllowWhileIdle limit, adb test commands.
- [ANR] https://developer.android.com/topic/performance/vitals/anr: ANR thresholds, causes, traces, "Show all ANRs".
- [Process lifecycle] https://developer.android.com/guide/components/activities/process-lifecycle: importance order, onDestroy not guaranteed, receiver threads.
- [Tasks] https://developer.android.com/guide/components/activities/tasks-and-back-stack: launch modes, flags, Android 12 root back.
- [Background starts] https://developer.android.com/guide/components/activities/background-starts: BAL exemptions. A plain FGS is *not* one, and SYSTEM_ALERT_WINDOW is.
- [TPE] https://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor: rejection policies.
- [SP.Editor] https://developer.android.com/reference/android/content/SharedPreferences.Editor: apply vs commit, the ANR caution.
- [SP] https://developer.android.com/reference/android/content/SharedPreferences: getStringSet must not be modified, not multi-process.
- [xckevin] https://xckevin.github.io/en/blog/android-sharedpreferences-datastore-deep-dive/: the QueuedWork internals behind apply() ANRs (secondary source).
- [Exo events] https://developer.android.com/media/media3/exoplayer/listening-to-player-events: onPlayerError and retry with prepare().
- [MediaPlayer] https://developer.android.com/reference/android/media/MediaPlayer: state machine, release, GC.
- [StrictMode] https://developer.android.com/reference/android/os/StrictMode: what it detects.
- [UsageStatsManager] https://developer.android.com/reference/android/app/usage/UsageStatsManager: permission, retention, null when locked.
- [UsageEvents.Event] https://developer.android.com/reference/android/app/usage/UsageEvents.Event: ACTIVITY_RESUMED, PAUSED and DEVICE_SHUTDOWN semantics.
- [AccessibilityService] https://developer.android.com/reference/android/accessibilityservice/AccessibilityService: onKeyEvent ordering, well-formed streams, copies.
- From the brief, not reopened this round: https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog (UsageStats polling pitfalls) and https://www.androidauthority.com/google-tv-update-blocks-projectivy-override-3649445 (a Google TV update broke custom HOME overrides).
