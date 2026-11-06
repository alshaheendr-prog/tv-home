# Lane 3 knowledge: Smoothness (60fps and memory on the 2GB box): archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #25 (2026-11-06, update 3: Islamic content)
- 2026-11-06 PR #25 (Update 3, Islamic content): no Must/Should. Nice: PrecomputedText for long verse lines in nextLine only if gfxinfo shows a slow frame at the switch; adhan tick per-second prefs read + string join. Curated Verses replaced the daily alquran.cloud fetch in both apps.

### PR #23 (2026-11-06, update 2: visual polish)
- No Must.
- Should: `StarField.onDraw` calls `setLocalMatrix` every frame, which rebuilds the native shader each frame. The app menu (`appMenuBox`) fades without `noAlphaLayers`, so the full-screen fade goes through an off-screen buffer. The hidden Quran page `StarField` (alpha 0, still VISIBLE) redraws every frame behind the player.
- Nice: the `lineFrame` layer is now 1920 wide (about +1 MB) and re-renders on every frame of each verse fade. `ThinBar.setColor` invalidates every tick. `MixText` passes a Spannable (`BufferType.SPANNABLE`). `IntroView.drawWelcome` runs `Paint.set` and `measureText` every frame. `ICONS` keeps dock icons twice (p:/a:) and is never trimmed.
- Fixed by the PR: `lineTimer` removed; Quran page layers hidden and the wallpaper paused (about 5.6x down to about 2x the screen per frame); icon cache and All apps grid reuse; no clip or elevation on 40 icons; `tick` skips view work while hidden; Kids banners and icons load off the UI thread; `refreshTiles` only rebuilds when something changed.

### PR #22 (2026-11-05, audit update 1)
- No Must/Should. The wallpaper player now calls stop() in onStop, which releases the decoder (verified in the 1.4.1 source). Home pressed on the home screen resumes the same clip without listFiles.
- Nice: restKeys re-animates every second while the pad rests (home and Kids); toast() setLayoutParams relayouts the stage; Kids refreshTiles runs in onStop; videoCover is not raised in onStop.
- Still open: LoadControl, onTrimMemory, lineTimer.

### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (lane 3):
  - Must: the Quran page and player redraw about 5.6x the screen per frame (`overlay()` scrim + 3 full-screen `StarField` rects + path, over the dim and the scrims) while the hidden wallpaper keeps decoding.
  - Should: wallpaper player not stopped in `onStop`, no `LoadControl`, no `onTrimMemory`; all icons rebuilt on every resume; All apps grid rebuilt on each open, with redundant `clipToOutline` and elevation on about 40 icons; `lineTimer` feeds a GONE bar 60 times a second, also in the background; `tick()` does view work while hidden; Kids `refreshTiles` + `banner()` on the UI thread every resume.
  - Nice: `drawSky` animates every minute; overlay dither; adhan `queryEvents` every second; leftover Projectivy wallpaper copy; Kids thumbs LRU by count; interpolator allocation per key; layers under a <100% stage scale.

### PR #21 (docs, 2026-11-05)
- Corrected the 1.4.1 buffer values, the compressed-buffer wording, the hidden-API status of `fullLengthAnimations()` and added the `StarField` risk.
- Corrected from lane 12's review: the box's animator scale is 1.0 (only window and transition are 0.5), so `fullLengthAnimations()` does nothing today.

### PR #20 (merged 2026-11-05)
- Must: `PinLock` ran 20,000 SHA-256 rounds on the UI thread. Fixed: single salted hash, `apply()`.
- Should: PIN migration hash and `commit()` on the UI thread. Fixed: cheap hash + `apply()`.
- Should: PIN card shake stacked ValueAnimators on repeated wrong PINs. Fixed: one animator, cancelled before restart.
- Rule added to the brief: anything over about 2 ms runs off the UI thread.
- Research notes recorded at the time (not fixed, still open): `lineTimer` keeps Choreographer busy for a hidden bar; no buffer caps on either player; no `onTrimMemory`; several full-screen translucent layers over the video; SurfaceView wallpaper means PixelCopy/blur copies come back black.

