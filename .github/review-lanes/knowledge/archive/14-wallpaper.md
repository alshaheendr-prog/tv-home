# Lane 14 knowledge: Wallpaper and backgrounds: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22: no Must. The decoder is released in onStop, and a Home press keeps the clip, which fixes the audit's hard-cut Should. Should: the Quran background was lost on return, and the return blinked black (both fixed in the PR). Nice: the Kids retry window is 30 s against the home's 120 s, and Kids retries the same bad pick.

- 2026-11-05, PR #21 (knowledge-only PR): lane 14 reviewed its own new files. Must: the `fps=60` judder "fix" was wrong (duplication keeps the 3:2 cadence); replaced by the `setpts=N/(30*TB),fps=30` retime. Should: `preferSixtyHz()` pins 60 Hz (was missing); colour tags pass through from the source (was "none written"). Recorded for code PRs: accent re-sampled on every loop; Kids wallpaper pick stored by index.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit on main d772bcc: no Must. Should: Home press on the home re-picks the clip with a hard cut (onPause→onNewIntent→onResume), double pick from `closePanels`; night dim leaves the clock and words at full brightness; Studio keeps 24/25 fps; 4K 8-bit HEVC with default AQ; no colour tags or tone-map; Kids pick by index. Nice: per-loop accent, 59.94 vs 60 mode choice, 50 Hz system output means a blackout per trip home, darker cover, Kids MediaPlayer seam, Kids accepts non-mp4, fixed-px scrims.


