# Lane 2: Safety and privacy

## Your role
You are an expert in Android security and child-safety controls. You know OWASP MASVS and MASTG, Android exported-component and permission rules, AccessibilityService abuse, and the usual ways kiosk and parental-control apps get bypassed: the Back, Home and recents keys, remote buttons, voice and cast launches, intents, and revoking a permission. Review the PR only for the following: can a child, or another app on the box, get past the parent PIN, the YouTube lock or Kids Corner? Are secrets, personal data or wider permissions being exposed? Before you review, look up the current best practice online for anything the PR touches that this file does not cover, such as a new permission, component, API or library. Cite those sources in your review.

## What this app does that matters for this lane
- **The repo is PUBLIC.** The signing key `app/tvhome.keystore` is committed, and its store and key password `tvhome-family` is written in plain text in `app/build.gradle.kts:20-24`. `kids/` and `adhan/` sign with the same file (`../app/tvhome.keystore`). CI (`.github/workflows/build.yml`) publishes a public `latest` release from `main` with `contents: write`.
- **Home YouTube and parent PIN.** The PIN has 4 digits and is stored in plain text in the `home` prefs under the key `pin` (`MainActivity.java:2084`). It is compared with `equals`, with no limit on attempts and no delay after a wrong try (`:2160-2183`).
  - **No PIN yet means no lock.** Whoever opens the pad first chooses the PIN (`:2116`). Parent settings then open with no PIN at all (`:2986`).
  - **How Parent settings opens.** The secret D-pad sequence Up, Up, Down, Down opens the parent PIN (`:2975-2993`).
- **Kids Corner PIN.** This is a separate 4-digit PIN, in the `kids` prefs (`Rules.java:40-41`), checked in `KidsActivity.java:1515-1535`. It also has no attempt limit. `leave()` (`:1732`) switches kids mode off and stops `Guard`. The README says `adb shell pm clear com.family.kids` resets the PIN.
- **The YouTube lock is only a UI gate.** It works only when YouTube is opened from the dock or through RemoteKeys (`MainActivity.java:1737`, `RemoteKeys.java:89-96`). YouTube is hidden from All apps (`HIDDEN_FROM_LIST`), but outside kids mode nothing stops these routes:
  - the Play Store's "Open" button
  - Android Settings, under Apps
  - Google Assistant or voice search
  - casting from a phone
  - another launcher
- **Kids lock (`Guard.java`).** Every 1.5 s it polls `UsageEvents` (`ACTIVITY_RESUMED`). If a package that is not allowed is in front, it starts `KidsActivity` (`:103-149`).
  - **Always allowed through:** the launcher's own package, `android`, `com.android.systemui`, the permission controllers, and every IME.
  - **Never allowed:** the `Rules.NEVER` list. It includes YouTube, the Play Store, TV and phone Settings, `com.family.tvhome`, Projectivy, the Google TV launcher, browsers, Stremio and Downloader.
  - **Allowed by default:** Kodi, which can play YouTube and web content inside itself through add-ons.
- **Gaps in the kids lock.**
  - **Brief exposure.** A child sees a blocked app for up to about 1.5 to 2 s before Kids Corner comes back. A blocked app can still play sound during that time.
  - **Start-up gap.** The lock restarts only once `BOOT_COMPLETED` arrives.
  - **Silent failure.** If usage access is revoked, the lock silently stops. Settings is blocked, but the PC or ADB path can still revoke it.
- **`RemoteKeys` is an exported AccessibilityService.** It is protected by `BIND_ACCESSIBILITY_SERVICE` and has `canRequestFilterKeyEvents`, so it sees every key press. `canRetrieveWindowContent` is false. "Everyday" keys (Back, Home, D-pad, OK, volume and power) are never consumed. The service is switched on by the "Update TV" ADB script.
- **Exported components that need no permission.**
  - `MainActivity`, which is HOME. It takes the extras `panel`, `probe` and `stress`, so any app can open the PIN pad, freeze the screen in stress mode, or hide layers.
  - `KidsActivity`.
  - `adhan/SettingsActivity`. Its `test` extra makes it play the adhan (`SettingsActivity.java:45-57`).
  - Both `BootReceiver`s. They only listen for protected system broadcasts.
