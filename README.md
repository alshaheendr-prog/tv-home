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

## Kids Corner (`kids/`, `kids-corner.apk`)
A separate home screen for the kids, in its own app (`com.family.kids`).
- Opening **Kids Corner** turns kids mode on. Only the parent PIN (the 🔒 at the end of the row, or **Grown-ups** on the rest screen) turns it off again. The first time it opens, it asks the parent to choose the PIN.
- Only the apps the parent ticks show up (Netflix, Crunchyroll, Prime Video, Shahid and Kodi to start with). YouTube, the Play Store, Settings, browsers and Stremio are never allowed.
- While kids mode is on, a small service brings Kids Corner back whenever anything else comes to the front (the Home button, the YouTube button on the remote, Settings), and keeps going after a restart.
- Daily screen time: 1 h 30 min on school days and 2 h on Fridays and Saturdays to start with, plus a bedtime (8:30 pm to 7 am). A note floats over his show 5 minutes and 1 minute before the end; then a sleepy rest screen stays up until tomorrow or until a parent adds time.
- A welcome card for Ghanim & Noha with the date and Kuwait weather, a clock with the time-left ring, and a glass dock like the family home screen.
- A different world each day (Power Up, Mushroom Hills, Speed Loop, Dream Sky, Block World) with its own colours, Pip the buddy whose eyes follow the remote, and a character of the day with a fun fact.
- The background is one of the family home screen's videos (`Movies/TVWallpapers`), picked to fit the world when a file name matches (Goku, Mario, Sonic, Kirby, Minecraft); otherwise the world's clip in `Movies/KidsCorner`.
- **My cartoons** plays videos from `Movies/Kids` on the box, or a `Kids` or `Cartoons` folder on a USB drive, and remembers where each episode stopped.
- Optional extras on the box: fallback world clips in `Movies/KidsCorner/` (`power-up.mp4`, `mushroom-hills.mp4`, …) and character pictures in `Pictures/KidsCorner/` named after the character (`Goku.png`).

One-time setup over ADB (the PC script "Kids Corner setup" does all of this):
```
adb install -r kids-corner.apk
adb shell appops set com.family.kids GET_USAGE_STATS allow
adb shell appops set com.family.kids SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.family.kids android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.family.kids android.permission.READ_MEDIA_IMAGES
```
Forgot the PIN? `adb shell pm clear com.family.kids` resets Kids Corner (the PIN, the settings and today's time).
