# Lane 3 knowledge: Smoothness (60fps and memory on the 2GB box)

## How to use this file
- Read this whole file before every review, then read the brief `../3-smoothness.md`.
- Research online whatever the PR touches that this file does not cover. Open the pages yourself and cite only what you opened.
- After every review, send the PR thread short, dated additions for: **Past findings**, **Lessons learned**, **Source library** and **Open questions**. The PR thread commits them here in the same PR.
- Keep entries short. If the file grows past about 400 lines, condense the oldest Past findings and Lessons into one-line summaries. Never delete a source that is still cited.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code or measured on the box.

## Core expertise

### 1. The frame budget and what jank means
- 60 fps gives **16 ms** per frame (90 fps: 11 ms, 120 fps: 8 ms). Overrun the window and Choreographer drops the whole frame, so the user sees a stutter, not a 1 ms delay. [vitals/render]
- **Slow frames** take 16 to 700 ms. **Frozen frames** take over 700 ms ("No frames in your app should ever take longer than 700ms"). Over 5 s is an ANR. The first frame of startup may exceed 16 ms. [vitals/render]
- Drawing has two phases. **Record View#draw** runs on the UI thread (every invalidated view's `draw(Canvas)`). **DrawFrame** runs on the RenderThread. Either can blow the budget. [vitals/render]
- JankStats calls a frame janky by default when it takes **twice** the refresh period. Tune it with `jankHeuristicMultiplier`. It builds on FrameMetrics (API 24+), tracks one Window, and must be re-enabled in `onResume`. Its listener must return quickly. [jankstats]
- The UI-thread rule this lane adopted after PR #20: **anything over about 2 ms goes off the UI thread** (hashing, parsing, disk, network, `MediaMetadataRetriever`, bitmap decode). Binder calls are "one of the most common reasons for the UI thread to stop running". Even `getRefreshRate()` can be a binder call. [vitals/render]
- GC runs on HeapTaskDaemon. Allocation in tight loops or per-frame code causes churn; the docs show a 94 ms GC. Never allocate `Paint`, `Path`, `RectF`, shaders, arrays, lambdas or formatters in `onDraw`. [vitals/render] [manage-app-memory] [hardware-accel-views]

### 2. Cheap and expensive changes in the View pipeline
- Cheap to animate (no re-record of the view): `alpha`, `translationX/Y/Z`, `x/y`, `scaleX/Y`, `rotation`, `rotationX/Y`, `pivotX/Y`. On a hardware-layered view they cause "optimal invalidation and no redrawing of the targeted view". [hardware-accel-views] [vitals/render]
- Expensive: anything that forces layout each frame (margins, padding, text size, `LayoutParams`). Weighted `LinearLayout` and `RelativeLayout` can go O(n²) with nesting depth. [vitals/render]
- Changing a `Path` creates a new GPU mask each time. Changing a bitmap's pixels re-uploads it as a texture on the next draw. [hardware-accel-views] [hardware-accel]
- `Canvas.drawPath()` rasterises on the CPU and uploads. Prefer `drawRect/RoundRect/Circle/Oval/Lines/Points`. Avoid `clipPath`; draw the shape, or use a `BitmapShader`. [vitals/render]
- The first draw of a bitmap uploads it ("Texture upload(id) w x h" in a trace), which can take several ms. Keep bitmaps no larger than drawn. Call `prepareToDraw()` after decoding so the upload happens while the RenderThread is idle (Android 7+). [vitals/render]
- Rounded-corner icons: do the decoration off the UI thread, or draw with a `BitmapShader` + `drawRoundRect` at draw time, not with CPU `drawRoundRect`+`MULTIPLY` on the UI thread. [vitals/render]
- Bitmaps scaled below 50% of their size look better with mipmaps (`setHasMipMap(true)`); trilinear filtering arrived in 4.2. [romain-guy-deck]

