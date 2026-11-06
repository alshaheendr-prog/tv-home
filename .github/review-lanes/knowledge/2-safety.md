# Lane 2 knowledge: Safety and privacy

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../2-safety.md`.
- History lives in [archive/2-safety.md](archive/2-safety.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
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
- Signing: `app/tvhome.keystore`, with its password in plain text in `app|kids|adhan/build.gradle.kts:20-23`. Kept, by the owner's decision.
- CI: `.github/workflows/build.yml`:
  - `permissions: contents: write` for the whole workflow.
  - Triggers on `main` and `claude/**`.
  - Actions pinned by tag.
  - Publishes the `latest` release from `main`.
- `.gitignore` covers `local.properties`, `build/`, `.gradle/` and `.idea/`.
- Grep found no `Log.*` call that prints a PIN.

Added by the whole-app audit (2026-11-05):
- Guard: TICK 1.5 s; `bringBack` throttled to once per 2 s; empty foreground counts as allowed; only ACTIVITY_RESUMED events are tracked.
- TV Home `openSettings` uses `NEW_TASK` only. The All apps grid always has a Settings tile with no PIN (outside kids mode this is a known YouTube route).
- Kids `openParent` has no idle timeout. The extra-time buttons leave the menu open.
- Adhan `swapWallpaper` writes `Movies/Projectivy/wallpaper.mp4`; `wallpaper_mode` defaults to `return`. MANAGE_EXTERNAL_STORAGE is used for nothing else.
- `Quran.reciters` builds playback URLs from the API's `server` field without checking the scheme.

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/2-safety.md](archive/2-safety.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Before calling a check "missing", read the current code. The brief said the Kids PIN had no attempt limit, but since #20 it uses PinLock.
- 2026-11-05: For any allow-listed app, ask whether a child can switch profiles, accounts or sources inside it. The allow list trusts the app, not the content.
- 2026-11-05: Any check that depends on a revocable grant (usage access, overlay, accessibility) must fail visibly. Look for `catch (Exception ignored)` around enforcement.
- 2026-11-05: On targetSdk 31+, `adb backup` no longer exposes app data. Weigh allowBackup findings against cloud and device-to-device backup only.
- (PR #21) On a docs-only PR, grep the added lines for secrets and personal data, and diff the removed brief lines so no check gets dropped. Briefs are reviewer instructions; dropping a check is a regression.
- (PR #21) PinLock uses `apply()` (memory now, disk later), so a power cut right after a wrong try could lose that count. Negligible; if hardened, `commit()` the fail count before comparing.
- 2026-11-05: Look at what happens when a panel is left open. A PIN gate means little if the screen behind it stays unlocked forever, so check for an idle timeout and for `onStop` closing it.
- 2026-11-05: A bounce-back lock leaks state between bounces. Ask whether the blocked app resumes where the child left it (tasks restore their last state), because that turns a 2 s window into steady progress.
- 2026-11-05: Every parental-looking control must change the real rule. Grep where its pref is read. TV Home's `kids` set is read only by TV Home.
- 2026-11-05: When a feature is removed (Projectivy), check which permissions existed only for it.

- 2026-11-05: Never use view visibility as the "unlocked" signal. ViewPropertyAnimator cancels a running animation on the same property and drops its withEndAction, so a faded panel can stay VISIBLE at alpha 0. Keep an explicit open flag.
- 2026-11-05: Verify action SHAs with `git ls-remote` on refs/tags/vX and vX^{} (gradle/actions uses annotated tags).

