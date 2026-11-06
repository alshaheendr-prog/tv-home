# Lane 2 knowledge: Safety and privacy: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings

### PR #23 (2026-11-06, update 2: visual polish)
- Nothing found. The manifests changed only icon and banner to @mipmap. No new component, permission, extra, host or CI change.
- Checked and fine:
  - The PIN "✕" to "×" change is in both the key labels and pinPress.
  - pressDip consumes no keys.
  - The cached All apps grid filters HIDDEN_FROM_LIST before building its key.
  - Kids refreshTiles(true) is keyed on the allowed list, and launch() checks resting.
  - Wallpaper names from Movies/ are only compared, never used as paths.
  - Guard and AdhanService kept drawable/icon.png at review time; after lane 13 S1 they use white-on-clear drawable/ic_notify.xml.
  - The new PNGs have no text or EXIF chunks.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (ecdab40): no Must. Should S1: Parent settings could come back without the PIN. closePanels() hid parentPanel only in withEndAction; an openPin() within the 180 ms fade cancelled that fade (dropping the end action), set pinOverParent from visibility, and Back or ✕ then called backToParent(). Fixed with a parentOpen flag. Nice: accept only known names in the `missing` extra. Audit S1-S4 are all fixed. The CI SHAs were verified against their tags, and `run:` uses env vars.

### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- **Whole-app audit (2026-11-05, main d772bcc).**
  - Must: none.
  - Should:
    - S1: the parent menus never auto-close (Kids `openParent`, TV Home `openParent` with KEEP_SCREEN_ON).
    - S2: Settings keeps its page between Home-button trips, so a child can creep to Force stop or Clear storage on Kids Corner (fix: `FLAG_ACTIVITY_CLEAR_TASK` in `openSettings`, and recheck NEVER apps sooner in Guard).
    - S3: TV Home's "Add to Kids Corner ⭐" only marks the app in TV Home and doesn't change Kids Corner's allow list.
    - S4: Adhan's `MANAGE_EXTERNAL_STORAGE` exists only for the dead Projectivy wallpaper swap.
  - Nice: check the scheme of the reciter `server` URL; `filterTouchesWhenObscured` on the PIN pads; known items unchanged.

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
- **PR #21 (2026-11-05, docs only).** Must: none. Should: a second plain-text copy of the signing password in this file (removed). Nice: dropped source URLs (restored). Checked fine: no new personal data, no brief check dropped, the Up-Up-Down-Down sequence was already public (the PIN is the gate), code facts spot-checked at 47e8c57.

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
- From the brief (first review), restored with URLs: https://mas.owasp.org/MASTG/tests/android/MASVS-STORAGE/MASTG-TEST-0001/ , https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0004/ , https://www.opensecurityarchitecture.org/frameworks/owasp-masvs-v2/controls , https://attack.mitre.org/techniques/T1453 , https://zahidaz.github.io/awake/attacks/accessibility-abuse , https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog , https://stuff.mit.edu/afs/sipb/project/android/docs/training/articles/security-tips.html (MASTG-TEST-0287 not reopened).
- [GH sensitive data] https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository: forks, clones and cached SHA views keep pushed data, so rotate rather than rely on deleting. Checked 2026-11-05.
- [adb] https://developer.android.com/studio/command-line/adb.html: wireless debugging pairs with a code and the port shown on the device; stays paired until revoked; "always allow on this network" auto-allows on that Wi-Fi; legacy `adb tcpip 5555` has no pairing. Checked 2026-11-05.
- [tj-actions] https://thehackernews.com/2025/03/github-action-compromise-puts-cicd.html: CVE-2025-30066 (March 2025): a bot PAT was stolen, all version tags repointed to a malicious commit, secrets dumped into public build logs; SHA-pinned users were not hit. Checked 2026-11-05.
- [Restricted settings] https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis and https://bayton.org/android/android-13-restricted-permissions/: Android 13+ blocks turning on accessibility services (bayton: also notification access, overlays) for apps from non-session installers; session installs and stores exempt; unlock via App info, menu, "Allow restricted settings". Neither covers adb. Checked 2026-11-05.
- [Tasks] https://developer.android.com/guide/components/activities/tasks-and-back-stack: an existing task comes back "with its last state restored"; `clearTaskOnLaunch` needs `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`.
- [MASWE-0030] https://mas.owasp.org/MASWE/MASVS-AUTH/MASWE-0030/: re-authentication on timeout and on returning from background (placeholder page, no mitigations yet; points to ASVS V3.3).
- [MASWE-0024] https://mas.owasp.org/MASWE/MASVS-AUTH/MASWE-0024/: sensitive data accessible after the session ends (only the outline loaded).
- [All files] https://developer.android.com/training/data-storage/manage-all-files: MANAGE_EXTERNAL_STORAGE only as a last resort, for core features.
- [Media] https://developer.android.com/training/data-storage/shared/media: raw File paths on 11+; another app's Downloads entries need the Storage Access Framework.
- [Netflix lock] https://help.netflix.com/en/node/114277: 4-digit Profile Lock, set from the phone app or the web.