### 3. Alpha, overdraw and fill-rate
- Overdraw is drawing the same pixel more than once a frame. Alpha blending is "a key contributor": a transparent pixel needs the pixels below drawn first. Fades and drop shadows add to it. [overdraw]
- Rule of thumb: **draw no more than 2.5x the screen's pixels per frame**, and transparent bitmap pixels count. [hardware-accel]
- Debug GPU overdraw colours: none = true colour, then blue, green, pink/red, deep red for 1x to 4x+ extra draws (1x means drawn twice). [romain-guy-deck]
- Google's own advice: unless you target a known weak GPU, focus on UI-thread work first. This box IS a weak GPU at a 4K output, so both matter. [overdraw]
- `setAlpha` / `animate().alpha()` / `AlphaAnimation` on a view with overlapping content renders it into an **offscreen buffer, which doubles the fill-rate**. Alternatives, in order: bake alpha into the text colour; `ImageView.setImageAlpha()`; alpha on the `Paint`; `hasOverlappingRendering()=false` / `forceHasOverlappingRendering(false)` when children do not overlap; or a hardware layer on very large views. [hardware-accel-views] [romain-guy-deck] [tv-memory]
- The TV memory guide also recommends `forceHasOverlappingRendering(false)` "to avoid intermediate renders", and finding them with Android GPU Inspector ("Textures"). [tv-memory]
- `saveLayer`/`saveLayerAlpha` (which `setAlpha` may trigger) and non-rectangular clips are reordering barriers: the renderer cannot merge draw calls across them. Unclipped layers run each command N times. [romain-guy-deck]
- Set the window background to the main colour and leave containers above it with no background. Remove backgrounds that a child fully covers. [overdraw]

### 4. Hardware layers (`LAYER_TYPE_HARDWARE`)
- A layered view's drawing code does not run again until it calls `invalidate()`. That pays off for complex views that move or fade but do not change content. [hardware-accel-views]
- Cost is video memory. Google "highly recommend[s]" enabling them only for the animation, then `LAYER_TYPE_NONE` in `onAnimationEnd`. [hardware-accel-views]
- **Layer memory = view width x height x 4 bytes**, not a fixed amount. A full 1920x1080 layer is about 8.3 MB; a 1920x420 clock column about 3.2 MB; a 600x200 card about 0.5 MB. (Arithmetic from ARGB_8888 = 4 bytes/px [manage-app-memory].)
- A layered view that invalidates every frame (progress bar, equalizer, ticking seconds, an `Accent` glide) re-renders the layer every frame: worse than no layer.
- Text shadows: `setShadowLayer` on text has always been hardware-accelerated; on non-text shapes it is only since API 28. `setMaskFilter` (so `BlurMaskFilter`) is still **not supported** by the hardware pipeline; the workaround is drawing once into a software bitmap. [hardware-accel]

### 5. Blur and effects
- `RenderEffect.createBlurEffect` is API 31+. It blurs the view it is set on and its children, not what is behind. It runs on the GPU, mostly on the RenderThread, and is re-done whenever the affected views redraw. [stylingandroid-rendereffect]
- It cannot see a SurfaceView's video (that is a separate layer composed by SurfaceFlinger/HWC). PixelCopy of the SurfaceView came back black on this box. [source-graphics] (box behaviour: verified in code comment `MainActivity.java:285`)
- Over a moving video, a blur would have to redraw every frame. Treat any large blur on this GPU as a red flag.

### 6. Video: SurfaceView, composition, frame rate
- Prefer `SurfaceView`: lower power, "more accurate frame timing, resulting in smoother video playback", secure output, better HDR. `TextureView` can add up to **30%** power during playback. [media3-surface] [media3-battery]
- On TVs that upscale the UI layer, a `SurfaceView` renders the video **at the full display resolution**; TextureView cannot. So a 1080p UI over 4K video is the expected setup. [media3-surface]
- SurfaceView animation sync was fixed in Android 7 (API 24); before that it lagged or went black when moved. From Android 14, `PlayerView` uses `setSurfaceLifecycle(SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)`. [media3-surface]
- SurfaceFlinger composites surfaces; the Hardware Composer can take layers as overlays, which uses less power than GPU composition. [source-graphics] A SurfaceView with alpha, a non-trivial transform, or many UI layers over it may force GPU composition (unverified on this box: check `dumpsys SurfaceFlinger` for "DEVICE" vs "CLIENT" composition).
- Perfetto FrameTimeline does **not** cover SurfaceViews, so it measures our UI frames, not the wallpaper's. [perfetto-frametimeline]
- **Frame rate matching.** A clip whose rate does not divide the display rate needs pulldown, which causes judder (24 fps on 60 Hz needs 3:2). For video call `setFrameRate(fps, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE, ...)`, pass the exact rate (29.97, not 30), call it once, not per frame. For clips of a few minutes or less, use `CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS`; non-seamless switches on set-top boxes can black the screen for 1 to 2 s. [frame-rate]
- Practical rule for wallpaper clips: encode them at 30 or 60 fps for a 60 Hz output (25/50 if the box runs 50 Hz). A 24 or 25 fps clip on 60 Hz will judder during slow pans, whatever the UI does.
- **Tunneling** moves decode and release timing to a hardware block. It can help high-resolution video on some TVs, but is "device-specific" and must be tested per device. Enable with `DefaultTrackSelector` `setTunnelingEnabled(true)` (a preference, not a guarantee). [media3-battery] [media3-track-selection]
- Audio offload saves power for long screen-off audio only; with the screen on it has no significant effect. [media3-battery]

