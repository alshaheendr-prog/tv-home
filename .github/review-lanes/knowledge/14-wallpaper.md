# Lane 14 knowledge: Wallpaper and backgrounds

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../14-wallpaper.md`.
- History lives in [archive/14-wallpaper.md](archive/14-wallpaper.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
- Mark anything you could not verify with **(unverified)**. Move it to "verified" only when you have read the code, probed the file, or measured on the box.
- Neighbouring lanes: 3 (decoder, buffers, memory, fill-rate: read `knowledge/3-smoothness.md` sections 6 and 7 before any player change), 11 (contrast of text over the video), 4 (whole-screen mood), 12 (fade timings), 1 (screensaver settings code).

## Core expertise

### 1. Frame rate, refresh rate and judder
- A 60 Hz display shows a new picture every 16.7 ms. A clip whose rate does not divide 60 needs pulldown. 24 fps on 60 Hz: 60/24 = 2.5 refreshes per frame is impossible, so frames are held **3 then 2** refreshes (**50 ms, 33 ms**), and "that uneven cadence makes equal-speed motion arrive at the eye unevenly": judder. [pulldown]
- 25 fps on 60 Hz: 60/25 = 2.4, so the pattern is 3,2,3,2,2 refreshes (50/33/50/33/33 ms), uneven and not even regular (arithmetic). 30 fps: every frame twice (even). 50 fps on 60 Hz: one frame in five held twice, a regular hitch (arithmetic).
- Android lets a video surface ask for its rate with `Surface.setFrameRate(fps, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE, …)`. Some devices switch refresh seamlessly; others "briefly show a black screen, usually lasting a second", so non-seamless switches are opt-in. [frame-rate]
- **ExoPlayer 1.4.1 does this for us by default**: `videoChangeFrameRateStrategy = VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS`; it calls the two-argument `setFrameRate(rate, FIXED_SOURCE)` (seamless only) once it is confident of the rate (5 s of matching frames for high confidence), and clears it with 0 when stopped. [exo-141] [vfrh-141] [media3-C]
- Both apps' `preferSixtyHz()` (home `MainActivity.java:740-755`, Kids `KidsActivity.java:233`) sets `preferredDisplayModeId` to a 59-61 Hz mode at the current resolution (only when the current mode isn't already that mode); with it set, "the video surface is forced to pick" that rate [frame-rate], so ExoPlayer's seamless `setFrameRate` vote can't change it. So the output is held at 60 Hz, 24/25 fps clips always judder, and **only 30 or 60 fps clips play without judder**. The chosen mode may be 59.94 or 60.00 Hz (unverified): a 29.97 fps clip on 60.00 Hz repeats one frame about every 33 s. Even if a switch to 24 Hz did happen, every UI animation on top would then run at 24 fps: wallpapers should simply be 30 or 60 fps.
- Aerial Views offers "Refresh rate switching for 24fps, 30fps, 50fps, 60fps content" for its own full-screen videos, which have no UI on top. [aerial]

### 2. Codecs, profiles and what the box decodes
- Android's required decoders: H.264 Baseline (3.0+) and Main (6.0+); **HEVC Main Profile Level 4.1 for Android TV** (Main L3 on mobile); VP9; AV1 decoder mandatory from Android 14. HDR rows: HDR10 and HDR10+ are HEVC 10-bit PQ. [formats]
- Devices must support resolution and frame-rate switching within one stream for VP8, VP9, H.264 and HEVC. [formats]
- This box (from lane 3's sources): Amlogic S905X5M, decode up to 4K60 (Xiaomi spec) for H.265 and AV1; Mali-G310; 2 GB RAM. [lane3: mi-box-specs, cnx-s905x5m]
- Our scripts: 1080p **H.264 High, level 4.2**; 4K **HEVC** via x265 with `pix_fmt yuv420p`, i.e. **Main (8-bit)**. Whether this decoder handles HEVC Main10 at 4K60 is very likely (the SoC is sold for HDR10) but **unverified**: check `adb shell dumpsys media.codec` or play a test file.
- HEVC levels (Main tier): 1080p60 needs L4.1 (20 Mb/s max); 4K30 needs L5 (25 Mb/s); 4K60 needs L5.1 (40 Mb/s). Android TV only guarantees Main L4.1, so 4K HEVC wallpapers depend on the box's own spec. The Studio's VBV (20 Mb/s at 30 fps, 30 Mb/s at 60 fps) fits L5/L5.1. [hevc-levels]
- Rule for wallpapers: H.264 High@4.1/4.2 at 1080p, HEVC Main or Main10 at 4K, AV1 only after testing on the box. One file format: `.mp4` with `+faststart`.

### 3. Bitrate, GOP and rate control (x264/x265)
- CRF is constant-quality; x265's default CRF is 28 (higher = lower quality). Combining CRF with VBV (`vbv-maxrate` + `vbv-bufsize`, both required) caps peaks so the decoder never starves. [x265-cli]
- `--keyint` default 250; set `min-keyint = keyint` for a fixed GOP; `--scenecut 0` disables adaptive keyframes. Our scripts use a 2 s GOP with scenecut off (good: predictable seeks and loop restarts). [x265-cli]
- `--open-gop` is **on by default** in x265 (I-slices can be non-IDR). For a file that restarts at frame 0 every loop this is harmless, but `--no-open-gop` makes every GOP self-contained, safer for seeking and looping. [x265-cli]
- `--aq-mode 3` = auto-variance with a **bias to dark scenes**, "recommended for 8-bit encodes or low-bitrate 10-bit encodes, to prevent color banding/blocking". Our 4K script uses the default (2). [x265-cli]
- Rough sizes that look premium (design judgement, check on the box): 1080p30 calm scene 3 to 6 Mb/s; 1080p60 5 to 9; 4K30 HEVC 12 to 20; 4K60 HEVC 20 to 30. The sample world clips are 0.9 to 3.0 Mb/s 1080p60 (flat cartoon art, so fine).

### 4. Bit depth and banding
- 8-bit has 256 levels per channel, 10-bit 1,024; a sky spanning 60 levels gets about 30 representable steps at 8-bit and about 240 at 10-bit, so banding (most visible in skies, fades and low light) drops sharply. Encoding 10-bit from an 8-bit source also saves bits (about 5 to 12% in HEVC tests at 4K, less at 1080p). [fora-10bit]
- `-pix_fmt yuv420p10le` with libx265 gives **HEVC Main 10**; with libx264 it gives High 10, which almost no hardware decodes, so **never 10-bit H.264**. Verify with `ffprobe -show_streams` (pix_fmt `yuv420p10le`, profile Main 10). [fora-10bit]
- `gradfun` (deband by dithering) "is designed for playback only. Do not use it prior to lossy compression, because compression tends to lose the dither and bring back the bands." `deband` replaces banded pixels with averages (defaults: thresholds 0.02, range 16). [ffmpeg-filters]
- Google's TV colour guide: banding is a common TV problem with gradients; avoid extreme transitions and dither (banding comes from few 8-bit steps spread wide: lane 11); most TVs run in Standard picture mode and support sRGB; its 562:1 example is labelled "Low" contrast. [color-on-tv]
- Our UI gradients (scrims, `videoCover`) sit on top of the video at 8-bit. Large dark gradients on top of a dark clip are the worst case: judge in Standard mode, in a dark room.

### 5. HDR vs SDR and colour tagging
- HDR playback needs an HDR-capable decoder and display (`Display.getHdrCapabilities()`); from Android 13, HLG10 is the minimum for HDR-capable devices. Use SurfaceView for HDR; TextureView transcodes HDR to SDR with "possible loss of detail including clipped colors and video banding". Android screenshots are SDR (HDR is tone-mapped). [hdr]
- For a wallpaper under SDR text, **ship SDR**: an HDR clip would switch the TV into HDR mode (brightness and colour jumps when returning home) and the UI's whites would look dull next to HDR highlights (design judgement; HDR mode switching on this box is unverified).
- HDR to SDR in ffmpeg needs linear light: `zscale=transfer=linear,tonemap=…,zscale=transfer=bt709,format=yuv420p` (tonemap algorithms: none, clip, linear, gamma, reinhard, hable, mobius). [ffmpeg-filters] `zscale` needs an ffmpeg build with zimg (the gyan.dev essentials build: unverified).
- Tag SDR HD as **BT.709**: `-colorspace bt709 -color_primaries bt709 -color_trc bt709` (ffmpeg) or x265 `--colorprim bt709` etc. (default "undefined, not signaled"). [x265-cli] `setparams` can force the tags in a filter graph. [ffmpeg-filters] Without explicit tags ffmpeg passes the source's tags through (tested: a BT.709 source through `fps,scale(lanczos),crop,setsar,format=yuv420p` + `-map_metadata -1` came out tagged BT.709); untagged stays untagged. An untagged or BT.601 (`bt470bg`) tagged HD file may be shown with the wrong matrix: slightly shifted greens and reds (design judgement; how this decoder treats untagged HD is unverified).

### 6. Seamless loops
- The cleanest loop is a clip that already loops (Wallpaper Engine clips often do): cut at the frame where the end matches the start. Wallpaper Studio finds it with SSIM over the last 4 s (score at least 0.9). Otherwise a cross-fade: play from F to the end and, during the last F seconds, fade in the first F seconds (`xfade=transition=fade:duration=F:offset=len-2F`; duration 0 to 60 s, default 1). [ffmpeg-filters]
- The cross-fade hides a jump but shows a brief double exposure; on fast motion it looks like a ghost. Prefer slow scenes (clouds, water, breathing), as the Studio guide already says.
- ExoPlayer `REPEAT_MODE_ONE` repeats the current item endlessly, and transitions between items are seamless (gapless). [playlists] MediaPlayer `setLooping(true)` keeps the player in Started state; no gaplessness promise is documented (`setNextMediaPlayer` exists for chaining). [mediaplayer] So the Kids loop may stall briefly at the seam (unverified).
- A fixed GOP and a keyframe at frame 0 make the restart cheap. Check the last frame is not a duplicate of the first (a hold of one frame is a visible hitch at 60 fps).

### 7. Scaling, cropping and the surface
- `VIDEO_SCALING_MODE_SCALE_TO_FIT`: "The content is scaled to the surface dimensions" (aspect ratio ignored; you "must position the output Surface so that it has the proper final display aspect ratio"). `SCALE_TO_FIT_WITH_CROPPING`: aspect kept, whole surface used, may crop; only for square-pixel content. [mediacodec]
- ExoPlayer 1.4.1's default is `VIDEO_SCALING_MODE_DEFAULT = SCALE_TO_FIT`, applied only to MediaCodec renderers on a SurfaceView. [media3-C] [exo-141] Our SurfaceView is 16:9 (screen + 2.2% each side), so 16:9 files are right and anything else stretches. The scripts crop to 16:9 (`force_original_aspect_ratio=increase,crop`).
- The drift margin means about 4.4% of the picture is never shown and a 1080p clip is upscaled 1.044x (soft, not visible from the sofa). Keep important content away from the edges.
- On TVs that upscale the UI, a SurfaceView video is composed at full display resolution, so a 4K clip can be sharper than the 1080p UI. [media3-surface]

### 8. Legibility, calm and mood
- Busy video behind text is the hardest case for contrast (lane 11 owns the maths: white text needs a backdrop luminance of at most about 0.16 for 4.5:1). Our protection: 8% dim everywhere, top scrim 50% to 0 over 380 px, bottom scrim 70% to 35% to 0 over 480 px, tight text shadows, glass tint.
- Apple-style calm (design judgement): slow camera, no cuts, gentle light change, mid-to-low brightness, cool or warm but not both; avoid flashing, strobing, fast fights and big text in the clip. A loop of 20 s to a few minutes; longer loops make the repeat less noticeable.
- The accent picks the clip's dominant hue from colourful pixels, then softens it; grey clips give no tint. It reads only two early frames, so a clip that starts on a title or a black fade gives a wrong or empty accent: the scripts skip the start for Quran backgrounds (middle 10 minutes) but not always for wallpapers.
- Flashing limits (ITU-R BT.1702-2): a flash sequence is harmful when it has more than 3 flashes (6 luminance changes) in any 1 s, the flashes together cover more than ¼ of the screen, and each change is ≥ 20 cd/m² (darker image below 160 cd/m²). A change to or from saturated red is harmful at any luminance. Flashes whose leading edges are ≥ 334 ms apart (60 Hz) are always fine. Use this to flag busy fan clips. [bt1702]
- Burn-in: Aerial Views alternates overlay positions to avoid burn-in on QD/OLED TVs. [aerial] Our clock and strip are static on a never-sleeping home; the 60% night dim after 15 minutes is the protection.

### 9. Screensaver
- A `DreamService` can be non-interactive, fullscreen and keep the screen bright (`setInteractive`, `setFullscreen`, `setScreenBright`). [dream-ref]
- Aerial Views: Apple, Amazon, Jetson Creative and Robin Fourcade videos plus USB/Samba/WebDAV/Immich; 4K Dolby Vision HDR "if your TV supports it" (Apple's videos are Dolby Vision only, a black screen otherwise); overlays (clock, location, date) with position alternation; playlist and loop options; refresh-rate switching. [aerial]
- Ours: the home sets `FLAG_KEEP_SCREEN_ON`, so the saver never runs over it; the Parent menu sets `screensaver_components` to Aerial Views or back to the previous one (default `com.google.android.apps.tv.dreamx/.service.Backdrop`).

### 10. Review recipes
Probe every clip a PR adds or a script makes:
```sh
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name,profile,level,width,height,r_frame_rate,pix_fmt,color_space,color_transfer,color_primaries,bit_rate -of compact clip.mp4
ffprobe -v error -select_streams a -show_entries stream=codec_name -of csv clip.mp4   # should print nothing
```
A wallpaper encode that follows this file (adapt; test on the box):
```sh
ffmpeg -i in.mp4 -an -vf "setpts=N/(30*TB),fps=30,scale=3840:2160:force_original_aspect_ratio=increase:flags=lanczos,crop=3840:2160,setsar=1" \
  -c:v libx265 -preset medium -crf 20 -pix_fmt yuv420p10le -tag:v hvc1 \
  -x265-params "keyint=60:min-keyint=60:scenecut=0:open-gop=0:aq-mode=3:vbv-maxrate=25000:vbv-bufsize=50000" \
  -colorspace bt709 -color_primaries bt709 -color_trc bt709 -movflags +faststart out.mp4