- [VPA] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/ViewPropertyAnimator.java : a running animation on the same property is cancelled, and onAnimationCancel removes its end action. Checked 2026-11-05.

- https://www.w3.org/TR/png-3/ : text and eXIf chunks (section 13.4 not loaded). Checked 2026-11-06.
- https://developer.android.com/develop/ui/views/notifications/badges : setSmallIcon takes a drawable. Checked 2026-11-06.

## Open questions to research next
- The 2025 tj-actions/changed-files tag-rewrite incident: a primary write-up was not opened this round (it is the classic example for SHA pinning).
- APK Signature Scheme v3 key rotation (source.android.com): exact steps and the minimum API, for when the owner revisits the key.
- OWASP MASVS v2 and MASWE entries for authentication and brute force (MASWE on local auth, MASTG local-auth tests): the brief cites MASTG-TEST-0287 and 0001 and MASWE-0004, which were not reopened this round.
- Does network ADB (port 5555) stay switched on after Update TV? If so, anyone on the Wi-Fi can run `pm clear` or revoke grants.
- Does a "Restricted settings" block (Android 13+ for sideloaded apps) affect turning on RemoteKeys or Kids usage access on Android TV 14? Search results exist (Lawnchair docs, bayton.org) but were not opened.
- Do Netflix, Prime Video and Shahid on Android TV support a profile lock that the owner should switch on? This would go on lane 8's checklist.
- Can Google TV's own Kids profile or Family Link work together with the launcher?
- (2026-11-05) tj-actions incident: answered, see Source library [tj-actions]; SHA-pin actions.
- (2026-11-05) Network ADB: the owner uses paired wireless debugging, so the open port-5555 worry mostly falls away. Still to ask: is wireless debugging left on after Update TV, and was "always allow on this network" ticked?
- (2026-11-05) Restricted settings: Update TV turns RemoteKeys on over ADB, which likely bypasses the UI block (unverified). Check on the box only if the service ever fails to turn on.
- Does `READ_MEDIA_AUDIO` alone let Adhan read `Download/takbir.mp3` (pushed over adb) by raw path on Android TV 14? Test on the box before dropping all-files access.
- On Google TV 14, is the quick-settings side panel (long-press Home or the gear) in `com.android.systemui`, which Guard lets through, or in launcherx? Can a child reach Settings pages from it without Settings coming to the front?
- Prime Video's profile PIN on Android TV, and whether Shahid has a profile lock (for lane 8).
- Does Android TV Settings use launch modes that ignore `CLEAR_TASK`? Check on the box when S2 is fixed.

- On TV 14, does READ_MEDIA_AUDIO alone play an adb-pushed Download/takbir.mp3? The chime fallback covers safety, and lane 8 should check that the takbir plays.

- Read the build-notification page to check whether a mipmap or adaptive icon works as a small icon.
- The Kids icon cache never invalidates. This is cosmetic and belongs to lane 13 or 3.