### 7. ExoPlayer / Media3 buffering
- Current `DefaultLoadControl` (release branch source): `DEFAULT_MIN_BUFFER_MS` 50,000; `DEFAULT_MAX_BUFFER_MS` 50,000; `DEFAULT_BUFFER_FOR_PLAYBACK_MS` 1,000; after rebuffer 2,000; back buffer 0. Newer versions add `*_FOR_LOCAL_PLAYBACK` values (min buffer 1,000 ms, video buffer 300 segments instead of 2,000) and prioritise time over size for local playback. [media3-loadcontrol-src]
- Target bytes, when unset, are summed per selected track: video 2000 x `C.DEFAULT_BUFFER_SEGMENT_SIZE` for streams, audio 200 x segment, clamped between min (200 x) and max sizes. (Segment size is 64 KB from memory, so about 128 MB for video: **unverified**, check `C.java`.) [media3-loadcontrol-src]
- Builder: `setBufferDurationsMs`, `setBufferDurationsMsForLocalPlayback`, `setTargetBufferBytes`, `setPrioritizeTimeOverSizeThresholds`, `setBackBuffer`. [media3-loadcontrol-src]
- **Whether 1.4.1 (this repo) has the local-playback constants is unverified.** If it does not, the wallpaper player can hold tens of MB of decoded-sample buffer for a short looping local file. A cap such as `setBufferDurationsMs(2000, 5000, 500, 1000)` plus a `setTargetBufferBytes` of a few MB is the usual fix; measure before and after with `dumpsys meminfo`.
- Google's TV guidance: media buffers sized for about 1 minute; for a 2 GB 2160p device 100 to 120 MB. Don't buffer more than 5 s of future while seeking. Free media allocations when changing an episode. Non-audio apps should stop playback in `onStop` and release resources. [tv-memory]

### 8. Memory on a 2 GB TV
- Device classes: 2 GB+ boxes play 2160p video with a 1080p UI and are usually **not** `isLowRamDevice()`; always ask the API rather than guessing. [tv-memory]
- Budgets Google gives for 1 GB low-RAM TVs (use them as the ceiling of good behaviour): Anon+Swap < 160 MB, Graphics 30 to 40 MB, File 60 to 80 MB, total < 280 MB, strongly recommended Anon+Swap+Graphics < 200 MB. [tv-memory]
- Don't load images larger than the UI resolution. Use hardware bitmaps where possible. Prefer colour placeholders over placeholder images. [tv-memory]
- Avoid `System.gc()`; on zRAM devices it can temporarily *increase* memory use. Frequent direct reclaim is a sign apps are using too much. [tv-memory]
- `onTrimMemory`: from Android 14 the system only delivers **`TRIM_MEMORY_UI_HIDDEN`** (release UI memory: bitmaps, player buffers, animation resources) and **`TRIM_MEMORY_BACKGROUND`** (release anything easy to rebuild). The other levels were formally deprecated in Android 15. Keep the handler in the Activity. [manage-app-memory]
- A 1000x1000 ARGB_8888 bitmap is 4 MB whatever the JPEG size. Bitmaps are usually the largest objects. [manage-app-memory]
- Leaks usually come from static references. Services left running are "one of the worst memory-management mistakes". [manage-app-memory]
- Low-RAM best practices: check `isLowRamDevice()` and `getMemoryClass()` and scale caches and image sizes; avoid wake locks and background work. [androidgo-best-practices]
- App memory budgets (`<memory-budget>` in the manifest, Android 17 QPR2+) are calibrated by a sweep: pass when janky frames rise at most 1 point and P95 frame time at most 3 ms. The same pass criteria are a good bar for any memory change here. [app-memory-budgets]

