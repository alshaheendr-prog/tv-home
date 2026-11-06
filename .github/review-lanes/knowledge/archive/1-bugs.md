# Lane 1 knowledge: Bugs and correctness: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
- 2026-11-06 PR #25 re-check at c031f6c: S1 fixed (inDaysAr n % 100, every case checked).

### PR #25 (2026-11-06, update 3: Islamic content)
- 2026-11-06 PR #25 (update 3): S1 Arabic day-count agreement wrong from 100 days on (fixed: n % 100); N1 sources toggle applied late (fixed); N2 dead ayahToday (removed). Offset maths, caches, rotations and the adhkar window verified.

### PR #23 (2026-11-06, update 2: visual polish)
- Must: none.
- Should:
  - `KidsActivity.wallpaperPick` erases the saved wallpaper (`wallpaper` -1, removes `wallpaper_name`) when `wallpapers()` comes back empty, which happens when storage isn't ready right after boot. Fix: if the list is empty, return -1 without writing.
  - `onResume` → `refreshTiles(true)` skips the rebuild, so the red `setupDot` on the Kids lock tile no longer updates when usage access or overlay permission changes. Fix: set its visibility in `onResume`, or put `setupComplete()` in `tilesKey`.
- Nice:
  - `pressDip` (home and Kids) returns to the scale it read mid-animation, so a tile can be left half-grown when OK is pressed during the focus grow.
  - `fadeTileIn`'s `animate().cancel()` can leave a half-finished `goldLift` on a fast close and reopen.
  - The countdown and adhan bar are gated on `resumed`, so they freeze while a translucent system panel covers the home.
- Checked and fine: `starsOn`/wallpaper hand-off (`onStop` `stop()` leaves IDLE, so `starsOver(false)` restarts), the `eventChip` move into the centre column, `nextLine` setting `lastLineSwitch`, `MixText` (API 28 `TypefaceSpan(Typeface)`, minSdk 28), the `ICONS` cache (keyed by `lastUpdateTime`, copies through `ConstantState`), adaptive icons in `mipmap-anydpi-v26`, splash and IntroView on `uptimeMillis`.

### PR #22 (2026-11-05, audit update 1)
- Must: none.
- Should:
  - The Kids Corner wallpaper retry (`clipAgain`) is cancelled in onPause and not restarted on a short pause. A short pause happens when Guard's SINGLE_TOP pull delivers a new intent while Kids Corner is already on top. `applyWorld` only plays a clip when the world changed, and `surfaceCreated` doesn't fire again. Fix: `if (player == null) playClip();` in onResume.
  - Events.badge reorder: on 9 Dhul Hijjah, "Eid al-Adha tomorrow" beats today's "Day of Arafah".
- Nice:
  - The playRandomClip `!resumed` early return leaves the cover opaque after a pause without a stop.
  - A pending `retryClips` causes a double clip change.
  - The cold-start missing-app note hides under the splash (elevation 60 over 50, splash at least 5.9 s).
  - There is no floor on the urgent Guard pull.
  - On-TV check: the takbir still plays with only READ_MEDIA_AUDIO now that MANAGE_EXTERNAL_STORAGE is removed.

### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- Must: none.
- Should: quiet Quran starts after leaving home and pauses Netflix (no `resumed` check after the bg hop); wallpaper retry and
  fade end-action restart the decoder in the background; home keeps its ExoPlayer decoder in onStop (placeholder surface);
  midnight passedMask change pauses the Quran with "Paused for the adhan"; Events.badge "Eid/Arafah tomorrow" unreachable;
  adhan sound decode error hides the banner with no chime fallback; Kids Corner wallpaper never retries after boot.
- Nice: Projectivy copier still runs; wall-clock throttles (line switch, weather, ambient, card, swap); adhan updateForeground
  begin>end; Quran.reciters caches the fallback; lastDock -1; kids player paused without ❚❚; tile width <= 0 at 30+ apps.
- Checked and NOT a bug: KidsActivity.onPause player.pause() on a preparing/failed MediaPlayer (no exception, error -38 via onError).

### PR #21 (2026-11-05, docs only)
- Must: none.
- Should: the README ready rule contradicted step 4 and CLAUDE.md; a tick throw is a process crash, not a silent stop; the Guard wall-clock throttles were missing from Known risks.
- Nice: line refs (chime `:562`, Quran player `:2861`), the orphan checklist item moved under Timing, Kids MediaPlayer mentioned, source tags fixed.

