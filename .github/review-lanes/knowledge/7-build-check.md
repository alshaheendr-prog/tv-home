# Lane 7 knowledge: Build check

## How to use this file
- Read this whole file before every review, then the brief (`../7-build-check.md`). The brief is the checklist. This file is the reference behind it.
- If the PR touches something this file doesn't cover, research it online first, cite what you opened, and add it here.
- After every review, send the PR's thread short, dated additions for **Past findings**, **Lessons learned**, **Source library** and **Open questions**, so they get committed in the same PR. Mark facts you couldn't verify "(unverified)".
- Keep the file under about 400 lines. If it grows past that, condense the oldest Past findings into one line per PR, and merge any duplicate lessons.
- Cite only pages you actually opened. If a page could not be fetched, say so instead of citing it.

## Core expertise

### 1. AGP, Gradle and JDK compatibility
- AGP sets a minimum Gradle version: 8.7 needs Gradle 8.9 or later, 8.8 needs 8.10.2, 8.9 and 8.10 need 8.11.1, and 8.11 to 8.13 need 8.13. AGP 9.0 needs Gradle 9.1.0, and later 9.x releases need newer Gradle. The latest stable AGP was 9.5 (checked 2026-11-05). [about-agp]
- compileSdk also sets a minimum AGP version: API 35 needs AGP 8.6.0 or later, API 36 needs 8.9.1, API 36.1 needs 8.13.0, and API 37 needs 9.1.1. [about-agp]
- This repo uses AGP 8.7.3 with Gradle 8.14.3, which is above the 8.9 minimum. So **raising compileSdk to 36 also means raising AGP to at least 8.9.1.**
- A library whose AAR metadata asks for a higher compileSdk or AGP fails the build at `checkReleaseAarMetadata`. Read the error and bump compileSdk or AGP. Never suppress the check. (General AGP behaviour; no single page opened.)
- Moving to AGP 9 is a major migration: it changes the DSL and needs Gradle 9.x. Treat it as its own PR, never as a side effect of another change. [about-agp]