### 9. This GPU and SoC
- Amlogic S905X5M: 6 nm, 4x Cortex-A55 up to 2.5 GHz, **Mali-G310 V2**, DDR4/LPDDR4 up to DDR3200, AV1/H.265 decode up to 4Kp75 (per article), an AI-SR video upscaler. GPU 3D scores are about 1.4x to 2.8x the S905X4; CPU 16 to 36% better. [cnx-s905x5m]
- Mali-G310: Arm's entry Valhall GPU for TVs and set-top boxes. Arm claims texturing up to 6x Mali-G31 and Android UI up to 2x previous generations; it supports AFBC. Core count is not public on the product page. [arm-mali-g310]
- Xiaomi TV Box S 3rd Gen: 2 GB DDR4, about 1 GB free after boot, Google TV 14 in 32-bit mode, Mali-G310 V2. The reviewer says the "Android UI renders at 4K" (unverified; this matters for fill-rate, see Open questions). [androidpctv-review]
- Fill-rate arithmetic: a 1080p frame is 2.07 M px; a 4K frame is 8.3 M px. Five full-screen translucent layers at 1080p is about 10 M blended px per frame (about 5x the screen, double the 2.5x rule of thumb). At a 4K UI it would be 41 M.

### 10. How to measure
- `adb shell dumpsys gfxinfo com.family.tvhome reset`, use the screen, then `dumpsys gfxinfo com.family.tvhome` for Janky frames and 50/90/95/99th percentiles; `... framestats` for per-frame rows (duration is `FrameCompleted - IntendedVsync`). (Field names from experience, not re-read this time.)
- Profile GPU rendering ("On screen as bars"): green line = 16.67 ms. Segments: Swap Buffers (GPU too busy), Command Issue, Sync & Upload (bitmap uploads), Draw (recording), Measure/Layout, Input & Animation, Misc/VSync delay (UI-thread work between frames). [inspect-gpu-rendering]
- Overdraw: Developer options > Debug GPU overdraw. If it is heavy and the "Process"/Swap bar spikes, it is an overdraw problem. [overdraw]
- System trace on the box: Developer options > Debugging > System Tracing (app menu works without touch). Android 10+ writes `.perfetto-trace` into `/data/local/traces/`; pull with adb, open in ui.perfetto.dev. Keeps a 10 to 30 s rolling buffer. [on-device-tracing]
- Perfetto FrameTimeline (Android 12+): red = app's fault, yellow = SurfaceFlinger's, blue = dropped. Jank types: App Deadline Missed, Buffer Stuffing, SF CPU/GPU deadline missed, Display HAL, Prediction Error. Config: `data_sources { config { name: "android.surfaceflinger.frametimeline" } }`. [perfetto-frametimeline]
- Trace markers cost about 10 µs each; don't put them on methods called dozens of times a frame. [vitals/render]
- Macrobenchmark (`FrameTimingMetric`, `StartupTimingMetric`) needs a separate `com.android.test` module, which is a build change: suggest it, don't require it in a polish PR. [macrobenchmark]
- Memory: `adb shell dumpsys meminfo com.family.tvhome` (Java, Native, Graphics, Total PSS). Compare before and after the PR, with the video playing, on the same clip.