## Source library
All checked 2026-11-05. Tags are used in Core expertise.
- [frame-rate] https://developer.android.com/media/optimize/performance/frame-rate: `setFrameRate`, FIXED_SOURCE, seamless vs non-seamless (about 1 s black), multiple surfaces; with `preferredDisplayModeId` "the video surface is forced to pick either 60Hz or 90Hz"; `setFrameRate` only switches when the switch is lightweight (re-checked 2026-11-05).
- [hevc-levels] https://en.wikipedia.org/wiki/High_Efficiency_Video_Coding_tiers_and_levels: level limits (luma sample rate, Main tier bitrate). Checked 2026-11-05.
- [bt1702] https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.1702-2-201910-S!!PDF-E.pdf: photosensitive flashing limits for TV. Checked 2026-11-05.
- [exo-repeat] https://medium.com/google-exoplayer/repeat-modes-in-exoplayer-19dd85f036d3: Repeat One loops the window; the next repeat is pre-buffered (gapless). Checked 2026-11-05.
- [mcvr-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/MediaCodecVideoRenderer.java and `VideoFrameReleaseControl.java`: first-frame state reset on stream change. Checked 2026-11-05.
- [formats] https://developer.android.com/media/platform/supported-formats: required codecs, HEVC Main L4.1 for TV, AV1 from Android 14, HDR format table, in-stream switching.
- [media3-surface] https://developer.android.com/media/media3/ui/surface: SurfaceView vs TextureView; video at full display resolution on TVs that upscale the UI.
- [hdr] https://developer.android.com/media/grow/hdr-playback: HDR capability checks, HLG10 minimum, SurfaceView for HDR, SDR screenshots.
- [playlists] https://developer.android.com/media/media3/exoplayer/playlists: repeat modes, seamless transitions between items.
- [exo-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/ExoPlayer.java: builder defaults (`VIDEO_SCALING_MODE_DEFAULT`, `ONLY_IF_SEAMLESS`), scaling only on SurfaceView.
- [vfrh-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/VideoFrameReleaseHelper.java: how and when ExoPlayer calls `Surface.setFrameRate`.
- [media3-C] https://developer.android.com/reference/androidx/media3/common/C: scaling-mode and frame-rate-strategy constants (also the 1.4.1 `C.java` on GitHub: `VIDEO_SCALING_MODE_DEFAULT = SCALE_TO_FIT`).
- [mediacodec] https://developer.android.com/reference/android/media/MediaCodec: what SCALE_TO_FIT and SCALE_TO_FIT_WITH_CROPPING do; pixel aspect ignored.
- [mediaplayer] https://developer.android.com/reference/android/media/MediaPlayer: `setLooping`, `setNextMediaPlayer`, states.
- [dream-ref] https://developer.android.com/reference/android/service/dreams/DreamService: screensaver flags.
- [tv-memory] https://developer.android.com/training/tv/playback/memory: media buffers on TV (details in lane 3).
- [color-on-tv] https://developer.android.com/design/ui/tv/guides/foundations/color-on-tv: banding, Standard picture mode, sRGB, panel contrast range.
- [aerial] https://github.com/theothernt/aerialviews (README read via raw.githubusercontent.com master): sources, Dolby Vision note, overlays and burn-in, refresh-rate switching.
- [pulldown] https://www.forasoft.com/learn/video-quality/glossary/terms-vqm/pulldown: 3:2 cadence, 50/33 ms, why it judders.
- [fora-10bit] https://www.forasoft.com/learn/video-encoding/articles/8bit-vs-10bit-encoding: levels, banding, 10-bit efficiency, ffmpeg flags, no 10-bit H.264 hardware.
- [ffmpeg-filters] https://raw.githubusercontent.com/FFmpeg/FFmpeg/master/doc/filters.texi (source of https://ffmpeg.org/ffmpeg-filters.html; also `ffmpeg -h filter=…`, ffmpeg 6.1.1): xfade, gradfun warning, deband, tonemap chain, setparams.
- [x265-cli] https://raw.githubusercontent.com/videolan/x265/master/doc/reST/cli.rst: CRF, keyint, VBV, aq-mode 3 for dark banding, open-gop default, colour tags, output depth.
- [tv-patterns-2014] https://spot.pcc.edu/~mgoodman/developer.android.com/preview/tv/design/patterns.html: archived Android TV patterns, background images full colour 1920x1080.
- From lane 3 (opened there the same day): [mi-box-specs], [cnx-s905x5m], [androidpctv-review], [media3-loadcontrol-141].
- To open next (blocked or out of budget on 2026-11-05): rtings.com motion-cadence article (needs JS), trac.ffmpeg.org H.264/H.265 encoding guides, Wikipedia AVC/HEVC level tables, Google TV ambient mode pages, x264 documentation.
- [ambient-mode] https://developer.android.com/training/tv/playback/ambient-mode: Ambient Mode after 10 min idle; "apps should not prevent… when playback is stopped or paused"; auto-playing video "must not set FLAG_KEEP_SCREEN_ON"; apps cannot prevent Energy Saver; purpose is OLED burn-in. Checked 2026-11-05.
- [aosp-newintent] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/app/Activity.java (`onNewIntent` javadoc): "If the activity was resumed, it will be paused and new intent will be delivered, followed by onResume." Checked 2026-11-05.
- [so-loop] https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/36109625/small-pause-between-looping-mediaplayer: single looping MediaPlayer has a "very noticeable" pause; workaround is two alternating players (audio, API 25 emulator). Checked 2026-11-05.

- [media3-player-141] https://raw.githubusercontent.com/androidx/media/1.4.1/libraries/common/src/main/java/androidx/media3/common/Player.java : stop() goes to IDLE and releases resources, but keeps the playlist and position. Checked 2026-11-05.
- [aosp-surfaceview] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/SurfaceView.java : the surface is created and destroyed as the window is shown and hidden. Checked 2026-11-05.
- [exo-hello] https://developer.android.com/media/media3/exoplayer/hello-world : release frees video decoders for other apps. Checked 2026-11-05.

## Open questions to research next
- Probe every clip in `/sdcard/Movies/TVWallpapers` and `QuranBackgrounds` on the box (fps, codec, bit depth, colour tags, audio). How many are 24/25 fps?
- What refresh rate and resolution does the box output? Does `preferSixtyHz()` land on 59.94 or 60.00 Hz (`adb shell dumpsys display | grep -i mode`)?
- Does an HDR-tagged 8-bit Studio output switch the TV into HDR mode? (Probe the PC's Originals for `color_transfer=smpte2084/arib-std-b67`.)
- How much does the per-loop accent re-glide cost on the box (`dumpsys gfxinfo` across a seam)?
- Does this decoder play HEVC Main10 4K60 smoothly, and is 10-bit actually less banded on the family's TV in Standard mode?
- Does the box's decoder honour BT.601 vs BT.709 tags, and how does it treat untagged 1080p?
- Does Kids `MediaPlayer.setLooping` stall at the seam on this box? If so, compare ExoPlayer or a doubled-length file.
- Does Kids Corner get the system screensaver after the idle timeout? Is that what the owner wants for the kids?
- Is the gyan.dev ffmpeg essentials build compiled with zimg (needed for `zscale` tone-mapping)?
- Find a good research or industry source on ambient video design (Apple aerials, Netflix/Google TV ambient mode) for calm-motion guidance.
- Is the family's TV (on the Xiaomi box) OLED? That decides how urgent S2 is.
- Is the box's system output 50 Hz or 60 Hz, and does `preferSixtyHz()` cause an HDMI blackout on each trip home?
- Does Ambient Mode or Aerial Views start over Kids Corner after the idle time, and does the owner want that for the kids?

- How long does it take from onResume to the first wallpaper frame now that the codec is re-created? (Measure with screenrecord.)
- Was the return blink visible on the TV before the fix?

