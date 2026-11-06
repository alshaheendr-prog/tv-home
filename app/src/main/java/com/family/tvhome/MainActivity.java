package com.family.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.text.format.Formatter;
import android.os.storage.StorageVolume;
import android.os.storage.StorageManager;
import android.os.StatFs;
import android.os.Build;
import android.content.IntentFilter;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.media.MediaMetadataRetriever;
import android.icu.util.ULocale;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Family TV Home: a calm, transparent home screen over a looping video.
 * Everything is placed on a 1920x1080 stage that is scaled to the screen, so the numbers below are TV pixels.
 */
public class MainActivity extends Activity {
    static final TimeZone KUWAIT = TimeZone.getTimeZone("Asia/Kuwait");
    static final int WHITE = 0xFFF5F5F7, SOFT = 0xD9FFFFFF, DIM = 0xB8FFFFFF; // DIM 72%: still a clear step below SOFT
    static final int GLASS = 0x29FFFFFF, GLASS_EDGE = 0x47FFFFFF, GLASS_HI = 0x4DFFFFFF, SHADE = 0x8C000000;

    /** The apps in the dock. pkgs are tried in order, then any installed app whose name matches, then the Play Store. */
    static final class Dock {
        final String id, name; final int color; final String[] pkgs;
        Dock(String id, String name, int color, String... pkgs) { this.id = id; this.name = name; this.color = color; this.pkgs = pkgs; }
    }
    static final Dock[] DOCK = {
            new Dock("netflix", "Netflix", 0xFFE50914, "com.netflix.ninja"),
            new Dock("crunchy", "Crunchyroll", 0xFFF47521, "com.crunchyroll.crunchyroid"),
            new Dock("stremio", "Stremio", 0xFF7B5BF5, "com.stremio.one"),
            new Dock("prime", "Prime Video", 0xFF1AA3E8, "com.amazon.amazonvideo.livingroom"),
            new Dock("shahid", "Shahid", 0xFF1FD3A5, "net.mbc.shahidTV", "net.mbc.shahid"),
            new Dock("kodi", "Movies & Shows", 0xFF17B2E7, "org.xbmc.kodi", "org.videolan.vlc"),
            new Dock("browser", "Browser", 0xFF4A8CFF, "com.tcl.browser", "com.phlox.tvwebbrowser", "org.mozilla.firefox", "com.android.chrome"),
            new Dock("quran", "Quran · القرآن", 0xFFE8C97A),
            new Dock("kids", "Kids Corner", 0xFFFFB238, "com.family.kids"),
            new Dock("apps", "All apps", 0xFF8A8FA8),
            new Dock("yt", "YouTube", 0xFFE62117, "com.google.android.youtube.tv", "com.google.android.youtube"),
    };
    static int dockIndex(String id) { for (int i = 0; i < DOCK.length; i++) if (DOCK[i].id.equals(id)) return i; return 0; }
    static Dock catalog(String id) { for (Dock d : DOCK) if (d.id.equals(id)) return d; return null; }
    /** The built-in tile for a package, if any (so re-adding Netflix from All apps brings back the Netflix tile). */
    static Dock catalogForPkg(String pkg) { for (Dock d : DOCK) for (String p : d.pkgs) if (p.equals(pkg)) return d; return null; }
    static final int DOCK_MAX = 11; // what fits across the screen
    static final Set<String> DOCK_FIXED = new HashSet<>(Arrays.asList("apps", "quran")); // always on the home row
    static final Set<String> HIDDEN_FROM_LIST = new HashSet<>(Arrays.asList("com.google.android.youtube.tv", "com.google.android.youtube"));

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();

    /** Hands a job to the background thread, or drops it once the screen has closed (never crashes). */
    private void runBg(Runnable job) {
        try { bg.execute(job); } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }
    private final Random random = new Random();
    private SharedPreferences prefs;
    private Typeface fMed, fSemi, fBold, fAr, fArSemi, fQuran, fSerif, fLight, fThin;

    private FrameLayout root, stage;
    private SurfaceView video;
    private View topScrim, bottomScrim;
    private LinearLayout dockBar;
    private View videoCover, dimView;
    private boolean resumed;
    private int lastDock;
    private final ExecutorService blurThread = Executors.newSingleThreadExecutor();
    private ExoPlayer player;
    private String currentClip = "";
    private int clipErrors;