### 11. Review recipes (patterns to suggest; adapt before use)
Hardware layer only for an animation [hardware-accel-views]:
```java
v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
v.animate().alpha(0f).setDuration(300)
    .withEndAction(() -> v.setLayerType(View.LAYER_TYPE_NONE, null)).start();
// or simply: v.animate().alpha(0f).withLayer().start();
```
Alpha without an offscreen buffer [romain-guy-deck] [overdraw]:
```java
text.setTextColor((Math.round(a * 255) << 24) | (base & 0xFFFFFF)); // TextView: bake alpha into colour
image.setImageAlpha(Math.round(a * 255));                           // ImageView
group.forceHasOverlappingRendering(false);                          // children do not overlap
```
A small buffer for a short local looping wallpaper (values to measure, not gospel) [media3-loadcontrol-src] [tv-memory]:
```java
LoadControl lc = new DefaultLoadControl.Builder()
    .setBufferDurationsMs(2_000, 5_000, 500, 1_000) // min, max, forPlayback, afterRebuffer
    .setTargetBufferBytes(8 * 1024 * 1024)
    .setPrioritizeTimeOverSizeThresholds(false)
    .build();
player = new ExoPlayer.Builder(this).setLoadControl(lc).build();
```
Trim memory on Android 14+ (only two levels arrive) [manage-app-memory]:
```java
@Override public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) { /* drop icon/bitmap caches, stop the wallpaper player */ }
    if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) { /* drop anything cheap to rebuild */ }
}
```
A repeating animation that respects visibility (the pattern `lineTimer` lacks):
```java
if (lineBar.getVisibility() == View.VISIBLE) lineTimer.start(); // never animate a GONE view
@Override protected void onPause() { lineTimer.pause(); super.onPause(); }
```
Bitmap upload off the critical frame [vitals/render]:
```java
Bitmap b = decodeOnBg(...); b.prepareToDraw(); ui.post(() -> icon.setImageBitmap(b));
```
JankStats for a one-off measurement build (do not ship as a feature) [jankstats]:
```java
jank = JankStats.createAndTrack(getWindow(), f -> { if (f.isJank()) Log.d("jank", f.toString()); });
// onResume: jank.setTrackingEnabled(true); onPause: jank.setTrackingEnabled(false);
```
Frame rate hint for a video surface, once per clip, never per frame [frame-rate]:
```java
video.getHolder().getSurface().setFrameRate(clipFps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
        Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS); // API 31 overload; clipFps exact, e.g. 29.97f
```

### 12. Commands for the box (copy and paste)
```sh
adb shell dumpsys gfxinfo com.family.tvhome reset      # then use the screen for 30 s
adb shell dumpsys gfxinfo com.family.tvhome | grep -E "Janky|percentile|Missed Vsync|Slow"
adb shell dumpsys meminfo com.family.tvhome            # Java, Native, Graphics, TOTAL PSS
adb shell setprop debug.hwui.overdraw show             # then reopen home (unset: setprop ... false)
adb shell setprop debug.hwui.profile visual_bars       # Profile GPU rendering bars
adb shell wm size; adb shell wm density                # is the UI 1080p or 4K?
adb shell dumpsys SurfaceFlinger | grep -iE "composition|DEVICE|CLIENT" # overlay or GPU?
adb shell getprop ro.config.low_ram                    # low-RAM flag
adb pull /data/local/traces/                           # traces from on-device System Tracing
```
(The `setprop` names and `wm` commands are standard developer tools but were not re-read from a page this time: verify if they fail.)

### 13. Numbers to remember
| What | Number | Source |
|---|---|---|
| Frame budget 60/90/120 Hz | 16 / 11 / 8 ms | [vitals/render] |
| Frozen frame | > 700 ms | [vitals/render] |
| JankStats default jank | 2x refresh period | [jankstats] |
| UI-thread work rule (this lane) | < 2 ms | PR #20 |
| Overdraw rule of thumb | <= 2.5x screen pixels per frame | [hardware-accel] |
| `setAlpha` on overlapping view | ~2x fill-rate | [hardware-accel-views] |
| Layer memory | w x h x 4 bytes (1920x1080 = 8.3 MB) | arithmetic |
| TextureView power penalty | up to 30% | [media3-battery] |
| ExoPlayer default min/max buffer | 50 s / 50 s | [media3-loadcontrol-src] |
| TV media buffer, 2 GB 2160p | 100 to 120 MB for ~1 min | [tv-memory] |
| Seek pre-buffer | <= 5 s, 15 to 25 MB | [tv-memory] |
| 1 GB TV budgets | Anon+Swap 160, Graphics 30-40, total 280 MB | [tv-memory] |
| Trace marker cost | ~10 us | [vitals/render] |
| STB refresh-rate switch | black for 1 to 2 s | [frame-rate] |