### PR #20 (merged 2026-11-05): PIN limits, crash after restart, missed adhan, Quran text
- Must: none.
- Should (both fixed): `PinLock.waitLeft` capped only the value it returned, not the stored `pin_until`, so it now clamps the stored value. The adhan `fired` set lived only in memory, so a restart inside the 3-minute late window replayed the adhan. It is now saved per day in prefs.
- Nice: 20k SHA-256 rounds ran on the UI thread (fixed: now a single salted hash). `ayahToday` keeps yesterday's verse if all 12 tries are too long (not fixed).
- Found by the original research and fixed in #20: the `MainActivity.onDestroy` tick crash (`RejectedExecutionException` after recreation).

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
- [RuntimeInit] https://git.aicp-rom.com/frameworks_base.git/plain/core/java/com/android/internal/os/RuntimeInit.java: AOSP mirror; uncaught exception handling (KillApplicationHandler, killProcess, System.exit(10)). Checked 2026-11-05.
- [CommonsWare stopped] https://commonsware.com/blog/2011/07/13/boot-completed-regression-confirmed.html: force-stop and BOOT_COMPLETED (secondary). Checked 2026-11-05.
- Rechecked 2026-11-05: FGS troubleshooting, ANR, Alarms, Doze and B15-all.
- From the brief, not reopened this round: https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog (UsageStats polling pitfalls) and https://www.androidauthority.com/google-tv-update-blocks-projectivy-override-3649445 (a Google TV update broke custom HOME overrides).
- [ExoPlayer #677] https://github.com/google/ExoPlayer/issues/677: when the surface goes away, ExoPlayer keeps the codec on a DummySurface (API 23+) instead of releasing it. Checked 2026-11-05.
- [MP -38] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/20072786/android-mediaplayer-unknow-error-38: error -38 means INVALID_OPERATION, an invalid state transition (secondary source, Stack Overflow archive). Checked 2026-11-05. The JNI route (NULL exception, so `notify(MEDIA_ERROR)`) is from memory of `android_media_MediaPlayer.cpp` *(unverified this round)*.

- [onNewIntent] https://learn.microsoft.com/dotnet/api/android.app.activity.onnewintent: a mirror of the Android reference text. It says "If the activity was resumed, it will be paused and new intent will be delivered, followed by onResume()", and that getIntent() still returns the original intent.
- [MediaPlayer state] https://developer.android.com/media/platform/mediaplayer/state-resources: release the player in onStop, create it again on resume, and stop() needs prepare() before start().

- [VPA withEndAction] https://learn.microsoft.com/en-us/dotnet/api/android.views.viewpropertyanimator.withendaction: a mirror of the Android reference. "if the ViewPropertyAnimator is canceled during that animation, the runnable will not run." Checked 2026-11-06.
- [TypefaceSpan API 28] https://developer.android.com/sdk/api_diff/28/changes/android.text.style.TypefaceSpan: the `TypefaceSpan(Typeface)` constructor was added in API 28. Checked 2026-11-06.

From PR #25 (2026-11-06, update 3: Islamic content):
- developer.android.com/reference/android/icu/util/IslamicCalendar (UMALQURA, day maths); Arabic number-noun agreement (تمييز العدد), alloschool.com/element/126391/pdf.

## Open questions to research next
- Does the Xiaomi TV Box S 3rd Gen suspend the CPU in standby (screen off by the remote)? If it does, `Handler` ticks stop and an adhan due while the box is in standby is missed, as the 3-minute window shows. Check with the owner (lane 8) and `dumpsys deviceidle` and `dumpsys power`.
- Does Android TV 14 on this box send BOOT_COMPLETED to a force-stopped app? Answered 2026-11-05: no. Force-stop puts the app back in the stopped state, which lasts across reboots until the user launches it (Hackborn via CommonsWare). Android 15 also cancels its PendingIntents and sends BOOT_COMPLETED when it is un-stopped. Update TV's `am start -S` un-stops adhan by launching it. [CommonsWare stopped] [B15-all]
- Does the box keep time across a power cut (RTC), or boot with a wrong clock until NTP syncs? That affects `fired_day`, `used_day` and the Guard throttles. (r/AndroidTV reports Xiaomi devices failing NTP after a power loss, unverified.)
- Does YouTube or Kodi on Android TV use picture-in-picture, so that it can keep playing with no new `ACTIVITY_RESUMED`?
- media3 1.4.1 compared with the latest release: are there known `SurfaceView` or loop bugs on Amlogic decoders?
- `Rules.extra_until` and a wall-clock jump: is clamping needed, in the same way as `pin_until`?
- Does the home's paused ExoPlayer really hold a hardware decoder on this Amlogic box? Check with `adb shell dumpsys media.resource_manager` and `dumpsys media.player` while Netflix plays over the home.
- The home sets `FLAG_KEEP_SCREEN_ON`. If the TV is switched off with its own remote while the home is in front, does the box ever go to standby (HDMI-CEC "auto power off")? If it doesn't, the wallpaper and the soft Quran run all night. With Kids Corner, `pm.isInteractive()` stays true, so screen time keeps counting while the TV is off.
- Should `Events.badge` follow Kuwait's moon-sighting announcement rather than Umm al-Qura? (Lane 6.)

- Does `File.canRead()` on an adb-pushed `Download/takbir.mp3` work with only READ_MEDIA_AUDIO, before MediaStore has scanned the file? It needs a test on the TV.
- How long after a bringBack does ACTIVITY_RESUMED for KidsActivity show up in queryEvents on this box? That decides how many repeat urgent pulls happen.

- Does the Google TV quick-settings side panel pause the home (as a translucent activity) on this box?
- After boot, how long until `Movies/TVWallpapers` lists as non-empty on this box? That decides how often the wallpaper erase would hit.

From PR #25 (2026-11-06, update 3: Islamic content):
- the Hijri offset is two settings (home Parent settings and Adhan settings); the owner must change both after a moon-sighting difference. Lane 8 checklist should say so.
