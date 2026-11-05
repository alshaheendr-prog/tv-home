# Lane 2 knowledge: Safety and privacy

## How to use this file

- Read this whole file before every review, then read the brief `../2-safety.md`.
- Research online anything the PR touches that this file does not cover: a new permission, component, library, host, or bypass route. Prefer primary sources: developer.android.com, source.android.com, NIST, OWASP MAS, and GitHub docs.
- After every review, send the PR's thread the additions for this file so they get committed with the PR:
  - **Past findings:** one dated block per PR.
  - **Lessons learned.**
  - **Source library:** URL, what it's good for, and the date checked.
  - **Open questions.**
- Keep entries dated and short. If the file grows past about 400 lines, condense the older entries and keep the rules.
- Cite only the sources you actually opened. Mark anything unverified as *(unverified)*.
- Owner decision, 2026-11-05: the committed signing key **stays**. Don't raise it as Must fix. Mention it only if a PR makes it worse, for example by copying the key or password, signing a new module with it, or giving the apps it signs more powers.

## Core expertise

### 1. PINs, throttling and lockout
- NIST SP 800-63B-4, rate limiting: a verifier "SHALL limit consecutive failed authentication attempts ... to no more than 100". It MAY use waits that grow as the account nears the limit ("30 seconds up to an hour"), and it SHOULD forget earlier failures after a success. [NIST]
- NIST 800-63B-4, activation secrets (a local PIN that unlocks a device authenticator):
  - The PIN must be at least 4 characters, and 6 SHOULD be required. All-numeric PINs are allowed.
  - After **no more than 10 consecutive failed attempts**, the authenticator is disabled.
  - A blocklist of common choices (such as 123456) SHOULD be used.
  - The secret stays inside the authenticator. [NIST]
- How Android's own lockscreen does it (Gatekeeper):
  - Gatekeeper "must write a failure counter before verifying a user password" and clears the counter on success. Writing first stops an attacker from cutting the power to skip the count.
  - The HAL returns a timeout in milliseconds and refuses calls until it has passed. Enrolling a PIN is throttled the same way.
  - The TEE needs "a secure, monotonic clock that ticks in suspend". The counter goes in secure storage (RPMB).
  - The HMAC key never leaves Gatekeeper. [Gatekeeper]
  - Our app can't use the TEE.
- What this means for an app-level PIN:
  - Count the failure *before* comparing. `PinLock.check` writes the count after the compare, but both run in one call on the UI thread. This is a gap only if the process could be killed between them.
  - Use a clock that wall-clock changes can't move. Gatekeeper's secure clock is the model. In app code the nearest equivalent is `SystemClock.elapsedRealtime()` together with `Settings.Global.BOOT_COUNT`. A wall-clock `pin_until` is cleared by moving the clock forward.
  - Throttle "set PIN" as well as "check PIN".
  - With 4 digits, the lockout is the real protection. Any hash of 10,000 values can be cracked offline in moments, so PBKDF2 only slows an attacker who already has the prefs file, and that needs root or a backup.
- Locking out the family: a hard lockout lets a child lock the parent out (a denial of service). NIST's growing waits with a cap, plus forgetting old failures, is the balance PinLock uses: 5 free tries, then 30 s doubling up to 5 minutes, with failures forgotten after an hour.
- Never show or log the PIN, its hash or its salt. Compare in constant time with `MessageDigest.isEqual`. Get the salt from `SecureRandom`. [Crypto]