## Source library
All checked 2026-11-05 unless noted. Tags are used in Core expertise.
- [vitals/render] https://developer.android.com/topic/performance/vitals/render: frame thresholds, Record View#draw vs DrawFrame, paths, clipPath, bitmap upload, binder, GC, trace marker cost.
- [overdraw] https://developer.android.com/topic/performance/rendering/overdraw: overdraw definition, alpha rendering cost, colour-with-alpha instead of view alpha.
- [hardware-accel] https://developer.android.com/topic/performance/hardware-accel: unsupported ops table (setMaskFilter unsupported, setShadowLayer non-text API 28), 2.5x pixels rule, don't modify paths/bitmaps often.
- [hardware-accel-views] https://developer.android.com/topic/performance/views/hardware-accel-views: layer types, cheap properties, layers only for the animation, setAlpha doubles fill-rate.
- [inspect-gpu-rendering] https://developer.android.com/topic/performance/rendering/inspect-gpu-rendering: Profile GPU rendering bar segments.
- [jankstats] https://developer.android.com/topic/performance/jankstats: JankStats setup for Views, 2x heuristic.
- [macrobenchmark] https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview: metrics, separate test module, compilation modes.
- [on-device-tracing] https://developer.android.com/topic/performance/tracing/on-device: recording a system trace on the device, pulling from /data/local/traces.
- [perfetto-frametimeline] https://perfetto.dev/docs/data-sources/frametimeline: jank types and colours; no SurfaceView support; Android 12+.
- [tv-memory] https://developer.android.com/training/tv/playback/memory: TV memory budgets, media buffer sizes, onStop release, forceHasOverlappingRendering, no System.gc.
- [manage-app-memory] https://developer.android.com/topic/performance/memory/manage-app-memory: onTrimMemory on Android 14+ (only UI_HIDDEN and BACKGROUND), bitmaps, churn, services.
- [app-memory-budgets] https://developer.android.com/topic/performance/memory/app-memory-budgets: memory budget sweep and pass criteria (jank +1 point, P95 +3 ms).
- [androidgo-best-practices] https://developer.android.com/guide/topics/androidgo/best-practices?hl=en: isLowRamDevice/getMemoryClass, scale down on low RAM.
- [media3-surface] https://developer.android.com/media/media3/ui/surface: SurfaceView vs TextureView, full-resolution video on TVs that upscale UI.
- [media3-battery] https://developer.android.com/media/media3/exoplayer/battery-consumption: TextureView up to 30% more power, tunneling caveats, audio offload.
- [media3-track-selection] https://developer.android.com/media/media3/exoplayer/track-selection: how to enable tunneling and audio offload.
- [media3-loadcontrol-src] https://raw.githubusercontent.com/androidx/media/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultLoadControl.java: default buffer values in the current release branch (newer than 1.4.1).
- [media3-loadcontrol-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultLoadControl.java: the defaults this repo actually uses (2,500 / 5,000, no local-playback values). Checked 2026-11-05.
- [media3-release-notes] https://github.com/androidx/media/blob/release/RELEASENOTES.md: 1.6.0 lowered defaults, 1.9.0 added local-playback values. Checked 2026-11-05.
- [aosp-valueanimator-14] aosp-mirror platform_frameworks_base, android14-release, `core/java/android/animation/ValueAnimator.java`: `@UnsupportedAppUsage` on `setDurationScale` and `sDurationScale` (hidden-API status). Checked 2026-11-05.
- [omnirom-a13f2d58] gerrit.omnirom.org, commit a13f2d58: GPU composition at a lower resolution (`max_graphics_width`). Checked 2026-11-05.
- [mi-box-specs] mi.com/uk, the Xiaomi TV Box S 3rd Gen specs page: official decode specs (up to 4K60). Checked 2026-11-05.
- [frame-rate] https://developer.android.com/media/optimize/performance/frame-rate: setFrameRate, FIXED_SOURCE, seamless vs always, set-top box black screens.
- [source-graphics] https://source.android.com/docs/core/graphics: SurfaceFlinger, HWC, BufferQueue modes.
- [stylingandroid-rendereffect] https://blog.stylingandroid.com/rendereffect-blur: RenderEffect blur API 31+, what it blurs, RenderThread cost.
- [romain-guy-deck] https://speakerdeck.com/romainguy/android-graphics-performance: Romain Guy, Google I/O 2013. Alpha alternatives, overdraw colours, layers as barriers, mipmaps. Old but the principles hold.
- [cnx-s905x5m] https://cnx-software.com/2024/07/01/amlogic-s905x5m-vs-s905x4-features-and-benchmarks-comparison: SoC specs and GPU/CPU gains.
- [arm-mali-g310] https://arm.com/products/silicon-ip-multimedia/gpu/mali-g310: Arm's G310 positioning (TVs, STBs), AFBC.
- [androidpctv-review] https://androidpctv.com/xiaomi-tv-box-s-3rd-gen-review: this exact box: 2 GB, ~1 GB free, 32-bit Google TV 14, UI said to be 4K.
- To open next: the `View` reference for `hasOverlappingRendering` (page too long for one fetch; read with an offset), `RenderEffect` reference, Perfetto command-line recording, `dumpsys gfxinfo` docs, "Drawn out: how Android renders" (Google I/O 2018 talk).
- [aosp-animationhandler] git.aicp-rom.com mirror of `frameworks/base/core/java/android/animation/AnimationHandler.java`: `requestAnimatorsEnabled`, `mPauser` pauses only `DURATION_INFINITE` animators, property `framework.pause_bg_animations.enabled` default true. Checked 2026-11-05.
- [hwui-rendernode] https://android.googlesource.com/platform/frameworks/base/+/main/libs/hwui/RenderNode.h: `nothingToDraw()` returns true when alpha ≤ 0, scale 0, or an empty clipping outline. Checked 2026-11-05.
- [arm-hsr] https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/hidden-surface-graphics-removal-methods-in-mali-gpus: Forward Pixel Kill is a limited queue, disabled by late-Z; no promise for blended layers. Checked 2026-11-05.
- [skia-ccpr-subpixel] https://skia.googlesource.com/skia/+/644341af03d22dfe10e8e6c47e0aa21f5346031b: on Android, Skia's (CCPR) path cache ignores sub-pixel translation "to match HWUI", so a translated, unchanged `Path` can hit the cache. CCPR is an older path renderer, so this is unverified for Android 14's renderer. Checked 2026-11-05.
- [media3-hello] https://developer.android.com/media/media3/exoplayer/hello-world: release the player to free video decoders for other apps. Checked 2026-11-05.

- [media3-exoimplinternal-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/ExoPlayerImplInternal.java: stopInternal resets renderers unless foregroundMode. Checked 2026-11-05.
- [media3-mediacodecrenderer-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/mediacodec/MediaCodecRenderer.java: onDisabled calls flushOrReleaseCodec, onReset calls releaseCodec. Checked 2026-11-05.
- [media3-player-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/common/src/main/java/androidx/media3/common/Player.java: stop() releases the loaded media and playback resources. Checked 2026-11-05.

- [aosp-shader-9] https://code.yawk.at/android/android-9.0.0_r35/android/graphics/Shader.java: `setLocalMatrix` → `discardNativeInstance` (runs the Cleaner) → `createNativeInstance` on the next use. Checked 2026-11-06.
- [aosp-textview-9] https://code.yawk.at/android/android-9.0.0_r35/android/widget/TextView.java: `setTextInternal` sets `mSpannable` for Spannable text. Checked 2026-11-06.

From PR #25 (2026-11-06, update 3: Islamic content):
- [android-p-text] https://android-developers.googleblog.com/2018/07/whats-new-for-text-in-android-p.html ("measuring text can take up to 90% of the time required to set the text"); pattern PrecomputedText.create(text, tv.getTextMetricsParams()) on an executor, setText on UI. developer.android.com PrecomputedText/Compat reference pages not readable by the fetch tool.

## Open questions to research next
- Is the UI composed at 1080p or 4K on this box? Check `adb shell wm size`, `adb shell dumpsys display | grep -i mode`, `dumpsys SurfaceFlinger --display-id`. If 4K, every full-screen fill costs 4x and the overdraw budget is tighter.
- Does SurfaceFlinger put the wallpaper SurfaceView on an HWC overlay or GPU-compose it while UI layers sit above? (`dumpsys SurfaceFlinger`, look for CLIENT vs DEVICE composition.)
- Answered 2026-11-05: media3 1.4.1 has no `*_FOR_LOCAL_PLAYBACK` values (they came in 1.9.0); segment size 64 KiB, video target 125 MiB. Still open: measure the paused wallpaper player's memory while another app plays.
- What frame rates are the wallpaper clips (`ffprobe` the files in `Movies/TVWallpapers`) and what refresh rate does the box output? Judder risk if 24/25 fps on 60 Hz.
- Does `isLowRamDevice()` return true on this box? (`adb shell getprop ro.config.low_ram`)
- Is Mali-G310 tile-based with transaction elimination, and how many cores in the S905X5M? (Arm developer docs, not the product page.)
- Answered 2026-11-05: `setDurationScale` is greylisted and works at target 35; the `sDurationScale` field is blocked.
- Read `ro.surface_flinger.max_graphics_width` on the box (is the UI composed below 4K?).
- Measure `StarField` with `dumpsys gfxinfo` while the Quran panel is open.
- Does HWUI cache an unchanged, translated stroked `Path`, or re-rasterise it every frame?
- Baseline Profiles for a launcher: worth it for cold start on a 32-bit A55 box?
- Is the `StarField` lattice re-rasterised each frame on Android 14 HWUI/Skia, or cached under translation? (Partly answered: older Skia CCPR ignored sub-pixel translation on Android. Check with a Perfetto trace on the Quran page.)
- Does Amlogic put the wallpaper on the dedicated video plane (HWC DEVICE) while the UI is above it? (`dumpsys SurfaceFlinger`)
- Does a hardware layer under `stage` scale 0.94 look visibly softer on the TV? (Compare at Screen size 94% with and without `cacheCards`.)
- How much PSS does the paused wallpaper player hold behind Netflix? (Measure for S1.)
- Is the adhan overlay band over Netflix forcing GPU composition of the film? (`dumpsys SurfaceFlinger` during an adhan.)

- Measure `dumpsys meminfo com.family.tvhome` behind Netflix before and after PR #22 to size the freed decoder and buffer memory.
- Does the stopped SurfaceView wallpaper flash black for one frame on return from another app? (Watch on the TV, or use a screen recording.)

- Does Spannable text pick `DynamicLayout` through `useDynamicLayout()` on Android 14? (unverified)
- Measure `dumpsys gfxinfo` on the Quran page and in the player before and after PR #23.

From PR #25 (2026-11-06, update 3: Islamic content):
- setText time for a 2-line Amiri verse on this A55? Measure with Perfetto/gfxinfo around a verse switch.