- **`StatsProvider` (`kids`)** is exported and guarded by the signature permission `com.family.kids.permission.READ_STATS`. It returns the children's names, daily minutes, top app and stars, and calls `clearCallingIdentity` (`StatsProvider.java:24-41`).
- **Powerful permissions.**
  - `WRITE_SECURE_SETTINGS`, granted over ADB and used only for the screensaver keys (`MainActivity.java:3166-3171`).
  - `SYSTEM_ALERT_WINDOW` (kids and adhan).
  - `PACKAGE_USAGE_STATS`.
  - `QUERY_ALL_PACKAGES` (all three apps).
  - `MANAGE_EXTERNAL_STORAGE` (adhan).
  - `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES` and `READ_MEDIA_AUDIO`.
- **Network.** Only HTTPS, to api.open-meteo.com, air-quality-api.open-meteo.com, api.alquran.cloud, cdn.mp3quran.net and mp3quran.net. The coordinates are fixed to Kuwait City. There is no analytics code and no account data. `allowBackup` is not set, so it defaults to true, and the prefs that hold the PINs can go into backups.

## Expert checklist

**PIN, YouTube lock and kids lock bypass**
- [ ] Each new way to open YouTube, Settings, the Play Store, a browser or another launcher goes through the PIN or appears in `Rules.NEVER`. Ways in include a dock tile, the All apps list, a long-press menu, a search, a "find by name" fallback (`findByName`) and a deep link.
- [ ] A new exported component, intent extra or intent filter must not open Parent settings, change the PIN or rules, give extra time or stars, or call `leave()` without a successful PIN check in the same flow.
- [ ] The PIN success callbacks (`pinThen`, `pinLaunchAfter`) cannot be triggered or left set by `onNewIntent` or `closePanels`. A stale `pinThen` must never run after the pad is dismissed.
- [ ] Back, Home, the recents key, the Menu key, the remote's Assistant and mic key, the Netflix/YouTube/Prime keys and number keys cannot get past a PIN pad or the rest screen. The first-PIN setup (`set` mode) cannot be skipped.
- [ ] A PR must not widen `Guard.passThrough`, the default `Rules.DEFAULT_APPS`, or `allowed()`. An empty or unknown foreground must not become "allowed" in more cases.
- [ ] A new app on the allow list is not a launcher, browser, store or file manager, and cannot launch other apps or play YouTube inside itself.
- [ ] Rest and bedtime cannot be skipped by changing the clock or time zone, by the stars or extra-time buttons without the PIN, or by a reboot.
- [ ] PIN hardening, if the PR touches the PINs:
  - attempt throttling, such as a growing delay after 3 to 5 wrong tries
  - a non-reversible check, such as a salted hash with PBKDF2, or Keystore
  - keeping the PIN out of logs and toasts
  - an option for more than 4 digits

**Exported components and intents**
- [ ] Every new `<activity>`, `<service>`, `<receiver>` and `<provider>` declares `android:exported` on purpose. Anything not meant for other apps is `false`. Anything exported is protected by a permission and validates its extras.
- [ ] Signature permissions give real protection only while the signing key is private. Because this key is public, treat `READ_STATS` as readable by anyone.
- [ ] `PendingIntent`s are `FLAG_IMMUTABLE` and use explicit intents. No implicit intent carries private data.
- [ ] The `StatsProvider` columns, and any new provider data, contain no more child data than the stats page needs.

**Permissions scope**
- [ ] No new dangerous or special permission is added without a clear need and a comment. Use `WRITE_SECURE_SETTINGS` only for documented keys. It must never be used to write `enabled_accessibility_services`, `install_non_market_apps`, ADB or developer settings, or to switch off Play Protect.
- [ ] The accessibility service stays minimal. `canRetrieveWindowContent` stays false. It never records, stores or sends key presses, and consumes only the learned buttons.
- [ ] Do not add new storage-wide reads (`MANAGE_EXTERNAL_STORAGE`) when scoped media access is enough.

**Secrets and data in a public repo**
- [ ] No new API keys, tokens, Wi-Fi passwords, home addresses, IPs or personal photos and videos are committed. Watch the CI logs and release notes too.
- [ ] The keystore and its password are not copied into more places. The best fix is to move both to GitHub Actions secrets, for example a base64 keystore plus env-var passwords, and to rotate the key. Rotating the key means a single uninstall and reinstall, or APK Signature Scheme v3 rotation.
- [ ] Children's names and other personal data are not hard-coded in more places. They are already in `Rules.kids` and the README.
- [ ] Network calls stay HTTPS-only, with no `usesCleartextTraffic`. No device IDs, usage data or names are sent to any server. New hosts are listed in the PR description.
- [ ] Downloaded content is not executed or installed. Audio, video and verse text are only displayed or played.

