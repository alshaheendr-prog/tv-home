# Family TV Home

A custom home screen for the Xiaomi TV Box S (3rd Gen): Kuwait City adhan times and countdown, five animated worlds for the kids, and tiles for YouTube (PIN), Netflix, Crunchyroll, Stremio, Prime Video, Shahid, Movies and TV Shows (Kodi, or VLC), plus Kids, Browser, All apps and Settings.

The screen itself is `app/src/main/assets/index.html`. The Android part is a full-screen WebView in `MainActivity.java` with a small bridge to launch apps.

## Get the app
Every push to `main` builds it. Download `tv-home.apk` from the **latest** release on this repository's Releases page.

## Install on the box
With ADB connected to the box (see the setup guide):
```
adb install -r tv-home.apk
```
Then press Home on the remote and pick **Family TV Home**, or make it the only home screen:
```
adb shell cmd package set-home-activity com.family.tvhome/.MainActivity
```
If Google TV still takes over the Home button, disable its home app (undo with `pm enable`):
```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

## Using it
- Arrows move, OK opens, Back closes a panel.
- YouTube asks for the parent PIN. The first time, you choose it. Hold OK on the YouTube tile to change it.
- **All apps**: hold OK on an app to add it to (or remove it from) the **Kids** tile.
- Movies and TV Shows open Kodi (or VLC if Kodi isn't installed).
- A tile for an app that isn't installed opens its Play Store page. Once installed, the tile shows the app's own artwork.
- Browser opens TV Bro (best with a remote); handy for hotel Wi-Fi login pages.