## This box and this code
Verified by reading the code on 2026-11-05 (line numbers drift; re-grep).
- Modules: `app` (minSdk 28, target 35, `androidx.media3:media3-exoplayer:1.4.1`), `kids` (minSdk 33), `adhan` (minSdk 28). Both apps set `android:hardwareAccelerated="true"`.
- `MainActivity.java` is about 3,500 lines; all views are built in code on a 1920x1080 `stage` scaled by `fitStage()` (~`:763`, `FITS = {100,97,94,91,88}` `:760`, default `"fit"` 100).
- Window: `FLAG_KEEP_SCREEN_ON`; `root` is black; `video` is a `SurfaceView` (`:205`), then full-screen `videoCover` (gradient, faded to alpha 0 after the first frame), `skyTint` (alpha 0 except at Fajr/Maghrib), `dimView` (black at `baseDim()`, `WALL_DIM 0.08` `:3076`), `topScrim` (380 px, 50% to 0), `bottomScrim` (480 px, 70% to 35% to 0). Views at alpha 0 are skipped by the renderer, so the real full-screen blends at rest are about 1 (dim) plus two partial scrims, rising to 2 when skyTint shows (reasoning from View behaviour, unverified by overdraw tool).
- Wallpaper player: `new ExoPlayer.Builder(this).build()` (`:249`), no `LoadControl`, `REPEAT_MODE_ONE`, volume 0. `onPause` pauses it; `onStop` does not stop or release it; `onDestroy` releases both players (`:432`). So while the family watches another app the paused player keeps its buffers and decoder (verified in code; memory size unmeasured).
- `playRandomClip()` (`:1459`) lists `Movies/TVWallpapers/*.mp4` on the UI thread (`listFiles`, `canRead`) on every `onResume`. Cheap for a few files; would matter on slow USB storage (unverified).
- No `onTrimMemory` in `app` or `kids` (grep).
- `cacheCards(true)` layers `topParts[0]`, `eventChip`, `sunCard`, `lineFrame`, `centerGroup` permanently (`:332`); `sunCard` and `topParts[0]` are GONE, so their layers are never drawn. `centerGroup` is a 1920-wide column (clock, weather, dates). `playerSurah`/`playerReciter` are layered too (`:518`, `:524`).
- `noAlphaLayers(root)` sets `forceHasOverlappingRendering(false)` on the whole tree after build (`:1890`); `show()` does it for new views.
- `lineTimer` (`:1621`): a 29 s linear ValueAnimator per 30 s verse (`LINE_SECONDS 30`) feeding `lineBar.setProgress` while `lineBar` is GONE (`:1124`). The screen never idles. Still present.
- `Accent.glideTo`: 1.6 s animator calling `invalidateSelf()` on every registered drawable (`Accent.java:40`, `:55`), held through `WeakReference`s.
- `EqualizerView` reuses its Paint/RectF and only re-posts while playing or settling (`:42`). `StarField` posts every frame unconditionally (`:60`) while attached and visible (check its visibility gating in a PR that touches it).
- `IntroView` (splash) creates new `RadialGradient`/`LinearGradient` shaders inside its per-frame draw (`:192`, `:198`, and more): allocation per frame, but only during the splash.
- `Glass.java` allocates Paints/RectFs/shaders only in the bitmap path for `glow > 0` (`:62-81`) and in `buildBody` on bounds change; `BlurMaskFilter` is drawn into a bitmap there, which is the correct workaround.
- `ThinBar` puts `setShadowLayer` on a non-text fill (`:21`): hardware-supported from API 28 but redrawn each frame it changes.
- Dock `magnify()` (`:1363`) starts `animate()` scale/translation on every tile per key press, 240 ms; the properties are all cheap ones.
- `drift()` (`:3423`) moves the SurfaceView by `translationX` over 1,400 ms; `DRIFT_ROOM 0.022`.
- `fullLengthAnimations()` (`:3349`) calls hidden `ValueAnimator.setDurationScale` or the `sDurationScale` field by reflection; fallback keeps the box's 0.5x.
- Text shadows: `TEXT_SHADOW = 4f` with `0x99000000` on most home text; clock `setShadowLayer(6, 0, 3, 0x73000000)` (`:1046`).
- Kids: `MediaPlayer` on a `SurfaceView` (`KidsActivity.java:118`, `:409`, looping), Baloo loaded once (`:112`), dim folded into shades (`:918`).
- Box: Mali-G310 V2, 2 GB, Google TV 14 (32-bit), about 1 GB free (from review, unverified on this unit). Whether the UI is composed at 1080p or 4K on this unit is **unverified**.