### 2. Lint in release builds
- `assembleRelease` runs `lintVitalRelease`. It checks only *fatal* issues (for example a missing `android:exported`, or some `NewApi` misuse) and stops the build if it finds one. This is controlled by `lint { checkReleaseBuilds }` (default true). [lint-dsl] [aosp-checkReleaseBuilds] (The DSL page's property text was not in the fetched part of the page. This rule is the well-known behaviour, confirmed by the AOSP commit title.)
- `abortOnError = false`, `checkReleaseBuilds = false`, a new `baseline = file(...)` that hides a new error, `disable += "..."` or a new `tools:ignore` all only hide a problem. Ask the PR why it needs each one.
- The repo has no `lint {}` block, so the defaults apply. Its existing `tools:ignore`s are `ProtectedPermissions` (WRITE_SECURE_SETTINGS, PACKAGE_USAGE_STATS), `ScopedStorage` (MANAGE_EXTERNAL_STORAGE) and `QueryAllPackagesPermission`. All are deliberate.

### 3. R8, minify and resource shrinking
- R8 removes code it thinks is unused, but it can't see code reached by reflection or JNI. That code needs narrow `-keep` or `-keepclassmembers` rules. [r8-keep-blog]
- The default `proguard-android-optimize.txt` already keeps the Activities, Services and BroadcastReceivers named in the manifest, so don't add manual rules for them. [r8-keep-blog]
- Anti-patterns:
  - Wide `-keep class com.x.** { *; }` rules.
  - `-keep class !pkg.**`, which keeps everything *outside* pkg.
  - `-dontoptimize`, `-dontobfuscate` or `-dontshrink` left in a release build.
  [r8-keep-blog]
- Debugging tools: `-printconfiguration build/outputs/logs/configuration.txt` and `-whyareyoukeeping`. [r8-keep-blog]
- With `isShrinkResources = true`, any resource that is only looked up by name with `getIdentifier()` is removed. Keep it with `res/raw/keep.xml` and `tools:keep`. [r8-keep-blog]
- The repo has no `getIdentifier` or `Class.forName` calls (grep, 2026-11-05). Minify is off in all three modules. Turning it on is Must-test on the TV: CI passing does not prove the app runs.
- With minify on, keep `build/outputs/mapping/release/mapping.txt` from each release, or crash stack traces can't be read. [r8-keep-blog]

### 4. Signing
- Updates install only when the new APK is signed with the same certificate as the installed one. If it isn't, the install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (-7), and the only way forward is to uninstall, which deletes the app's data. [install-errors] [pm-codes]
- **The owner decided on 2026-11-05 to keep the committed keystore** (`app/tvhome.keystore`, with its passwords in the build files). Don't recommend rotating it, moving it to secrets, or regenerating it. Any edit to `signingConfigs` is Must fix unless the owner asked for it.
- Key rotation exists (APK Signature Scheme v3 / v3.1 with a proof-of-rotation lineage):
  - v3.1 (Android 13+) can target rotation at an SDK level.
  - Devices on Android 12L or lower ignore the v3.1 block and use the original signer in the v3 block.
  - With rotation, apksig uses v3.1 by default.
  [apk-v3.1]
- So rotation *could* work without an uninstall, but it is a migration nobody needs here. Note it, don't propose it.
- The `READ_STATS` custom permission is `protectionLevel="signature"`, and both `app` and `kids` declare it. If the two apps are ever signed with different keys:
  - Installing the second one fails with `INSTALL_FAILED_DUPLICATE_PERMISSION`.
  - The provider read fails with a SecurityException.
  [so-dup-perm, from the brief]
- Commands to check signing:
  - `apksigner verify --print-certs app-release.apk` shows the signer's SHA-256. Compare it with the previous release.
  - `keytool -list -v -keystore app/tvhome.keystore` shows the key itself.
  (Standard SDK tools, not run here: the SDK is not available.)

### 5. versionCode and install failures (`adb install -r`)
- versionCode must be a positive integer. Android refuses to install a lower versionCode over a higher one. The Play maximum is 2100000000. versionName is shown to people and nothing enforces it. [versioning]
- `GITHUB_RUN_NUMBER` is "a unique number for each run of a particular workflow". It starts at 1 and does **not** change when a run is re-run. `GITHUB_RUN_ATTEMPT` is the one that counts re-runs. [gha-vars]
- Consequences for this repo:
  - Renaming or recreating the workflow restarts the run number at 1, which gives `INSTALL_FAILED_VERSION_DOWNGRADE` (-25) on the TV.
  - Re-running an **older** main run republishes `latest` with an older versionCode, so Update TV fails with VERSION_DOWNGRADE.
  - `claude/**` runs use up numbers too. That is harmless, because the numbers only go up.
- Install codes that matter here (number and meaning from PackageManager [pm-codes], cause and fix from [install-errors]):

| Code | Value | Meaning here |
|---|---|---|
| INSTALL_FAILED_ALREADY_EXISTS | -1 | install without `-r` |
| INSTALL_FAILED_INSUFFICIENT_STORAGE | -4 | box storage full (big wallpaper clips) |
| INSTALL_FAILED_UPDATE_INCOMPATIBLE | -7 | different signing key: forces uninstall and data loss |
| INSTALL_FAILED_SHARED_USER_INCOMPATIBLE | -8 | sharedUserId added or changed: never add one |
| INSTALL_FAILED_OLDER_SDK | -12 | minSdk above API 34 (the box) |
| INSTALL_FAILED_CONFLICTING_PROVIDER | -13 | another app owns `com.family.kids.stats` |
| INSTALL_FAILED_TEST_ONLY | -15 | `android:testOnly` / debug-from-IDE APK |
| INSTALL_FAILED_MISSING_FEATURE | -17 | a `uses-feature` without `required="false"` |
| INSTALL_FAILED_VERSION_DOWNGRADE | -25 | lower versionCode |
| INSTALL_FAILED_DUPLICATE_PERMISSION | n/a | custom permission defined by an app with a different key |
| INSTALL_PARSE_FAILED_NO_CERTIFICATES | -103 | unsigned APK |
| INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE | n/a | targetSdk dropped below 23 |
| INSTALL_FAILED_DEPRECATED_SDK_VERSION | n/a | targetSdk < 23 on Android 14 (bypass: `--bypass-low-target-sdk-block`) |

  [android14-minsdk] covers the last row.

### 6. targetSdk 34 and 35 behaviour
- **Foreground service types (Android 14, target 34+).** Every FGS must declare `android:foregroundServiceType`.
  - If the type is missing, `startForeground` throws `MissingForegroundServiceTypeException`.
  - If the type's permission is missing, it throws a `SecurityException`.
  - `specialUse` needs `FOREGROUND_SERVICE_SPECIAL_USE`, a normal permission that is granted automatically, plus a `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">`.
  - `mediaPlayback` needs `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
  - `systemExempted` throws `ForegroundServiceTypeNotAllowedException` unless the app meets its criteria, for example exact-alarm apps.
  [fgs-types]
- **Android 15 (target 35) BOOT_COMPLETED.** Receivers may not start `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` or `microphone` FGS: the start throws `ForegroundServiceStartNotAllowedException`. `specialUse` is not on that list. [a15]
- **Android 15, starting an FGS while holding SYSTEM_ALERT_WINDOW.** A background FGS start that relies on the overlay permission now also needs a *visible* `TYPE_APPLICATION_OVERLAY` window. Test with `adb shell am compat enable FGS_SAW_RESTRICTIONS <pkg>`. [a15]
- **Android 15 time limit.** `dataSync` and `mediaProcessing` FGS get 6 hours per 24 hours, after which the system calls `Service.onTimeout`. [a15]
- **Android 15 audio focus.** `requestAudioFocus` fails with `AUDIOFOCUS_REQUEST_FAILED` unless the app is on top or running an FGS. [a15]
  - The adhan calls it from `AdhanService`, which is an FGS (`AdhanService.java:574`), so it is OK.
- **Android 15 edge-to-edge** is enforced:
  - `setStatusBarColor` and `setDecorFitsSystemWindows` are deprecated or do nothing.
  - `Configuration.screenWidthDp` now includes the system bars.
  [a15]
  - The box runs 14 today, so this is Nice to have unless the PR touches insets.
- **Android 15 other changes.** PendingIntent creators block background activity launches by default. TLS 1.0 and 1.1 are disabled. `elegantTextHeight` defaults to true, which matters for Arabic line height. [a15]
- **Android 14 (target 34) intents.** Implicit intents reach only *exported* components. A mutable PendingIntent with an implicit intent throws. [a14]
- **Android 14 receivers.** `registerReceiver` needs `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`, except for receivers that listen only to system broadcasts. [a14]
- **Android 14 dynamic code** must be loaded from a read-only file. [a14]
- **Android 14 zip paths.** Zip entries containing `..` throw a `ZipException`. [a14]
- **16 KB page sizes.** An app that is only Java or Kotlin, libraries included, already works on 16 KB devices. APKs with uncompressed `.so` files need AGP 8.5.1 or later for 16 KB zip alignment. [16kb]
  - The Play rule (TV-G6, from 2026-08-01) doesn't apply to sideloading. [tv-quality]
  - To check: `unzip -l app-release.apk | grep '\.so$'`. If the list is empty, there's nothing to do.

### 7. Permissions that need adb or appops
Sort every new `<uses-permission>` into one of these buckets. Every bucket except "normal" needs a line in Update TV.

| Bucket | Examples in repo | How Update TV grants it |
|---|---|---|
| normal | INTERNET, FOREGROUND_SERVICE(_SPECIAL_USE), RECEIVE_BOOT_COMPLETED, QUERY_ALL_PACKAGES | automatically at install |
| runtime (dangerous) | READ_MEDIA_VIDEO/IMAGES/AUDIO, POST_NOTIFICATIONS (adhan) | `adb shell pm grant <pkg> android.permission.READ_MEDIA_VIDEO` |
| appops / special access | SYSTEM_ALERT_WINDOW, PACKAGE_USAGE_STATS, MANAGE_EXTERNAL_STORAGE | `adb shell appops set <pkg> SYSTEM_ALERT_WINDOW allow`, `... GET_USAGE_STATS allow`, `... MANAGE_EXTERNAL_STORAGE allow` [hexnode] |
| signature/privileged with development flag | WRITE_SECURE_SETTINGS | `adb shell pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS` [fmd] |
| accessibility service | RemoteKeys | `settings put secure enabled_accessibility_services <pkg>/<.Class>` then `settings put secure accessibility_enabled 1` [tvlauncher-noroot] |
| home activity | MainActivity | `cmd package set-home-activity --user 0 com.family.tvhome/.MainActivity`, check with `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` [tvlauncher-noroot] |

- `enabled_accessibility_services` is a list of services separated by colons. `put` replaces the whole list. [a11y-gist] [tvlauncher-noroot]
- Some OEMs, Xiaomi among them, need "USB debugging (Security settings)" turned on in Developer options before `pm grant WRITE_SECURE_SETTINGS` works. [fmd]
- On a Xiaomi remote the Home button can send `KEYCODE_ALL_APPS` instead of `KEYCODE_HOME`. That is why RemoteKeys exists. [tvlauncher-noroot]
- `pm clear` deletes all of an app's data. Never put it in a script or checklist casually. [adb-gist]

### 8. Manifest merger
- Priority order: build-variant manifests, then the main module manifest, then library manifests (in the order of the `dependencies` block). [manifests]
- Library `<uses-permission>`s are merged into the app. Remove an unwanted one with `tools:node="remove"` (optionally scoped with `tools:selector`). [manifests]
- Other markers: `tools:replace="attr"`, `tools:remove`, `tools:node="replace|removeAll|strict"`. [manifests]
- A library with a higher minSdk fails the merge unless the app uses `tools:overrideLibrary`. That is a crash risk: flag it. [manifests]
- The merge report is at `build/outputs/logs/manifest-merger-release-report.txt`. Anyone with a local SDK can diff it between builds. [manifests]
- After a dependency bump, check whether the library added permissions or components (for example media3 session services).

### 9. Dependencies (media3)
- The repo uses `media3-exoplayer:1.4.1`. The latest stable on 2026-11-05 was **1.11.1** (2026-09-10). Then come 1.11.0 (2026-08-05), 1.10.1 (2026-05-12) and 1.10.0 (2026-03-26). [media3]
- 1.11.0 removed deprecated symbols, for example `exoplayer.MetadataRetriever` moved to `media3-inspector`, and turned on dynamic scheduling of the playback loop by default. That is a behaviour change, so test the wallpaper. [media3]
- 1.12.0-alpha01 raises minSdk to 24, which is still fine here (minSdk is 28 or 33). [media3]
- The compileSdk and AGP that 1.10+ need were not in the fetched part of the notes (see Open questions). A big jump (1.4 to 1.11) is its own PR, tested on the TV for wallpaper playback, memory and the adhan sound.

### 10. GitHub Actions hardening and release safety
- **Pinning.** "Pinning an action to a full length commit SHA is currently the only way to use an action as an immutable release." Tags can be moved. Check that the SHA comes from the action's own repository, not a fork. [gha-secure] [pin-sha]
  - In March 2025 every tag of `tj-actions/changed-files` was moved to point at malicious code. [pin-sha]
  - Tools that keep pins current: pinact, Dependabot, Renovate's `helpers:pinGitHubActionDigestsToSemver`. Note that Dependabot *alerts* don't cover SHA-pinned actions. [pin-sha] [gha-secure]
- **Token.** Default the workflow to read-only `contents: read`. Grant `contents: write` per job, only to the job that publishes. [gha-secure]
- **Script injection.** Never paste `${{ github.event.* }}` text (PR titles, branch names) straight into `run:`. Pass it through `env:` and quote it. `github.sha`, `github.run_number` and `github.repository` are safe. [gha-secure]
- **Triggers.** Avoid `pull_request_target` and `workflow_run` unless they are truly needed. [gha-secure]
- **Secrets.** Redaction of secrets in logs is not guaranteed. [gha-secure]
- **OpenSSF Scorecards** flags unpinned actions and broad token permissions. [gha-secure]
- **Concurrency.**
  - A group allows at most one running and one pending run. A new pending run replaces the older pending one.
  - `cancel-in-progress: true` also cancels the run that is in progress.
  - `queue: max` allows up to 100 pending runs and can't be combined with cancel-in-progress.
  [gha-concurrency]
  - For releases, use a group like `release-${{ github.ref }}` with `cancel-in-progress: false`, so a publish is never killed halfway between delete and create.
- **The release race.**
  - Today `gh release delete latest` runs and then `gh release create`. If the create fails, there is no release at all.
  - `gh release upload latest <files> --clobber` replaces assets of the same name in one step. It is safer, but if the upload fails the old asset is already gone. [gh-upload]
  - Two quick pushes to main with no concurrency group can finish out of order, leaving the *older* versionCode in `latest`.
- **Artifacts (upload-artifact v4).**
  - Retention defaults to the repo setting (90 days) and can be set from 1 to 90 with `retention-days`.
  - An artifact name can't be uploaded twice in the same run.
  - `if-no-files-found` defaults to `warn`. Set it to `error` so a missing APK fails loudly.
  - A job can create at most 10 artifacts.
  [upload-artifact]
- **Gradle cache (setup-gradle).**
  - It caches the Gradle User Home, which holds downloaded dependencies and build-script caches.
  - **Only the default branch writes the cache.** Other branches, here `claude/**`, only read it.
  - `cache-cleanup` defaults to `on-success`.
  - `gradle-version` takes a fixed version.
  - The current docs show `setup-gradle@v6`. The repo is on `@v4`.
  [setup-gradle]

## This box and this code (verified 2026-11-05)
- **Box:** Xiaomi TV Box S 3rd Gen, Android TV 14 (API 34), 2 GB RAM, no root. Updated by the owner's PowerShell "Update TV" script over wireless ADB. **The script is on his PC, not in the repo.** It installs the three APKs with `-r`, grants permissions, enables RemoteKeys, sets the home activity and saves screenshots to `C:\TVWallpapers`.
- **Build (all three modules):** root `build.gradle.kts` sets AGP 8.7.3. compileSdk and targetSdk are 35, Java 17. minSdk is 28 for app and adhan, **33** for kids (`kids/build.gradle.kts:11`). `isMinifyEnabled = false`. There is no `lint {}` block.
- **Version:** `versionCode = GITHUB_RUN_NUMBER` (1 locally) and `versionName = "0.1.<run>"`, at `*/build.gradle.kts:13-14`.
- **Signing:** `signingConfigs.home` uses `app/tvhome.keystore`. adhan and kids point to `../app/tvhome.keystore`. The alias is `tvhome`. Kept on purpose (owner decision, 2026-11-05).
- **Dependencies:** the only one is `androidx.media3:media3-exoplayer:1.4.1`, in `app/build.gradle.kts`. adhan and kids have none.
- **CI (`.github/workflows/build.yml`):**
  - Runs on push to `main` and `claude/**`, and on manual dispatch.
  - Uses `ubuntu-24.04` with checkout@v5, setup-java@v5 (temurin 21) and setup-gradle@v4 (gradle-version 8.14.3), all pinned to tags.
  - `permissions: contents: write` is set for the whole workflow. There is no `concurrency:`.
  - It installs the SDK platform with `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-35" "build-tools;35.0.0"`.
  - Build step: `gradle :app:assembleRelease :adhan:assembleRelease :kids:assembleRelease --stacktrace | tee build.log`, then `test ${PIPESTATUS[0]} -eq 0`.
  - On failure it comments on the commit with the last 60 lines of the log.
  - It copies the APKs to `tv-home.apk`, `family-adhan.apk` and `kids-corner.apk` and uploads the artifact `tv-home-apk` (default retention).
  - On main it runs `gh release delete latest --yes --cleanup-tag || true` and then `gh release create latest ...`.
- **Permissions by app:**
  - app: INTERNET, QUERY_ALL_PACKAGES, READ_MEDIA_VIDEO/AUDIO, READ_EXTERNAL_STORAGE (maxSdk 32), WRITE_SECURE_SETTINGS. It declares and uses `com.family.kids.permission.READ_STATS`.
  - adhan: INTERNET, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE(_SPECIAL_USE), POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, READ_MEDIA_AUDIO, MANAGE_EXTERNAL_STORAGE (checked at `SettingsActivity.java:173`), PACKAGE_USAGE_STATS (`AdhanService.java:531`), QUERY_ALL_PACKAGES.
  - kids: INTERNET, SYSTEM_ALERT_WINDOW, FGS(_SPECIAL_USE), BOOT, READ_MEDIA_VIDEO/IMAGES/AUDIO (requested at `KidsActivity.java:170`), PACKAGE_USAGE_STATS (`Guard.java:66`), QUERY_ALL_PACKAGES. It declares READ_STATS.
- **Components:**
  - app: `.MainActivity` (HOME + LEANBACK_LAUNCHER, singleTask) and `.RemoteKeys` (accessibility service).
  - adhan: `.SettingsActivity`, `.AdhanService` (FGS specialUse, `startForeground` at `AdhanService.java:123`), `.BootReceiver` (BOOT_COMPLETED, MY_PACKAGE_REPLACED).
  - kids: `.KidsActivity`, `.PlayerActivity` (not exported), `.Guard` (FGS specialUse, `Guard.java:76`), `.StatsProvider` (authority `com.family.kids.stats`, readPermission READ_STATS), `.BootReceiver`.
- **Overlays:** `TYPE_APPLICATION_OVERLAY` at `AdhanService.java:174` and `Guard.java:198,245`.
- **README.md is outdated.** It describes a WebView with `assets/index.html`. The UI is native Java.

## Past findings
- **2026-11-05, PR #20** (PIN limits, crash after restart, missed adhan, Quran text):
  - CI run #122 passed: 3 × assembleRelease plus lint vital. No Must fix or Should fix.
  - Nice to have: `PinLock` used `String.format` without a Locale. Fixed by removing the `String.format`.
  - Nice to have: one-way PIN hash migration. Fine, because `adb install -r` can't downgrade.
  - Recorded as background risks:
    - kids minSdk 33 against 28 for the others.
    - The release deletes `latest` before recreating it, with no concurrency guard.
    - `contents: write` is set for the whole workflow, and the actions are pinned to tags.
    - Renaming or recreating the workflow resets the run number, which gives VERSION_DOWNGRADE.
    - app and kids both declare READ_STATS.
    - Renaming MainActivity or RemoteKeys breaks Update TV's setup.
    - README.md is outdated.

## Lessons learned
- 2026-11-05: Ask for the CI run by head SHA. Don't take "it built" from the PR text.
- 2026-11-05: "Rollback" can't mean installing an older APK. Its versionCode is lower, so the install fails with VERSION_DOWNGRADE. A safe rollback is a **revert PR**: it builds with a new, higher run number and contains the old code.
- 2026-11-05: Re-running an old main run is not harmless. It republishes an older versionCode to `latest`.
- 2026-11-05: adhan also needs usage access (PACKAGE_USAGE_STATS) and MANAGE_EXTERNAL_STORAGE, not just kids. When a grant goes missing, check both apps.
- 2026-11-05: The keystore stays in the repo. Don't re-raise it as a finding.

## Open questions to research next
- Does `adb install -r -d` (allow downgrade) work for a non-debuggable release APK on Android 14 user builds? (Unverified. The fetch budget ran out.) Until it's confirmed, assume it doesn't.
- Are `MY_PACKAGE_REPLACED` receivers exempt from the background FGS start limits the way BOOT_COMPLETED is? Exact list for targetSdk 35.
- What compileSdk and AGP minimum do media3 1.10 and 1.11 declare? Is there any `.so` in media3-exoplayer (16 KB)?
- Does Xiaomi's Android TV 14 build keep `appops` and accessibility grants across `adb install -r`? (Update TV re-applies them anyway.)
- The exact `lint` DSL property descriptions (the page text was cut off). Does `lintVitalRelease` run `NewApi` as fatal by default?
- Is `actions/checkout@v5` / `setup-java@v5` / `upload-artifact@v4` still current? Find the SHAs for each tag in case the owner wants them pinned.

## Source library
All checked 2026-11-05 unless noted.
- [about-agp] https://developer.android.com/build/releases/about-agp: the AGP to Gradle table, and the minimum AGP for each API level. The latest stable is 9.5.
- [lint-dsl] https://developer.android.com/reference/tools/gradle-api/com/android/build/api/dsl/Lint: the lint DSL. The fetched part held only the index, so the property text was not seen.
- [aosp-checkReleaseBuilds] https://android.googlesource.com/platform/tools/base/+/cc9093a%5E%21: the commit "Add lintOptions#checkReleaseBuilds which runs lint on release builds" (title only, seen in search).
- [r8-keep-blog] https://developer.android.com/blog/posts/configure-and-troubleshoot-r8-keep-rules: keep-rule good practice, anti-patterns and debug flags.
- [apk-v3.1] https://source.android.com/docs/security/features/apksigning/v3-1: key rotation, lineage, and v3.1 rotation targeted at SDK 33.
- [install-errors] https://bayton.org/android/resolve-app-install-errors: every INSTALL_FAILED_* code with its cause and fix.
- [pm-codes] https://gist.github.com/dagezi/9501179: numeric PackageManager install codes.
- [android14-minsdk] https://bayton.org/android/android-14-minimum-sdk/: target 23 minimum on Android 14, and `--bypass-low-target-sdk-block`.
- [versioning] https://developer.android.com/studio/publish/versioning: versionCode rules and downgrade protection.
- [gha-vars] https://docs.github.com/en/actions/reference/variables-reference: GITHUB_RUN_NUMBER and RUN_ATTEMPT semantics.
- [a15] https://developer.android.com/about/versions/15/behavior-changes-15: target 35 changes (BOOT FGS, SAW FGS, timeouts, audio focus, edge-to-edge).
- [a14] https://developer.android.com/about/versions/14/behavior-changes-14: target 34 changes (implicit intents, receivers, dynamic code, zip).
- [fgs-types] https://developer.android.com/about/versions/14/changes/fgs-types-required: FGS types, permissions and exceptions.
- [16kb] https://developer.android.com/16kb-page-size: 16 KB page sizes. Java-only apps are fine. AGP 8.5.1 for `.so` alignment.
- [tv-quality] https://developer.android.com/docs/quality-guidelines/tv-app-quality: the TV quality IDs, including TV-G6 and TV-PS (minSdk ≤ 31 for Play).
- [manifests] https://developer.android.com/build/manage-manifests: merge priority, markers, the report path and overrideLibrary.
- [media3] https://developer.android.com/jetpack/androidx/releases/media3: media3 versions and breaking changes. Only the first 100k characters were read.
- [gha-secure] https://docs.github.com/en/enterprise-cloud@latest/actions/reference/secure-use-reference: SHA pinning, token scope, script injection, Scorecards.
- [pin-sha] https://dev.to/suzukishunsuke/pin-github-actions-to-a-full-length-commit-sha-for-security-2n7p: the tj-actions incident, pinact, Renovate.
- [gha-concurrency] https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency: group, cancel-in-progress and queue semantics.
- [gh-upload] https://cli.github.com/manual/gh_release_upload: `--clobber` semantics and its risk.
- [upload-artifact] https://gitea.psi.ch/actions/upload-artifact/raw/commit/6c139afa6f18a1359e5a9185f9415433473e3793/README.md: a mirror of the actions/upload-artifact v4 README (retention, immutability, if-no-files-found).
- [setup-gradle] https://www.github.com/gradle/actions/blob/main/docs/setup-gradle.md: caching, read-only caches on non-default branches, and inputs. The docs show v6.
- [hexnode] https://www.hexnode.com/mobile-device-management/help/grant-permissions-for-hexnode-apps-using-android-debug-bridge-adb/: `appops set` lines for SAW, GET_USAGE_STATS and MANAGE_EXTERNAL_STORAGE.
- [fmd] https://fmd-foss.org/docs/fmd-android/granting-secure-settings-access: `pm grant WRITE_SECURE_SETTINGS`, and the "USB debugging (Security settings)" tip.
- [a11y-gist] https://gist.github.com/mrk-han/67a98616e43f86f8482c5ee6dd3faabe: `settings put secure enabled_accessibility_services`, which takes a colon-separated list.
- [tvlauncher-noroot] https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/: set-home-activity plus how to verify it, an accessibility Home catcher, the Xiaomi remote sending KEYCODE_ALL_APPS, and the undo steps.
- [adb-gist] https://gist.github.com/Pulimet/5013acf2cd5b28e55036c82c91bd56d8: an adb cheat sheet (pm grant/clear, keyevents, screencap, logcat).
- [so-dup-perm] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/a/27090838: DUPLICATE_PERMISSION. Cited by the first reviewer, not reopened in this session.