### 2. Hashing and keys on Android
- Recommended primitives: SHA-2 for `MessageDigest`, AES-GCM, HMAC-SHA256, ECDSA. Avoid MD5, SHA-1, ECB and so on. [Crypto]
- Hard-coded keys and secrets break the security model (Kerckhoffs's principle, CWE-321). Keep keys in the Android Keystore or KeyChain. [Hardcoded]
- PBKDF2 (`SecretKeyFactory "PBKDF2WithHmacSHA256"`) is available on API 26+ *(from general knowledge, not on the Crypto page)*. It makes sense only for secrets with real entropy. For a 4-digit PIN, put the effort into throttling.

### 3. Exported components and intents
- Always set `android:exported` explicitly. The defaults have changed over the years. An exported component can be started by its exact class name by any app. Impact: denial of service, outside apps "modify your app's internal functionality", data leaks. [Exported]
- An exported component that uses one of your own granted permissions with no permission check of its own lends that permission to every app on the box. Require the same permission on the component (`android:permission`, `enforceCallingPermission`), or don't export it. [Access control]
  - Here: `MainActivity` holds WRITE_SECURE_SETTINGS but is exported without a permission. Its extras must never lead to a secure-settings write without the PIN.
- Intent redirection: never start an `Intent` (or a URI or component name) taken from the extras of an incoming intent. If you must, check it with `resolveActivity` or `IntentSanitizer`, and strip the `FLAG_GRANT_*_URI_PERMISSION` flags. [Intent redirection]
- On targetSdk 34+, implicit intents reach only exported components. A *mutable* PendingIntent with an implicit intent throws. [B14]
- Android 15 adds `StrictMode.detectUnsafeIntentLaunch()`. [B15]
- StrandHogg (task-affinity hijack) is fixed by the OS on API 30+. The box runs API 34, so that's fine. Still, never set `allowTaskReparenting` or a foreign `taskAffinity`. [StrandHogg]

### 4. Custom and signature permissions
- `protectionLevel="signature"` grants the permission only to apps signed with the same certificate. "Ensure you are using a dedicated (not reused) signing certificate and store it securely." [Custom perms, Permissions overview]
- Risks:
  - A typo in a permission name, or an orphaned permission that no installed app defines, lets another app define and take it.
  - Before Android 10 there was an uninstall race (CVE-2019-2200).
  - Fix: every app that relies on the permission should define it too, which our app and kids manifests both do for `READ_STATS`. Or check signatures directly. [Custom perms]
- With a **public** signing key, a signature permission protects nothing against anyone who can install an APK on the box. Treat `READ_STATS` data as readable by such an app.

### 5. PendingIntent
- Use `FLAG_IMMUTABLE` and set the component, package and action explicitly. Use `FLAG_ONE_SHOT` for actions that must not be replayed. A mutable PendingIntent with unfilled fields can be redirected to non-exported components. [PendingIntent]
- On targetSdk 31+, the mutability flag is required. [B12]
- Android 14 and 15: neither the sender nor the creator grants background-activity-launch (BAL) rights by default. Opt in with `ActivityOptions` (`MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE`). [Background starts, B15]

### 6. Overlays, tapjacking, accessibility abuse
- Tapjacking means tricking the user into a security-relevant tap by covering it.
  - Android 12+ blocks touches that pass through untrusted overlays. For SYSTEM_ALERT_WINDOW windows, only layers with opacity ≥ 0.8 are blocked.
  - `filterTouchesWhenObscured="true"` protects a view. Partial occlusion needs a check of `FLAG_WINDOW_IS_PARTIALLY_OBSCURED`. [Tapjacking, B12-all]
  - On a D-pad TV with no touch, the main risk is an overlay that *hides* a PIN pad or the Kids rest screen. Key events still reach the focused window.
- Android 14+: `accessibilityDataSensitive` on a view lets only `isAccessibilityTool="true"` services read or act on it. Android 16 also blocks `dispatchGesture` on it. Setting `filterTouchesWhenObscured` turns it on implicitly. Worth setting on the PIN pads. [Tapjacking]
- An AccessibilityService with `canRequestFilterKeyEvents` sees every key before the IME and the apps do, which makes it a keylogger by design. It must never store, log or send keys. [AccessibilityService]
  - Our `RemoteKeys` stores only learned key *codes*, never sequences.
  - It returns false for "everyday" keys, but still sees them, including PIN digits typed on the pads.

### 7. Background launches and how Guard can pull the child back
- An app may start an activity from the background if it holds SYSTEM_ALERT_WINDOW, has a visible window, is the current IME, gets a system PendingIntent, and in a few other cases. A plain FGS is **not** an exemption. Guard relies on SYSTEM_ALERT_WINDOW, which Kids Corner holds. If that grant is revoked, `bringBack` silently fails. [Background starts]
- On targetSdk 35, a background FGS start needs SYSTEM_ALERT_WINDOW **and** a visible overlay. Starting `Guard` from the background with no exemption throws `ForegroundServiceStartNotAllowedException`, which `Guard.start` swallows. [B15]

### 8. Usage stats as an enforcement signal (fail-open risks)
- Usage access is a user-granted app-op. It can be revoked in Settings or over ADB (`appops set ... GET_USAGE_STATS ignore`). After that, `queryEvents` returns no events and enforcement stops *(adb command from general knowledge)*. [UsageStatsManager]
- `queryEvents` returns null while the user is locked (Android 11+). Events are kept only "a few days". [UsageStatsManager]
- `ACTIVITY_RESUMED` fires per activity. A picture-in-picture window, an overlay from another app, or audio from a service can keep playing with no new RESUMED event *(PiP on TV unverified)*. [UsageEvents.Event]
- Polling leaves an exposure window of the tick length plus the event delay. Sound from a blocked app can play during it.

### 9. Ways a child gets around an Android TV kids lock (threat model)
Check each PR against this list. Status in this repo is in brackets.
- **Remote keys:** Home, Back, Recents, Menu, the Assistant or mic key, and the vendor buttons (YouTube, Netflix, Prime). [Home and Back are handled. Vendor keys are filtered by RemoteKeys only when they have been learned.]
- **Voice:** Google Assistant opens YouTube or another app by voice. [Guard blocks it after it comes to the front. The home YouTube gate does not.]
- **Casting:** a phone casts YouTube to the box. YouTube's receiver starts YouTube. [Guard blocks it in kids mode. Not gated outside kids mode.]
- **Apps inside apps:** Kodi add-ons, a browser inside an allowed app, and **profile switching inside allowed apps.** Netflix, Prime Video and Shahid are in `Rules.DEFAULT_APPS` "with kids profiles". A child can switch to an adult profile unless each service has a profile PIN or lock *(app behaviour from general knowledge)*.
- **Store and settings:** the Play Store "Open" button, Settings > Apps > Open, Settings > Apps > Force stop on Kids Corner, revoking usage access or overlays. [Store and Settings are in `Rules.NEVER`. Force stop or revoking from a PC via ADB still works.]
- **Recents and the task switcher:** the system UI is passed through, so check that apps can't be opened from it.
- **Clock and time zone:** moving the clock clears the PIN lockout and changes extra time. [Settings is blocked in kids mode, so this needs ADB or the time to be set outside kids mode.]
- **Reboot:** the gap between boot and BOOT_COMPLETED, plus Guard's first poll. A force-stopped app gets no BOOT_COMPLETED on Android 14 until it is launched *(standard stopped-state rule)*. Android 15 changes this. [B15-all, Intent]
- **ADB:** `pm clear` resets the Kids PIN (documented in the README). Network ADB, if it stays switched on after Update TV, lets any device on the Wi-Fi do the same *(check with the owner)*.
- **Another launcher, or a "find by name" fallback**, that opens a hidden app.

### 10. Backup and data extraction
- Auto Backup is on by default. The standard system encrypts backups in transit and at rest. From Android 9, a device with a lock screen encrypts them end to end. Exclude sensitive data with `dataExtractionRules` (API 31+), plus `fullBackupContent` for API 30 and below. Use `disableIfNoEncryptionCapabilities`. [Backup]
- On targetSdk 31+, `android:allowBackup="false"` turns off cloud backup, but on some devices it "doesn't disable device-to-device transfers". [Auto Backup]
- On targetSdk 31+, `adb backup` excludes app data unless the app is debuggable. That closes the ADB route to the PIN prefs on this box. The remaining exposure is cloud and device-to-device backup. [B12]
- Don't build a custom "export" that writes app data into shared storage. [Backup]

### 11. Network
- Cleartext is off by default for targetSdk 28+. Any `cleartextTrafficPermitted="true"` or `usesCleartextTraffic` is a finding. Even non-secret data sent over HTTP can be changed in transit (ARP or DNS poisoning). [Cleartext, NSC]
- On targetSdk 35, TLS 1.0 and 1.1 can't be used. [B15]
- Downloaded content (JSON, audio, text) must never become code, an intent, a URL to open, or a file path without checks. [Unsafe URI]

### 12. Logs, storage, WebView
- Logcat is readable only by privileged apps from API 16, but preloaded OEM apps sometimes hold READ_LOGS. So never log a PIN, a hash, child names or usage data. [Log]
- External or shared storage (`Movies/`, `Download/`) is readable and writable by other apps that have media or all-files access. Treat files there as untrusted input. The adhan sound from `Download/` can be swapped by any app with storage access, which is a nuisance, not a breach. [External storage]
- WebView, if one is ever added:
  - Keep JavaScript off unless needed.
  - No `addJavascriptInterface` on untrusted content.
  - No file access from file URLs.
  - Load only allow-listed HTTPS origins. [WebView bridge, Unsafe URI]

### 13. Signing key and supply chain (public repo)
- Anyone with the signing key can ship an update to the app. Play App Signing allows an upload-key reset. Outside Play, a lost or leaked key means you can't update safely without a reinstall, or without key rotation (APK Signature Scheme v3, API 28+, *rotation details not reopened this round*). [App signing]
- GitHub's hardening guide [GHA]:
  - "Pin references to actions to a commit SHA". Check that the SHA comes from the action's own repo, not a fork.
  - Set the default `GITHUB_TOKEN` permission to read-only and raise it only for the job that needs it.
  - Never put `${{ github.event.* }}` text (PR titles, branch names) directly into `run:`. Pass it through an `env:` variable.
  - Avoid `pull_request_target` and `workflow_run` with untrusted code.
  - Never put secrets in plaintext in workflow files. Mask derived values (such as base64) too.
  - If a secret is printed in a log, delete the log and rotate the secret.
- This repo's CI against that guide:
  - Actions are pinned by tag (`@v5`, `@v4`), not by SHA.
  - `contents: write` is set for the whole workflow, and it also runs on `claude/**` pushes.
  - The `latest` release is published from `main` only.
  - Nothing untrusted is interpolated into `run:` today (`github.sha`, `github.repository` and `github.run_number` are safe).
  - Fixes worth suggesting: split the build and release permissions, and pin to SHAs (Nice to have unless a PR widens the triggers or permissions).

### 14. Children's data and privacy
- Data kept on the box: the children's names (`Rules.kids`, README), per-day minutes, top app, and stars. None of it leaves the box. The only network calls are to weather, Quran text and Quran audio hosts, with fixed Kuwait coordinates and no IDs.
- Data minimisation: providers return only what the stats page needs. Don't add new identifiers (photos, birthdays, school). Don't send usage data anywhere. Keep names out of logs and release notes.

## This box and this code

Verified on 2026-11-05 at commit 47e8c57 unless marked otherwise.
- **Manifests**:
  - app: `MainActivity` (exported, HOME and LAUNCHER, singleTask) and `RemoteKeys` (exported, protected by `BIND_ACCESSIBILITY_SERVICE`).
  - kids: `KidsActivity` (exported, LAUNCHER), `PlayerActivity` (not exported), `Guard` (not exported, specialUse), `StatsProvider` (exported, `readPermission=com.family.kids.permission.READ_STATS`) and `BootReceiver` (exported, system broadcasts only).
  - adhan: `SettingsActivity` (exported, LAUNCHER), `AdhanService` (not exported) and `BootReceiver`.
  - No manifest sets `allowBackup`, `dataExtractionRules`, `usesCleartextTraffic` or `networkSecurityConfig`.
- `READ_STATS` is defined as `signature` in **both** the app and the kids manifests, so it is not orphaned.
- `StatsProvider.query` answers only `/week`. It returns child names, per-day minutes, the top app label, stars spent and stars left, and calls `clearCallingIdentity` only around its own reads. insert, update and delete throw.
- `RemoteKeys` config: `canRetrieveWindowContent="false"`, `flagRequestFilterKeyEvents`, events limited to `typeWindowStateChanged`. It saves only the learned key code to prefs (`RemoteKeys.java:70-77`). "Everyday" keys pass through (`:105-117`). Digit keys can still be learned (a known risk).
- `PinLock` (the same in app and kids):
  - Salted SHA-256 with a 16-byte `SecureRandom` salt, compared with `MessageDigest.isEqual`.
  - 5 free tries, then 30 s doubling up to 5 minutes. Failures are forgotten after 1 hour.
  - Wall-clock `pin_until`, clamped to 5 minutes on read.
  - `set()` resets the counters, but setting a PIN is not throttled.
  - It migrates an old plain `pin`.
- Kids PIN gating: KidsActivity requires a PIN to be chosen first (`:162`, `:271`) and uses `PinLock.waitLeft` and `check` (`:1507-1546`).
- `Rules.NEVER` covers both YouTube apps, YouTube Kids, the Play Store, both Settings apps, TV Home, Projectivy, the Google TV launcher, four browsers, Stremio, Puffin and Downloader. `DEFAULT_APPS` covers Netflix, Crunchyroll, Prime Video, Shahid (2) and Kodi.
- `Guard.passThrough` contains the Kids package, `android`, `com.android.systemui`, both permission controllers, and every IME.
- Exported extras: MainActivity reads `panel` and `probe` in `onNewIntent` (`:307-320`) and `panel=pin` in `onCreate` (`:271`). The adhan SettingsActivity reads `test` (`SettingsActivity.java:56`).
- Signing: `app/tvhome.keystore`, with the password `tvhome-family` in `app|kids|adhan/build.gradle.kts:20-23`. Kept, by the owner's decision.
- CI: `.github/workflows/build.yml`:
  - `permissions: contents: write` for the whole workflow.
  - Triggers on `main` and `claude/**`.
  - Actions pinned by tag.
  - Publishes the `latest` release from `main`.
- `.gitignore` covers `local.properties`, `build/`, `.gradle/` and `.idea/`.
- Grep found no `Log.*` call that prints a PIN.

## Past findings

### PR #20 (merged 2026-11-05): PIN limits, crash after restart, missed adhan, Quran text
- Must and Should: none.
- Nice, not done:
  - The lockout uses the wall clock, so moving the clock forward clears it. Idea: `elapsedRealtime` + `BOOT_COUNT`.
  - Add PBKDF2 and `android:allowBackup="false"`.
  - A child can keep the pad resting by guessing (accepted trade-off: the waits are capped and failures are forgotten after an hour).
- Owner decision 2026-11-05: keep the committed signing key.
- From the original research (still open unless noted):
  - Exported MainActivity and SettingsActivity accept the `panel`, `probe`, `stress` and `test` extras (a nuisance only).
  - Guard fails open without usage access. The Parent menu and the Kids setup line now warn about it.
  - Kodi is allowed in kids mode and can reach YouTube add-ons.
  - The YouTube lock guards only the dock and the remote button.

## Lessons learned
- 2026-11-05: Before calling a check "missing", read the current code. The brief said the Kids PIN had no attempt limit, but since #20 it uses PinLock.
- 2026-11-05: For any allow-listed app, ask whether a child can switch profiles, accounts or sources inside it. The allow list trusts the app, not the content.
- 2026-11-05: Any check that depends on a revocable grant (usage access, overlay, accessibility) must fail visibly. Look for `catch (Exception ignored)` around enforcement.
- 2026-11-05: On targetSdk 31+, `adb backup` no longer exposes app data. Weigh allowBackup findings against cloud and device-to-device backup only.

## Open questions to research next
- The 2025 tj-actions/changed-files tag-rewrite incident: a primary write-up was not opened this round (it is the classic example for SHA pinning).
- APK Signature Scheme v3 key rotation (source.android.com): exact steps and the minimum API, for when the owner revisits the key.
- OWASP MASVS v2 and MASWE entries for authentication and brute force (MASWE on local auth, MASTG local-auth tests): the brief cites MASTG-TEST-0287 and 0001 and MASWE-0004, which were not reopened this round.
- Does network ADB (port 5555) stay switched on after Update TV? If so, anyone on the Wi-Fi can run `pm clear` or revoke grants.
- Does a "Restricted settings" block (Android 13+ for sideloaded apps) affect turning on RemoteKeys or Kids usage access on Android TV 14? Search results exist (Lawnchair docs, bayton.org) but were not opened.
- Do Netflix, Prime Video and Shahid on Android TV support a profile lock that the owner should switch on? This would go on lane 8's checklist.
- Can Google TV's own Kids profile or Family Link work together with the launcher?

## Source library

All checked 2026-11-05 unless noted. The [tags] are used above.
- [NIST] https://pages.nist.gov/800-63-4/sp800-63b.html: rate limiting (≤100 failures, growing waits) and activation secrets (≥4, SHOULD 6, ≤10 failures, blocklist).
- [Gatekeeper] https://source.android.com/docs/security/features/authentication/gatekeeper: counter written before verify, HAL timeouts, a secure monotonic clock that ticks in suspend, the LockSettingsService → gatekeeperd → TEE chain.
- [Exported] https://developer.android.com/privacy-and-security/risks/android-exported: always set exported, and the impact.
- [Access control] https://developer.android.com/privacy-and-security/risks/access-control-to-exported-components: exported components lending your permissions.
- [Custom perms] https://developer.android.com/privacy-and-security/risks/custom-permissions: typos, orphaned permissions, protectionLevel, CVE-2019-2200, a dedicated key.
- [Permissions overview] https://developer.android.com/guide/topics/permissions/overview: what signature permissions are.
- [Intent redirection] https://developer.android.com/privacy-and-security/risks/intent-redirection: nested intents and IntentSanitizer.
- [PendingIntent] https://developer.android.com/privacy-and-security/risks/pending-intent: mutability and FLAG_ONE_SHOT.
- [Tapjacking] https://developer.android.com/privacy-and-security/risks/tapjacking: occlusion, filterTouchesWhenObscured, accessibilityDataSensitive.
- [StrandHogg] https://developer.android.com/privacy-and-security/risks/strandhogg: task affinity, fixed on API 30+.
- [Hardcoded] https://developer.android.com/privacy-and-security/risks/hardcoded-cryptographic-secrets: CWE-321, Keystore.
- [Crypto] https://developer.android.com/privacy-and-security/cryptography: recommended algorithms.
- [Backup] https://developer.android.com/privacy-and-security/risks/backup-best-practices: dataExtractionRules and end-to-end encryption.
- [Auto Backup] https://developer.android.com/identity/data/autobackup: the allowBackup=false caveat for device-to-device, and the rules syntax.
- [B12] https://developer.android.com/about/versions/12/behavior-changes-12: adb backup excludes app data, PendingIntent mutability.
- [B12-all] https://developer.android.com/about/versions/12/behavior-changes-all: untrusted touches blocked.
- [B14] https://developer.android.com/about/versions/14/behavior-changes-14: implicit intent and PendingIntent rules.
- [B15] https://developer.android.com/about/versions/15/behavior-changes-15: BAL creator opt-in, FGS SAW rule, TLS 1.0 and 1.1 off, unsafe intent StrictMode.
- [B15-all] https://developer.android.com/about/versions/15/behavior-changes-all: stopped state and BOOT_COMPLETED.
- [Background starts] https://developer.android.com/guide/components/activities/background-starts: BAL exemptions and opt-ins.
- [Intent] https://developer.android.com/reference/android/content/Intent: BOOT_COMPLETED and the protected time broadcasts.
- [UsageStatsManager] https://developer.android.com/reference/android/app/usage/UsageStatsManager: permission, null when locked, retention.
- [UsageEvents.Event] https://developer.android.com/reference/android/app/usage/UsageEvents.Event: RESUMED per activity.
- [AccessibilityService] https://developer.android.com/reference/android/accessibilityservice/AccessibilityService: onKeyEvent sees keys before the IME and apps.
- [Cleartext] https://developer.android.com/privacy-and-security/risks/cleartext-communications: the risks, including tampering.
- [NSC] https://developer.android.com/privacy-and-security/security-config: cleartextTrafficPermitted and pinning.
- [Log] https://developer.android.com/privacy-and-security/risks/log-info-disclosure: READ_LOGS on preloaded apps.
- [External storage] https://developer.android.com/privacy-and-security/risks/sensitive-data-external-storage: shared storage is untrusted.
- [Unsafe URI] https://developer.android.com/privacy-and-security/risks/unsafe-uri-loading: loading untrusted URIs.
- [WebView bridge] https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges: JS interfaces.
- [Security tips] https://developer.android.com/privacy-and-security/security-tips: signature-protected providers, checkCallingPermission.
- [GHA] https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions: SHA pinning, least-privilege token, script injection, secrets.
- [App signing] https://developer.android.com/studio/publish/app-signing: key compromise, upload key reset.
- From the brief, not reopened this round: the MASTG-TEST-0287, MASTG-TEST-0001 and MASWE-0004 pages, opensecurityarchitecture MASVS v2.1, MITRE ATT&CK T1453, zahidaz accessibility-abuse notes, the dev.to UsageStats blocking write-up, and the MIT archive of the security tips.