## Past findings
### PR #20 (merged 2026-11-05)
- Must: `PinLock` ran 20,000 SHA-256 rounds on the UI thread. Fixed: single salted hash, `apply()`.
- Should: PIN migration hash and `commit()` on the UI thread. Fixed: cheap hash + `apply()`.
- Should: PIN card shake stacked ValueAnimators on repeated wrong PINs. Fixed: one animator, cancelled before restart.
- Rule added to the brief: anything over about 2 ms runs off the UI thread.
- Research notes recorded at the time (not fixed, still open): `lineTimer` keeps Choreographer busy for a hidden bar; no buffer caps on either player; no `onTrimMemory`; several full-screen translucent layers over the video; SurfaceView wallpaper means PixelCopy/blur copies come back black.

## Lessons learned
- 2026-11-05: Security code is also performance code. Key-derivation loops belong on `bg`, and their cost must be measured on this CPU (4x A55), not assumed from a phone.
- 2026-11-05: Repeated key presses are the usual way animations stack. Look for `animate()`/`ValueAnimator` started from key handlers without a `cancel()`.
- 2026-11-05: "Hardware layer = 8 MB" was wrong as a general rule: layer memory is the view's own w x h x 4. Compute it per view.
- 2026-11-05: An invisible view can still cost frames if an animator keeps feeding it (`lineTimer`). Check what a GONE view's animators are doing.

## Open questions to research next
- Is the UI composed at 1080p or 4K on this box? Check `adb shell wm size`, `adb shell dumpsys display | grep -i mode`, `dumpsys SurfaceFlinger --display-id`. If 4K, every full-screen fill costs 4x and the overdraw budget is tighter.
- Does SurfaceFlinger put the wallpaper SurfaceView on an HWC overlay or GPU-compose it while UI layers sit above? (`dumpsys SurfaceFlinger`, look for CLIENT vs DEVICE composition.)
- Does media3 1.4.1 have the `*_FOR_LOCAL_PLAYBACK` LoadControl values? Check its `DefaultLoadControl.java` tag and `C.DEFAULT_BUFFER_SEGMENT_SIZE`. Measure the paused wallpaper player's memory while another app plays.
- What frame rates are the wallpaper clips (`ffprobe` the files in `Movies/TVWallpapers`) and what refresh rate does the box output? Judder risk if 24/25 fps on 60 Hz.
- Does `isLowRamDevice()` return true on this box? (`adb shell getprop ro.config.low_ram`)
- Is Mali-G310 tile-based with transaction elimination, and how many cores in the S905X5M? (Arm developer docs, not the product page.)
- Is hidden-API reflection in `fullLengthAnimations()` still allowed on Android 14 for target 35? Look up the non-SDK interface lists for `ValueAnimator.setDurationScale`.
- Baseline Profiles for a launcher: worth it for cold start on a 32-bit A55 box?

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
- [media3-loadcontrol-src] https://raw.githubusercontent.com/androidx/media/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultLoadControl.java: actual default buffer values in the current release branch (newer than 1.4.1).
- [frame-rate] https://developer.android.com/media/optimize/performance/frame-rate: setFrameRate, FIXED_SOURCE, seamless vs always, set-top box black screens.
- [source-graphics] https://source.android.com/docs/core/graphics: SurfaceFlinger, HWC, BufferQueue modes.
- [stylingandroid-rendereffect] https://blog.stylingandroid.com/rendereffect-blur: RenderEffect blur API 31+, what it blurs, RenderThread cost.
- [romain-guy-deck] https://speakerdeck.com/romainguy/android-graphics-performance: Romain Guy, Google I/O 2013. Alpha alternatives, overdraw colours, layers as barriers, mipmaps. Old but the principles hold.
- [cnx-s905x5m] https://cnx-software.com/2024/07/01/amlogic-s905x5m-vs-s905x4-features-and-benchmarks-comparison: SoC specs and GPU/CPU gains.
- [arm-mali-g310] https://arm.com/products/silicon-ip-multimedia/gpu/mali-g310: Arm's G310 positioning (TVs, STBs), AFBC.
- [androidpctv-review] https://androidpctv.com/xiaomi-tv-box-s-3rd-gen-review: this exact box: 2 GB, ~1 GB free, 32-bit Google TV 14, UI said to be 4K.
- To open next: the `View` reference for `hasOverlappingRendering` (page too long for one fetch; read with an offset), `RenderEffect` reference, Perfetto command-line recording, `dumpsys gfxinfo` docs, "Drawn out: how Android renders" (Google I/O 2018 talk).