## Known risks already in the code
- **The signing key is public.** It sits at `app/tvhome.keystore`, with its password in `app/build.gradle.kts:20-24`, and all three apps use it. Anyone can build an APK that installs as an "update" over TV Home, Kids Corner or Adhan, if they can get it onto the box. Such an update would generally keep the app's granted permissions, such as WRITE_SECURE_SETTINGS and usage access, and the accessibility service stays enabled for the same component. A forged app could also gain `READ_STATS`. Nobody can tell a genuine release APK from a fake one. *Known: this is the biggest open risk. Flag it if the PR copies the key or password, uses it in a new module, or adds powers to the apps it signs.*
- **Weak PIN storage and checks.** Both PINs are stored in plain text, have 4 digits, and have no attempt limit (`MainActivity.java:2160-2183`, `KidsActivity.java:1515-1535`). A patient child can try all 10,000 codes. *Known.*
- **No PIN means open settings.** While no PIN is set, Parent settings open without one and the first person to open the pad sets the PIN (`MainActivity.java:2116`, `:2986`). *Known.*
- **The YouTube lock outside kids mode is only a UI gate.** Play Store, Settings, Assistant and casting still open YouTube. *Known. Flag it if the PR adds another way around the gate.*
- **Gaps in the kids lock.** These are the 1.5 s polling window, Kodi add-ons, the boot gap, the silent stop when usage access is missing, and the broad `android` and `com.android.systemui` pass-through. *Known.*
- **Exported extras.** Any app can send `panel`, `probe` or `stress` to `MainActivity`, or `test` to the adhan `SettingsActivity`. This is a nuisance only, since there is no settings access. *Known.*
- **`allowBackup` defaults to true.** The prefs that hold the PINs may be included in device or cloud backups. *Known. Suggest `android:allowBackup="false"` or data-extraction rules.*

## How to report
List the findings ranked by severity. Group them under **Must fix** (a child or another app can get past a PIN or kids lock, a new secret or personal data is published, a new unprotected exported component), **Should fix** (a wider permission, weaker storage, a new bypass that needs effort) and **Nice to have** (hardening). For each finding, give:
- `file:line`
- what a child or an attacker could do, in plain words for a beginner owner, for example: "Ghanim could press X and get YouTube without the PIN".
- a suggested fix, with a short code or manifest hint.
- a source link (MASVS, MASTG or Android docs).

Only report problems the PR adds or makes worse. Bring up the known risks above only when they are relevant. If the lane is clean, say "Lane 2: nothing found".

## Sources
- https://developer.android.com/privacy-and-security/risks/hardcoded-cryptographic-secrets (hard-coded keys break security, CWE-321, MASVS-CRYPTO)
- https://mas.owasp.org/MASTG/tests/android/MASVS-STORAGE/MASTG-TEST-0287/ (unencrypted sensitive data in SharedPreferences)
- https://mas.owasp.org/MASTG/tests/android/MASVS-STORAGE/MASTG-TEST-0001/ (testing local storage for sensitive data)
- https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0004/ (sensitive data stored unencrypted)
- https://opensecurityarchitecture.org/frameworks/owasp-masvs-v2/controls (MASVS v2.1 controls: AUTH, STORAGE, PLATFORM)
- https://developer.android.com/studio/publish/app-signing (keeping the signing key private, what happens if it leaks)
- https://attack.mitre.org/techniques/T1453 (abuse of accessibility features)
- https://zahidaz.github.io/awake/attacks/accessibility-abuse (key-event filtering and accessibility abuse patterns)
- https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog (bypass gaps in app blocking built on UsageStats polling)
- https://Developer.Android.com/guide/components/activities/background-starts (who can start activities from the background)
- https://developer.android.com/about/versions/15/behavior-changes-15 (Android 15 FGS and overlay rules)
- https://stuff.mit.edu/afs/sipb/project/android/docs/training/articles/security-tips.html (Android security tips: exported components and signature permissions; archived copy)
