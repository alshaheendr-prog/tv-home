# Lane 7 knowledge: Build check

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../7-build-check.md`.
- History lives in [archive/7-build-check.md](archive/7-build-check.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.

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
  - Changing the workflow file's path or its `name:` line can restart the run number at 1, which gives `INSTALL_FAILED_VERSION_DOWNGRADE` (-25) on the TV. Treat either as Must fix. [gha-name-reset]
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
| INSTALL_FAILED_DUPLICATE_PERMISSION | -112 [so-112] | custom permission defined by an app with a different key |
| INSTALL_PARSE_FAILED_NO_CERTIFICATES | -103 | unsigned APK |
| INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE | -26 (unverified) | targetSdk dropped below 23 |
| INSTALL_FAILED_DEPRECATED_SDK_VERSION | -29 (unverified) | targetSdk < 23 on Android 14 (bypass: `--bypass-low-target-sdk-block`) |

  [android14-minsdk] covers the last row.

### 6. targetSdk 34 and 35 behaviour
- **Foreground service types (Android 14, target 34+).** Every FGS must declare `android:foregroundServiceType`.
  - If the type is missing, `startForeground` throws `MissingForegroundServiceTypeException`.
  - If the type's permission is missing, it throws a `SecurityException`.
  - `specialUse` needs `FOREGROUND_SERVICE_SPECIAL_USE`, a normal permission that is granted automatically, plus a `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">`.
  - `mediaPlayback` needs `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
  - `systemExempted` throws `ForegroundServiceTypeNotAllowedException` unless the app meets its criteria, for example exact-alarm apps.
  [fgs-types]
- **Android 15 (target 35) BOOT_COMPLETED.** Receivers may not start `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` or `microphone` FGS: the start throws `ForegroundServiceStartNotAllowedException`. `specialUse` is not on that list. [a15] Test it with `adb shell am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS <pkg>`.
- **`MY_PACKAGE_REPLACED`** is a listed exemption from the background FGS start limits, and the Android 15 type block names only `BOOT_COMPLETED`, so the `specialUse` starts from our BootReceivers are OK. [fgs-bg-start]
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
- [tvlauncher-noroot] says a Xiaomi Home button can send `KEYCODE_ALL_APPS`. That is not verified on this box: our RemoteKeys passes `KEYCODE_HOME` through and treats `KEYCODE_ALL_APPS` as the Apps button (opens Kodi). Home reaches Family TV Home only because Update TV sets the home activity.
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
- **Checkout:** `actions/checkout` v6.0.0 exists; the repo is on `@v5`.

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

Added by the whole-app audit (2026-11-05, verified from the published APKs, build #134):
- APK sizes: tv-home 8.8 MB, of which classes.dex is 7.5 MB, stored uncompressed. adhan 1.2 MB, kids 0.5 MB.
- All three are signed v2 only (block IDs 0x7109871a v2, 0x504b4453 AGP dependency info, 0x42726577 verity padding). No v1, no v3.
- No `.so` files. Not debuggable or testOnly.
- AGP shortens resource paths in release (`res/H8.ttf`).
- `META-INF/version-control-info.textproto` holds the git revision. Use it to prove which commit an APK came from (`unzip -p x.apk META-INF/version-control-info.textproto`).
- The `latest` release's tag is created at main's tip, not the built SHA (`target_commitish: "main"`).

Added by the PR #22 review (2026-11-05):
- The actions are pinned to SHAs: checkout v5.1.0 fbc6f39, setup-java v5.7.0 b6effb0, setup-gradle v4.4.4 748248d, upload-artifact v4.6.2 ea165f8.
- `concurrency: build-${{ github.ref }}` with `cancel-in-progress: false`.
- The artifact keeps `retention-days: 14` and `if-no-files-found: error`.
- The release step skips publishing if the `#N` in `latest`'s title is higher than this run's number. It uses `--target $GITHUB_SHA` and retries the create 3 times. It still deletes `latest` before creating it.
- adhan no longer requests MANAGE_EXTERNAL_STORAGE, because the Projectivy wallpaper swap was removed. It reads Download/takbir.mp3 via READ_MEDIA_AUDIO and falls back to the chime.
- In the brief's "Known risks", drop the lines about tag-pinned actions and the missing concurrency group. Keep the delete-then-create risk.

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/7-build-check.md](archive/7-build-check.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: Ask for the CI run by head SHA. Don't take "it built" from the PR text.
- 2026-11-05: "Rollback" can't mean installing an older APK. Its versionCode is lower, so the install fails with VERSION_DOWNGRADE. A safe rollback is a **revert PR**: it builds with a new, higher run number and contains the old code.
- 2026-11-05: Re-running an old main run is not harmless. It republishes an older versionCode to `latest`.
- 2026-11-05: adhan also needs usage access (PACKAGE_USAGE_STATS) and MANAGE_EXTERNAL_STORAGE, not just kids. When a grant goes missing, check both apps.
- 2026-11-05: The keystore stays in the repo. Don't re-raise it as a finding.
- 2026-11-05: A docs-only PR still publishes `latest` on merge. Check main has no app-code drift, then tell the owner Update TV is optional.
- 2026-11-05: You *can* inspect the real release APKs from the container. `curl -L https://github.com/alshaheendr-prog/tv-home/releases/download/latest/<name>.apk` works through the proxy. Then use `unzip -lv` for size and stored/compressed, and a Python read of the APK Signing Block for the scheme IDs. Do this rather than guessing.
- 2026-11-05: `git ls-remote --tags https://github.com/<owner>/<action>.git` works from the container. Use it to get pin SHAs, and take the `^{}` line for annotated tags.
- 2026-11-05: lintVitalRelease checks only fatal-severity issues. NewApi is not fatal by default, so a minSdk violation passes CI. On this box (API 34) it doesn't matter, but don't treat green CI as proof of minSdk safety.
- 2026-11-05: Commit comments need a write token, so workflow-wide `contents: write` is needed while `claude/**` failure comments exist (unverified permission detail).

- 2026-11-05: Gradle stops at the first failing task. A compile error in :app means :adhan and :kids were never compiled. Review their diffs by eye, and require all 3 tasks to be green on the next run.
- 2026-11-05: The CI failure comment (tail -60 with --stacktrace) can contain only stack frames. To find the first real error, read the job log (`gh api repos/<o>/<r>/actions/jobs/<id>/logs`, then grep for `error:`).
- 2026-11-05: Check action SHA pins with `git ls-remote https://github.com/<owner>/<repo> refs/tags/<tag> refs/tags/<tag>^{}`. For an annotated tag, the pin must be the `^{}` commit (gradle/actions v4.4.4 = 748248d).

- 2026-11-06: PR artifacts (actions/artifacts/<id>/zip) can't be downloaded from the container, because the blob host productionresultssa14.blob.core.windows.net is refused by the proxy. Only `latest` release APKs (after merge) can be inspected. Use the job log (`gh api repos/<o>/<r>/actions/jobs/<id>/logs`, grep `Task :.*:(lintVital|assemble)Release`) to prove all 3 modules ran.
- 2026-11-06: When launcher art moves to adaptive icons, grep for `(BitmapDrawable)` casts on getApplicationIcon/loadIcon. MainActivity.java:1963 guards with instanceof, so it is safe. An unguarded cast would crash the home screen on our own apps' AdaptiveIconDrawable icons.
- 2026-11-06: When a resource folder changes (drawable to mipmap), check that every R.drawable.X and @drawable/X still resolves. Notification small icons (AdhanService.java:118, Guard.java:74) still use R.drawable.icon.