    private View centerGroup, bottomGroup;
    private View skyTint;
    private Uri playingUri;
    private TextView dateText, hijriText, tempText, skyText, clockText, ampmText, pillText, lineKicker, lineAr, lineEn;
    private LinearLayout lineBox;
    private final LinearLayout[] prayerCells = new LinearLayout[5];
    private final TextView[][] prayerTexts = new TextView[5][3];
    private static final int[] BAR_PRAYERS = {0, 2, 3, 4, 5}; // Fajr, Dhuhr, Asr, Maghrib, Isha
    private final List<FrameLayout> dockTiles = new ArrayList<>();
    private TextView dockLabel;
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.6f);
    /** For exits: starts promptly and speeds away (the standard accelerate curve), so nothing leaves like it arrives. */
    static final android.view.animation.PathInterpolator EXIT = new android.view.animation.PathInterpolator(0.3f, 0f, 1f, 1f);
    /** The focused dock app gets a bright glow behind it and a light ring on top (one pair per tile). */
    private int shownNext = -2, shownPassedMask = -1;
    private String shownClock = "";

    private int[] today, tomorrow;
    private String cachedDay = "", ayahDay = "";
    private String[] ayahToday;
    private volatile double tempNow = Double.NaN, dustNow = Double.NaN;
    private long lastWeather, lastLineSwitch;
    private int lineIndex = -1, shownAdhkar = -1, listenSec, remindedFor = -1;

    private FrameLayout pinPanel, appsPanel, quranPanel, parentPanel;
    private TextView pinTitle, pinSub;
    private final View[] pinDots = new View[4];
    private View firstPinKey;
    private String pinEntry = "", pinMode = "check", newPin = "";
    private boolean pinLaunchAfter;
    private Runnable pinThen;
    private GridLayout appGrid;
    private TextView appsTitle;
    private String appsMode = "apps";
    private volatile List<AppEntry> appCache;

    static final class AppEntry { String pkg, label; Drawable icon; }

    // ================================================================== lifecycle

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        preferSixtyHz();
        fullLengthAnimations();
        fMed = getResources().getFont(R.font.inter_medium);
        fSemi = getResources().getFont(R.font.inter_semibold);
        fLight = getResources().getFont(R.font.inter_light);
        fThin = getResources().getFont(R.font.inter_extralight); // big numbers read finer and more premium in a light weight
        fBold = getResources().getFont(R.font.inter_bold);
        fAr = getResources().getFont(R.font.plex_arabic_medium);
        fArSemi = getResources().getFont(R.font.plex_arabic_semibold);
        fQuran = getResources().getFont(R.font.amiri_bold);
        fSerif = getResources().getFont(R.font.cormorant_semibold_italic);
        MixText.arabic = fAr; MixText.arabicSemi = fArSemi; MixText.quran = fQuran; // Arabic inside Latin labels: Plex

        // The home screen never times out or starts a screensaver while it is in front.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        // The video plays on its own hardware layer behind the window: the smoothest path on this box (no extra copy per frame).
        video = new SurfaceView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        videoCover = new View(this);
        videoCover.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF1B2A4A, 0xFF0E1426, 0xFF241A3A}));
        root.addView(videoCover, new FrameLayout.LayoutParams(-1, -1));
        // the light of the day: cool at Fajr, warm after Maghrib (see drawSky)
        skyTint = new View(this);
        skyTint.setAlpha(0);
        root.addView(skyTint, new FrameLayout.LayoutParams(-1, -1));
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(baseDim());
        root.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        // Soft dark gradients at the top and bottom keep the text readable on any video.
        topScrim = new View(this);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x80000000, 0x00000000}));
        // only the top and bottom thirds carry any shade, so each scrim covers just that third (less to paint every frame)
        topScrim.getBackground().setDither(true); // the two largest ramps on the home, over dark clips at night: no bands
        root.addView(topScrim, new FrameLayout.LayoutParams(-1, 380, Gravity.TOP));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitStage());
        bottomScrim = new View(this);

        bottomScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xB3000000, 0x59000000, 0x00000000}));
        bottomScrim.getBackground().setDither(true);
        root.addView(bottomScrim, new FrameLayout.LayoutParams(-1, 480, Gravity.BOTTOM)); // reaches up behind the adhan strip

        stage = new FrameLayout(this);
        stage.setClipChildren(false);
        root.addView(stage, new FrameLayout.LayoutParams(1920, 1080));
        setContentView(root);

        buildTop();
        buildCenter();
        buildBottom();
        buildPin();
        buildApps();
        buildQuran();
        buildParent();
        buildPlayer();
        lineFrame.setVisibility(prefs.getBoolean("verse", true) ? View.VISIBLE : View.GONE);
        noAlphaLayers(root);
        cacheCards(true);
        registerDriveWatcher();
        ui.postDelayed(() -> runBg(() -> Quran.reciters(getCacheDir())), 20000); // warm the reciter list (names in Arabic)

        player = new ExoPlayer.Builder(this).build();
        player.setVideoSurfaceView(video);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(0f);
        player.addListener(new Player.Listener() {
            @Override public void onRenderedFirstFrame() {
                clipErrors = 0; videoCover.animate().alpha(0).setDuration(700).start(); splashReady();
                ui.removeCallbacks(sampleAccent);
                ui.postDelayed(sampleAccent, 1200); // once the clip is on screen, learn its colour
            }
            @Override public void onPlayerError(androidx.media3.common.PlaybackException e) {
                // a clip that can't play (an unusual format): keep the calm gradient and try another one
                videoCover.setAlpha(1);
                // right after the box turns on the files may not be readable yet, so keep trying for a while
                // (retryClips only plays while the home is in front, so a retry never starts a decoder behind a film)
                if (++clipErrors < 12) { ui.removeCallbacks(retryClips); ui.postDelayed(retryClips, clipErrors < 4 ? 500 : 3000); }
            }
        });

        dockTiles.get(0).requestFocus();
        if (coldStart) showSplash(); else introAnimation();
        tick();
        // The remote's YouTube button can start the home screen straight onto the PIN pad.
        if ("pin".equals(getIntent().getStringExtra("panel"))) ui.postDelayed(() -> openPin(false), coldStart ? 1500 : 400);
        missingApp(getIntent());
    }

    /** A remote button (Prime, Apps) whose app isn't on the box brings the home here with a short note, never silence. */
    private void missingApp(Intent i) {
        String name = i == null ? null : i.getStringExtra("missing");
        if (name != null) ui.postDelayed(() -> toast(name + " isn't installed. Run Update TV on the PC."), 700);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullLengthAnimations();
        resumed = true;
        lastInput = System.currentTimeMillis();
        if (ambient) leaveAmbient();
        hideBars();
        // a new wallpaper only after the home was really left (another app in front); Home pressed on the home
        // itself just carries on with the same clip, with no cut
        // the Quran background (borrowed wallpaper player) comes back as it was: stop() kept its playlist
        if (starsOn) { /* the opaque star field is up: the wallpaper waits until it closes */ }
        else if (wallStopped && playerVideo && playingUri != null) { videoCover.setAlpha(1); player.prepare(); player.play(); }
        else if (wallStopped || player.getPlaybackState() == Player.STATE_IDLE) playRandomClip(); else player.play();
        wallStopped = false;
        updateDrive();
        quietStopped = false;
        ui.removeCallbacks(startQuiet);
        ui.postDelayed(startQuiet, 4000);
        // No frosted backdrop: on this box the video is on a hardware layer and a copy of it comes back black.
        ui.postDelayed(() -> { loadDockIcons(); refreshApps(); }, 600);
        // the tick skipped the countdown and the card while home was hidden: bring them up to date now
        if (today != null) {
            updatePill();
            if (System.currentTimeMillis() - lastLineSwitch >= LINE_SECONDS * 1000) nextLine(Calendar.getInstance(KUWAIT));
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        ui.removeCallbacks(startQuiet);
        player.pause();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ambient) leaveAmbient();
        closePanels();
        endArrange(null);
        dockTiles.get(Math.max(0, Math.min(lastDock, dockTiles.size() - 1))).requestFocus();
        missingApp(intent);
        // Update TV opens each panel in turn to photograph it for polishing (never the parent settings, which need the PIN).
        String probe = intent.getStringExtra("probe");
        if (probe != null) { probe(probe); return; }
        String panel = intent.getStringExtra("panel");
        if (panel != null) ui.postDelayed(() -> {
            switch (panel) {
                case "apps": openApps("apps"); break;
                case "quran": openQuran(); break;
                case "pin": openPin(false); break;
                case "calendar": openCalendar(); break;
                case "stress": stressTest(); break;
                case "reciters": openQuran(); ui.postDelayed(this::openReciters, 300); break;
                case "player": if (quranSurah > 0) { openQuran(); openPlayer(); } break;
                default: break;
            }
        }, 400);
    }

    /**
     * Update TV's smoothness sweep: hide one layer at a time and measure, to find what costs the most per frame.
     * "none" shows everything again.
     */
    /**
     * Each home card is kept as a ready-made picture on the graphics chip and only redrawn when its own text changes,
     * so moving along the dock repaints just the dock instead of every card's glass, glow and text each frame.
     */
    private void cacheCards(boolean on) {
        for (View v : new View[]{topParts[0], sunCard, lineFrame, centerGroup})
            if (v != null) v.setLayerType(on ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
    }

    private void probe(String what) {
        cacheCards(!what.equals("nolayers"));
        boolean all = what.equals("none") || what.equals("nolayers");
        java.util.List<View> cards = new java.util.ArrayList<>(java.util.Arrays.asList(topParts));
        cards.add(sunCard); cards.add(eventChip); cards.add(lineFrame);
        for (View v : cards) if (v != null) v.setAlpha(!all && what.equals("cards") ? 0 : 1);
        centerGroup.setAlpha(!all && what.equals("clock") ? 0 : 1);
        topScrim.setAlpha(!all && what.equals("scrims") ? 0 : 1);
        bottomScrim.setAlpha(!all && what.equals("scrims") ? 0 : 1);
        if (skyTint != null) skyTint.setVisibility(!all && what.equals("tint") ? View.INVISIBLE : View.VISIBLE);
        dockBar.setBackground(!all && what.equals("dockglow") ? null : brandGlow);
        video.setVisibility(!all && what.equals("video") ? View.INVISIBLE : View.VISIBLE);
    }

    // ================================================================== stress test

    /** Update TV photographs the home screen with the longest text each card can get, to prove nothing overlaps or is cut. */
    private boolean stress;

    // widths that keep the top row's pieces apart: sky text beside the temperature, the event chip
    // under the left card, and the now-reciting card in the gap between the two top cards
    static final int SKY_W = 420, CHIP_W = 1100, NOW_W = 380;

    /** "a · b" on one line when it fits in max pixels, otherwise a above b, so nothing is cut or crowded. */
    /** A status line in the column's own colours: only its leading dot carries the colour. */
    private static CharSequence dotLine(String s, int dot) {
        android.text.SpannableString sp = new android.text.SpannableString(s);
        sp.setSpan(new android.text.style.ForegroundColorSpan(dot), 0, 1, 0);
        return sp;
    }

    private static void fitPair(TextView t, String a, String b, int max) {
        String one = a + "  ·  " + b;
        // a little room to spare: Arabic and English laid out together come out slightly wider than measured
        t.setText(t.getPaint().measureText(one) <= max - 30 ? one : a + "\n" + b);
    }

    /** The strip stays one height: the surah and reciter always take one line each, and the strip widens to fit them.
     *  Only a name too long for the screen drops its English half (never cut off with "..."). */
    static final int NOW_LINE_MAX = 520;
    private static void oneLine(TextView t, String ar, String en) {
        String both = ar + "  ·  " + en;
        t.setText(t.getPaint().measureText(both) <= NOW_LINE_MAX ? both : ar);
    }

    private static void fitPair(TextView t, String pair, int max) {
        int i = pair.indexOf(" · ");
        if (i < 0) { t.setText(pair); return; }
        fitPair(t, pair.substring(0, i).trim(), pair.substring(i + 3).trim(), max);
    }

    private void stressTest() {
        stress = true;
        boolean h24 = prefs.getBoolean("clock24", false);
        clockText.setText(h24 ? "23:58" : "12:58");
        ampmText.setText(h24 ? "" : "PM");
        tempText.setText("48°");
        fitPair(skyText, "Thunderstorm with hail · عاصفة رعدية مع برد", SKY_W);
        dateText.setText("Wednesday 28 September");
        hijriText.setText("٢٩ ذو الحجة ١٤٤٨ هـ");
        dayText.setText("Today  48° / 36°     Tomorrow  Thunderstorm  47° / 35°");
        driveText.setText("●  Drive unplugged");
        driveText.setVisibility(View.VISIBLE);
        fitPair(eventText, "First ten days of Dhul Hijjah", "العشر الأوائل من ذي الحجة", CHIP_W);
        eventChip.setVisibility(View.VISIBLE);
        adhanName.setText("Suhoor ends");
        adhanAr.setText("الإمساك · الفجر");
        adhanCountdown.setText("10:59:59");
        oneLine(nowSurah, "سورة الشعراء", "Ash-Shu'ara");
        oneLine(nowReciter, "محمد صديق المنشاوي", "Mohammed Siddiq Al-Minshawi"); // the longest name, to test the strip
        showNowReciting(true);
        lineFrame.setVisibility(View.VISIBLE);
        lineKicker.setText("VERSE OF THE DAY  ·  آية اليوم   ·   Al-Baqarah 2:286");
        lineAr.setText("رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا");
        lineAr.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32);
        lineEn.setText("Our Lord, do not take us to task if we forget or make a mistake. Our Lord, do not place on us a burden like the one You placed on those before us.");
        lineEn.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28);
    }

    private void endStress() {
        stress = false;
        cachedDay = "";
        shownClock = "";
        updateDrive();
        drawQuranState();
        fetchWeather();
        nextLine(Calendar.getInstance(KUWAIT));
        tick();
    }

    @Override
    protected void onStop() {
        // Leaving home for another app stops the recitation so it never plays over a film.
        if (quran != null && quran.getPlayWhenReady()) { quran.pause(); drawQuranState(); }
        // Pausing keeps the hardware video decoder (on a placeholder surface); stop lets it go for the film app.
        player.stop();
        wallStopped = true;
        // the video surface goes when the screen is hidden, so the cover is up for the return (no black blink)
        videoCover.animate().cancel();
        videoCover.setAlpha(1);
        // Parent settings never stay open behind another app.
        if (parentPanel.getVisibility() == View.VISIBLE) closePanels();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Stop the 1-second tick and every other pending job first: a loop left running after the
        // screen is recreated would later hand work to the shut-down threads below and crash.
        ui.removeCallbacksAndMessages(null);
        player.release();
        if (quran != null) quran.release();
        try { unregisterReceiver(driveWatcher); } catch (Exception ignored) { }
        blurThread.shutdownNow();
        bg.shutdownNow();
        super.onDestroy();
    }

    private void hideBars() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        lastInput = System.currentTimeMillis();
        if (parentPanel != null && parentPanel.getVisibility() == View.VISIBLE) { ui.removeCallbacks(parentIdle); ui.postDelayed(parentIdle, PARENT_IDLE_MS); }
        if (splash != null) { if (e.getAction() == KeyEvent.ACTION_UP) hideSplash(); return true; } // a press skips the intro
        if (stress && e.getAction() == KeyEvent.ACTION_UP) endStress();
        if (mediaKey(e)) return true;
        if (playerOpen() && playerKey(e)) return true;
        if (ambient) { if (e.getAction() == KeyEvent.ACTION_UP) leaveAmbient(); return true; } // the first press only wakes the screen
        if (arrangeTile != null && arrangeKey(e)) return true;
        pressDip(e, getCurrentFocus());
        if (code == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) {
                // Back closes exactly one layer
                if (appMenuBox != null) closeAppMenu(appMenuFrom);
                else if (reciterPanel != null && reciterPanel.getVisibility() == View.VISIBLE) closeReciters();
                else if (playerOpen()) closePlayer();
                else if (pinOverParent && pinPanel.getVisibility() == View.VISIBLE) backToParent(1);
                else if (statsShown) drawParent(0);
                else if (calendarFromApps && calendarPanel.getVisibility() == View.VISIBLE) closeCalendarToApps();
                else closePanels();
            }
            return true; // the home screen never closes itself
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0 && !anyPanelOpen()) secretKey(code);
        if (pinPanel.getVisibility() == View.VISIBLE && e.getAction() == KeyEvent.ACTION_DOWN) {
            if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) { pinPress(String.valueOf(code - KeyEvent.KEYCODE_0)); return true; }
            if (code == KeyEvent.KEYCODE_DEL) { pinPress("⌫"); return true; }
        }
        return super.dispatchKeyEvent(e);
    }

    // ================================================================== Quran player

    /** The remote's Play/Pause keys (and the TV's own remote over HDMI) work while the Quran page or player is open:
     *  Play only plays, Pause only pauses, Play/Pause toggles. */
    private boolean mediaKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (code != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && code != KeyEvent.KEYCODE_MEDIA_PLAY && code != KeyEvent.KEYCODE_MEDIA_PAUSE) return false;
        if (!(playerOpen() || quranPanel.getVisibility() == View.VISIBLE) || quran == null || quranSurah == 0) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            boolean play = code == KeyEvent.KEYCODE_MEDIA_PLAY || (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && !quran.getPlayWhenReady());
            quran.setPlayWhenReady(play);
            drawQuranState();
            if (playerOpen()) showPlayerControls();
        }
        return true;
    }

    /** Parent settings close by themselves after 90 seconds with no key press, so a child handed the remote can't change them. */
    static final long PARENT_IDLE_MS = 90_000;
    private final Runnable parentIdle = () -> { if (parentPanel.getVisibility() == View.VISIBLE) closePanels(); };

    /** The full-screen player: the surah's name large and soft in the middle, the reciter under it, Arabic only,
     *  with quiet controls at the bottom that fade away when the remote is left alone. */
    private FrameLayout playerPanel, playerBgBox;
    private LinearLayout playerControls;
    private TextView playerSurah, playerReciter, playerPlay, playerShuffle, playerRepeat;
    private ThinBar playerBar;
    private boolean loopFadeIn;
    private ValueAnimator loopFade;
    static final long LOOP_FADE_MS = 4000;

    private boolean playerOpen() { return playerPanel != null && playerPanel.getVisibility() == View.VISIBLE; }

    /** 0 off, 1 all (carry on to the next surah), 2 one (the same surah again). */
    private int repeatMode() { return prefs.getInt("q_repeat", 0); }

    private int nextSurah(int n, int step) {
        if (prefs.getBoolean("q_shuffle", false)) { int k; do { k = 1 + random.nextInt(114); } while (k == n); return k; }
        return ((n - 1 + step) % 114 + 114) % 114 + 1;
    }

    private void buildPlayer() {
        playerPanel = new TrapFrame(this);
        playerPanel.setElevation(35);
        playerPanel.setClipChildren(false);
        playerPanel.setVisibility(View.GONE);
        // Backgrounds come later; until then the slow gold star pattern stands in.
        playerBgBox = new FrameLayout(this);
        playerBgBox.setTag("bg");
        playerBgBox.addView(new StarField(this), new FrameLayout.LayoutParams(-1, -1));
        playerPanel.addView(playerBgBox, bleed());
        // Over a background video: a soft darkening at the top and bottom so the words always read clearly.
        playerShade = new View(this);
        playerShade.setTag("bg");
        GradientDrawable shade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x80000000, 0x59000000, 0x59000000, 0xA6000000});
        shade.setDither(true); // a full-screen dark ramp: dithered so it never bands
        playerShade.setBackground(shade);
        playerShade.setVisibility(View.GONE);
        playerPanel.addView(playerShade, bleed());

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER_HORIZONTAL);
        playerSurah = text("", 168, fQuran, 0xD9FFFFFF); // Amiri, as everywhere a surah name is shown
        playerSurah.setIncludeFontPadding(true);
        playerSurah.setGravity(Gravity.CENTER);
        playerSurah.setPadding(90, 40, 90, 50); // room for the Arabic swashes and the soft shadow, so neither is cut off
        playerSurah.setShadowLayer(8, 0, 4, 0x80000000);
        playerSurah.setLayerType(View.LAYER_TYPE_HARDWARE, null); // the big blurred title is drawn once, not every frame
        playerReciter = text("", 44, fAr, 0xE6FFFFFF);
        playerReciter.setIncludeFontPadding(true);
        playerReciter.setGravity(Gravity.CENTER);
        playerReciter.setPadding(60, 10, 60, 20);
        playerReciter.setShadowLayer(5, 0, 2, 0xB3000000);
        playerReciter.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mid.addView(playerSurah, new LinearLayout.LayoutParams(-2, -2));
        mid.addView(playerReciter, new LinearLayout.LayoutParams(-2, -2));
        playerPanel.addView(mid, new FrameLayout.LayoutParams(1920, -2, Gravity.CENTER));
        // Sleep dimming: a black veil over the background and the words, under the controls so they stay usable.
        playerDim = new View(this);
        playerDim.setTag("bg");
        playerDim.setBackgroundColor(Color.BLACK);
        playerDim.setAlpha(0);
        playerPanel.addView(playerDim, bleed());

        playerControls = new LinearLayout(this);
        playerControls.setOrientation(LinearLayout.VERTICAL);
        playerControls.setGravity(Gravity.CENTER_HORIZONTAL);
        playerBar = new ThinBar(this);
        playerBar.setColor(0xB3F1D58A);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(720, 3);
        bp.bottomMargin = 34;
        playerControls.addView(playerBar, bp);
        LinearLayout row = new LinearLayout(this);
        playerRow = row;
        row.setGravity(Gravity.CENTER_VERTICAL);
        // room for the focused button to grow: without it the row cut its top and bottom off
        row.setPadding(20, 16, 20, 16);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        playerControls.setClipChildren(false);
        playerControls.setClipToPadding(false);
        playerButton(row, "⏮", () -> { quranQuiet = false; playSurah(nextSurah(quranSurah > 0 ? quranSurah : 1, -1), false); });
        playerPlay = playerButton(row, "⏸", () -> {
            if (quran == null) return;
            if (quranSurah == 0 && playerLast > 0) { quranQuiet = false; playSurah(playerLast, false); return; }
            quran.setPlayWhenReady(!quran.getPlayWhenReady());
            drawQuranState();
        });
        playerButton(row, "⏭", () -> { quranQuiet = false; playSurah(nextSurah(quranSurah > 0 ? quranSurah : 1, 1), false); });
        playerShuffle = playerButton(row, "", () -> { prefs.edit().putBoolean("q_shuffle", !prefs.getBoolean("q_shuffle", false)).apply(); drawPlayer(); });
        playerRepeat = playerButton(row, "", () -> { prefs.edit().putInt("q_repeat", (repeatMode() + 1) % 3).apply(); drawPlayer(); });
        playerSleep = playerButton(row, "", () -> {
            prefs.edit().putInt("q_dim", (prefs.getInt("q_dim", 0) + 1) % SLEEP_DIM.length).apply();
            drawPlayer();
        });
        playerControls.addView(row, new LinearLayout.LayoutParams(-2, -2));
        playerPanel.addView(playerControls, at(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 74));
        stage.addView(playerPanel, new FrameLayout.LayoutParams(1920, 1080));
    }

    private LinearLayout playerRow;
    private boolean swallowUp;

    /** The player moves focus itself, one button at a time along its row, so a press can never land on the grid or
     *  buttons hidden behind it. While the controls are faded out, the first press only brings them back. */
    private boolean playerKey(KeyEvent e) {
        if (reciterPanel != null && reciterPanel.getVisibility() == View.VISIBLE) return false;
        int code = e.getKeyCode();
        boolean dpad = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (swallowUp && (dpad || ok)) { swallowUp = false; return true; }
            return dpad;
        }
        if (e.getAction() != KeyEvent.ACTION_DOWN) return dpad;
        boolean hidden = playerControls.getAlpha() < 0.5f;
        showPlayerControls();
        View f = getCurrentFocus();
        int at = -1;
        for (int i = 0; i < playerRow.getChildCount(); i++) if (playerRow.getChildAt(i) == f) at = i;
        if (at < 0) { playerPlay.requestFocus(); swallowUp = dpad || ok; return dpad || ok; }
        if (hidden && e.getRepeatCount() == 0 && (dpad || ok)) { swallowUp = true; return true; }
        if (!dpad) return false;
        int to = at + (code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 0);
        if (to != at && to >= 0 && to < playerRow.getChildCount()) playerRow.getChildAt(to).requestFocus();
        return true;
    }
    private PlayerIcon playIcon;

    private TextView playerButton(LinearLayout row, String label, Runnable onClick) {
        boolean icon = label.length() == 1;
        TextView b = text(icon ? "" : label, 24, fSemi, WHITE);
        if (icon) {
            PlayerIcon pi = new PlayerIcon(label.equals("⏮") ? PlayerIcon.PREV : label.equals("⏭") ? PlayerIcon.NEXT : PlayerIcon.PAUSE);
            b.setForeground(pi);
            if (!label.equals("⏮") && !label.equals("⏭")) playIcon = pi;
        }
        b.setGravity(Gravity.CENTER);
        b.setPadding(icon ? 0 : 30, 0, icon ? 0 : 30, 2);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackground(glass(99, 0x1AFFFFFF, GLASS_EDGE));
        b.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? focusGlass(99) : glass(99, 0x1AFFFFFF, GLASS_EDGE));
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(220).setInterpolator(new DecelerateInterpolator(2f)).start();
        });
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(icon ? 84 : -2, 84);
        p.leftMargin = 12; p.rightMargin = 12;
        row.addView(b, p);
        return b;
    }

    private int playerLast;
    private View playerDim;
    private TextView playerSleep;
    /** Bedtime dimming for the player, chosen with its Sleep button and remembered. */
    static final float[] SLEEP_DIM = {0f, 0.5f, 0.72f, 0.88f};
    static final String[] SLEEP_NAMES = {"Sleep: Off", "Sleep: Dim", "Sleep: Darker", "Sleep: Darkest"};
    private View playerShade;
    private boolean playerVideo; // the wallpaper player is showing a Quran background while the player is open

    /** A random Quran background from Movies/QuranBackgrounds (made by Make Quran Backgrounds on the PC), or null. */
    private Uri quranBackground() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "QuranBackgrounds");
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".mp4"));
        List<File> ok = new ArrayList<>();
        if (files != null) for (File f : files) if (f.canRead()) ok.add(f);
        return ok.isEmpty() ? null : Uri.fromFile(ok.get(random.nextInt(ok.size())));
    }

    private void openPlayer() {
        if (playerPanel == null) return;
        Uri bgVideo = quranBackground();
        playerVideo = bgVideo != null;
        playerBgBox.setVisibility(playerVideo ? View.GONE : View.VISIBLE);
        playerShade.setVisibility(playerVideo ? View.VISIBLE : View.GONE);
        // the grid steps out of the way, so it isn't drawn under the player every frame
        quranPanel.animate().alpha(0).setDuration(400).start();
        if (playerVideo) {
            // the background video is the backdrop now: the home's layers come back (the player takes the clip below)
            ui.removeCallbacks(starsUp);
            if (starsOn) { starsOn = false; for (View h : new View[]{dimView, topScrim, bottomScrim, skyTint}) if (h != null) h.setVisibility(View.VISIBLE); }
            wallStopped = false;
            // the wallpaper's own video layer plays the background, so it costs nothing extra
            videoCover.animate().alpha(1).setDuration(300).withEndAction(() -> {
                playingUri = bgVideo;
                player.setMediaItem(MediaItem.fromUri(bgVideo));
                player.prepare();
                player.play();
            }).start();
        }
        playerPanel.setAlpha(0);
        playerPanel.setVisibility(View.VISIBLE);
        playerPanel.animate().alpha(1).setDuration(500).setInterpolator(new DecelerateInterpolator(2f)).start();
        drawPlayer();
        showPlayerControls();
        placePill(PILL_PLAYER);
        focusLayer(playerPanel); // the surah page under it stays visible only as a dim picture
        ui.post(() -> playerPlay.requestFocus());
        ui.removeCallbacks(playerTick);
        ui.post(playerTick);
    }

    /**
     * While the opaque star field (the Quran page, or the player with no background video) fills the screen, the home's
     * own layers under it are taken out of the frame and the wallpaper pauses: the box would otherwise blend about five
     * full screens every frame for nothing.
     */
    private boolean starsOn;
    private final Runnable starsUp = () -> {
        if (quranPanel.getVisibility() == View.VISIBLE && !playerVideo) starsOver(true);
    };
    private void starsOver(boolean on) {
        if (on == starsOn) return;
        starsOn = on;
        int v = on ? View.INVISIBLE : View.VISIBLE;
        for (View h : new View[]{dimView, topScrim, bottomScrim, skyTint}) if (h != null) h.setVisibility(v);
        if (on) player.pause();
        else if (resumed) {
            if (wallStopped || player.getPlaybackState() == Player.STATE_IDLE) playRandomClip(); else player.play();
            wallStopped = false;
        }
    }

    private void closePlayer() {
        quranPanel.animate().alpha(1).setStartDelay(0).setDuration(300).setInterpolator(EASE).start();
        if (playerVideo) {
            playerVideo = false;
            if (quranPanel.getVisibility() == View.VISIBLE) { wallStopped = true; starsOver(true); } // a new wallpaper when home shows again
            else playRandomClip(); // back to a home wallpaper
        }
        playerPanel.animate().alpha(0).setDuration(250).setInterpolator(EXIT).withEndAction(() -> playerPanel.setVisibility(View.GONE)).start();
        if (quranPanel.getVisibility() == View.VISIBLE) {
            focusLayer(quranPanel);
            surahGrid.getChildAt((quranSurah > 0 ? quranSurah : Math.max(1, playerLast)) - 1).requestFocus();
            placePill(PILL_PANEL);
        } else { focusLayer(null); placePill(PILL_HOME); }
    }

    private final Runnable hideControls = () -> {
        if (playerControls != null) playerControls.animate().alpha(0).translationY(20).setDuration(800).withLayer().start();
    };

    private void showPlayerControls() {
        if (playerControls.getAlpha() < 1f) playerControls.animate().alpha(1).translationY(0).setDuration(250).withLayer().start();
        ui.removeCallbacks(hideControls);
        ui.postDelayed(hideControls, 6000);
    }

    private void drawPlayer() {
        if (playerPanel == null) return;
        if (quranSurah > 0) playerLast = quranSurah;
        int n = playerLast;
        if (n > 0) playerSurah.setText("سورة " + Quran.AR[n - 1]);
        if (quranReciter != null) playerReciter.setText(quranReciter.ar.isEmpty() ? quranReciter.en : quranReciter.ar);
        playIcon.setKind(quran != null && quranSurah > 0 && quran.getPlayWhenReady() ? PlayerIcon.PAUSE : PlayerIcon.PLAY);
        boolean sh = prefs.getBoolean("q_shuffle", false);
        playerShuffle.setText(sh ? "Shuffle: On" : "Shuffle: Off");
        playerShuffle.setTextColor(sh ? GOLD : WHITE);
        int rp = repeatMode();
        playerRepeat.setText(rp == 0 ? "Repeat: Off" : rp == 1 ? "Repeat: All" : "Repeat: This surah");
        playerRepeat.setTextColor(rp == 0 ? WHITE : GOLD);
        int dim = prefs.getInt("q_dim", 0) % SLEEP_DIM.length;
        playerSleep.setText(SLEEP_NAMES[dim]);
        playerSleep.setTextColor(dim == 0 ? WHITE : GOLD);
        playerDim.animate().alpha(SLEEP_DIM[dim]).setDuration(1200).start();
    }

    /** Four times a second while the player is open: the progress line, and the soft fade out before a repeat. */
    private final Runnable playerTick = new Runnable() {
        @Override public void run() {
            boolean manual = quran != null && quranSurah > 0 && !quranQuiet;
            if (!playerOpen() && !manual) return; // nothing to show or fade: stop ticking
            if (quran != null && quranSurah > 0) {
                long d = quran.getDuration(), pos = quran.getCurrentPosition();
                if (d > 0) {
                    playerBar.setProgress(pos / (float) d);
                    if (repeatMode() != 0 && !quranQuiet && (loopFade == null || !loopFade.isRunning()) && d - pos < LOOP_FADE_MS)
                        quran.setVolume(Math.max(0f, (d - pos) / (float) LOOP_FADE_MS));
                }
            }
            ui.postDelayed(this, 250);
        }
    };

    private void fadeLoopIn() {
        if (loopFade != null) loopFade.cancel();
        loopFade = ValueAnimator.ofFloat(0f, 1f);
        loopFade.setDuration(3000);
        loopFade.addUpdateListener(v -> { if (quran != null && !quranQuiet) quran.setVolume((float) v.getAnimatedValue()); });
        loopFade.start();
    }

    // ================================================================== smoothness

    /** Ask for a 60 Hz (or faster) display mode at the current resolution, so every animation can run at 60 fps. */
    private void preferSixtyHz() {
        try {
            android.view.Display d = getWindowManager().getDefaultDisplay();
            if (d == null) return;
            android.view.Display.Mode cur = d.getMode(), best = null;
            for (android.view.Display.Mode m : d.getSupportedModes()) {
                if (m.getPhysicalWidth() != cur.getPhysicalWidth() || m.getPhysicalHeight() != cur.getPhysicalHeight()) continue;
                if (m.getRefreshRate() < 59f || m.getRefreshRate() > 61f) continue;
                best = m;
            }
            if (best == null || best.getModeId() == cur.getModeId()) return;
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = best.getModeId();
            getWindow().setAttributes(lp);
        } catch (Exception ignored) { }
    }

    // ================================================================== fitting the TV

    /** Screen size in percent: many TVs crop the picture's edges (overscan), so the whole layout can be shrunk to fit inside. */
    static final int[] FITS = {100, 97, 94, 91, 88};

    /** Scales the 1920 x 1080 layout to the screen, inside the chosen margin; the video and scrims still fill the whole screen. */
    private void fitStage() {
        int w = root.getWidth(), h = root.getHeight();
        if (w == 0 || h == 0) return;
        float s = Math.min(w / 1920f, h / 1080f) * prefs.getInt("fit", 100) / 100f;
        stage.setPivotX(0); stage.setPivotY(0);
        stage.setScaleX(s); stage.setScaleY(s);
        stage.setTranslationX((w - 1920 * s) / 2);
        stage.setTranslationY((h - 1080 * s) / 2);
        GlassFrame.setScreen(w, h, s);
        // the video is a little larger than the screen, so it can drift with focus without showing an edge
        int px = Math.round(w * DRIFT_ROOM), py = Math.round(h * DRIFT_ROOM);
        FrameLayout.LayoutParams vp = (FrameLayout.LayoutParams) video.getLayoutParams();
        if (vp.leftMargin != -px || vp.topMargin != -py) {
            vp.setMargins(-px, -py, -px, -py);
            video.setLayoutParams(vp);
        }
        driftRange = px * 0.8f;
    }

    /** Full-screen layers inside the stage reach past its edges, so a shrunk layout leaves no hard border. */
    static final int BLEED = 200;

    private static FrameLayout.LayoutParams bleed() {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(1920 + 2 * BLEED, 1080 + 2 * BLEED);
        p.leftMargin = -BLEED; p.topMargin = -BLEED;
        return p;
    }

    // ================================================================== helpers

    private TextView text(String s, float px, Typeface tf, int color) {
        TextView t;
        if (tf == fAr || tf == fArSemi || tf == fQuran) t = new TextView(this);
        else { MixText m = new MixText(this); m.boldArabic(tf == fSemi || tf == fBold); t = m; }
        t.setTypeface(tf);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (px >= 100) t.setShadowLayer(5, 0, 2, 0x66000000);
        return t;
    }

    private static GradientDrawable glass(float radius, int fill, int edge) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (edge != 0) d.setStroke(2, edge);
        return d;
    }

    /** The panel look shared by every card: frosted glass with a soft glow. */
    static final int CARD_GLOW = 28;

    /** Puts content in a frosted glass card and adds it to parent. x, y and w, h describe the card itself, not its glow. */
    private GlassFrame glassCard(ViewGroup parent, View content, int w, int h, int gravity, int x, int y,
                                 float radius, int glow, int glowColor, int tint, boolean frosted) {
        GlassFrame f = new GlassFrame(this, radius, glow, glowColor, tint, frosted);
        f.setContent(content, w >= 0, h >= 0);
        int fw = w < 0 ? -2 : w + 2 * glow, fh = h < 0 ? -2 : h + 2 * glow;
        if (gravity == Gravity.CENTER) parent.addView(f, new FrameLayout.LayoutParams(fw, fh, Gravity.CENTER));
        else parent.addView(f, at(fw, fh, gravity, x - glow, y - glow));
        return f;
    }

    private GlassFrame card(View content, int w, int h, int gravity, int x, int y) {
        return glassCard(stage, content, w, h, gravity, x, y, 34, CARD_GLOW, 0x33FFFFFF, CARD_TINT, false);
    }

    private static GradientDrawable ring(float radius, int width, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radius);
        d.setStroke(width, color);
        return d;
    }

    private static FrameLayout.LayoutParams at(int w, int h, int gravity, int x, int y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h, gravity);
        if ((gravity & Gravity.END) == Gravity.END) p.rightMargin = x; else p.leftMargin = x;
        if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) p.bottomMargin = y; else p.topMargin = y;
        return p;
    }

    private void toast(String msg) {
        pillTextOverride = msg;
        // over a panel the note sits at the bottom centre, clear of the panel's title; on the home screen, top left
        boolean low = anyPanelOpen() || (reciterPanel != null && reciterPanel.getVisibility() == View.VISIBLE);
        placePill(playerPanel != null && playerPanel.getVisibility() == View.VISIBLE ? PILL_PLAYER : low ? PILL_PANEL : PILL_HOME);
        ui.removeCallbacks(clearOverride);
        // long enough to read: about 3 s plus 1 s for every 3 words, between 3.5 and 7 s
        int words = msg.trim().split("\\s+").length;
        ui.postDelayed(clearOverride, Math.min(7000, Math.max(3500, 2000 + 330 * words)));
        updatePill();
    }
    private String pillTextOverride;
    private static final int PILL_HOME = 0, PILL_PANEL = 1, PILL_PLAYER = 2;
    private int pillAt = PILL_HOME;

    /** Where the message pill sits: top left on the home screen, bottom centre over a panel, top centre over the
     *  Quran player (its buttons are at the bottom). Only re-laid out when the place changes. */
    private void placePill(int where) {
        if (where == pillAt) return;
        pillAt = where;
        pillText.setLayoutParams(where == PILL_PLAYER ? at(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 56)
                : where == PILL_PANEL ? at(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 64)
                : at(-2, -2, Gravity.TOP | Gravity.START, 80, 56));
    }
    private final Runnable clearOverride = () -> { pillTextOverride = null; updatePill(); };

    // ================================================================== layout

    private TextView adhanName, adhanAr, adhanCountdown, driveText, dayText;
    private volatile String hiLo = "";

    static final int CARD_W = 560, CARD_H = 330;
    // Light dark wash behind the glass so text stays readable on busy wallpapers
    static final int CARD_TINT = 0x3A000000;

    /** Small spaced capitals over each block, in a soft gold so every card shares one accent. */
    private static TextView kicker(TextView t) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22); // the size floor for anything read from the sofa
        t.setLetterSpacing(CAPS_TRACK); t.setTextColor(GOLD); return t;
    }
    /** One letter spacing for every caps label in the three apps. */
    static final float CAPS_TRACK = 0.12f;

    /** Two matching cards, mirrored: weather and dates top left, the next adhan top right. */
    private void buildTop() {
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(36, 28, 36, 24);
        LinearLayout kRow = new LinearLayout(this);
        kRow.setGravity(Gravity.CENTER_VERTICAL);
        kRow.addView(kicker(text("KUWAIT  ·  الكويت", 20, fSemi, DIM)), new LinearLayout.LayoutParams(0, -2, 1));
        driveText = text("", 24, fMed, SOFT); // the size of the forecast it sits beside
        driveText.setVisibility(View.GONE);
        kRow.addView(driveText);
        left.addView(kRow, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout wx = new LinearLayout(this);
        wx.setGravity(Gravity.CENTER_VERTICAL);
        wx.setPadding(0, 12, 0, 0);
        tempText = text("--°", 66, fLight, WHITE);
        skyText = text("", 24, fMed, SOFT);
        skyText.setIncludeFontPadding(true);
        skyText.setPadding(18, 0, 0, 0);
        wx.addView(tempText);
        wx.addView(skyText, new LinearLayout.LayoutParams(0, -2, 1));
        left.addView(wx);
        dateText = text("", 30, fSemi, WHITE);
        dateText.setPadding(0, 10, 0, 0);
        hijriText = text("", 26, fAr, SOFT);
        hijriText.setIncludeFontPadding(true);
        hijriText.setPadding(0, 4, 0, 0);
        left.addView(dateText);
        left.addView(hijriText);
        dayText = text("", 24, fMed, DIM);
        dayText.setPadding(0, 8, 0, 0);
        left.addView(dayText);
        leftCard = left;
        View leftFrame = card(left, CARD_W, CARD_H, Gravity.TOP | Gravity.START, 80, 56);

        // A small gold chip under the card on special days: Ramadan, the Eids, Arafah, Ashura, the White Days.
        eventText = text("", 22, fSemi, 0xFFF1D58A);
        eventText.setIncludeFontPadding(true);
        eventText.setPadding(26, 10, 28, 12);
        eventChip = glassCard(stage, eventText, -1, -1, Gravity.TOP | Gravity.START, 80, 56 + CARD_H + 16, 26, 18, 0x40F1D58A, CARD_TINT, false);
        eventChip.setVisibility(View.GONE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 28, 36, 22);
        card.addView(kicker(text("NEXT ADHAN  ·  الأذان القادم", 20, fSemi, DIM)));
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.BOTTOM);
        nameRow.setPadding(0, 10, 0, 0);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        adhanName = text("", 38, fSemi, WHITE);
        adhanName.setMaxLines(1); // a long name ("Suhoor ends") shrinks a little to stay on one line beside the countdown
        adhanName.setAutoSizeTextTypeUniformWithConfiguration(30, 38, 1, TypedValue.COMPLEX_UNIT_PX);
        adhanAr = text("", 30, fArSemi, SOFT); // Arabic a touch larger than the 32 px English beside it reads as equal
        adhanAr.setIncludeFontPadding(true);
        names.addView(adhanName, new LinearLayout.LayoutParams(-2, -2));
        names.addView(adhanAr, new LinearLayout.LayoutParams(-2, -2));
        adhanCountdown = text("--:--:--", 60, fLight, WHITE);
        adhanCountdown.setFontFeatureSettings("tnum");
        adhanCountdown.setGravity(Gravity.END);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1);
        np.rightMargin = 20; // a clear gap before the countdown
        nameRow.addView(names, np);
        nameRow.addView(adhanCountdown);
        card.addView(nameRow);
        // How much of the wait from the last prayer to the next one has passed.
        adhanBar = new ThinBar(this);
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(-1, 3);
        abp.topMargin = 10;
        card.addView(adhanBar, abp);
        LinearLayout times = new LinearLayout(this);
        times.setPadding(0, 4, 0, 0);
        for (int i = 0; i < 5; i++) {
            int k = BAR_PRAYERS[i];
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding(0, 8, 0, 8);
            TextView en = text(PrayerTimes.EN[k], 22, fMed, SOFT);
            TextView ar = text(PrayerTimes.AR[k], 24, fAr, DIM); // Arabic no smaller than the English above it
            TextView tm = text("", 24, fSemi, WHITE);
            tm.setFontFeatureSettings("tnum");
            tm.setPadding(0, 4, 0, 0);
            for (TextView t : new TextView[]{en, ar, tm}) { t.setGravity(Gravity.CENTER); col.addView(t, new LinearLayout.LayoutParams(-1, -2)); }
            prayerTexts[i][0] = en; prayerTexts[i][1] = ar; prayerTexts[i][2] = tm;
            prayerCells[i] = col;
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
            cp.leftMargin = 3; cp.rightMargin = 3;
            times.addView(col, cp);
        }
        card.addView(times, new LinearLayout.LayoutParams(-1, -2));
        sunLine = text("", 22, fMed, SOFT);
        sunLine.setGravity(Gravity.CENTER);
        sunLine.setFontFeatureSettings("tnum");
        sunLine.setPadding(0, 6, 0, 0);
        card.addView(sunLine, new LinearLayout.LayoutParams(-1, -2));
        View rightFrame = card(card, CARD_W, CARD_H, Gravity.TOP | Gravity.END, 80, 56);

        // The sun card, mirroring the event chip on the left: where the sun is now, with sunrise and sunset.
        LinearLayout sunRow = new LinearLayout(this);
        sunRow.setGravity(Gravity.CENTER_VERTICAL);
        sunRow.setPadding(24, 12, 30, 12);
        sunArc = new SunArc(this);
        sunRow.addView(sunArc, new LinearLayout.LayoutParams(150, 64));
        sunText = text("", 22, fMed, SOFT);
        sunText.setIncludeFontPadding(true);
        sunText.setPadding(18, 0, 0, 0);
        sunRow.addView(sunText);
        sunCard = glassCard(stage, sunRow, -1, -1, Gravity.TOP | Gravity.END, 80, 56 + CARD_H + 16, 26, 18, 0x33FFFFFF, CARD_TINT, false);

        sunCard.setVisibility(View.GONE); // folded into the adhan card, to keep the screen calm
        topParts = new View[]{leftFrame, rightFrame};
    }

    private View[] topParts;
    private TextView eventText, sunText, sunLine;
    private SunArc sunArc;
    private View sunCard;

    private void drawSun() {
        if (today == null || sunText == null) return;
        Calendar now = Calendar.getInstance(KUWAIT);
        int min = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        sunArc.set(today[1], today[4], min);
        sunText.setText("Sunrise " + PrayerTimes.clock(today[1]) + "  ·  Sunset " + PrayerTimes.clock(today[4])
                + "\nالشروق " + PrayerTimes.clock(today[1]) + "  ·  الغروب " + PrayerTimes.clock(today[4]));
        sunLine.setText("Sunrise " + PrayerTimes.clock(today[1]) + "   ·   Sunset " + PrayerTimes.clock(today[4]));
    }
    private View eventChip;

    private void drawEventChip(Calendar now) {
        String[] b = Events.badge(now);
        if (b == null) { eventChip.setVisibility(View.GONE); return; }
        fitPair(eventText, b[0], b[1], CHIP_W);
        eventChip.setVisibility(View.VISIBLE);
    }
    private ThinBar adhanBar, lineBar, nowBar;
    static final long LINE_SECONDS = 30;
    private LinearLayout leftCard;
    private View lineFrame;

    /** The clock's typefaces, chosen in Parent settings: {name, digits font, AM/PM font}. */
    static final String[] CLOCK_FONTS = {"Fraunces", "Outfit", "Jost", "Cormorant", "Inter"};

    private void applyClockFont() {
        int k = prefs.getInt("clock_face", 0) % CLOCK_FONTS.length;
        int[] digits = {R.font.fraunces_soft_light, R.font.outfit_thin, R.font.jost_extralight, R.font.cormorant_light, R.font.inter_extralight};
        Typeface tf = getResources().getFont(digits[k]);
        clockText.setTypeface(tf);
        ampmText.setTypeface(k == 4 ? fLight : tf);
        // each face sits a little differently on the line: tune size, spacing and the AM/PM baseline to match.
        // The clock is the centrepiece: as large as fits between the two top cards.
        float[] size = {340, 310, 306, 330, 304}, spacing = {-0.01f, -0.02f, 0f, -0.01f, -0.03f};
        int[] ampmLift = {58, 50, 50, 56, 48};
        clockText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size[k]);
        clockText.setLetterSpacing(spacing[k]);
        ampmText.setPadding(16, 0, 0, ampmLift[k]);
    }

    /** Middle: a large, slightly see-through clock and the rotating verse / hadith line. */
    /** Text shadows stay tight around the letters (a soft drop shadow, no halo box). */
    static final float TEXT_SHADOW = 4f;
    static final int LINE_FADE = 44;
    static final int CLOCK_TOP = 34, LINE_Y = 494, STRIP_Y = 700, STRIP_H = 124, RIGHT_EDGE = 110;

    private static void moveTo(View v, ViewGroup to, ViewGroup.LayoutParams lp) {
        if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
        to.addView(v, lp);
    }

    private void buildCenter() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.END);
        c.setPadding(0, 0, RIGHT_EDGE, 0);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM | Gravity.END);
        clockText = text("--:--", 230, fThin, 0xF2FFFFFF);
        clockText.setFontFeatureSettings("tnum");
        clockText.setShadowLayer(6, 0, 3, 0x73000000); // close to the digits, so no faint box shows around them
        ampmText = text("", 50, fLight, 0xB3FFFFFF);
        ampmText.setLetterSpacing(0.08f);
        clockRow.addView(clockText);
        clockRow.addView(ampmText);
        applyClockFont();

        // The weather and the date live with the clock now (the weather card is gone): the weather above it, the dates below.
        LinearLayout wxRow = new LinearLayout(this);
        wxRow.setGravity(Gravity.CENTER_VERTICAL);
        tempText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 58);
        skyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28);
        skyText.setTextColor(WHITE);
        moveTo(tempText, wxRow, new LinearLayout.LayoutParams(-2, -2));
        moveTo(skyText, wxRow, new LinearLayout.LayoutParams(-2, -2));
        for (TextView t : new TextView[]{tempText, skyText}) t.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        c.addView(wxRow, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setGravity(Gravity.CENTER_VERTICAL);
        dayText.setPadding(0, 0, 0, 0);
        dayText.setTextColor(SOFT);
        driveText.setPadding(28, 0, 0, 0);
        moveTo(dayText, dayRow, new LinearLayout.LayoutParams(-2, -2));
        moveTo(driveText, dayRow, new LinearLayout.LayoutParams(-2, -2));
        for (TextView t : new TextView[]{dayText, driveText}) t.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        c.addView(dayRow, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(-1, -2);
        crp.topMargin = -62; crp.bottomMargin = -66;
        c.addView(clockRow, crp);
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateText.setPadding(0, 0, 0, 0);
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 34);
        hijriText.setPadding(0, 0, 0, 0);
        hijriText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32);
        hijriText.setTextColor(SOFT);
        TextView dot = text("   ·   ", 30, fMed, DIM);
        moveTo(dateText, dateRow, new LinearLayout.LayoutParams(-2, -2));
        dateRow.addView(dot);
        moveTo(hijriText, dateRow, new LinearLayout.LayoutParams(-2, -2));
        for (TextView t : new TextView[]{dateText, dot, hijriText}) t.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
        c.addView(dateRow, dp);
        // Special days (Ramadan, the Eids, Arafah, Ashura, the White Days): one boxless gold line under the dates, in the
        // same column, so it drops with the clock in ambient mode. It used to be a lone glass chip on the left.
        View oldChip = eventChip;
        ((ViewGroup) oldChip.getParent()).removeView(oldChip);
        ((ViewGroup) eventText.getParent()).removeView(eventText);
        eventText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 24);
        eventText.setPadding(0, 6, 0, 0);
        eventText.setGravity(Gravity.END);
        eventText.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        eventText.setVisibility(View.GONE);
        c.addView(eventText, new LinearLayout.LayoutParams(-2, -2));
        eventChip = eventText;
        topParts[0].setVisibility(View.GONE); // the weather card itself

        // the dates and sunrise sit under the clock; the next adhan, the prayer times and the recitation share one strip below
        ((ViewGroup) sunLine.getParent()).removeView(sunLine); // sunrise and sunset are no longer shown on the home screen
        stage.addView(c, at(1920, -2, Gravity.TOP | Gravity.START, 0, CLOCK_TOP)); // layout 1: one centred column, the clock its heart
        centerGroup = c;

        // The verse / hadith card, low on the screen just above the dock: Amiri for the Arabic, an italic serif for the English.
        lineBox = new LinearLayout(this);
        lineBox.setOrientation(LinearLayout.VERTICAL);
        lineBox.setGravity(Gravity.END);
        lineBox.setPadding(56, 6, 56, 10);
        lineKicker = text("", 22, fSemi, SOFT);
        lineKicker.setLetterSpacing(CAPS_TRACK);
        lineAr = text("", 38, fQuran, WHITE);
        lineAr.setIncludeFontPadding(true);
        lineAr.setGravity(Gravity.END);
        lineAr.setMaxWidth(1700);
        lineAr.setTextSize(TypedValue.COMPLEX_UNIT_PX, 36);
        lineAr.setPadding(0, 6, 0, 0);
        lineEn = text("", 32, fSerif, SOFT);
        lineEn.setGravity(Gravity.END);
        lineEn.setMaxWidth(1700);
        lineEn.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30);
        lineEn.setLineSpacing(0, 1.05f);
        lineEn.setPadding(0, 2, 0, 0);
        lineBox.addView(lineKicker, new LinearLayout.LayoutParams(-2, -2));
        lineBox.addView(lineAr, new LinearLayout.LayoutParams(-2, -2));
        lineBox.addView(lineEn, new LinearLayout.LayoutParams(-2, -2));
        // Fills over the 30 seconds each line stays up.
        lineBar = new ThinBar(this);
        lineBar.setColor(0x66FFFFFF);
        LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(220, 2);
        lbp.topMargin = 14;
        lineBox.addView(lineBar, lbp);
        lineBar.setVisibility(View.GONE); // no stray lines on the home screen
        // no box around it any more: just the words, with a soft shadow so they read on any wallpaper
        for (TextView t : new TextView[]{lineKicker, lineAr, lineEn}) t.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        // held in its own frame: the line's own fade moves lineBox, the quiet-mode slide moves the frame, so neither undoes the other
        FrameLayout lineHolder = new FrameLayout(this);
        lineHolder.setClipChildren(false);
        lineHolder.addView(lineBox, new FrameLayout.LayoutParams(-2, -2, Gravity.END));
        // A soft full-width fade behind the words (no box edge), so they still read over a bright sky or snow clip:
        // clear, about 40% black across the text, then clear again.
        GradientDrawable lineFade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0x66000000, 0x66000000, 0x00000000});
        lineFade.setDither(true);
        lineHolder.setBackground(lineFade);
        lineHolder.setPadding(0, LINE_FADE, RIGHT_EDGE - 56, LINE_FADE); // lineBox has 56 of its own padding
        // a long, low band between the clock and the adhan strip
        stage.addView(lineHolder, at(1920, -2, Gravity.TOP | Gravity.START, 0, LINE_Y - LINE_FADE));
        lineFrame = lineHolder;

        // A small message pill, used for short notices like "PIN saved".
        pillText = text("", 28, fMed, WHITE);
        pillText.setGravity(Gravity.CENTER);
        pillText.setPadding(34, 14, 34, 16);
        pillText.setBackground(new Glass(40, 0, 0, 0x4D000000));
        pillText.setAlpha(0);
        pillText.setElevation(50); // above every panel (30), the player (35) and the reciter list (40); under the intro (60)
        stage.addView(pillText, at(-2, -2, Gravity.TOP | Gravity.START, 80, 56)); // top left, clear of the weather on the right
    }

    static final int TILE = 132, DOCK_H = 188, DOCK_Y = 36;

    /** Bottom: the app dock, centred. Moving across it magnifies the focused app and, a little, its neighbours. */
    private void buildBottom() {
        LinearLayout dock = new LinearLayout(this);
        dockBar = dock;
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(48, 0, 48, 0); // room for the end tiles to grow and slide
        dock.setClipChildren(false);
        dock.setBackground(brandGlow);
        dock.setClipToPadding(false);
        for (Dock d : dockList()) addDockTile(dock, d, -1);
        // just the icons, no panel around them: the soft dark fade at the bottom of the screen keeps them readable
        FrameLayout dockWrap = new FrameLayout(this);
        dockWrap.setClipChildren(false);
        dockWrap.setClipToPadding(false);
        dockWrap.addView(dock, new FrameLayout.LayoutParams(-2, DOCK_H, Gravity.CENTER));
        stage.addView(dockWrap, at(-2, DOCK_H, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, DOCK_Y));
        bottomGroup = dockWrap;

        // the focused app's name, small and quiet, just under its icon
        dockLabel = text("", 22, fMed, 0xF2FFFFFF);
        dockLabel.setLetterSpacing(0.02f);
        dockLabel.setShadowLayer(TEXT_SHADOW, 0, 2, 0x99000000);
        dockLabel.setAlpha(0);
        stage.addView(dockLabel, at(-2, -2, Gravity.TOP | Gravity.START, 0, 0));
    }

    private List<Dock> dockList() {
        List<Dock> out = new ArrayList<>();
        String saved = prefs.getString("dock_order", null);
        if (saved == null) { out.addAll(Arrays.asList(DOCK)); return out; }
        for (String id : saved.split(",")) {
            if (id.isEmpty()) continue;
            Dock d = catalog(id);
            if (d == null && id.startsWith("pkg:")) {
                String pkg = id.substring(4);
                String label = prefs.getString("dock_label:" + pkg, pkg);
                d = new Dock(id, label, 0xFF5A6478, pkg);
            }
            if (d != null) out.add(d);
        }
        for (String fixed : DOCK_FIXED) if (catalog(fixed) != null && !hasId(out, fixed)) out.add(catalog(fixed));
        return out;
    }

    private static boolean hasId(List<Dock> l, String id) { for (Dock d : l) if (d.id.equals(id)) return true; return false; }

    private void saveDock() {
        StringBuilder b = new StringBuilder();
        for (FrameLayout t : dockTiles) b.append(((Dock) t.getTag()).id).append(',');
        prefs.edit().putString("dock_order", b.toString()).apply();
    }

    private FrameLayout tileFor(String id) {
        for (FrameLayout t : dockTiles) if (((Dock) t.getTag()).id.equals(id)) return t;
        return dockTiles.isEmpty() ? null : dockTiles.get(0);
    }

    private void addDockTile(LinearLayout dock, Dock d, int at) {
        FrameLayout tile = new FrameLayout(this);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setTag(d);
        tile.setClipChildren(false);
        // The icon fills the tile edge to edge, with rounded corners, like Apple TV.
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_XY); // the icon bitmap already has rounded corners (fullBleed)
        tile.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        TextView mark;
        if (d.id.equals("quran")) {
            mark = text("القرآن", 44, fQuran, 0xFFF1D58A);
            mark.setIncludeFontPadding(true);
            mark.setBackground(tileFace(0xFF14504A, 0xFF0A2A27));
        } else if (d.id.equals("apps")) {
            mark = text("•••", 44, fBold, WHITE); // Inter has these dots; "⋯" came from another font
            mark.setLetterSpacing(CAPS_TRACK);
            mark.setBackground(new Glass(30, 0, 0, 0x33000000));
        } else {
            mark = text(d.name.substring(0, 1), 64, fBold, WHITE);
            mark.setBackground(tileFace(d.color, darker(d.color)));
        }
        mark.setGravity(Gravity.CENTER);
        tile.addView(mark, new FrameLayout.LayoutParams(-1, -1));
        tile.setPivotX(TILE / 2f); // grow from the middle: setting only pivotY left pivotX at 0, so the icon grew off to the right of its name
        tile.setPivotY(TILE);
        tile.setForeground(ring(30, 2, 0x4DFFFFFF)); // a fine light edge so every icon reads as one set
        tile.setOnLongClickListener(v -> { startArrange((FrameLayout) v); return true; }); // hold OK: move or remove it
        tile.setOnClickListener(v -> openDock(d));
        tile.setOnFocusChangeListener((v, has) -> {
            if (has) { lastDock = dockTiles.indexOf(v); magnify(lastDock); showDockLabel(v, arrangeTile == v ? ARRANGE_HINT : d.name); }
            else if (!anyDockFocused()) { magnify(-1); dockLabel.animate().alpha(0).setDuration(150).start(); }
        });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(TILE, TILE);
        tp.leftMargin = 11; tp.rightMargin = 11;
        if (at < 0) { dock.addView(tile, tp); dockTiles.add(tile); }
        else { dock.addView(tile, at, tp); dockTiles.add(at, tile); }
    }

    // ---------------------------------------------------------------- arranging the home row

    static final String ARRANGE_HINT = "◀ ▶  move     ▼  remove     OK  done";
    private FrameLayout arrangeTile;
    private boolean arrangeSkipUp;

    /** Hold OK on an app in the home row: it lifts with a gold edge, the arrows move it, down removes it, OK finishes. */
    private void startArrange(FrameLayout tile) {
        arrangeTile = tile;
        arrangeSkipUp = true; // the OK that started this is still held: its release must not end it at once
        tile.setForeground(ring(30, 4, GOLD));
        showDockLabel(tile, ARRANGE_HINT);
    }

    private void endArrange(String note) {
        if (arrangeTile == null) return;
        FrameLayout t = arrangeTile;
        arrangeTile = null;
        t.setForeground(ring(30, 2, 0x4DFFFFFF));
        saveDock();
        if (note != null) toast(note);
        if (t.getParent() != null && t.hasFocus()) showDockLabel(t, ((Dock) t.getTag()).name);
    }

    private boolean arrangeKey(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (ok && arrangeSkipUp) { arrangeSkipUp = false; return true; }
            if (ok || code == KeyEvent.KEYCODE_BACK) { endArrange(null); return true; }
            return true;
        }
        if (ok || code == KeyEvent.KEYCODE_BACK) return true;
        int i = dockTiles.indexOf(arrangeTile);
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int to = i + (code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
            if (to < 0 || to >= dockTiles.size()) return true;
            ViewGroup.LayoutParams lp = arrangeTile.getLayoutParams();
            dockBar.removeView(arrangeTile);
            dockBar.addView(arrangeTile, to, lp);
            dockTiles.remove(i);
            dockTiles.add(to, arrangeTile);
            arrangeTile.requestFocus();
            lastDock = to;
            ui.post(() -> { magnify(to); if (arrangeTile != null) showDockLabel(arrangeTile, ARRANGE_HINT); });
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            Dock d = (Dock) arrangeTile.getTag();
            if (DOCK_FIXED.contains(d.id)) { toast(d.name + " always stays on the home row."); return true; }
            FrameLayout gone = arrangeTile;
            arrangeTile = null;
            dockBar.removeView(gone);
            dockTiles.remove(gone);
            saveDock();
            lastDock = Math.max(0, Math.min(i, dockTiles.size() - 1));
            dockTiles.get(lastDock).requestFocus();
            toast("Removed " + d.name + ". Add apps back from All apps (hold OK).");
            return true;
        }
        return true; // up and anything else: stay in arranging
    }

    /** Puts an app on the home row, just before All apps. */
    private void addToDock(String pkg, String label) {
        Dock d = catalogForPkg(pkg);
        if (d == null) { d = new Dock("pkg:" + pkg, label, 0xFF5A6478, pkg); prefs.edit().putString("dock_label:" + pkg, label).apply(); }
        for (FrameLayout t : dockTiles) if (((Dock) t.getTag()).id.equals(d.id)) { toast(label + " is already on the home row."); return; }
        if (dockTiles.size() >= DOCK_MAX) { toast("The home row is full. Hold OK on an app there to remove one first."); return; }
        int at = dockTiles.indexOf(tileFor("apps"));
        addDockTile(dockBar, d, at < 0 ? -1 : at);
        noAlphaLayers(dockBar);
        saveDock();
        loadDockIcons();
        toast("Added " + label + " to the home row.");
    }

    private boolean onDock(String pkg) {
        for (FrameLayout t : dockTiles) for (String p : ((Dock) t.getTag()).pkgs) if (p.equals(pkg)) return true;
        return false;
    }

    private void removeFromDock(String pkg) {
        for (FrameLayout t : new ArrayList<>(dockTiles)) {
            Dock d = (Dock) t.getTag();
            if (DOCK_FIXED.contains(d.id)) continue;
            for (String p : d.pkgs) if (p.equals(pkg)) { dockBar.removeView(t); dockTiles.remove(t); break; }
        }
        lastDock = Math.max(0, Math.min(lastDock, dockTiles.size() - 1));
        saveDock();
        toast("Removed from the home row.");
    }

    private void resetDock() {
        prefs.edit().remove("dock_order").apply();
        for (FrameLayout t : dockTiles) dockBar.removeView(t);
        dockTiles.clear();
        for (Dock d : dockList()) addDockTile(dockBar, d, -1);
        noAlphaLayers(dockBar);
        lastDock = 0;
        loadDockIcons();
    }

    private static GradientDrawable tileFace(int top, int bottom) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(30);
        return g;
    }

    private static int darker(int c) {
        return Color.rgb(Color.red(c) * 6 / 10, Color.green(c) * 6 / 10, Color.blue(c) * 6 / 10);
    }

    private final BrandGlow brandGlow = new BrandGlow();

    private boolean anyDockFocused() {
        for (View t : dockTiles) if (t.hasFocus() || t.isFocused()) return true;
        return false;
    }

    /** Dock magnification: focused 1.3x and lifted, neighbours 1.1x, the rest at rest. */
    private void magnify(int focused) {
        for (int i = 0; i < dockTiles.size(); i++) {
            View t = dockTiles.get(i);
            int dist = focused < 0 ? 9 : Math.abs(i - focused);
            float s = dist == 0 ? 1.2f : dist == 1 ? 1.06f : 1f;
            float lift = dist == 0 ? -20 : dist == 1 ? -5 : 0;
            // the others slide aside so the enlarged tiles never cover their neighbours
            float side = focused < 0 || dist == 0 ? 0 : Math.signum(i - focused) * (dist == 1 ? 22 : 26);
            // focus is only a slight grow and lift with a soft slow-down, plus the app's own colour glowing behind it
            t.animate().scaleX(s).scaleY(s).translationX(side).translationY(lift).setDuration(240).setInterpolator(new DecelerateInterpolator(2f)).start();
        }
        drift(focused);
        if (focused < 0) brandGlow.moveTo(-1, 0);
        else {
            View f = dockTiles.get(focused);
            Dock d = (Dock) f.getTag();
            int c = d.id.equals("quran") ? 0xFF2FA58C : d.id.equals("apps") ? 0xFFDDE6FF : d.color;
            if (f.getWidth() == 0) f.post(() -> brandGlow.moveTo(f.getLeft() + f.getWidth() / 2f, c));
            else brandGlow.moveTo(f.getLeft() + f.getWidth() / 2f, c);
        }
    }

    private void showDockLabel(View tile, String name) {
        if (tile.getWidth() == 0 || !tile.isLaidOut()) { tile.post(() -> { if (tile.hasFocus()) showDockLabel(tile, name); }); return; }
        dockLabel.setText(name);
        dockLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // the tile's resting centre in stage coordinates (layout only, so a slide or grow in progress can't push it off)
        float cx = tile.getLeft() + tile.getWidth() / 2f;
        for (View v = (View) tile.getParent(); v != null && v != stage; v = (View) v.getParent()) cx += v.getLeft();
        dockLabel.animate().cancel();
        dockLabel.setX(Math.max(60, Math.min(1860 - dockLabel.getMeasuredWidth(), cx - dockLabel.getMeasuredWidth() / 2f)));
        float y = 1080 - DOCK_Y - (DOCK_H - TILE) / 2f - 20 + 4; // just under the lifted icon, inside the dock
        dockLabel.setAlpha(0);
        dockLabel.setTranslationY(y + 10);
        dockLabel.animate().alpha(1).translationY(y).setDuration(220).setInterpolator(EASE).start();
    }

    private int clipRetries;
    private boolean wallStopped = true; // the wallpaper player was stopped (home left, or not started yet)
    private final Runnable retryClips = () -> { if (resumed) playRandomClip(); };

    // ================================================================== start-up intro

    /** True only for the first start after the box turns on (or after the app is updated). */
    private static boolean coldStart = true;
    private FrameLayout splash;
    private long splashSince;

    /** An animated intro while the first wallpaper loads; it fades into the home screen once both are done. */
    private void showSplash() {
        coldStart = false;
        splashSince = android.os.SystemClock.uptimeMillis();
        for (View v : new View[]{topParts[0], topParts[1], centerGroup, lineFrame, bottomGroup}) v.setAlpha(0);
        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.BLACK);
        splash.setElevation(60);
        // One of ten animated intros, picked at random and never the same one twice in a row.
        int last = prefs.getInt("last_intro", -1), pick = random.nextInt(IntroView.COUNT);
        if (pick == last) pick = (pick + 1 + random.nextInt(IntroView.COUNT - 1)) % IntroView.COUNT;
        prefs.edit().putInt("last_intro", pick).apply();
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(1920, 1080);
        ip.leftMargin = BLEED; ip.topMargin = BLEED;
        splash.addView(new IntroView(this, pick, fQuran, fSemi), ip);
        stage.addView(splash, bleed());
        ui.postDelayed(this::hideSplash, SPLASH_MAX); // never wait longer than this, even without a video
    }

    static final long SPLASH_MIN = IntroView.LENGTH + 300, SPLASH_MAX = 14000;

    private void splashReady() {
        if (splash == null) return;
        long wait = SPLASH_MIN - (android.os.SystemClock.uptimeMillis() - splashSince);
        ui.postDelayed(this::hideSplash, Math.max(0, wait));
    }

    private void hideSplash() {
        if (splash == null) return;
        FrameLayout s = splash;
        splash = null;
        s.animate().alpha(0).setDuration(900).withEndAction(() -> stage.removeView(s)).start();
        introAnimation();
    }

    private void introAnimation() {
        View[] parts = {topParts[0], topParts[1], centerGroup, lineFrame, bottomGroup};
        for (int i = 0; i < parts.length; i++) {
            View v = parts[i];
            v.setAlpha(0);
            v.setTranslationY(i >= 3 ? 70 : 30);
            v.animate().alpha(1).translationY(0).setStartDelay(120 + i * 120L).setDuration(800)
                    .setInterpolator(new DecelerateInterpolator(2.2f)).start();
        }
    }

    // ================================================================== video

    /** A random clip from Movies/TVWallpapers if the parent added some, otherwise one of the built-in worlds. */
    private void playRandomClip() {
        ui.removeCallbacks(retryClips); // one cut at a time
        List<Uri> clips = new ArrayList<>();
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "TVWallpapers");
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".mp4"));
        // In Ramadan only clips with "ramadan" in their name play (when there are any); the rest of the year they rest.
        boolean ramadan = Events.isRamadan(Calendar.getInstance(KUWAIT)), anyRamadan = false;
        if (files != null) for (File f : files) if (f.getName().toLowerCase(Locale.ROOT).contains("ramadan")) anyRamadan = true;
        if (files != null) for (File f : files) {
            boolean r = f.getName().toLowerCase(Locale.ROOT).contains("ramadan");
            if (f.canRead() && (ramadan && anyRamadan ? r : !r)) clips.add(Uri.fromFile(f));
        }
        if (clips.isEmpty()) {
            // no clips yet: the calm gradient below. Just after the box turns on the storage may still be
            // getting ready, so look again every few seconds for the first two minutes.
            videoCover.setAlpha(1);
            player.stop();
            ui.removeCallbacks(retryClips);
            if (resumed && ++clipRetries < 40) ui.postDelayed(retryClips, 3000);
            return;
        }
        clipRetries = 0;
        Uri pick = clips.get(random.nextInt(clips.size()));
        if (clips.size() > 1) while (pick.toString().equals(currentClip)) pick = clips.get(random.nextInt(clips.size()));
        currentClip = pick.toString();
        final Uri chosen = pick;
        videoCover.animate().alpha(1).setDuration(player.isPlaying() ? 300 : 0).withEndAction(() -> {
            if (!resumed || starsOn) { wallStopped = true; return; } // nobody can see it (another app, or the star field): pick again later
            playingUri = chosen;
            player.setMediaItem(MediaItem.fromUri(chosen));
            player.prepare();
            player.play();
        }).start();
    }

    // ================================================================== clock, adhan, widgets

    private void tick() {
        if (stress) return; // the stress test holds its long texts until a key is pressed
        Calendar now = Calendar.getInstance(KUWAIT);
        int y = now.get(Calendar.YEAR), m = now.get(Calendar.MONTH) + 1, d = now.get(Calendar.DAY_OF_MONTH);
        String key = y + "-" + m + "-" + d;
        if (!key.equals(cachedDay)) {
            cachedDay = key;
            today = PrayerTimes.forDate(y, m, d);
            Calendar tm = (Calendar) now.clone();
            tm.add(Calendar.DAY_OF_MONTH, 1);
            tomorrow = PrayerTimes.forDate(tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH));
            for (int i = 0; i < 5; i++) prayerTexts[i][2].setText(PrayerTimes.clock(today[BAR_PRAYERS[i]]));
            drawDayLine();
            drawEventChip(now);
            SimpleDateFormat f = new SimpleDateFormat("EEEE d MMMM", Locale.UK);
            f.setTimeZone(KUWAIT);
            dateText.setText(f.format(now.getTime()));
            try {
                DateFormat hf = DateFormat.getDateInstance(DateFormat.LONG, new ULocale("ar_SA@calendar=islamic-umalqura"));
                hf.setTimeZone(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
                hijriText.setText(hf.format(now.getTime()));
            } catch (Exception ignored) { }
        }
        if (!key.equals(ayahDay)) { ayahDay = key; fetchAyah(now.get(Calendar.DAY_OF_YEAR) + y * 400); }

        int h = now.get(Calendar.HOUR_OF_DAY), mi = now.get(Calendar.MINUTE);
        boolean h24 = prefs.getBoolean("clock24", false);
        String clock = (h24 ? (h < 10 ? "0" + h : "" + h) : "" + ((h + 11) % 12 + 1)) + ":" + (mi < 10 ? "0" : "") + mi;
        if (!clock.equals(shownClock)) {
            shownClock = clock;
            drawSun();
            drawSky(h * 60 + mi);
            clockText.setText(clock);
            ampmText.setText(h24 ? "" : h < 12 ? "AM" : "PM");
        }
        updatePill();

        int adhkar = Widgets.adhkarMode(now, today);
        if (!resumed) {
            // behind another app: keep the time checks (reminders, the adhan pause) but no fades nobody sees;
            // onResume catches the card up straight away
            if (adhkar != shownAdhkar) { shownAdhkar = adhkar; lineIndex = -1; lastLineSwitch = 0; }
        } else if (adhkar != shownAdhkar) {
            // Fajr or Maghrib has come in: the adhkar take over the card straight away
            boolean begins = shownAdhkar != -1 && adhkar != 0;
            shownAdhkar = adhkar;
            lineIndex = -1;
            nextLine(now);
            if (begins) toast(adhkar == 1 ? "Morning adhkar · أذكار الصباح" : "Evening adhkar · أذكار المساء");
        } else if (System.currentTimeMillis() - lastLineSwitch >= LINE_SECONDS * 1000) nextLine(now);
        if (quran != null && quran.isPlaying() && ++listenSec >= 60) { Stats.listened(this, listenSec); listenSec = 0; }
        if (nowBar != null && quran != null && quran.getDuration() > 0) nowBar.setProgress(quran.getCurrentPosition() / (float) quran.getDuration());
        if (System.currentTimeMillis() - lastWeather > 30 * 60 * 1000) { lastWeather = System.currentTimeMillis(); fetchWeather(); }
        ui.postDelayed(this::tick, 1000 - (System.currentTimeMillis() % 1000));
    }

    private int nextPrayer(int nowSec) {
        for (int i = 0; i < 6; i++) if (i != 1 && today[i] * 60 > nowSec) return i;
        return -1;
    }

    private void updatePill() {
        if (today == null) return;
        Calendar now = Calendar.getInstance(KUWAIT);
        int nowSec = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
        int next = nextPrayer(nowSec);
        int target = next >= 0 ? today[next] * 60 : tomorrow[0] * 60 + 86400;
        if (next < 0) next = 0;
        int left = target - nowSec;
        if (left <= 600 && left > 590 && remindedFor != target) {
            // ten minutes to go (inside other apps the adhan app shows the same reminder)
            remindedFor = target;
            int n = next >= 0 ? next : 0;
            toast(PrayerTimes.EN[n] + " in 10 minutes · " + PrayerTimes.AR[n] + " بعد ١٠ دقائق");
        }
        int prev = -1;
        for (int i = 0; i < 6; i++) if (i != 1 && today[i] * 60 <= nowSec) prev = today[i] * 60;
        if (prev < 0) prev = today[5] * 60 - 86400; // after midnight: counting from last night's Isha
        if (resumed) adhanBar.setProgress((nowSec - prev) / (float) Math.max(1, target - prev));
        adhanBar.setColor(left <= 15 * 60 ? 0xB3F1D58A : 0x8CFFFFFF);
        if (resumed) adhanCountdown.setText(String.format(Locale.US, "%d:%02d:%02d", left / 3600, left % 3600 / 60, left % 60));
        adhanCountdown.setTextColor(left <= 15 * 60 ? 0xFFF1D58A : WHITE); // gold in the last 15 minutes
        if (!ambient && resumed && !anyPanelOpen() && System.currentTimeMillis() - lastInput > AMBIENT_AFTER) enterAmbient();
        if (ambient && !nightDim && System.currentTimeMillis() - lastInput > DIM_AFTER) enterNightDim();
        boolean ramadan = Events.isRamadan(now), friday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        if (ramadan && next == 4) { adhanName.setText("Iftar"); adhanAr.setText("الإفطار · المغرب"); }
        else if (ramadan && next == 0) { adhanName.setText("Suhoor ends"); adhanAr.setText("الإمساك · الفجر"); }
        else if (friday && next == 2) { adhanName.setText("Jumu'ah"); adhanAr.setText("صلاة الجمعة"); }
        else { adhanName.setText(PrayerTimes.EN[next]); adhanAr.setText(PrayerTimes.AR[next]); }
        if (friday && next == 2 && left <= 3600 && left > 3590 && remindedFor != -target) {
            remindedFor = -target;
            toast("Jumu'ah in 1 hour: ghusl and go early · الجمعة بعد ساعة");
        }
        if (pillTextOverride != null) {
            pillText.setText(pillTextOverride);
            pillText.animate().alpha(1).setDuration(200).setInterpolator(EASE).start();
            if (nowWidget != null && nowWidget.getVisibility() == View.VISIBLE) nowWidget.animate().alpha(0).setDuration(150).start();
        } else if (pillText.getAlpha() > 0) {
            pillText.animate().alpha(0).setDuration(300).setInterpolator(EXIT).start();
            if (nowWidget != null && nowWidget.getVisibility() == View.VISIBLE) nowWidget.animate().alpha(1).setDuration(300).start();
        }
        int passedMask = 0;
        for (int i = 0; i < 5; i++) if (BAR_PRAYERS[i] != next && today[BAR_PRAYERS[i]] * 60 <= nowSec) passedMask |= 1 << i;
        if (next == shownNext && passedMask == shownPassedMask) return; // only redraw the bar when it changes
        // only a prayer time arriving pauses the Quran (at midnight the passed prayers reset, but no adhan is due)
        if (shownNext != -2 && next != shownNext && quran != null && quran.getPlayWhenReady()) { quran.pause(); drawQuranState(); toast("Paused for the adhan · توقف للأذان"); }
        shownNext = next;
        shownPassedMask = passedMask;
        for (int i = 0; i < 5; i++) {
            boolean isNext = BAR_PRAYERS[i] == next;
            // no box in the boxless strip: the next prayer is marked in the accent, its name white and its time gold
            prayerTexts[i][0].setTextColor(isNext ? WHITE : SOFT);
            prayerTexts[i][2].setTextColor(isNext ? GOLD : WHITE);
            prayerCells[i].setAlpha((passedMask & (1 << i)) != 0 ? 0.55f : 1f);
        }
    }

    private void nextLine(Calendar now) {
        lastLineSwitch = System.currentTimeMillis();
        List<String[]> items = Widgets.items(this, now, today, ayahToday, tempNow, dustNow);
        if (items.isEmpty()) return;
        lineIndex = (lineIndex + 1) % items.size();
        String[] it = items.get(lineIndex); // {kicker, ar, en, ref}
        // the most frequent motion on the home: a calm opacity-only cross-fade, about 650 ms in all
        lineBox.animate().setStartDelay(0).alpha(0).setDuration(250).setInterpolator(EXIT).withEndAction(() -> {
            lineKicker.setText(it[0] + (it[3].isEmpty() ? "" : "   ·   " + it[3]));
            lineAr.setText(it[1]);
            lineAr.setTextSize(TypedValue.COMPLEX_UNIT_PX, it[1].length() > 110 ? 32 : 38);
            lineEn.setTextSize(TypedValue.COMPLEX_UNIT_PX, it[2].length() > 150 ? 28 : 32);
            lineAr.setVisibility(it[1].isEmpty() ? View.GONE : View.VISIBLE);
            lineEn.setText(it[2]);
            lineBox.setTranslationY(0);
            lineBox.animate().setStartDelay(0).alpha(1).setDuration(400).setInterpolator(EASE).start();
            // no progress timer: the line's bar is hidden, and a 60 fps animator for it woke the box all day
        }).start();
    }

    static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        } finally {
            c.disconnect();
        }
        return sb.toString();
    }

    private void fetchWeather() {
        runBg(() -> {
            try {
                JSONObject all = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=29.3759&longitude=47.9774&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=2&timezone=Asia%2FKuwait"));
                try {
                    JSONObject daily = all.getJSONObject("daily");
                    org.json.JSONArray mx = daily.getJSONArray("temperature_2m_max"), mn = daily.getJSONArray("temperature_2m_min");
                    org.json.JSONArray wc = daily.optJSONArray("weather_code");
                    hiLo = "Today  " + Math.round(mx.getDouble(0)) + "° / " + Math.round(mn.getDouble(0)) + "°";
                    if (mx.length() > 1)
                        hiLo += "     Tomorrow  " + (wc != null && wc.length() > 1 ? skyWord(wc.getInt(1)) + "  " : "")
                                + Math.round(mx.getDouble(1)) + "° / " + Math.round(mn.getDouble(1)) + "°";
                } catch (Exception ignored) { }
                JSONObject cur = all.getJSONObject("current");
                tempNow = cur.getDouble("temperature_2m");
                int code = cur.getInt("weather_code");
                try {
                    dustNow = new JSONObject(get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=29.3759&longitude=47.9774&current=dust"))
                            .getJSONObject("current").getDouble("dust");
                } catch (Exception ignored) { }
                String sky = !Double.isNaN(dustNow) && dustNow > 150 ? "Dusty · غبار" : skyName(code);
                long t = Math.round(tempNow);
                ui.post(() -> { tempText.setText(t + "°"); fitPair(skyText, sky, SKY_W); drawDayLine(); });
            } catch (Exception e) {
                lastWeather = System.currentTimeMillis() - 25 * 60 * 1000; // try again in 5 minutes
            }
        });
    }

    /** Bottom of the weather card: today's high and low, and sunrise (الشروق). */
    /** Bottom of the weather card: today's and tomorrow's high and low (sunrise and sunset live in the sun card). */
    private void drawDayLine() {
        dayText.setText(hiLo.isEmpty() ? "Forecast loading…" : hiLo);
        drawSun();
    }

    /** Tomorrow's sky as a word, in the column's own white: colour emoji would be the only cartoon on the calm screen. */
    private static String skyWord(int code) {
        String n = skyName(code);
        return n.substring(0, n.indexOf(" · "));
    }

    private static String skyName(int code) {
        if (code == 0) return "Clear · صافٍ";
        if (code <= 3) return "Partly cloudy · غائم جزئيًا";
        if (code <= 48) return "Hazy · ضباب";
        if (code <= 67 || (code >= 80 && code <= 82)) return "Rain · مطر";
        if (code >= 95) return "Thunderstorm · رعد";
        return "Cloudy · غائم";
    }

    private void fetchAyah(int seed) {
        runBg(() -> {
            for (int attempt = 0; attempt < 12; attempt++) {
                int n = (int) (((long) (seed + attempt) * 7919L) % 6236) + 1;
                try {
                    JSONObject d = new JSONObject(get("https://api.alquran.cloud/v1/ayah/" + n + "/editions/quran-uthmani,en.sahih"));
                    JSONArray arr = d.getJSONArray("data");
                    JSONObject ar = arr.getJSONObject(0), en = arr.getJSONObject(1);
                    JSONObject sur = ar.getJSONObject("surah");
                    String text = withoutBismillah(ar.getString("text"), sur.getInt("number"), ar.getInt("numberInSurah"));
                    if (text.length() > 120) continue; // never a long one: after 12 tries the built-in verses are used
                    JSONObject surah = ar.getJSONObject("surah");
                    String ref = surah.getString("englishName") + " " + surah.getInt("number") + ":" + ar.getInt("numberInSurah");
                    String[] a = {text, en.getString("text"), ref};
                    ui.post(() -> ayahToday = a);
                    return;
                } catch (Exception e) {
                    return; // offline: the built-in verses are used
                }
            }
        });
    }

    /**
     * The Qur'an service puts the Bismillah in front of verse 1 of every surah except Al-Fatihah (where it IS verse 1)
     * and At-Tawbah (which has none). It isn't part of that verse, so it is taken off: its four words.
     */
    static String withoutBismillah(String text, int surah, int ayah) {
        if (ayah != 1 || surah == 1) return text;
        String t = text.replace("\uFEFF", "").trim();
        String[] w = t.split("\\s+", 5);
        // The first word without its vowel marks must be بسم, however the marks are encoded.
        String first = w[0].replaceAll("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]", "");
        if (w.length < 5 || !first.equals("\u0628\u0633\u0645")) return text;
        return w[4];
    }

    // ================================================================== dock

    private void loadDockIcons() {
        final PackageManager pm = getPackageManager();
        runBg(() -> {
            for (FrameLayout tile : dockTiles) {
                Dock d = (Dock) tile.getTag();
                Drawable icon = null;
                for (String p : d.pkgs) {
                    icon = appIcon(pm, p);
                    if (icon != null) break;
                }
                if (icon == null && d.pkgs.length > 0) {
                    String pkg = findByName(dockSearchName(d));
                    if (pkg != null) icon = appIcon(pm, pkg);
                }
                final Drawable ic = icon;
                ui.post(() -> {
                    ((ImageView) tile.getChildAt(0)).setImageDrawable(ic);
                    tile.getChildAt(1).setVisibility(ic == null ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private void openDock(Dock d) {
        switch (d.id) {
            case "yt": openPin(false); return;
            case "apps": openApps("apps"); return;
            case "quran": openQuran(); return;
            case "kids": if (!launch("com.family.kids")) toast("Kids Corner isn't installed yet. Run Update TV on the PC."); return;
            default:
                if (launchAny(d.pkgs, dockSearchName(d))) return;
                toast(d.name + " isn't installed yet. Opening the Play Store…");
                ui.postDelayed(() -> openStore(d.pkgs[0]), 1200);
        }
    }

    private boolean launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return true; } catch (ActivityNotFoundException e) { return false; }
    }

    private boolean launchAny(String[] pkgs, String name) {
        for (String p : pkgs) if (launch(p)) return true;
        String pkg = findByName(name);
        return pkg != null && launch(pkg);
    }

    private static String dockSearchName(Dock d) { return d.id.equals("browser") ? "browse" : d.name; }

    /** An installed app whose name contains the word, e.g. "browse" finds BrowseHere. */
    private String findByName(String name) {
        String w = name.toLowerCase(Locale.ROOT);
        PackageManager pm = getPackageManager();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER})
            for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (r.loadLabel(pm).toString().toLowerCase(Locale.ROOT).contains(w)) return pkg;
            }
        return null;
    }

    /** Draws an app icon edge to edge: adaptive icons lose their mask and show their full square art. */
    /**
     * Finished icons, kept by package and rebuilt only when the app itself changes (its lastUpdateTime), so coming back
     * home no longer redraws forty icons while the screen fades in. Each caller gets its own copy of the picture.
     */
    private static final java.util.concurrent.ConcurrentHashMap<String, Object[]> ICONS = new java.util.concurrent.ConcurrentHashMap<>();

    private Drawable cachedIcon(PackageManager pm, String key, String pkg, java.util.function.Supplier<Drawable> raw) {
        long stamp;
        try { stamp = pm.getPackageInfo(pkg, 0).lastUpdateTime; } catch (Exception e) { ICONS.remove(key); return null; }
        Object[] hit = ICONS.get(key);
        if (hit != null && (long) hit[0] == stamp) {
            Drawable.ConstantState cs = (Drawable.ConstantState) hit[1];
            if (cs != null) return cs.newDrawable(getResources());
        }
        Drawable d;
        try { d = fullBleed(raw.get()); } catch (Exception e) { return null; }
        if (d == null) return null;
        if (d instanceof android.graphics.drawable.BitmapDrawable) ((android.graphics.drawable.BitmapDrawable) d).getBitmap().prepareToDraw();
        Drawable.ConstantState cs = d.getConstantState();
        if (cs != null) { ICONS.put(key, new Object[]{stamp, cs}); return cs.newDrawable(getResources()); }
        return d;
    }

    private Drawable appIcon(PackageManager pm, String pkg) {
        return cachedIcon(pm, "p:" + pkg, pkg, () -> {
            try { return pm.getApplicationIcon(pkg); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    private Drawable fullBleed(Drawable icon) {
        int size = 168;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) icon;
            int pad = size / 4; // the visible part of an adaptive icon is the middle two thirds
            for (Drawable layer : new Drawable[]{a.getBackground(), a.getForeground()}) {
                if (layer == null) continue;
                layer.setBounds(-pad, -pad, size + pad, size + pad);
                layer.draw(c);
            }
        } else {
            // Older icons carry empty margins: cut to the visible part and stretch it over the whole tile.
            int big = size * 2;
            Bitmap raw = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, big, big);
            icon.draw(new Canvas(raw));
            int l = big, t = big, r = -1, btm = -1;
            for (int y = 0; y < big; y += 2) for (int x = 0; x < big; x += 2) {
                if (Color.alpha(raw.getPixel(x, y)) > 40) { l = Math.min(l, x); r = Math.max(r, x); t = Math.min(t, y); btm = Math.max(btm, y); }
            }
            if (r < 0) { l = 0; t = 0; r = big - 1; btm = big - 1; }
            int side = Math.max(r - l, btm - t) + 2, cx = (l + r) / 2, cy = (t + btm) / 2;
            // Rounded or round icons leave corners empty: fill them with the colour along the icon's edge.
            c.drawColor(edgeColor(raw, cx, cy, side / 2));
            int inset = Math.round(side * 0.04f); // trim the soft rim so the art meets the tile's edge
            Rect src = new Rect(cx - side / 2 + inset, cy - side / 2 + inset, cx + side / 2 - inset, cy + side / 2 - inset);
            c.drawBitmap(raw, src, new Rect(0, 0, size, size), new Paint(Paint.FILTER_BITMAP_FLAG));
            raw.recycle();
        }
        // Round the corners once here, so the dock never has to clip each icon to a rounded shape every frame.
        Bitmap round = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        rp.setShader(new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float rr = 30f * size / TILE;
        new Canvas(round).drawRoundRect(new RectF(0, 0, size, size), rr, rr, rp);
        b.recycle();
        return new BitmapDrawable(getResources(), round);
    }

    /** Average of the solid pixels a little inside the icon's edge, used to fill the tile behind it. */
    private static int edgeColor(Bitmap raw, int cx, int cy, int half) {
        long rs = 0, gs = 0, bs = 0, n = 0;
        int in = Math.max(4, half / 6);
        int[][] pts = {{cx, cy - half + in}, {cx, cy + half - in}, {cx - half + in, cy}, {cx + half - in, cy}};
        for (int[] p : pts) {
            int x = Math.max(0, Math.min(raw.getWidth() - 1, p[0])), y = Math.max(0, Math.min(raw.getHeight() - 1, p[1]));
            int px = raw.getPixel(x, y);
            if (Color.alpha(px) < 200) continue;
            rs += Color.red(px); gs += Color.green(px); bs += Color.blue(px); n++;
        }
        return n == 0 ? 0xFF20242E : Color.rgb((int) (rs / n), (int) (gs / n), (int) (bs / n));
    }

    private void openStore(String pkg) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    // ================================================================== overlays (shared look)

    /** Keep a view's drawing inside its own box even when its parent lets children overflow. */
    private static void clipToBounds(View v) {
        v.setOutlineProvider(android.view.ViewOutlineProvider.BOUNDS);
        v.setClipToOutline(true);
    }

    /**
     * A layer that keeps the highlight inside itself. Android otherwise searches the whole screen for the next item
     * when an arrow is pressed at a panel's edge, and the home row behind a panel is only faded (alpha 0), not gone,
     * so the highlight could land on an app you can't see. Here an arrow with nowhere to go simply stays put.
     */
    static class TrapFrame extends FrameLayout {
        TrapFrame(Context c) { super(c); }
        @Override public View focusSearch(View focused, int direction) {
            View next = android.view.FocusFinder.getInstance().findNextFocus(this, focused, direction);
            return next != null ? next : focused;
        }
    }

    /** Only the top layer may hold the highlight: everything under it is blocked (Kids Corner does the same in syncFocus).
     *  top is the open layer, or null for the home row itself. */
    private void focusLayer(View top) {
        block(bottomGroup, top != null);
        for (View p : new View[]{pinPanel, appsPanel, calendarPanel, quranPanel, parentPanel, playerPanel, reciterPanel})
            block(p, p != top);
    }

    private static void block(View v, boolean blocked) {
        if (v instanceof ViewGroup) ((ViewGroup) v).setDescendantFocusability(blocked ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    private FrameLayout overlay() {
        FrameLayout o = new TrapFrame(this);
        o.setClipChildren(false);
        // a soft scrim: darker at the top and bottom where the headers and last rows sit, lighter in the middle
        View scrim = new View(this);
        scrim.setTag("bg");
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xCC050810, 0x8C050810, 0xD9050810}));
        scrim.getBackground().setDither(true);
        o.addView(scrim, bleed());
        o.setVisibility(View.GONE);
        o.setElevation(30);
        stage.addView(o, new FrameLayout.LayoutParams(1920, 1080));
        return o;
    }

    /**
     * A view drawn see-through (alpha below 1) is normally painted into its own off-screen picture first, every frame.
     * On this box those extra full-size pictures were the main cost per frame, so fade each piece on its own instead.
     */
    private static void noAlphaLayers(View v) {
        v.forceHasOverlappingRendering(false);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) noAlphaLayers(g.getChildAt(i));
        }
    }

    private void show(View v) {
        noAlphaLayers(v);
        v.bringToFront(); // panels share one elevation, so the one opened last must also be drawn last
        focusLayer(v);
        placePill(v == playerPanel ? PILL_PLAYER : PILL_PANEL);
        if (v == quranPanel) ui.postDelayed(starsUp, 300);
        v.setAlpha(0);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1).setDuration(260).setInterpolator(EASE).start();
        // every panel rises a little into place
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if ("bg".equals(c.getTag())) continue; // backdrops stay put
            c.setTranslationY(28);
            c.animate().translationY(0).setStartDelay(i * 40L).setDuration(420).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        }
        for (View p : homeParts()) if (p != null) p.animate().alpha(0).setStartDelay(0).setDuration(220).setInterpolator(EASE).start(); // no leftover intro delay
    }

    /** Everything on the home screen that steps back while a panel is open. */
    private View[] homeParts() {
        return new View[]{centerGroup, bottomGroup, lineFrame, topParts[0], topParts[1], eventChip, sunCard, nowWidget};
    }

    /** The shared panel header: a small spaced kicker over a large title, top left. Returns the title. */
    private TextView panelHeader(FrameLayout panel, String kickerText, String titleText) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.addView(kicker(text(kickerText, 20, fSemi, 0xCCF1D58A)), new LinearLayout.LayoutParams(-2, -2));
        TextView t = text(titleText, 52, fSemi, WHITE);
        t.setPadding(0, 8, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(-2, -2));
        View bar = new View(this);
        bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFFF1D58A, 0x00F1D58A}));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(140, 3);
        bp.topMargin = 12;
        h.addView(bar, bp);
        panel.addView(h, at(-2, -2, Gravity.TOP | Gravity.START, 110, 56));
        return t;
    }

    static final int GOLD = 0xFFF1D58A;

    /** The focused look everywhere: brighter glass with a gold edge. */
    private static GradientDrawable focusGlass(float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x47FFFFFF, 0x1FFFFFFF});
        d.setCornerRadius(radius);
        d.setStroke(3, 0xE6F1D58A);
        return d;
    }

    /** Lifts a focused item with a warm gold shadow (drawn by the system, so it stays smooth). */
    private static void goldLift(View v, boolean has, float z) {
        v.setOutlineAmbientShadowColor(GOLD);
        v.setOutlineSpotShadowColor(GOLD);
        v.animate().setStartDelay(0).setInterpolator(EASE).translationZ(has ? z : 0).setDuration(200).start(); // never waits behind a fade-in
    }

    /** A solid glass card in the middle of a panel. Plain gradient and edge only, clipped to its rounded shape. */
    private FrameLayout panelCard(FrameLayout panel, View content, int w, int h) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(new PanelBg(44));
        f.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 44); }
        });
        f.setClipToOutline(true);
        f.addView(content, new FrameLayout.LayoutParams(-1, h < 0 ? -2 : -1));
        panel.addView(f, new FrameLayout.LayoutParams(w, h < 0 ? -2 : h, Gravity.CENTER));
        return f;
    }

    /**
     * A centred glass card: a fixed header (kicker and title) and a scrolling list under it. The list has spare room
     * on every side, so a focused row can grow and glow without being cut at the card's edges.
     */
    private ScrollView listCard(FrameLayout panel, String kick, String heading, View list, int w, int h) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(56, 44, 56, 24);
        col.addView(kicker(text(kick, 20, fSemi, DIM)));
        TextView title = text(heading, 46, fSemi, WHITE);
        title.setPadding(0, 10, 0, 12);
        col.addView(title);
        ScrollView sv = new ScrollView(this);
        clipToBounds(sv);
        sv.setVerticalScrollBarEnabled(false);
        // the room for a growing row sits inside the scrolled list itself, so the soft fade at the top and bottom
        // edges covers whole rows evenly (no half-faded row)
        list.setPadding(30, 14, 30, 30);
        sv.setVerticalFadingEdgeEnabled(true);
        sv.setFadingEdgeLength(110);
        sv.addView(list);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1);
        lp.leftMargin = -30; lp.rightMargin = -30; // the list's side room reaches into the card's own padding
        col.addView(sv, lp);
        panelCard(panel, col, w, h);
        return sv;
    }

    /** A hint pill at the top right of a panel, e.g. which button does what. */
    private TextView panelHint(FrameLayout panel, String hint) {
        TextView t = text(hint, 22, fMed, SOFT);
        t.setPadding(26, 12, 28, 14);
        t.setBackground(glass(99, 0x14FFFFFF, GLASS_EDGE));
        panel.addView(t, at(-2, -2, Gravity.TOP | Gravity.END, 110, 74));
        return t;
    }

    private void closePanels() {
        RemoteKeys.stopLearning();
        ui.removeCallbacks(learnTimeout);
        ui.removeCallbacks(parentIdle);
        parentOpen = false;
        pinOverParent = false;
        statsShown = false;
        calendarFromApps = false;
        if (playerPanel != null) playerPanel.setVisibility(View.GONE);
        if (playerVideo) { playerVideo = false; playRandomClip(); } // leave the Quran background, back to a wallpaper
        ui.removeCallbacks(starsUp);
        starsOver(false);
        if (reciterPanel != null) reciterPanel.setVisibility(View.GONE);
        recitersOpen = false;
        boolean wasOpen = pinPanel.getVisibility() == View.VISIBLE || appsPanel.getVisibility() == View.VISIBLE
                || quranPanel.getVisibility() == View.VISIBLE || parentPanel.getVisibility() == View.VISIBLE
                || calendarPanel.getVisibility() == View.VISIBLE;
        View back = tileFor(quranPanel.getVisibility() == View.VISIBLE ? "quran"
                : pinPanel.getVisibility() == View.VISIBLE ? "yt" : appsPanel.getVisibility() == View.VISIBLE ? "apps"
                : ((Dock) dockTiles.get(Math.max(0, Math.min(lastDock, dockTiles.size() - 1))).getTag()).id);
        // The highlight goes to the right app first and the panels fade after, so it never flashes on the first app
        // or stays inside a panel that is going away (a quick OK can't press something in it).
        focusLayer(null);
        if (wasOpen && back != null) back.requestFocus();
        for (View v : new View[]{pinPanel, appsPanel, quranPanel, parentPanel, calendarPanel}) {
            if (v.getVisibility() == View.VISIBLE)
                v.animate().alpha(0).setStartDelay(0).setDuration(180).setInterpolator(EXIT).withEndAction(() -> v.setVisibility(View.GONE)).start();
        }
        for (View p : homeParts()) if (p != null && !(p == nowWidget && p.getVisibility() != View.VISIBLE)) p.animate().alpha(1).setStartDelay(0).setDuration(250).setInterpolator(EASE).start();
        placePill(PILL_HOME);
    }

    // ================================================================== YouTube PIN

    private ImageView pinIcon;
    private View pinBox;

    /** The YouTube lock: a centred glass card with the YouTube icon, four dots and a round keypad. */
    private void buildPin() {
        pinPanel = overlay();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(64, 48, 64, 44);
        box.setClipChildren(false);

        pinIcon = new ImageView(this);
        pinIcon.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 24); }
        });
        pinIcon.setClipToOutline(true);
        box.addView(pinIcon, new LinearLayout.LayoutParams(96, 96));
        TextView k = kicker(text("PARENT LOCK  ·  قفل الوالدين", 20, fSemi, DIM));
        k.setPadding(0, 22, 0, 0);
        box.addView(k, new LinearLayout.LayoutParams(-2, -2));
        pinTitle = text("", 44, fSemi, WHITE);
        pinTitle.setGravity(Gravity.CENTER);
        pinTitle.setPadding(0, 12, 0, 0);
        box.addView(pinTitle, new LinearLayout.LayoutParams(-1, -2));
        pinSub = text("", 24, fMed, DIM);
        pinSub.setGravity(Gravity.CENTER);
        pinSub.setLineSpacing(0, 1.15f);
        pinSub.setPadding(0, 10, 0, 30);
        box.addView(pinSub, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(26, 26);
            p.leftMargin = 18; p.rightMargin = 18;
            dots.addView(dot, p);
            pinDots[i] = dot;
        }
        box.addView(dots, new LinearLayout.LayoutParams(-1, 40));

        GridLayout keys = new GridLayout(this);
        keys.setColumnCount(3);
        keys.setPadding(0, 30, 0, 0);
        keys.setClipChildren(false);
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "×", "0", "⌫"};
        for (String label : labels) {
            boolean digit = Character.isDigit(label.charAt(0));
            TextView key = text(label, digit ? 40 : 32, fSemi, digit ? WHITE : SOFT);
            key.setGravity(Gravity.CENTER);
            key.setFocusable(true);
            key.setClickable(true);
            key.setTag("dip"); // has its own press dip
            GradientDrawable off = new GradientDrawable();
            off.setShape(GradientDrawable.OVAL);
            off.setColor(digit ? 0x1FFFFFFF : 0x0DFFFFFF);
            off.setStroke(2, 0x2EFFFFFF);
            GradientDrawable on = new GradientDrawable();
            on.setShape(GradientDrawable.OVAL);
            on.setColors(new int[]{0x59FFFFFF, 0x26FFFFFF});
            on.setStroke(3, 0xE6F1D58A);
            key.setBackground(off);
            key.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? on : off);
                v.animate().scaleX(has ? 1.12f : 1f).scaleY(has ? 1.12f : 1f).setDuration(200).setInterpolator(EASE).start();
                goldLift(v, has, 16);
            });
            key.setOnClickListener(v -> {
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(() ->
                        v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140).setInterpolator(EASE).start()).start();
                pinPress(label);
            });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 96; p.height = 96;
            p.setMargins(18, 12, 18, 12);
            keys.addView(key, p);
            if (digit) pinKeys.add(key);
            if (label.equals("1")) firstPinKey = key;
        }
        box.addView(keys, new LinearLayout.LayoutParams(-2, -2));
        pinBox = panelCard(pinPanel, box, 600, -1);
    }

    private boolean pinSet() { return PinLock.isSet(prefs); }

    /** The parent lock face: a gold padlock on deep night blue, for the parent settings code. */
    private Drawable lockFace() {
        Bitmap b = Bitmap.createBitmap(168, 168, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new android.graphics.LinearGradient(0, 0, 0, 168, 0xFF1E2A50, 0xFF0B1022, android.graphics.Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 168, 168, p);
        p.setShader(null);
        p.setColor(0xFFF1D58A);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(12);
        c.drawArc(new android.graphics.RectF(56, 34, 112, 100), 180, 180, false, p);
        c.drawLine(56, 68, 56, 82, p);
        c.drawLine(112, 68, 112, 82, p);
        p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new android.graphics.RectF(42, 78, 126, 140), 16, 16, p);
        p.setColor(0xFF0B1022);
        c.drawCircle(84, 104, 9, p);
        c.drawRect(80, 106, 88, 124, p);
        return new BitmapDrawable(getResources(), b);
    }

    /** True while the PIN pad was opened from Parent settings ("Change the YouTube PIN"): it closes back to them. */
    private boolean pinOverParent;
    /** Parent settings are really open. Visibility can't say so: a fade cut short by another fade keeps the panel VISIBLE. */
    private boolean parentOpen;

    private void openPin(boolean change) {
        pinLaunchAfter = !change;
        pinThen = null;
        pinOverParent = parentOpen;
        if (pinOverParent) parentPanel.animate().alpha(0).setDuration(200).start(); // the pad is the only thing to look at
        try { pinIcon.setImageDrawable(fullBleed(getPackageManager().getApplicationIcon(DOCK[dockIndex("yt")].pkgs[0]))); }
        catch (Exception e) { pinIcon.setImageDrawable(tileFace(0xFFE62117, 0xFF8A140E)); }
        show(pinPanel);
        pinBox.setScaleX(0.92f); pinBox.setScaleY(0.92f); pinBox.setAlpha(0); pinBox.setTranslationY(24);
        pinBox.animate().scaleX(1).scaleY(1).alpha(1).translationY(0).setDuration(380).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        setPinMode(!pinSet() ? "set" : change ? "old" : "check");
        firstPinKey.requestFocus();
    }

    /** Closes the PIN pad and brings Parent settings back, with the highlight on the row it was opened from. */
    private void backToParent(int row) {
        if (!parentOpen) { closePanels(); return; } // never brings the settings back without the PIN
        pinOverParent = false;
        ui.removeCallbacks(pinWaitTick);
        ui.removeCallbacks(pinNoteEnd);
        pinPanel.animate().alpha(0).setDuration(180).withEndAction(() -> pinPanel.setVisibility(View.GONE)).start();
        parentPanel.animate().alpha(1).setDuration(240).start();
        focusLayer(parentPanel);
        View f = parentList.getChildAt(Math.min(row, parentList.getChildCount() - 1));
        if (f != null) f.requestFocus();
        ui.removeCallbacks(parentIdle);
        ui.postDelayed(parentIdle, PARENT_IDLE_MS);
    }

    private void setPinMode(String m) {
        pinMode = m;
        pinEntry = "";
        pinSub.setTextColor(DIM);
        switch (m) {
            case "old": pinTitle.setText("Change the PIN"); pinSub.setText("Enter the current PIN first"); break;
            case "set": pinTitle.setText("Choose a parent PIN"); pinSub.setText("4 digits to open YouTube.\nChange it later in Parent settings."); break;
            case "confirm": pinTitle.setText("Choose a parent PIN"); pinSub.setText("Enter the same 4 digits again"); break;
            default: pinTitle.setText("YouTube is locked"); pinSub.setText("Enter the parent PIN");
        }
        ui.removeCallbacks(pinNoteEnd);
        pinLine = null;
        if (!m.equals("set") && !m.equals("confirm")) showPinWait(); else restKeys(false);
        drawDots();
    }

    /** While the pad rests after too many wrong tries its number keys dim (still reachable; the countdown says why). */
    private final List<View> pinKeys = new ArrayList<>();
    /** While the pad rests only the digits dim, so the focused key keeps its full gold ring. Only redrawn on a change. */
    private boolean pinResting;
    private void restKeys(boolean resting) {
        if (resting == pinResting) return;
        pinResting = resting;
        for (View k : pinKeys) ((TextView) k).setTextColor(resting ? 0x66FFFFFF : WHITE);
    }

    /** A short message under the title for two seconds (wrong PIN, PINs that didn't match), then the usual line again. */
    private CharSequence pinLine;
    private void pinNote(String msg) {
        if (PinLock.waitLeft(prefs) > 0) return; // the rest countdown says more
        if (pinLine == null) pinLine = pinSub.getText();
        pinSub.setText(msg);
        pinSub.setTextColor(SOFT); // brighter than the usual line, so the message is read
        ui.removeCallbacks(pinNoteEnd);
        ui.postDelayed(pinNoteEnd, 2000);
    }
    private final Runnable pinNoteEnd = () -> {
        CharSequence line = pinLine;
        pinLine = null;
        pinSub.setTextColor(DIM);
        if (line != null && pinPanel.getVisibility() == View.VISIBLE && PinLock.waitLeft(prefs) <= 0) pinSub.setText(line);
    };

    private int dotColor = GOLD;

    private void drawDots() {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            boolean f = i < pinEntry.length();
            d.setColor(f ? dotColor : 0x14FFFFFF);
            d.setStroke(2, f ? dotColor : 0x80FFFFFF);
            pinDots[i].setElevation(f ? 10 : 0);
            pinDots[i].setOutlineAmbientShadowColor(dotColor);
            pinDots[i].setOutlineSpotShadowColor(dotColor);
            boolean justFilled = f && i == pinEntry.length() - 1 && pinDots[i].getTag() == null;
            pinDots[i].setBackground(d);
            pinDots[i].setTag(f ? Boolean.TRUE : null);
            if (justFilled) {
                pinDots[i].setScaleX(1.5f); pinDots[i].setScaleY(1.5f);
                pinDots[i].animate().scaleX(1).scaleY(1).setDuration(220).setInterpolator(EASE).start();
            }
        }
    }

    private void flashDots(int color, Runnable after) {
        dotColor = color;
        drawDots();
        ui.postDelayed(() -> { dotColor = GOLD; if (after != null) after.run(); }, 380);
    }

    private void pinPress(String k) {
        if (k.equals("×")) { if (pinOverParent) backToParent(1); else closePanels(); return; }
        if (k.equals("⌫")) { if (!pinEntry.isEmpty()) pinEntry = pinEntry.substring(0, pinEntry.length() - 1); drawDots(); return; }
        if (pinEntry.length() >= 4) return;
        boolean checking = pinMode.equals("check") || pinMode.equals("old");
        // resting: the key's soft dip is the only answer (no shake, no red), and the countdown stays
        if (checking && PinLock.waitLeft(prefs) > 0) { showPinWait(); return; }
        pinEntry += k;
        drawDots();
        if (pinEntry.length() < 4) return;
        switch (pinMode) {
            case "check": if (PinLock.check(prefs, pinEntry)) pinDone(); else { pinWrong(); showPinWait(); pinNote(WRONG_PIN); } break;
            case "old": if (PinLock.check(prefs, pinEntry)) ui.postDelayed(() -> setPinMode("set"), 150); else { pinWrong(); showPinWait(); pinNote(WRONG_PIN); } break;
            case "set": newPin = pinEntry; ui.postDelayed(() -> setPinMode("confirm"), 150); break;
            case "confirm":
                if (pinEntry.equals(newPin)) { PinLock.set(prefs, newPin); toast("PIN saved"); pinDone(); }
                else { pinWrong(); ui.postDelayed(() -> { setPinMode("set"); pinNote("The two PINs were different.\nChoose one again."); }, 400); }
                break;
        }
    }

    static final String WRONG_PIN = "That PIN didn't match. Try again.";

    /** After too many wrong tries, the line under the title says how long the pad rests. */
    private void showPinWait() {
        long left = PinLock.waitLeft(prefs);
        restKeys(left > 0);
        if (left <= 0) return;
        ui.removeCallbacks(pinNoteEnd);
        pinLine = null;
        pinSub.setTextColor(DIM);
        pinSub.setText(PinLock.waitText(left));
        ui.removeCallbacks(pinWaitTick);
        ui.postDelayed(pinWaitTick, Math.min(left, 1000));
    }

    private final Runnable pinWaitTick = () -> {
        if (pinPanel == null || pinPanel.getVisibility() != View.VISIBLE) return;
        if (PinLock.waitLeft(prefs) > 0) showPinWait(); else { restKeys(false); pinSub.setText(pinMode.equals("old") ? "Enter the current PIN first" : "Enter the parent PIN"); }
    };

    private ValueAnimator pinShake;

    private void pinWrong() {
        if (pinShake != null) pinShake.cancel(); // one shake at a time, however fast keys are pressed
        ValueAnimator a = pinShake = ValueAnimator.ofFloat(0, -18, 16, -12, 8, -4, 0);
        a.setDuration(420);
        a.addUpdateListener(v -> pinBox.setTranslationX((float) v.getAnimatedValue()));
        a.start();
        flashDots(0xFFFF6B6B, () -> { pinEntry = ""; drawDots(); });
    }

    private void pinDone() {
        flashDots(0xFF5BE38A, () -> {
            Runnable then = pinThen;
            pinThen = null;
            if (pinOverParent) { backToParent(1); return; } // the PIN was changed from Parent settings: back to them
            closePanels();
            if (then != null) { ui.postDelayed(then, 260); return; }
            if (pinLaunchAfter && !launchAny(DOCK[dockIndex("yt")].pkgs, "YouTube")) toast("YouTube isn't installed.");
        });
    }

    // ================================================================== All apps / Kids

    private void buildApps() {
        appsPanel = overlay();
        calendarPanel = overlay();
        panelHeader(appsPanel, "ALL APPS  ·  كل التطبيقات", "Apps");
        appsTitle = panelHint(appsPanel, "Hold OK for options: home row or show first ⭐");
        ScrollView sv = new ScrollView(this);
        clipToBounds(sv); // overlays don't clip their children (for the edge bleed), so a scroller must clip itself
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(30, 20, 30, 60); // 30 px each side inside the clip for a focused tile's gold edge and glow
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(7);
        appGrid.setClipChildren(false);
        sv.addView(appGrid);
        sv.setVerticalFadingEdgeEnabled(true);
        sv.setFadingEdgeLength(90);
        appsPanel.addView(sv, at(1782, 1080 - 230, Gravity.TOP | Gravity.START, 69, 230)); // tiles at 109..1811, level with the header
    }

    private void refreshApps() {
        final PackageManager pm = getPackageManager();
        runBg(() -> {
            List<AppEntry> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            seen.add(getPackageName());
            for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                    String pkg = r.activityInfo.packageName;
                    if (pkg.equals("com.android.tv.settings") || !seen.add(pkg)) continue; // our own Settings tile opens it
                    AppEntry a = new AppEntry();
                    a.pkg = pkg;
                    a.label = r.loadLabel(pm).toString();
                    a.icon = cachedIcon(pm, "a:" + pkg, pkg, () -> r.loadIcon(pm)); // square, edge to edge, ready for the rounded tile
                    out.add(a);
                }
            }
            Collections.sort(out, (x, y) -> x.label.compareToIgnoreCase(y.label));
            AppEntry settings = new AppEntry();
            settings.pkg = SETTINGS;
            settings.label = "Settings";
            settings.icon = appIcon(pm, "com.android.tv.settings");
            out.add(0, settings);
            AppEntry cal = new AppEntry();
            cal.pkg = CALENDAR;
            cal.label = "Islamic calendar";
            cal.icon = getDrawable(R.drawable.ic_calendar);
            out.add(0, cal);
            appCache = out;
        });
    }

    private Set<String> kidPicks() { return new HashSet<>(prefs.getStringSet("kids", new HashSet<>())); }

    private void openApps(String mode) {
        appsMode = mode;
        List<AppEntry> all = appCache;
        if (all == null) { refreshApps(); ui.postDelayed(() -> openApps(mode), 600); return; }
        Set<String> kids = kidPicks();
        appsTitle.setText("Hold OK for options: home row or show first ⭐");
        // Apps chosen to show first come first, marked with a star.
        List<AppEntry> shown = new ArrayList<>();
        for (AppEntry a : all) if (!HIDDEN_FROM_LIST.contains(a.pkg) && kids.contains(a.pkg)) shown.add(a);
        for (AppEntry a : all) if (!HIDDEN_FROM_LIST.contains(a.pkg) && !kids.contains(a.pkg)) shown.add(a);
        StringBuilder key = new StringBuilder();
        for (AppEntry a : shown) {
            Object[] ic = ICONS.get("a:" + a.pkg);
            key.append(a.pkg).append('|').append(a.label).append('|').append(kids.contains(a.pkg)).append('|').append(ic == null ? 0 : ic[0]).append(';');
        }
        if (key.toString().equals(gridKey) && appGrid.getChildCount() == shown.size()) {
            // nothing changed since the last time: keep the tiles and only play the row-by-row fade again
            for (int i = 0; i < appGrid.getChildCount(); i++) fadeTileIn(appGrid.getChildAt(i), i);
            show(appsPanel);
            if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
            return;
        }
        gridKey = key.toString();
        appGrid.removeAllViews();
        View first = null;
        int index = 0;
        for (AppEntry a : shown) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(12, 18, 12, 12);
            item.setFocusable(true);
            item.setClickable(true);
            item.setClipChildren(false);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(a.icon);
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            icon.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 30f * v.getWidth() / TILE); }
            });
            // the bitmap is already rounded (fullBleed): the outline only shapes the focus shadow, with no clip
            // and no resting shadow, so the grid draws no masks and no shadows until a tile is focused
            item.addView(icon, new LinearLayout.LayoutParams(124, 124));
            TextView label = text((kids.contains(a.pkg) ? "⭐ " : "") + a.label, 24, fMed, SOFT);
            label.setGravity(Gravity.CENTER);
            label.setLineSpacing(0, 1.05f);
            label.setPadding(0, 14, 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));
            item.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? focusGlass(30) : null);
                label.setTextColor(has ? WHITE : SOFT);
                v.animate().setStartDelay(0).scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).setDuration(200).setInterpolator(EASE).start(); // the row fade-in delay must not hold back focus
                goldLift(v, has, 22);
            });
            fadeTileIn(item, index);
            index++;
            item.setOnClickListener(v -> {
                if (a.pkg.equals(SETTINGS)) openSettings();
                else if (a.pkg.equals(CALENDAR)) openCalendar();
                else if (!launch(a.pkg)) toast("Couldn't open " + a.label);
            });
            if (!a.pkg.equals(SETTINGS) && !a.pkg.equals(CALENDAR)) item.setOnLongClickListener(v -> { appMenu(a, v); return true; });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 226; p.height = 236; // seven columns fill the row edge to edge under the header
            p.setMargins(10, 10, 10, 10);
            appGrid.addView(item, p);
            if (first == null) first = item;
            if (a.pkg.equals(CALENDAR)) calendarItem = item;
        }
        show(appsPanel);
        if (first != null) first.requestFocus();
    }

    /** The apps the grid was last built from; the same list opens with the same tiles. */
    private String gridKey;

    /** The grid fades in row by row. */
    private static void fadeTileIn(View item, int index) {
        item.animate().cancel();
        item.setAlpha(0);
        item.setTranslationY(18);
        item.animate().alpha(1).translationY(0).setStartDelay(Math.min(index, 28) / 7 * 60L + 80).setDuration(360).setInterpolator(EASE).start();
    }

    private FrameLayout appMenuBox;
    private View appMenuFrom, calendarItem;

    /** Hold OK on an app in All apps: put it on (or take it off) the home row, or the Kids Corner. */
    private void appMenu(AppEntry a, View from) {
        if (appMenuBox != null) appsPanel.removeView(appMenuBox);
        appMenuFrom = from;
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(a.label, 34, fSemi, WHITE);
        title.setPadding(10, 0, 10, 16);
        col.addView(title);
        boolean home = onDock(a.pkg), kid = kidPicks().contains(a.pkg);
        TextView first = menuButton(col, home ? "Remove from the home row" : "Add to the home row", () -> {
            closeAppMenu(from);
            if (home) removeFromDock(a.pkg); else addToDock(a.pkg, a.label);
        });
        // the star only moves an app to the front of this list (Kids Corner keeps its own list, behind its PIN)
        menuButton(col, kid ? "Stop showing it first" : "Show it first in All apps ⭐", () -> { closeAppMenu(from); toggleKid(a.pkg); });
        menuButton(col, "Cancel", () -> closeAppMenu(from));
        appMenuBox = new TrapFrame(this); // the arrows stay in the menu, never on the apps behind it
        appMenuBox.setBackgroundColor(0x8C000000);
        appMenuBox.setClickable(true);
        col.setPadding(40, 30, 40, 30); // room inside the card's rounded clip for a focused button (1.03 larger)
        panelCard(appMenuBox, col, 640, -1); // the same card family as every other panel card
        appsPanel.addView(appMenuBox, bleed());
        appMenuBox.setAlpha(0);
        appMenuBox.animate().alpha(1).setDuration(200).setInterpolator(EASE).start();
        first.requestFocus();
    }

    private void closeAppMenu(View back) {
        if (appMenuBox == null) return;
        View box = appMenuBox;
        appMenuBox = null;
        if (back != null) back.requestFocus(); // first, so the highlight never passes through anything else
        box.setFocusable(false);
        if (box instanceof ViewGroup) ((ViewGroup) box).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        box.animate().alpha(0).setDuration(180).setInterpolator(EXIT).withEndAction(() -> appsPanel.removeView(box)).start();
    }

    private TextView menuButton(LinearLayout col, String label, Runnable run) {
        TextView b = text(label, 28, fMed, SOFT);
        b.setPadding(30, 18, 30, 18);
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? focusGlass(22) : null);
            b.setTextColor(has ? WHITE : SOFT);
            v.animate().setStartDelay(0).scaleX(has ? 1.03f : 1f).scaleY(has ? 1.03f : 1f).setDuration(180).setInterpolator(EASE).start(); // like a parent row
        });
        b.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = 6;
        col.addView(b, p);
        return b;
    }

    private void toggleKid(String pkg) {
        Set<String> kids = kidPicks();
        boolean added = kids.add(pkg);
        if (!added) kids.remove(pkg);
        prefs.edit().putStringSet("kids", kids).apply();
        appsTitle.setText(added ? "Now shown first ⭐" : "No longer shown first");
        ui.postDelayed(() -> openApps(appsMode), 700);
    }

    static final String SETTINGS = "__settings", CALENDAR = "__calendar";

    private void openSettings() {
        Intent i = new Intent(Settings.ACTION_SETTINGS);
        // always its first page: a child bounced back by Kids Corner can't carry on, trip by trip, where they left off
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    // ================================================================== Quran player

    private ExoPlayer quran;
    private int quranSurah, quranErrors;
    private Quran.Reciter quranReciter;
    private TextView quranNow, quranPlayBtn, nowSurah, nowReciter;
    private View nowWidget;
    private LinearLayout nowRow;

    /** Layout 1: the next adhan, the five prayer times and (while it plays) the recitation in one slim glass strip under
     *  the clock. Built from the adhan card's own parts, which keep updating as before. */
    private void assembleStrip() {
        View oldCard = topParts[1];
        LinearLayout strip = new LinearLayout(this);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(34, 18, 34, 18);
        LinearLayout colA = new LinearLayout(this);
        colA.setOrientation(LinearLayout.VERTICAL);
        LinearLayout adhanCard = (LinearLayout) adhanBar.getParent();
        View kick = adhanCard.getChildAt(0), nameRow = adhanCard.getChildAt(1);
        adhanCard.removeView(kick); // "Fajr  1:07:07" says it on its own; the strip stays slim
        moveTo(nameRow, colA, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, -2);
        nlp.rightMargin = 36; // names, then the countdown, sized to their text
        ((View) adhanName.getParent()).setLayoutParams(nlp);
        adhanName.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        adhanName.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 3);
        bp.topMargin = 8;
        moveTo(adhanBar, colA, bp);
        strip.addView(colA, new LinearLayout.LayoutParams(-2, -2));
        strip.addView(stripDivider());
        View times = prayerCells[0] == null ? null : (View) prayerCells[0].getParent();
        moveTo(times, strip, new LinearLayout.LayoutParams(520, -2));
        LinearLayout nowPart = new LinearLayout(this);
        nowPart.setGravity(Gravity.CENTER_VERTICAL);
        nowPart.addView(stripDivider());
        nowRow.setPadding(6, 0, 4, 0);
        moveTo(nowRow, nowPart, new LinearLayout.LayoutParams(-2, -2));
        strip.addView(nowPart, new LinearLayout.LayoutParams(-2, -2));
        nowWidget = nowPart;
        nowPart.setAlpha(0);
        nowPart.setVisibility(View.GONE);
        oldCard.setVisibility(View.GONE);
        // a fixed height: it only grows sideways (when a recitation joins it, or for a long name), never taller
        // slim: the adhan's two names side by side, prayer columns of name and time, no recitation kicker
        LinearLayout names = (LinearLayout) adhanName.getParent();
        names.setOrientation(LinearLayout.HORIZONTAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        adhanAr.setPadding(18, 0, 0, 0);
        adhanCountdown.setTextSize(TypedValue.COMPLEX_UNIT_PX, 44);
        adhanAr.setTextSize(TypedValue.COMPLEX_UNIT_PX, 26);
        for (TextView[] p : prayerTexts) p[1].setVisibility(View.GONE);
        ((ViewGroup) nowSurah.getParent()).getChildAt(0).setVisibility(View.GONE);
        shadowAll(strip);
        // no box: just the row, wide, over the soft dark fade at the bottom of the screen
        strip.setPadding(0, 0, 0, 0);
        stage.addView(strip, at(-2, STRIP_H, Gravity.TOP | Gravity.END, RIGHT_EDGE, STRIP_Y));
        strip.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        // no lines in the strip: spacing alone separates its parts
        adhanBar.setVisibility(View.GONE);
        nowBar.setVisibility(View.GONE);
        // the recitation row was built hidden (it used to be its own card); the part around it now shows and hides it
        nowRow.setVisibility(View.VISIBLE);
        nowRow.setAlpha(1);
        topParts[1] = strip;
    }

    private static void shadowAll(View v) {
        if (v instanceof TextView) ((TextView) v).setShadowLayer(TEXT_SHADOW, 0, 2, 0xB3000000);
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) shadowAll(((ViewGroup) v).getChildAt(i));
    }

    private View stripDivider() {
        View d = new View(this);
        d.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 70);
        p.leftMargin = 20; p.rightMargin = 20; // just a gap, no visible line
        d.setLayoutParams(p);
        return d;
    }
    private LinearLayout quranControls;
    private GridLayout surahGrid;

    /** Surah tiles: deep navy glass with a faint gold edge, so the star pattern shows in the gaps, not through the names. */
    static final int SURAH_FILL = 0xB30D1428, SURAH_EDGE = 0x33F1D58A;

    static final int SURAH_STEP = 164, QNOW_W = 640;
    private TextView quranNowReciter;

    private void buildQuran() {
        quranPanel = overlay();
        quranPanel.getChildAt(0).setVisibility(View.GONE); // the star field is opaque: the overlay scrim would never be seen
        StarField field = new StarField(this);
        field.setTag("bg");
        quranPanel.addView(field, bleed());
        TextView title = panelHeader(quranPanel, "THE HOLY QURAN  ·  A NEW RECITER EVERY TIME", "القرآن الكريم");
        title.setTypeface(fQuran);
        title.setIncludeFontPadding(true);
        title.setTextDirection(View.TEXT_DIRECTION_LTR); // keep the Arabic title on the left, under the kicker

        quranControls = new LinearLayout(this);
        quranControls.setGravity(Gravity.CENTER_VERTICAL);
        // what is playing, large and centred in the open space between the title and the buttons
        LinearLayout nowBox = new LinearLayout(this);
        nowBox.setOrientation(LinearLayout.VERTICAL);
        nowBox.setGravity(Gravity.CENTER_HORIZONTAL);
        quranNow = text("", 40, fQuran, WHITE); // Amiri, as everywhere a surah name is shown
        quranNow.setIncludeFontPadding(true);
        quranNow.setGravity(Gravity.CENTER);
        quranNowReciter = text("", 30, fAr, SOFT);
        quranNowReciter.setIncludeFontPadding(true);
        quranNowReciter.setGravity(Gravity.CENTER);
        quranNowReciter.setLineSpacing(0, 1.05f);
        nowBox.addView(quranNow, new LinearLayout.LayoutParams(-1, -2));
        nowBox.addView(quranNowReciter, new LinearLayout.LayoutParams(-1, -2));
        quranPanel.addView(nowBox, at(QNOW_W, -2, Gravity.TOP | Gravity.START, 430, 96));
        quranControls.addView(new View(this), new LinearLayout.LayoutParams(0, 0)); // keeps the buttons from index 1
        quranPlayBtn = quranButton("Pause", () -> {
            if (quran == null) return;
            quran.setPlayWhenReady(!quran.getPlayWhenReady());
            drawQuranState();
        });
        reciterBtn = quranButton("", this::openReciters);
        drawReciterBtn();
        quranButton("Another reciter", () -> playSurah(quranSurah, true));
        quranButton("Stop", this::stopQuran);
        quranPanel.addView(quranControls, at(-2, -2, Gravity.TOP | Gravity.END, 110, 80));

        // Rows always stop whole at the top edge (no half-cut row under the header): every scroll is a whole number of rows.
        ScrollView sv = new ScrollView(this) {
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) {
                int d = super.computeScrollDeltaToGetChildRectOnScreen(r);
                if (d == 0) return 0;
                float rows = (getScrollY() + d) / (float) SURAH_STEP;
                int to = (d > 0 ? (int) Math.ceil(rows - 0.01f) : (int) Math.floor(rows + 0.01f)) * SURAH_STEP;
                int max = Math.max(0, getChildAt(0).getHeight() + getPaddingTop() + getPaddingBottom() - getHeight());
                return Math.max(0, Math.min(to, max)) - getScrollY();
            }
        };
        clipToBounds(sv); // overlays don't clip their children (for the edge bleed), so a scroller must clip itself
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(30, 8, 30, 2); // 30 px each side for the focused tile's edge; 8 + 5 rows of 164 + 2 = 830, the scroller's height: five whole rows, always
        surahGrid = new GridLayout(this);
        surahGrid.setColumnCount(6);
        surahGrid.setClipChildren(false);
        for (int i = 0; i < 114; i++) {
            final int n = i + 1;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(22, 2, 22, 6); // the tile is 146 tall: keep the English name clear of the bottom edge
            cell.setFocusable(true);
            cell.setClickable(true);
            cell.setBackground(glass(24, SURAH_FILL, SURAH_EDGE));
            // a small gold medallion with the surah number, the name in Amiri, then the English name
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView num = text(String.valueOf(n), 22, fSemi, 0xFFF1D58A);
            num.setFontFeatureSettings("tnum");
            num.setGravity(Gravity.CENTER);
            GradientDrawable medal = new GradientDrawable();
            medal.setShape(GradientDrawable.OVAL);
            medal.setColor(0x1AF1D58A);
            medal.setStroke(2, 0x99F1D58A);
            num.setBackground(medal);
            top.addView(num, new LinearLayout.LayoutParams(46, 46));
            TextView ar = text(Quran.AR[i], 36, fQuran, WHITE);
            ar.setIncludeFontPadding(true);
            ar.setPadding(14, 0, 0, 0);
            top.addView(ar);
            TextView en = text(Quran.EN[i], 22, fMed, DIM);
            en.setPadding(60, 2, 0, 0); // under the name, past the medallion
            cell.addView(top);
            cell.addView(en);
            cell.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? focusGlass(24) : glass(24, SURAH_FILL, SURAH_EDGE));
                en.setTextColor(has ? SOFT : DIM);
                v.animate().scaleX(has ? 1.07f : 1f).scaleY(has ? 1.07f : 1f).setDuration(200).setInterpolator(EASE).start();
                goldLift(v, has, 18);
            });
            cell.setOnClickListener(v -> { quranQuiet = false; playSurah(n, false); openPlayer(); });
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 268; gp.height = SURAH_STEP - 18;
            gp.setMargins(9, 9, 9, 9);
            surahGrid.addView(cell, gp);
        }
        sv.addView(surahGrid);
        quranPanel.addView(sv, at(1780, 1080 - 250, Gravity.TOP | Gravity.START, 70, 250));

        // On the home screen: what is playing, at the bottom of the weather card.
        buildNowReciting();
        assembleStrip();
        drawQuranState();
    }

    private TextView quranButton(String label, Runnable onClick) {
        TextView b = text(label, 24, fSemi, WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(28, 14, 28, 16);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackground(glass(99, 0x1AFFFFFF, GLASS_EDGE));
        b.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? focusGlass(99) : glass(99, 0x1AFFFFFF, GLASS_EDGE));
            v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(180).setInterpolator(EASE).start();
            goldLift(v, has, 14);
        });
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.leftMargin = 14;
        quranControls.addView(b, p);
        return b;
    }

    // ------------------------------------------------------------------ choosing a reciter

    private TextView reciterBtn;
    private FrameLayout reciterPanel;

    private void drawReciterBtn() {
        reciterBtn.setText("Choose reciter"); // the chosen name already shows large in the now-playing lines; a long one here ran into them
    }

    /** A list of every reciter: "Any" (a new voice every time) first, then the ones stored on the box, then the rest. */
    private void openReciters() {
        // the list comes from the internet the first time each week: if it takes a moment, open at once with
        // "Any reciter" and a loading line, so the press never looks dead
        final boolean[] done = {false}, waiting = {false};
        ui.postDelayed(() -> { if (!done[0] && quranPanel.getVisibility() == View.VISIBLE) { waiting[0] = true; showReciters(null, null); } }, 300);
        runBg(() -> {
            List<Quran.Reciter> onBox = Quran.local(1), all = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Quran.Reciter r : onBox) if (seen.add(r.en)) all.add(r);
            for (Quran.Reciter r : Quran.reciters(getCacheDir())) if (seen.add(r.en)) all.add(r);
            Set<String> local = new HashSet<>();
            for (Quran.Reciter r : onBox) local.add(r.en);
            ui.post(() -> {
                done[0] = true;
                // fill the list in, unless the family already picked "Any reciter" or pressed Back while it loaded
                if (waiting[0] ? recitersOpen : quranPanel.getVisibility() == View.VISIBLE) showReciters(all, local);
            });
        });
    }

    private void showReciters(List<Quran.Reciter> all, Set<String> local) {
        if (reciterPanel == null) { reciterPanel = new TrapFrame(this); reciterPanel.setElevation(40); stage.addView(reciterPanel, bleed()); }
        boolean wasShown = recitersOpen;
        recitersOpen = true;
        focusLayer(reciterPanel);
        placePill(PILL_PANEL);
        reciterPanel.removeAllViews();
        reciterPanel.setBackgroundColor(0xB3050810);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setClipChildren(false);
        String chosen = prefs.getString("reciter", "");
        View focus = reciterTile(grid, "أي قارئ", "Any reciter", "A new voice every time", "");
        View any = focus;
        if (all == null) {
            TextView wait = text("Loading reciters…  ·  جارٍ التحميل", 26, fMed, SOFT);
            wait.setIncludeFontPadding(true);
            wait.setPadding(40, 40, 40, 0);
            GridLayout.LayoutParams wl = new GridLayout.LayoutParams();
            wl.columnSpec = GridLayout.spec(0, 2);
            grid.addView(wait, wl);
            all = new ArrayList<>();
            local = new HashSet<>();
        }
        for (Quran.Reciter r : all) {
            View t = reciterTile(grid, r.ar, r.en, local.contains(r.en) ? "●  Saved on the box" : "", r.en);
            if (r.en.equals(chosen)) focus = t;
        }
        if (chosen.isEmpty()) focus = any;
        listCard(reciterPanel, "RECITER  ·  القارئ", "Choose a reciter", grid, 1500, 940);
        reciterPanel.setVisibility(View.VISIBLE);
        noAlphaLayers(reciterPanel);
        if (!wasShown) { reciterPanel.setAlpha(0); reciterPanel.animate().alpha(1).setDuration(240).setInterpolator(EASE).start(); }
        View f = focus;
        f.post(f::requestFocus);
    }

    static final int RECITER_TILE_W = 674; // two columns centred in the card

    /** One reciter: the Arabic name large, the English name under it, and a gold note when it is saved on the box. */
    private View reciterTile(GridLayout grid, String ar, String en, String note, String value) {
        boolean on = value.equals(prefs.getString("reciter", ""));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(30, 16, 30, 18);
        t.setFocusable(true);
        t.setClickable(true);
        Drawable rest = on ? goldEdge() : glass(22, 0x0FFFFFFF, 0x24FFFFFF);
        t.setBackground(rest);
        TextView big = text(ar.isEmpty() ? en : ar, 32, ar.isEmpty() ? fSemi : fAr, on ? GOLD : WHITE);
        big.setIncludeFontPadding(true);
        t.addView(big);
        if (!ar.isEmpty()) {
            TextView small = text((on ? "✓  " : "") + en, 24, fMed, on ? GOLD : SOFT);
            small.setPadding(0, 2, 0, 0);
            t.addView(small);
        }
        if (!note.isEmpty()) {
            TextView n = text(note, 22, fMed, 0xCCF1D58A);
            n.setPadding(0, 6, 0, 0);
            t.addView(n);
        }
        t.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? focusGlass(22) : rest);
            v.animate().scaleX(has ? 1.03f : 1f).scaleY(has ? 1.03f : 1f).setDuration(180).setInterpolator(EASE).start();
        });
        t.setOnClickListener(v -> {
            prefs.edit().putString("reciter", value).apply();
            drawReciterBtn();
            closeReciters();
            if (quranSurah > 0 && !quranQuiet) playSurah(quranSurah, false); // hear the new voice straight away
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = RECITER_TILE_W;
        lp.height = 160;
        lp.setMargins(10, 10, 10, 10);
        grid.addView(t, lp);
        return t;
    }

    /** The chosen item at rest: a quiet glass with a thin gold edge. */
    private static GradientDrawable goldEdge() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x24F1D58A, 0x0AF1D58A});
        d.setCornerRadius(22);
        d.setStroke(2, 0x99F1D58A);
        return d;
    }

    private boolean recitersOpen;

    private void closeReciters() {
        recitersOpen = false;
        focusLayer(quranPanel);
        // first, so the highlight never passes through anything else; if the button is hidden (the surah ended), the surah cell
        if (reciterBtn.getVisibility() == View.VISIBLE) reciterBtn.requestFocus();
        else surahGrid.getChildAt((quranSurah > 0 ? quranSurah : Math.max(1, playerLast)) - 1).requestFocus();
        reciterPanel.animate().alpha(0).setDuration(180).setInterpolator(EXIT).withEndAction(() -> reciterPanel.setVisibility(View.GONE)).start();
        placePill(playerPanel != null && playerPanel.getVisibility() == View.VISIBLE ? PILL_PLAYER : PILL_PANEL);
    }

    private void openQuran() {
        show(quranPanel);
        int focus = quranSurah > 0 ? quranSurah - 1 : 0;
        ui.post(() -> surahGrid.getChildAt(focus).requestFocus());
    }

    /** Plays a surah from a random reciter (never Mishary Alafasy). anotherReciter keeps the surah and changes the voice. */
    /** While the home screen is in front, a random surah from a random reciter plays softly (parent menu can turn it off). */
    static final float QUIET_VOLUME = 0.22f;
    private boolean quranQuiet, quietStopped;
    private final Runnable startQuiet = this::backgroundQuran;

    private void backgroundQuran() {
        if (!resumed || quietStopped || !prefs.getBoolean("quiet_quran", true)) return;
        if (quran != null && quranSurah > 0) {
            if (quranQuiet && !quran.getPlayWhenReady()) { fadeInQuiet(); quran.play(); } // carry on where it paused
            return;
        }
        quranQuiet = true;
        Calendar fr = Calendar.getInstance(KUWAIT);
        int frMin = fr.get(Calendar.HOUR_OF_DAY) * 60 + fr.get(Calendar.MINUTE);
        String frDay = fr.get(Calendar.YEAR) + "-" + fr.get(Calendar.DAY_OF_YEAR);
        if (today != null && fr.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && frMin >= today[0] && frMin < today[2]
                && !frDay.equals(prefs.getString("kahf_day", ""))) {
            // Jumu'ah morning: Surat Al-Kahf once, softly
            prefs.edit().putString("kahf_day", frDay).apply();
            taraweeh = false;
            playSurah(18, false);
            return;
        }
        if (taraweehTime()) {
            // Ramadan nights: the Quran in order, carrying on each night where it stopped, from the imams of the Haramain when stored
            int n = prefs.getInt("khatma_next", 1);
            prefs.edit().putInt("khatma_next", n % 114 + 1).apply();
            taraweeh = true;
            playSurah(n, false);
            return;
        }
        taraweeh = false;
        playSurah(1 + random.nextInt(114), false);
    }

    private boolean taraweeh;
    private ValueAnimator quietFade;
    static final long QUIET_FADE_MS = 45_000;

    /** The soft Quran starts silent and rises very slowly to its low volume. */
    private void fadeInQuiet() {
        if (quietFade != null) quietFade.cancel();
        quran.setVolume(0f);
        quietFade = ValueAnimator.ofFloat(0f, QUIET_VOLUME);
        quietFade.setDuration(QUIET_FADE_MS);
        quietFade.setInterpolator(new android.view.animation.AccelerateInterpolator(1.4f)); // gentlest at the start
        quietFade.addUpdateListener(v -> { if (quran != null && quranQuiet) quran.setVolume((float) v.getAnimatedValue()); });
        quietFade.start();
    }
    static final java.util.regex.Pattern IMAMS = java.util.regex.Pattern.compile("(?i)suda[iy]?s|sudaes|shura[iy]m|maher|qatami|سديس|شريم|المعيقلي|القطامي");

    /** Ramadan, between Isha and Fajr. */
    private boolean taraweehTime() {
        Calendar now = Calendar.getInstance(KUWAIT);
        if (!Events.isRamadan(now) || today == null) return false;
        int min = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return min >= today[5] || min < today[0];
    }

    private void playSurah(int n, boolean anotherReciter) {
        if (n <= 0) return;
        if (!anotherReciter) quranErrors = 0;
        final Quran.Reciter not = anotherReciter ? quranReciter : null;
        quranNow.setText("Loading… · جارٍ التحميل");
        quranNowReciter.setText("");
        runBg(() -> {
            // Recitations stored on the box play first (no internet needed); otherwise stream one.
            Quran.cacheDir = getCacheDir();
            List<Quran.Reciter> all = Quran.local(n);
            if (all.isEmpty() || (all.size() == 1 && not != null && all.get(0).server.equals(not.server))) all = Quran.reciters(getCacheDir());
            if (taraweeh && quranQuiet) {
                List<Quran.Reciter> imams = new ArrayList<>();
                for (Quran.Reciter x : all) if (IMAMS.matcher(x.en + " " + x.ar).find() && x.has(n)) imams.add(x);
                if (!imams.isEmpty() && (not == null || imams.size() > 1)) all = imams;
            }
            Quran.Reciter r = null;
            String chosen = prefs.getString("reciter", "");
            if (!quranQuiet && not == null && !chosen.isEmpty()) {
                for (Quran.Reciter x : Quran.local(n)) if (x.en.equals(chosen)) r = x; // stored on the box first
                if (r == null) for (Quran.Reciter x : Quran.reciters(getCacheDir())) if (x.en.equals(chosen) && x.has(n)) r = x;
            }
            if (r == null) r = Quran.pick(all, n, not, random);
            final Quran.Reciter use = r;
            ui.post(() -> {
                // the bg queue can hold this for many seconds: if the family opened Netflix meanwhile, the soft Quran
                // must not start behind it (it would take the audio and pause the film)
                if (quranQuiet && !resumed) { quranSurah = 0; drawQuranState(); return; }
                startQuran(n, use);
            });
        });
    }

    private void startQuran(int n, Quran.Reciter r) {
        if (quran == null) {
            quran = new ExoPlayer.Builder(this).build();
            quran.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true);
            quran.addListener(new Player.Listener() {
                @Override public void onIsPlayingChanged(boolean playing) { if (playing) quranErrors = 0; drawQuranState(); }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED && !quranQuiet && quranSurah > 0 && repeatMode() != 0) {
                        // repeat: a 3 second pause after the fade, then the same surah (or the next one) fades back in
                        final int again = repeatMode() == 2 ? quranSurah : nextSurah(quranSurah, 1);
                        ui.postDelayed(() -> {
                            if (quran == null || quranQuiet) return;
                            if (again == quranSurah) { quran.seekTo(0); quran.setVolume(0f); quran.play(); fadeLoopIn(); }
                            else { loopFadeIn = true; playSurah(again, false); }
                        }, 3000);
                        return;
                    }
                    if (state == Player.STATE_ENDED) {
                        quranSurah = 0;
                        drawQuranState();
                        if (quranQuiet) ui.postDelayed(MainActivity.this::backgroundQuran, 2000); // the next one, quietly
                    }
                }
                @Override public void onPlayerError(PlaybackException e) {
                    if (++quranErrors > 3) { if (!quranQuiet) toast("The Quran couldn't load. Check the internet."); stopQuran(); return; }
                    if (!quranQuiet) toast("That recitation didn't load. Trying another reciter…");
                    playSurah(quranSurah, true);
                }
            });
        }
        quranSurah = n;
        quranReciter = r;
        Stats.surahPlayed(this, n, r.en.isEmpty() ? r.ar : r.en);
        if (quranQuiet) fadeInQuiet();
        else if (loopFadeIn) { loopFadeIn = false; quran.setVolume(0f); ui.post(this::fadeLoopIn); }
        else { if (quietFade != null) quietFade.cancel(); quran.setVolume(1f); }
        ui.removeCallbacks(playerTick);
        ui.post(playerTick); // keeps the progress line and the repeat fade going
        quran.setMediaItem(MediaItem.fromUri(r.url(n)));
        quran.prepare();
        quran.play();
        drawQuranState();
    }

    private void stopQuran() {
        quietStopped = true; // Stop means quiet, until the next return to the home screen
        quranQuiet = false;
        if (quran != null) quran.stop();
        quranSurah = 0;
        drawQuranState();
    }

    private void drawQuranState() {
        if (quranNow == null) return;
        boolean active = quran != null && quranSurah > 0;
        // Stop (or a surah ending) hides these buttons: move the highlight to the surah that was playing first,
        // or Android would drop it on the first app of the hidden home row
        if (!active && quranControls.hasFocus()) {
            View cell = surahGrid.getChildAt(Math.max(1, playerLast) - 1);
            if (cell != null) cell.requestFocus();
        }
        for (int i = 1; i < quranControls.getChildCount(); i++) quranControls.getChildAt(i).setVisibility(active ? View.VISIBLE : View.GONE);
        if (!active) {
            quranNow.setText("");
            quranNowReciter.setText("");
            showNowReciting(false);
            drawPlayer();
            return;
        }
        fitPair(quranNow, "سورة " + Quran.AR[quranSurah - 1], Quran.EN[quranSurah - 1], QNOW_W);
        if (quranReciter.ar.isEmpty()) quranNowReciter.setText(quranReciter.en);
        else fitPair(quranNowReciter, quranReciter.ar, quranReciter.en, QNOW_W);
        quranPlayBtn.setText(quran.getPlayWhenReady() ? "Pause" : "Play");
        oneLine(nowSurah, "سورة " + Quran.AR[quranSurah - 1], Quran.EN[quranSurah - 1]);
        drawPlayer();
        if (quranReciter.ar.isEmpty()) nowReciter.setText(quranReciter.en); else oneLine(nowReciter, quranReciter.ar, quranReciter.en);
        showNowReciting(true);
        animateBars(quran.getPlayWhenReady());
    }

    /** Top centre, between the two cards: which surah is playing and who is reciting, with three softly moving bars. */
    private void buildNowReciting() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(30, 18, 34, 18);
        nowEq = new EqualizerView(this);
        row.addView(nowEq, new LinearLayout.LayoutParams(46, 56));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(20, 0, 0, 0);
        TextView k = kicker(text("NOW RECITING  ·  يُتلى الآن", 20, fSemi, DIM));
        nowSurah = text("", 26, fQuran, WHITE);
        nowSurah.setIncludeFontPadding(true);
        nowReciter = text("", 24, fAr, SOFT);
        nowReciter.setIncludeFontPadding(true);
        nowReciter.setMaxLines(1);
        nowSurah.setMaxLines(1);
        texts.addView(k);
        texts.addView(nowSurah);
        texts.addView(nowReciter);
        nowBar = new ThinBar(this);
        nowBar.setColor(0x99F1D58A);
        LinearLayout.LayoutParams nbp = new LinearLayout.LayoutParams(-1, 3);
        nbp.topMargin = 8;
        texts.addView(nowBar, nbp);
        row.addView(texts);
        nowRow = row;
        nowWidget = row;
        nowWidget.setAlpha(0);
        nowWidget.setVisibility(View.GONE);
    }

    private void showNowReciting(boolean on) {
        if (nowWidget == null) return;
        if (on && nowWidget.getVisibility() != View.VISIBLE) {
            nowWidget.setVisibility(View.VISIBLE);
            nowWidget.setTranslationY(-16);
            nowWidget.animate().alpha(1).translationY(0).setDuration(500).setInterpolator(EASE).start();
        } else if (!on && nowWidget.getVisibility() == View.VISIBLE) {
            nowWidget.animate().alpha(0).setDuration(300).withEndAction(() -> nowWidget.setVisibility(View.GONE)).start();
            animateBars(false);
        }
    }

    /** Three gold bars that rise and fall gently while a recitation plays; flat when paused. */
    private EqualizerView nowEq;

    private void animateBars(boolean playing) {
        if (nowEq != null) nowEq.setPlaying(playing);
    }

    // ================================================================== hard drive status

    private BroadcastReceiver driveWatcher;

    private void registerDriveWatcher() {
        IntentFilter f = new IntentFilter();
        for (String a : new String[]{Intent.ACTION_MEDIA_MOUNTED, Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_REMOVED,
                Intent.ACTION_MEDIA_BAD_REMOVAL, Intent.ACTION_MEDIA_EJECT}) f.addAction(a);
        f.addDataScheme("file");
        driveWatcher = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { ui.postDelayed(MainActivity.this::updateDrive, 800); }
        };
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(driveWatcher, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(driveWatcher, f);
    }

    /** A small line in the weather card: the USB drive's free space, or a warning once a known drive is unplugged. */
    private void updateDrive() {
        runBg(() -> {
            String label = null;
            try {
                StorageManager sm = getSystemService(StorageManager.class);
                for (StorageVolume v : sm.getStorageVolumes()) {
                    if (!v.isRemovable() || !Environment.MEDIA_MOUNTED.equals(v.getState())) continue;
                    File dir = Build.VERSION.SDK_INT >= 30 ? v.getDirectory() : null;
                    if (dir == null) { label = "USB drive"; break; }
                    StatFs st = new StatFs(dir.getPath());
                    label = "USB  " + Formatter.formatShortFileSize(this, st.getAvailableBytes()) + " free";
                    break;
                }
            } catch (Exception ignored) { }
            final String found = label;
            ui.post(() -> {
                if (found != null) {
                    prefs.edit().putBoolean("drive_seen", true).apply();
                    driveText.setText(dotLine("●  " + found, DIM));
                    driveText.setTextColor(SOFT);
                    driveText.setVisibility(View.VISIBLE);
                } else if (prefs.getBoolean("drive_seen", false)) {
                    driveText.setText(dotLine("●  Drive unplugged", GOLD)); // the palette's attention colour, not a red
                    driveText.setTextColor(WHITE);
                    driveText.setVisibility(View.VISIBLE);
                } else driveText.setVisibility(View.GONE);
            });
        });
    }

    // ================================================================== secret parent menu (Up Up Down Down)

    private final int[] secret = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN};
    private final int[] lastKeys = new int[4];
    private final long[] lastTimes = new long[4];

    private boolean anyPanelOpen() {
        for (View v : new View[]{pinPanel, appsPanel, quranPanel, parentPanel, calendarPanel, playerPanel}) if (v != null && v.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    private void secretKey(int code) {
        System.arraycopy(lastKeys, 1, lastKeys, 0, 3);
        System.arraycopy(lastTimes, 1, lastTimes, 0, 3);
        lastKeys[3] = code;
        lastTimes[3] = System.currentTimeMillis();
        if (lastTimes[3] - lastTimes[0] > 2500) return;
        for (int i = 0; i < 4; i++) if (lastKeys[i] != secret[i]) return;
        Arrays.fill(lastKeys, 0);
        ui.post(() -> {
            // With no PIN yet, the pad asks for one to be chosen first, so the settings are never open to anyone.
            boolean had = pinSet();
            openPin(false);
            pinLaunchAfter = false;
            pinThen = this::openParent;
            if (had) pinTitle.setText("Parent settings");
            else pinSub.setText("4 digits for Parent settings and YouTube.");
            pinIcon.setImageDrawable(lockFace());
        });
    }

    private LinearLayout parentList;

    private void buildParent() {
        parentPanel = overlay();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(56, 44, 56, 40);
        TextView k = kicker(text("PARENTS ONLY  ·  للوالدين", 20, fSemi, DIM));
        box.addView(k);
        TextView title = text("Parent settings", 46, fSemi, WHITE);
        title.setPadding(0, 10, 0, 24);
        box.addView(title);
        parentList = new LinearLayout(this);
        parentList.setOrientation(LinearLayout.VERTICAL);
        // the list scrolls (focus leads it) so every row fits on screen, fading softly under the title
        ScrollView scroll = new ScrollView(this);
        clipToBounds(scroll); // overlays don't clip their children (for the edge bleed), so a scroller must clip itself
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setVerticalFadingEdgeEnabled(true);
        scroll.setFadingEdgeLength(40);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 6, 0, 6);
        scroll.addView(parentList, new FrameLayout.LayoutParams(-1, -2));
        // 30 px of room each side inside the clip, so a focused row (1.03 larger) keeps its whole gold edge
        parentList.setPadding(30, 0, 30, 0);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, 760);
        slp.leftMargin = slp.rightMargin = -30;
        box.addView(scroll, slp);
        panelCard(parentPanel, box, 900, -1);
    }

    /**
     * Every control answers OK the moment it goes down: a small dip (x 0.96 over 70 ms) and back, the PIN key's press
     * scaled to the item. The PIN keys keep their own deeper dip (tagged "dip").
     */
    static void pressDip(KeyEvent e, View f) {
        int c = e.getKeyCode();
        if (e.getAction() != KeyEvent.ACTION_DOWN || e.getRepeatCount() != 0 || f == null || !f.isClickable() || "dip".equals(f.getTag())) return;
        if (c != KeyEvent.KEYCODE_DPAD_CENTER && c != KeyEvent.KEYCODE_ENTER && c != KeyEvent.KEYCODE_NUMPAD_ENTER) return;
        float sx = f.getScaleX(), sy = f.getScaleY();
        f.animate().setStartDelay(0).scaleX(sx * 0.96f).scaleY(sy * 0.96f).setDuration(70).setInterpolator(EASE).withEndAction(() ->
                f.animate().scaleX(sx).scaleY(sy).setDuration(140).setInterpolator(EASE).start()).start();
    }

    /** A small side-to-side nudge: "heard you, but that can't be done yet". */
    private static void nudge(View v) {
        if (v == null) return;
        ValueAnimator a = ValueAnimator.ofFloat(0, -10, 9, -5, 0);
        a.setDuration(320);
        a.addUpdateListener(x -> v.setTranslationX((float) x.getAnimatedValue()));
        a.start();
    }

    private void openParent() {
        parentOpen = true;
        show(parentPanel);
        drawParent(0);
        ui.removeCallbacks(parentIdle);
        ui.postDelayed(parentIdle, PARENT_IDLE_MS);
        View box = parentPanel.getChildAt(1); // 0 is the scrim
        box.setScaleX(0.94f); box.setScaleY(0.94f); box.setAlpha(0);
        box.animate().scaleX(1).scaleY(1).alpha(1).setDuration(340).setInterpolator(new DecelerateInterpolator(2.2f)).start();
    }

    /** The wallpaper always sits a little darker (8%) so text reads on bright scenes; the brightness setting adds to it. */
    static final float WALL_DIM = 0.08f;
    private float baseDim() { return 1 - (1 - WALL_DIM) * prefs.getInt("brightness", 100) / 100f; }

    /** The row OK was last pressed on: each setting redraws the list with the highlight back on that same row. */
    private int parentClicked;
    private boolean statsShown;

    private void drawParent(int focus) {
        statsShown = false;
        parentList.removeAllViews();
        int bright = prefs.getInt("brightness", 100);
        parentRow("This week's family stats", "›", this::drawStats);
        parentRow("Change the YouTube PIN", "›", () -> openPin(true));
        parentRow("Home row apps: back to the original set", "›", () -> { resetDock(); toast("The home row is back to its original apps."); });
        parentRow("Play a different wallpaper", "›", () -> { closePanels(); playRandomClip(); });
        parentRow("Wallpaper brightness", bright + "%", () -> {
            int next = bright == 100 ? 85 : bright == 85 ? 70 : 100;
            prefs.edit().putInt("brightness", next).apply();
            dimView.animate().alpha(baseDim()).setDuration(300).start();
            drawParent(parentClicked);
        });
        boolean verse = prefs.getBoolean("verse", true);
        parentRow("Verse and hadith card", verse ? "Shown" : "Hidden", () -> {
            prefs.edit().putBoolean("verse", !verse).apply();
            lineFrame.setVisibility(!verse ? View.VISIBLE : View.GONE);
            drawParent(parentClicked);
        });
        boolean h24 = prefs.getBoolean("clock24", false);
        parentRow("Clock", h24 ? "24-hour" : "12-hour", () -> {
            prefs.edit().putBoolean("clock24", !h24).apply();
            shownClock = "";
            drawParent(parentClicked);
        });
        boolean quiet = prefs.getBoolean("quiet_quran", true);
        parentRow("Soft Quran on the home screen", quiet ? "On" : "Off", () -> {
            prefs.edit().putBoolean("quiet_quran", !quiet).apply();
            if (quiet && quranQuiet) stopQuran(); else if (!quiet) { quietStopped = false; backgroundQuran(); }
            drawParent(parentClicked);
        });
        int fit = prefs.getInt("fit", 100);
        parentRow("Screen size (fit the TV's edges)", fit + "%", () -> {
            int i = 0;
            while (i < FITS.length && FITS[i] != fit) i++;
            prefs.edit().putInt("fit", FITS[(i + 1) % FITS.length]).apply();
            fitStage();
            drawParent(parentClicked);
        });
        int cf = prefs.getInt("clock_face", 0) % CLOCK_FONTS.length;
        parentRow("Clock font", CLOCK_FONTS[cf], () -> {
            prefs.edit().putInt("clock_face", (cf + 1) % CLOCK_FONTS.length).apply();
            applyClockFont();
            drawParent(parentClicked);
        });
        boolean sky = prefs.getBoolean("sky_tint", true);
        parentRow("Light of the day", sky ? "On" : "Off", () -> {
            prefs.edit().putBoolean("sky_tint", !sky).apply();
            drawSky(-1);
            drawParent(parentClicked);
        });
        boolean acc = prefs.getBoolean("accent", true);
        parentRow("Wallpaper colour on glass", acc ? "On" : "Off", () -> {
            prefs.edit().putBoolean("accent", !acc).apply();
            if (acc) Accent.glideTo(Accent.color, 0, null); else ui.post(sampleAccent);
            drawParent(parentClicked);
        });
        String aerial = aerialDream();
        String saver = Settings.Secure.getString(getContentResolver(), SAVER_KEY);
        boolean aerialOn = aerial != null && saver != null && saver.startsWith(AERIAL_PKG);
        String saverValue = screensaverNote != null ? screensaverNote
                : aerial == null ? "Get Aerial Views ›" : aerialOn ? "Aerial Views" : "TV default";
        screensaverNote = null;
        parentRow("Screensaver (in other apps)", saverValue, () -> {
            if (aerial == null) { closePanels(); openStore(AERIAL_PKG); return; }
            screensaverNote = setScreensaver(aerialOn ? null : aerial) ? null : "Run Update TV once first";
            drawParent(parentClicked);
        });
        if (aerialOn) parentRow("Screensaver videos and clock", "›", () -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(AERIAL_PKG);
            if (i == null) i = getPackageManager().getLeanbackLaunchIntentForPackage(AERIAL_PKG);
            if (i != null) { closePanels(); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); try { startActivity(i); } catch (Exception ignored) { } }
        });
        for (String[] b : RemoteKeys.BUTTONS) {
            int row = parentList.getChildCount();
            boolean set = prefs.contains(b[0]) || b[0].equals("rk_apps");
            String value = remoteNote != null && remoteNoteKey.equals(b[0]) ? remoteNote
                    : !RemoteKeys.running ? "Run Update TV once first" : set ? b[2] : "Press to set up";
            parentRow(b[1], value, () -> learnButton(b[0], row));
            // while the helper isn't running these rows can't do anything yet: their names step back
            if (!RemoteKeys.running) ((TextView) ((ViewGroup) parentList.getChildAt(row)).getChildAt(0)).setTextColor(DIM);
        }
        remoteNote = null;
        parentRow("Android settings", "›", () -> { closePanels(); openSettings(); });
        View f = parentList.getChildAt(Math.min(focus, parentList.getChildCount() - 1));
        f.post(f::requestFocus);
    }

    // Remote buttons (RemoteKeys): each row waits for its button to be pressed once and remembers it.
    private String remoteNote, remoteNoteKey = "";
    private int learnRow;
    private final Runnable learnTimeout = () -> {
        RemoteKeys.stopLearning();
        remoteNote = "Nothing came through";
        if (parentPanel.getVisibility() == View.VISIBLE) drawParent(learnRow);
    };

    private void learnButton(String key, int row) {
        if (!RemoteKeys.running) {
            // answer the press in place, so the row never looks dead
            remoteNoteKey = key;
            remoteNote = "Run Update TV on the PC first";
            drawParent(row);
            nudge(parentList.getChildAt(row));
            return;
        }
        remoteNoteKey = key;
        learnRow = row;
        remoteNote = "Press that button now…";
        drawParent(row);
        ui.removeCallbacks(learnTimeout);
        ui.postDelayed(learnTimeout, 10000);
        RemoteKeys.learn(key, code -> {
            ui.removeCallbacks(learnTimeout);
            remoteNoteKey = key;
            remoteNote = "Done ✓";
            if (parentPanel.getVisibility() == View.VISIBLE) drawParent(row);
        });
    }

    // Screensaver: the home screen itself never sleeps (it is a moving wallpaper already), so this is the
    // screensaver other apps get when left idle, e.g. a paused film in Kodi. Switching needs
    // WRITE_SECURE_SETTINGS, which Update TV grants over ADB.
    private static final String AERIAL_PKG = "com.neilturner.aerialviews";
    private static final String SAVER_KEY = "screensaver_components";
    private static final String TV_DEFAULT_SAVER = "com.google.android.apps.tv.dreamx/.service.Backdrop";
    private String screensaverNote;

    /** Aerial Views' screensaver, as "package/class", or null when the app isn't installed. */
    private String aerialDream() {
        Intent q = new Intent("android.service.dreams.DreamService").setPackage(AERIAL_PKG);
        for (ResolveInfo r : getPackageManager().queryIntentServices(q, 0))
            return r.serviceInfo.packageName + "/" + r.serviceInfo.name;
        return null;
    }

    /** Turns Aerial Views on (component) or puts back the screensaver the TV had before (null). */
    private boolean setScreensaver(String component) {
        try {
            android.content.ContentResolver cr = getContentResolver();
            if (component != null) {
                String before = Settings.Secure.getString(cr, SAVER_KEY);
                if (before != null && !before.startsWith(AERIAL_PKG)) prefs.edit().putString("saver_before", before).apply();
                Settings.Secure.putString(cr, SAVER_KEY, component);
                Settings.Secure.putInt(cr, "screensaver_enabled", 1);
            } else {
                Settings.Secure.putString(cr, SAVER_KEY, prefs.getString("saver_before", TV_DEFAULT_SAVER));
            }
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** The weekly family stats page, inside the parent menu: screen time from Kids Corner and the Quran heard this week. */
    private void drawStats() {
        parentList.removeAllViews();
        statsShown = true; // the remote's Back goes back to the list, like the Back row below
        statHeading("SCREEN TIME · LAST 7 DAYS  ·  وقت الشاشة");
        List<String[]> kids = Stats.kidsWeek(this);
        if (kids == null) statLine("Kids Corner will share its numbers after its next update.", DIM);
        else if (kids.isEmpty()) statLine("No time in Kids Corner this week.", DIM);
        else for (String[] k : kids) {
            int min = Integer.parseInt(k[1]);
            if (min > 0 || "Kids".equals(k[0]))
                statLine((("Kids".equals(k[0])) ? "Kids Corner" : k[0]) + "   " + Stats.hm(min) + (k[2].isEmpty() ? "" : "  ·  most: " + k[2]), WHITE);
            else
                statLine(k[0] + "   ⭐ " + k[3] + " stars used this week  ·  " + k[4] + " saved", SOFT);
        }
        statHeading("QURAN THIS WEEK  ·  القرآن هذا الأسبوع");
        Object[] q = Stats.quranWeek(this);
        statLine(Stats.hm((int) q[0]) + " of listening  ·  " + q[1] + " surahs played", WHITE);
        @SuppressWarnings("unchecked") List<Map.Entry<String, Integer>> top = (List<Map.Entry<String, Integer>>) q[2];
        if (!top.isEmpty()) {
            StringBuilder sb = new StringBuilder("Most played: ");
            for (int i = 0; i < top.size(); i++) {
                int n = Integer.parseInt(top.get(i).getKey());
                sb.append(i == 0 ? "" : ",  ").append(Quran.EN[n - 1]).append(" (").append(top.get(i).getValue()).append(")");
            }
            statLine(sb.toString(), SOFT);
        }
        @SuppressWarnings("unchecked") List<Map.Entry<String, Integer>> rec = (List<Map.Entry<String, Integer>>) q[3];
        if (!rec.isEmpty()) {
            StringBuilder sb = new StringBuilder("Reciters: ");
            for (int i = 0; i < rec.size(); i++) sb.append(i == 0 ? "" : ",  ").append(rec.get(i).getKey());
            statLine(sb.toString(), SOFT);
        }
        View back = new View(this);
        parentList.addView(back, new LinearLayout.LayoutParams(-1, 18));
        parentRow("Back", "‹", () -> drawParent(0));
        View b = parentList.getChildAt(parentList.getChildCount() - 1);
        b.post(b::requestFocus);
    }

    // ================================================================== Islamic calendar

    private FrameLayout calendarPanel;

    /** A page of the coming Islamic dates with how many days are left, opened from All apps. */
    /** True when the calendar was opened from All apps: Back goes back there, on the calendar's own item. */
    private boolean calendarFromApps;

    private void openCalendar() {
        boolean fromApps = appsPanel.getVisibility() == View.VISIBLE;
        calendarPanel.removeViews(1, calendarPanel.getChildCount() - 1); // keep the scrim
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        Calendar now = Calendar.getInstance(KUWAIT);
        SimpleDateFormat g = new SimpleDateFormat("EEE d MMM yyyy", Locale.UK);
        g.setTimeZone(KUWAIT);
        View first = null;
        for (Object[] e : Events.upcoming(now)) {
            int days = (int) e[2];
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(26, 14, 30, 14);
            r.setFocusable(true);
            // a reading guide, not a button (OK does nothing here): a soft fill on the row you're on, no gold edge or zoom
            r.setOnFocusChangeListener((v, has) -> v.setBackground(has ? glass(20, 0x14FFFFFF, 0) : null));
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(text((String) e[0], 28, fMed, WHITE));
            TextView ar = text(g.format(new java.util.Date((long) e[3])) + "   ·   " + e[1], 26, fAr, DIM);
            ar.setIncludeFontPadding(true);
            ar.setTextDirection(View.TEXT_DIRECTION_LTR);
            ar.setGravity(Gravity.START);
            ar.setPadding(0, 4, 0, 0);
            names.addView(ar);
            r.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
            TextView left = text(days == 0 ? "Today" : days == 1 ? "Tomorrow" : "in " + days + " days", 28, fSemi, days <= 7 ? 0xFFF1D58A : SOFT);
            left.setGravity(Gravity.END);
            r.addView(left, new LinearLayout.LayoutParams(250, -2));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = 6;
            box.addView(r, rp);
            if (first == null) first = r;
        }
        listCard(calendarPanel, "ISLAMIC CALENDAR  ·  التقويم الهجري", "Coming up", box, 1100, 940);
        show(calendarPanel);
        if (first != null) first.requestFocus(); // before All apps goes, so the highlight never drops to the home row
        // All apps fades out under the calendar instead of vanishing, so the screen never dips to the wallpaper
        if (fromApps) appsPanel.animate().alpha(0).setStartDelay(0).setDuration(260).setInterpolator(EASE)
                .withEndAction(() -> appsPanel.setVisibility(View.GONE)).start();
        calendarFromApps = fromApps;
    }

    private void closeCalendarToApps() {
        calendarFromApps = false;
        calendarPanel.animate().alpha(0).setDuration(180).withEndAction(() -> calendarPanel.setVisibility(View.GONE)).start();
        appsPanel.setAlpha(0);
        appsPanel.setVisibility(View.VISIBLE);
        appsPanel.bringToFront();
        appsPanel.animate().alpha(1).setStartDelay(0).setDuration(220).setInterpolator(EASE).start();
        focusLayer(appsPanel);
        if (calendarItem != null) calendarItem.requestFocus();
    }

    private void statHeading(String s) {
        TextView t = kicker(text(s, 20, fSemi, DIM));
        t.setPadding(30, 22, 30, 8);
        parentList.addView(t);
    }

    private void statLine(String s, int color) {
        TextView t = text(s, 26, fMed, color);
        t.setPadding(30, 6, 30, 6);
        t.setLineSpacing(0, 1.1f);
        parentList.addView(t, new LinearLayout.LayoutParams(-1, -2));
    }

    private void parentRow(String label, String value, Runnable onClick) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(30, 0, 30, 0);
        r.setFocusable(true);
        r.setClickable(true);
        r.addView(text(label, 28, fMed, WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(text(value, 26, fSemi, DIM));
        r.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? focusGlass(22) : null);
            v.animate().scaleX(has ? 1.03f : 1f).scaleY(has ? 1.03f : 1f).setDuration(180).setInterpolator(EASE).start();
        });
        r.setOnClickListener(v -> { parentClicked = parentList.indexOfChild(v); onClick.run(); });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 96); // the TV list minimum (48 dp)
        p.topMargin = 8;
        parentList.addView(r, p);
    }

    // ================================================================== polish: animation speed, wallpaper colour, drift, light of the day

    /**
     * The box runs its animations at 0.5x (snappier menus elsewhere), which would also halve every fade and glide
     * on this screen. The home screen keeps its own animations at their designed length; if animations are turned
     * off altogether (0), that choice is respected.
     */
    private void fullLengthAnimations() {
        float scale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        if (scale <= 0f || scale == 1f) return;
        try {
            ValueAnimator.class.getMethod("setDurationScale", float.class).invoke(null, 1f);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = ValueAnimator.class.getDeclaredField("sDurationScale");
                f.setAccessible(true);
                f.setFloat(null, 1f);
            } catch (Throwable ignored) { } // not allowed on this system: the animations simply stay at the box's speed
        }
    }

    /** Copies a small frame of the playing video and lets the glass lean towards its colour. */
    private final Runnable sampleAccent = new Runnable() {
        @Override public void run() {
            if (!prefs.getBoolean("accent", true)) return;
            final Uri uri = playingUri;
            if (uri == null) return;
            if (video.getHolder().getSurface() == null || !video.getHolder().getSurface().isValid()) { accentFromFile(uri); return; }
            final Bitmap small = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888);
            try {
                PixelCopy.request(video, small, result -> {
                    int[] c = result == PixelCopy.SUCCESS ? Accent.pick(small) : null;
                    if (c == null) accentFromFile(uri); // the copy came back black on this box: read a frame from the file instead
                    else applyAccent(uri, c);
                }, ui);
            } catch (Throwable e) { accentFromFile(uri); }
        }
    };

    /** Reads one frame from the clip itself, once per clip; the answer is remembered. */
    private void accentFromFile(Uri uri) {
        final String key = "accent:" + uri;
        String known = prefs.getString(key, null);
        if (known != null) {
            String[] p = known.split(",");
            applyAccent(uri, new int[]{(int) Long.parseLong(p[0]), Integer.parseInt(p[1])});
            return;
        }
        runBg(() -> {
            int[] c = null;
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            try {
                r.setDataSource(this, uri);
                for (long t : new long[]{1_500_000L, 6_000_000L}) {
                    Bitmap f = r.getScaledFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 64, 36);
                    if (f == null) continue;
                    c = Accent.pick(f);
                    f.recycle();
                    if (c != null) break;
                }
            } catch (Throwable ignored) {
            } finally {
                try { r.release(); } catch (Throwable ignored) { }
            }
            if (c == null) c = new int[]{0, 0};
            prefs.edit().putString(key, (c[0] & 0xFFFFFFFFL) + "," + c[1]).apply();
            final int[] fc = c;
            ui.post(() -> applyAccent(uri, fc));
        });
    }

    private void applyAccent(Uri uri, int[] c) {
        if (!uri.equals(playingUri) || !prefs.getBoolean("accent", true)) return; // another clip has started since
        Accent.glideTo(c[0] | 0xFF000000, c[1] / 1000f, () -> { if (anyDockFocused()) magnify(lastDock); });
    }

    /** How much larger than the screen the video is on each side, as a share of the screen, and how far it may drift. */
    static final float DRIFT_ROOM = 0.022f;
    private float driftRange;

    /** The wallpaper drifts gently the other way as focus moves along the dock, as if the screen had depth. */
    private void drift(int focused) {
        int n = dockTiles.size();
        float x = focused < 0 || n < 2 ? 0 : -(focused - (n - 1) / 2f) / ((n - 1) / 2f) * driftRange;
        video.animate().cancel();
        video.animate().translationX(x).setDuration(1400).setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    static final int SKY_COOL = 0xFF5B86E5, SKY_WARM = 0xFFFF8C42;

    /** A gentle wash over the video: cool blue from Fajr to a little after sunrise, warm amber from just before
     *  Maghrib, softening through the night. min is minutes since midnight (-1 = now). */
    private void drawSky(int min) {
        if (skyTint == null) return;
        if (min < 0) { Calendar c = Calendar.getInstance(KUWAIT); min = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE); }
        float cool = 0, warm = 0;
        if (today != null && prefs.getBoolean("sky_tint", true)) {
            int fajr = today[0], sunrise = today[1], maghrib = today[4], isha = today[5];
            cool = ramp(min, fajr - 25, fajr) * (1 - ramp(min, sunrise, sunrise + 50)) * 0.11f;
            float evening = ramp(min, maghrib - 40, maghrib) * 0.13f;                     // rises into sunset
            evening -= ramp(min, maghrib + 30, isha + 60) * 0.07f;                       // settles after Isha
            float night = min < fajr ? 0.06f * (1 - ramp(min, fajr - 70, fajr - 25)) : 0; // after midnight, until Fajr nears
            warm = Math.max(night, min >= maghrib - 40 ? evening : 0);
        }
        int color = cool >= warm ? SKY_COOL : SKY_WARM;
        float a = Math.max(cool, warm);
        skyTint.setBackgroundColor(color);
        skyTint.animate().alpha(a).setDuration(2000).start();
    }

    private static float ramp(int x, int from, int to) {
        if (x <= from) return 0;
        if (x >= to) return 1;
        return (x - from) / (float) (to - from);
    }

    // ================================================================== ambient mode

    /** After two quiet minutes the dock fades away, leaving the clock, the cards, the verse and the video. */
    static final long AMBIENT_AFTER = 2 * 60 * 1000;
    private boolean ambient;
    private long lastInput = System.currentTimeMillis();

    static final float QUIET_DROP = 110; // clock and verse slide down together, by the same amount, keeping their gap

    private void enterAmbient() {
        ambient = true;
        bottomGroup.animate().alpha(0).translationY(40).setStartDelay(0).setDuration(1200).setInterpolator(EASE).start();
        dockLabel.animate().alpha(0).setDuration(400).start();
        for (View t : topParts) t.animate().alpha(0.7f).setDuration(1500).start();
        // the clock and the verse card settle lower together, into the dock's space, keeping their gap
        centerGroup.animate().translationY(QUIET_DROP).setDuration(1500).setInterpolator(EASE).start();
        lineFrame.animate().translationY(QUIET_DROP).setDuration(1500).setInterpolator(EASE).start();
        topParts[1].animate().translationY(QUIET_DROP).setDuration(1500).setInterpolator(EASE).start();
    }

    /** After fifteen quiet minutes the whole screen dims; the video and the soft Quran keep playing. */
    static final long DIM_AFTER = 15 * 60 * 1000;
    static final float DIM_LEVEL = 0.6f;
    private boolean nightDim;

    private void enterNightDim() {
        nightDim = true;
        float a = Math.max(DIM_LEVEL, baseDim());
        dimView.animate().alpha(a).setDuration(4000).start();
        // the words dim with the picture: no bright white clock in a dark room, and kinder to the panel
        centerGroup.animate().alpha(NIGHT_TEXT).setDuration(4000).setInterpolator(EASE).start();
        lineFrame.animate().alpha(NIGHT_TEXT).setDuration(4000).setInterpolator(EASE).start();
        for (View t : topParts) t.animate().alpha(NIGHT_TEXT).setDuration(4000).setInterpolator(EASE).start();
    }

    static final float NIGHT_TEXT = 0.55f;

    private void leaveAmbient() {
        ambient = false;
        if (nightDim) {
            nightDim = false;
            dimView.animate().alpha(baseDim()).setDuration(450).start();
        }
        lastInput = System.currentTimeMillis();
        bottomGroup.animate().alpha(1).translationY(0).setStartDelay(0).setDuration(450).setInterpolator(EASE).start();
        lineFrame.animate().alpha(1).translationY(0).setDuration(450).setInterpolator(EASE).start();
        for (View t : topParts) t.animate().alpha(1).setDuration(450).start();
        centerGroup.animate().alpha(1).translationY(0).setDuration(450).setInterpolator(EASE).start();
        topParts[1].animate().translationY(0).setDuration(450).setInterpolator(EASE).start();
        lastDock = Math.max(0, Math.min(lastDock, dockTiles.size() - 1));
        View f = dockTiles.get(lastDock);
        if (f.hasFocus()) showDockLabel(f, ((Dock) f.getTag()).name); else f.requestFocus();
    }
}
