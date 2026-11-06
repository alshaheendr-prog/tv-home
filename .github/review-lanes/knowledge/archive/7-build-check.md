# Lane 7 knowledge: Build check: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- 2026-11-06, PR #23 (update 2, visual polish): CI #145 for 809f108 passed, with 3 × assembleRelease and lint vital. No Must or Should.
  - The manifests changed only icon/banner (@drawable to @mipmap).
  - Adaptive icons were added to app and adhan (mipmap-anydpi-v26 with drawable/icon_back and icon_fore vectors).
  - Banners were added to mipmap-xhdpi (320×180) and mipmap-xxxhdpi (640×360) in all three apps. Kids got legacy icon PNGs in mipmap-x/xx/xxxhdpi.
  - drawable/icon.png is kept in adhan and kids for notification small icons.
  - Nice: the -v26 qualifier is redundant at minSdk 28. The javac deprecation notes in app and kids are likely from GradientDrawable.setDither. Banners ship only at xhdpi and xxxhdpi.

### PR #22 (2026-11-05, audit update 1)
- **2026-11-05, PR #22** (audit update 1): CI #135 failed. javac reported `View cannot be converted to ViewGroup` at MainActivity.java:1940 (Must). Gradle stopped at :app, so adhan and kids were never built. Should: the failure comment showed only stack frames. Should: adhan lost MANAGE_EXTERNAL_STORAGE, so the takbir read now relies on READ_MEDIA_AUDIO (test on the TV). OK: the SHA pins were verified, a concurrency group was added, and the release step now refuses to publish a run number lower than the one in `latest`'s title.

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
- **2026-11-05, PR #21** (docs only): CI #130 passed; no Must; Should: run_number resets on a `name:` change; all code line refs verified correct.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit on main d772bcc. CI #134 passed. No Must.
  - Should: S1 publish race (delete before create, no `--target`, no concurrency, an older re-run can downgrade); S2 actions on tags and old majors.
  - Nice: unused fonts (app amiri_regular and chivo_mono_bold, adhan inter_light); 441 KB WAV; unguarded API 29 `startForeground` in adhan (minSdk 28); R8 off with 7.5 MB dex; outdated README; artifact retention; hidden-API animator reflection.


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
- [tvlauncher-noroot] https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/: set-home-activity plus how to verify it, an accessibility Home catcher, and the undo steps. Its Xiaomi KEYCODE_ALL_APPS claim is not verified on this box.
- [adb-gist] https://gist.github.com/Pulimet/5013acf2cd5b28e55036c82c91bd56d8: an adb cheat sheet (pm grant/clear, keyevents, screencap, logcat).
- [so-dup-perm] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/a/27090838: DUPLICATE_PERMISSION. Cited by the first reviewer, not reopened in this session.
- [fgs-bg-start] https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start: the background FGS start exemption list (MY_PACKAGE_REPLACED). Checked 2026-11-05.
- [gha-name-reset] https://github.com/orgs/community/discussions/26709: run_number restarts when the workflow's `name:` changes. Checked 2026-11-05.
- [so-112] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/41257994/what-does-error-code-112-mean-in-android: -112 is DUPLICATE_PERMISSION. Checked 2026-11-05.
- [aosp-f1d44db] https://android.googlesource.com/platform/frameworks/base/+/f1d44db88067%5E%21/: downgrade allowed only on a debuggable build or app, for system apps. Checked 2026-11-05.
- [gh-create] https://cli.github.com/manual/gh_release_create: `--target` defaults to the default branch, and with no tag one is created "from the latest state of the default branch". Also `--verify-tag` and `--latest`. Checked 2026-11-05.
- [lint-agp-dsl] https://googlesamples.github.io/android-custom-lint-rules/usage/agp-dsl.md.html: checkReleaseBuilds is "limited to issues with severity configured to be `fatal`" and aborts on fatal issues. Checked 2026-11-05.
- [setup-java-rel] https://github.com/actions/setup-java/releases: v6.0.0 (24 Aug) and v6.1.0 (14 Oct, the latest shown at fetch time). `git ls-remote` showed v6.2.0 on 2026-11-05.
- [upload-artifact-rel] https://github.com/actions/upload-artifact/releases: v5 adds Node 24 support; v6 runs on node24 by default and needs runner ≥ 2.327.1; latest is v7.0.2. Checked 2026-11-05.
- [durationscale] https://learn.microsoft.com/zh-tw/dotnet/api/android.animation.valueanimator.durationscale?view=net-android-37.0: `getDurationScale` is public from API 33, read-only, with no public setter.
- [so-durationscale] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/64226137/ignore-developer-s-settings-animation-scale-in-android: the reflection `setDurationScale` override is reset by WindowManagerGlobal if done too early, so re-apply it in onResume.