```
(For 24/25 fps sources: `setpts=N/(30*TB),fps=30` retimes to an even 30 fps (plays 25%/20% faster; tested: 48 frames in, 48 unique frames out, each once), fine for slow ambient scenes. For 50 fps: `setpts=N/(60*TB),fps=60`. For 30/60 fps sources drop the `setpts` and keep `fps=30`/`fps=60`. `fps=60` or `-r 60` on a 24/25 fps source does **not** remove judder: it bakes the same cadence into the file (ffmpeg 6.1.1 framemd5: 24→60 repeats 3,2,3,2; 25→60 repeats 2,3,2,3,2). Other options: `minterpolate=fps=60:mi_mode=mci` (slow, warps edges and occlusions) or choose 30/60 fps sources.)
Check a recipe's real cadence: `ffmpeg -f lavfi -i testsrc2=r=24:d=1 -vf "…" -f framemd5 -` and `uniq -c` on the hashes.
Look at a loop seam: `ffmpeg -sseof -1 -i clip.mp4 -frames:v 60 end%02d.png` and `ffmpeg -i clip.mp4 -frames:v 60 start%02d.png`, then compare the last end frame and the first start frame.
On the box: `adb shell dumpsys SurfaceFlinger | grep -iE "refresh|fps"` (output refresh), `adb shell dumpsys media.codec` (decoders), `adb shell screenrecord --time-limit 20 /sdcard/seam.mp4` across a loop.

### 11. Numbers to remember
| What | Number | Source |
|---|---|---|
| 24 on 60 Hz cadence | 3:2, 50 ms / 33 ms | [pulldown] |
| Non-seamless refresh switch | black for about 1 s | [frame-rate] |
| ExoPlayer frame-rate confidence | 5 s of matching frames | [vfrh-141] |
| Android TV HEVC minimum | Main L4.1 | [formats] |
| 8 / 10-bit levels | 256 / 1,024 | [fora-10bit] |
| x265 default CRF / keyint | 28 / 250 | [x265-cli] |
| Wall dim / night dim | 8% / 60% after 15 min | code |
| Scrims | top 50%→0 over 380 px; bottom 70%→35%→0 over 480 px | code |
| Drift room | 2.2% per side | code |

## This box and this code
Verified by reading the code and the scripts, and probing the sample clips, on 2026-11-05 (line numbers drift; re-grep).
- Home layer order and values: see the brief. `baseDim()` = `1 - (1 - 0.08) * brightness/100` (`:3077`); brightness 100/85/70 gives dim 8/22/36%. `skyTint` ramps over 2 s (`drawSky`, `:3434`).
- `ExoPlayer.Builder(this).build()` (`:249`), `setVideoSurfaceView(video)`, `REPEAT_MODE_ONE`, `setVolume(0f)`. No `setVideoScalingMode`, no `setVideoChangeFrameRateStrategy`, no audio-track disabling (grep). `onPause` pauses; `onStop` keeps the player (lane 3).
- `playRandomClip()` (`:1459`) lists `*.mp4` only (a `.mkv` or `.mov` is ignored). Ramadan filter by the word "ramadan" in the name. Retries: 40 x 3 s when empty; 12 player errors (500 ms then 3 s).
- Quran backgrounds: `quranBackground()` (`:635`) picks a random readable `*.mp4` in `Movies/QuranBackgrounds`; `openPlayer()` swaps it onto the wallpaper player behind `playerShade`; `closePlayer()` calls `playRandomClip()`.
- `preferSixtyHz()` (`:740-755`, Kids `:233-248`) is called in `onCreate` (`:189`, Kids `:143`).
- `onRenderedFirstFrame` (`:254`) fires on every loop: `REPEAT_MODE_ONE` pre-buffers each repeat as a new period and media3 1.4.1 `MediaCodecVideoRenderer.onProcessedStreamChange()` → `VideoFrameReleaseControl.onProcessedStreamChange()` resets the first-frame state [mcvr-141] [exo-repeat]. Each seam re-runs `sampleAccent` (cached accent → 1.6 s `Accent.glideTo` to the same colour, redrawing every glass drawable → `magnify(lastDock)` if the dock is focused). Read in source; not seen on the box.
- Clip switch: the cover's 300 ms fade-out only runs while a clip is playing (`player.isPlaying() ? 300 : 0`, `:1484`); on `onResume` the player is paused, so the cover appears at once and the new clip fades in over 700 ms. By design, not a flash.
- Sky tint has a third state: warm 6% after midnight, fading out from 70 to 25 min before Fajr (`drawSky`).
- Accent: `sampleAccent` (`:3364`), `accentFromFile` (`:3382`, `getScaledFrameAtTime` at 1.5 s and 6 s, `OPTION_CLOSEST_SYNC`, 64x36), cached as `accent:<uri>`; `Accent.pick` 24 hue bins, ignores pixels with value or saturation below 0.12; tint off when colourful share < 4%.
- Kids (`KidsActivity.java`): `playClip()` (~`:405`) `MediaPlayer` with `setDisplay`, `setLooping(true)`, volume 0, cover fades 700 ms; `stopClip()` in `onStop` and `surfaceDestroyed`; `clipFor()` uses the parent's pick (`wallpaper` pref, -2 = random; stored as an index into the sorted list, `:372`, so it drifts when clips change), else a clip whose name contains a world word (`clipWords`: goku, mario, sonic, kirby, minecraft…), else any. Shades: three bands of a third each; top `0x73`, bottom `0xA6` with the dim folded in by `withDim()`; no `FLAG_KEEP_SCREEN_ON` (grep).
- **Wallpaper Studio** (`/mnt/project-files/tv-box/wallpaper-studio/Wallpaper Studio.ps1`): frame rate from ffprobe, odd rates fall back to the average or 30, `fps=$rate` (kept, not converted to 60); 1080p H.264 `-preset slow -crf 21 -profile:v high -level:v 4.2 -maxrate 6M` (9M above 31 fps) `-bufsize 12M`; 4K `libx265 -preset medium -crf 20 -tag:v hvc1`, VBV 20,000 (30,000 above 31 fps), `pix_fmt yuv420p` (8-bit Main); GOP = 2 x fps, scenecut 0; loop by SSIM >= 0.9 or cross-fade 0.75 to 1.5 s; `-an -map_metadata -1 +faststart`; sets no colour tags (the source's pass through), no HDR tone-mapping. Output `<name>.tv.mp4`; the guide expects 5 to 8 GB on the TV.
- **Quran Backgrounds** script: 10 minutes from the middle, 1.5 s cross-fade, 1080p H.264 CRF 22, High@4.2, maxrate 6M, GOP 60 frames (whatever the fps), source fps kept, no tone-mapping. Output `<name>.qbg.mp4`.
- Sample "world" clips on the share (`/mnt/project-files/tv-box/wallpapers/*.mp4`, made-up, no longer used on the TV): 1920x1080, 60 fps, H.264 High L4.2, 20 s, 0.9 to 3.0 Mb/s, `yuv420p`, tagged `color_space=bt470bg` with unknown transfer and primaries. They show drawn look-alikes of Kirby, Mario, Sonic and dragon balls: never commit or publish them.
- The real clips on the box (`/sdcard/Movies/TVWallpapers`) have not been probed by this lane (see Open questions).

Corrections from the whole-app audit (2026-11-05):
- `enterNightDim()` (`:3483`) animates only `dimView`; `centerGroup`, `lineFrame`, and the dates/weather stay at alpha 1 / 0.7.
- `onResume()` (`:282`) runs `playRandomClip()` after every `onPause`, including Home presses while on the home (manifest `singleTask`).

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/14-wallpaper.md](archive/14-wallpaper.md). Search it when a PR touches an area with history; don't read it in full.

## Lessons learned
- 2026-11-05: The PC scripts are part of the wallpaper's quality even though they are not in the repo. Read them when a PR or the owner touches wallpapers.
- 2026-11-05: Library defaults matter more than our code: ExoPlayer already asks for a seamless frame-rate switch and stretches to fit by default. Read the pinned version's source (1.4.1), not the latest docs.
- 2026-11-05: Probe files with ffprobe before judging them; tags (`bt470bg`), frame rate and bit depth are invisible in a thumbnail.
- 2026-11-05: Test every ffmpeg recipe before writing it down: `testsrc2` + `framemd5` + `uniq -c` shows the real frame cadence in seconds.
- 2026-11-05: Grep for window-level display settings (`preferredDisplayModeId`, `preferredRefreshRate`) before reasoning about refresh-rate switching. App code can override the player's defaults.
- 2026-11-05: ExoPlayer callbacks named "first" (`onRenderedFirstFrame`) can repeat on every loop. Check what the code hangs off them.
- 2026-11-05: "Every onResume" also means every Home press on the home screen: Android pauses a resumed activity before `onNewIntent`, then resumes it. Anything hung off `onResume` must ask whether the activity was really stopped.
- 2026-11-05: Check which layers a dim actually covers. `dimView` is under `stage`, so dimming it darkens only the video; static UI stays bright.
- 2026-11-05: Correct §8 and §9: the 60% night dim does **not** protect the static clock from burn-in (it dims only the video).

- 2026-11-05: After onStop calls player.stop(), the return always starts on an empty SurfaceView, so raise the cover in onStop. When changing resume logic, check the Quran background, which borrows the wallpaper player.