- [runs-on-concurrency] https://runs-on.com/github-actions/concurrency/: with `cancel-in-progress: false`, a newer pending run replaces the older pending one. Checked 2026-11-05.
- [datalogic-a13] https://developer.datalogic.com/mobile-computers/news/a13_new_permissions_for_file_access: the scope of Android 13's READ_MEDIA_* permissions. The page does not say whether raw File paths work. Checked 2026-11-05.

- [tv-icons] https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines: android:icon and android:banner are both mandatory.
  - The banner is 16:9, in mipmap-* folders, from mdpi 160×90 to xxxhdpi 640×360 (xhdpi 320×180).
  - Adaptive banners are supported.
  - Themed (monochrome) icons are not supported on Android TV or Google TV.
  - roundIcon is deprecated.
  - Checked 2026-11-06.
- [so-manifest-res] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/50832824/lint-error-when-using-adaptive-launcher-icon: the lint error "Resources referenced from the manifest cannot vary by configuration" (version qualifiers are exempt). Checked 2026-11-06.

## Open questions to research next
- `adb install -r -d`: commit f1d44db allows a downgrade only on a debuggable build or app (for system apps). Confirm the rule applies to normal release APKs; until then assume `-d` fails. [aosp-f1d44db]
- Answered 2026-11-05: `MY_PACKAGE_REPLACED` is a listed bg-FGS exemption; the Android 15 block names only BOOT_COMPLETED; `specialUse` is OK. [fgs-bg-start]
- Does a file-only rename of the workflow (same `name:`) reset run_number?
- What compileSdk and AGP minimum do media3 1.10 and 1.11 declare? Is there any `.so` in media3-exoplayer (16 KB)?
- Does Xiaomi's Android TV 14 build keep `appops` and accessibility grants across `adb install -r`? (Update TV re-applies them anyway.)
- The exact `lint` DSL property descriptions (the page text was cut off). Does `lintVitalRelease` run `NewApi` as fatal by default?
- Is `actions/checkout@v5` / `setup-java@v5` / `upload-artifact@v4` still current? checkout v6.0.0 exists. Still open: the latest setup-java and upload-artifact majors, and the SHAs for each tag in case the owner wants them pinned.
- Is `ValueAnimator.setDurationScale` or `sDurationScale` on the hidden-API blocklist for targetSdk 35? (Search didn't find the hiddenapi-flags entry.)
- Does creating a commit comment really need `contents: write` for GITHUB_TOKEN? Open the REST "commit comments" permission table.
- What is the exact Node 20 removal date for GitHub-hosted runners, after which upload-artifact v4 and setup-gradle v4 stop working?
- How much does R8 shrink tv-home's dex with media3-exoplayer 1.4.1? (Try it in a `claude/` branch and read the CI APK size.)

- Does `appops set <pkg> MANAGE_EXTERNAL_STORAGE allow` error, or do nothing, when the app no longer requests the permission?
- On Android 14, can READ_MEDIA_AUDIO read an adb-pushed Download/*.mp3 through java.io.File? In other words, is the file MediaStore-indexed when it is pushed?
- Should the failure comment grep for the error lines before the tail? (Proposed in PR #22, not yet adopted.)

- Does the ManifestResource lint check exempt density qualifiers (mipmap-xhdpi/xxxhdpi banners)? CI #145 passed, so it seems to in practice. Confirm in the lint docs.
- Does Google TV's launcher refresh a sideloaded app's banner right after `adb install -r`, or only after a reboot?
