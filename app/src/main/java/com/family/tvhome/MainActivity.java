package com.family.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.PixelCopy;
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
    static final int WHITE = 0xFFF5F5F7, SOFT = 0xD9FFFFFF, DIM = 0xA6FFFFFF;
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
    static final Set<String> HIDDEN_FROM_LIST = new HashSet<>(Arrays.asList("com.google.android.youtube.tv", "com.google.android.youtube"));

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private SharedPreferences prefs;
    private Typeface fMed, fSemi, fBold, fAr, fArSemi, fQuran, fSerif;

    private FrameLayout root, stage;
    private SurfaceView video;
    private View videoCover, dimView;
    private boolean resumed;
    private int lastDock;
    private final ExecutorService blurThread = Executors.newSingleThreadExecutor();
    private ExoPlayer player;
    private String currentClip = "";
    private int clipErrors;

    private View centerGroup, bottomGroup;
    private TextView dateText, hijriText, tempText, skyText, clockText, ampmText, pillText, lineKicker, lineAr, lineEn;
    private LinearLayout lineBox;
    private final LinearLayout[] prayerCells = new LinearLayout[5];
    private final TextView[][] prayerTexts = new TextView[5][3];
    private static final int[] BAR_PRAYERS = {0, 2, 3, 4, 5}; // Fajr, Dhuhr, Asr, Maghrib, Isha
    private final List<FrameLayout> dockTiles = new ArrayList<>();
    private TextView dockLabel;
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.6f);
    /** The focused dock app gets a bright glow behind it and a light ring on top (one pair per tile). */
    private final List<Drawable[]> tileFocus = new ArrayList<>();
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
        fMed = getResources().getFont(R.font.inter_medium);
        fSemi = getResources().getFont(R.font.inter_semibold);
        fBold = getResources().getFont(R.font.inter_bold);
        fAr = getResources().getFont(R.font.plex_arabic_medium);
        fArSemi = getResources().getFont(R.font.plex_arabic_semibold);
        fQuran = getResources().getFont(R.font.amiri_bold);
        fSerif = getResources().getFont(R.font.cormorant_semibold_italic);

        // The home screen never times out or starts a screensaver while it is in front.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new SurfaceView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        videoCover = new View(this);
        videoCover.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF1B2A4A, 0xFF0E1426, 0xFF241A3A}));
        root.addView(videoCover, new FrameLayout.LayoutParams(-1, -1));
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(1 - prefs.getInt("brightness", 100) / 100f);
        root.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        // Soft dark gradients at the top and bottom keep the text readable on any video.
        View topScrim = new View(this);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x80000000, 0x00000000, 0x00000000}));
        root.addView(topScrim, new FrameLayout.LayoutParams(-1, -1));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            float s = Math.min((r - l) / 1920f, (b - t) / 1080f);
            stage.setPivotX(0); stage.setPivotY(0);
            stage.setScaleX(s); stage.setScaleY(s);
            stage.setTranslationX(((r - l) - 1920 * s) / 2);
            stage.setTranslationY(((b - t) - 1080 * s) / 2);
            GlassFrame.setScreen(r - l, b - t, s);
        });
        View bottomScrim = new View(this);
        bottomScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xA6000000, 0x00000000, 0x00000000}));
        root.addView(bottomScrim, new FrameLayout.LayoutParams(-1, -1));

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
        lineFrame.setVisibility(prefs.getBoolean("verse", true) ? View.VISIBLE : View.GONE);
        registerDriveWatcher();
        ui.postDelayed(() -> bg.execute(() -> Quran.reciters(getCacheDir())), 20000); // warm the reciter list (names in Arabic)

        player = new ExoPlayer.Builder(this).build();
        player.setVideoSurfaceView(video);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(0f);
        player.addListener(new Player.Listener() {
            @Override public void onRenderedFirstFrame() { clipErrors = 0; videoCover.animate().alpha(0).setDuration(700).start(); splashReady(); }
            @Override public void onPlayerError(androidx.media3.common.PlaybackException e) {
                // a clip that can't play (an unusual format): keep the calm gradient and try another one
                videoCover.setAlpha(1);
                // right after the box turns on the files may not be readable yet, so keep trying for a while
                if (++clipErrors < 12) ui.postDelayed(MainActivity.this::playRandomClip, clipErrors < 4 ? 500 : 3000);
            }
        });

        dockTiles.get(0).requestFocus();
        if (coldStart) showSplash(); else introAnimation();
        tick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        lastInput = System.currentTimeMillis();
        if (ambient) leaveAmbient();
        hideBars();
        playRandomClip();
        updateDrive();
        quietStopped = false;
        ui.removeCallbacks(startQuiet);
        ui.postDelayed(startQuiet, 4000);
        // No frosted backdrop: on this box the video is on a hardware layer and a copy of it comes back black.
        ui.postDelayed(() -> { loadDockIcons(); refreshApps(); }, 600);
    }

    @Override
    protected void onPause() {
        resumed = false;
        ui.removeCallbacks(grabBackdrop);
        ui.removeCallbacks(startQuiet);
        player.pause();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ambient) leaveAmbient();
        closePanels();
        dockTiles.get(lastDock).requestFocus();
    }

    @Override
    protected void onStop() {
        // Leaving home for another app stops the recitation so it never plays over a film.
        if (quran != null && quran.getPlayWhenReady()) { quran.pause(); drawQuranState(); }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
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
        if (splash != null) { if (e.getAction() == KeyEvent.ACTION_UP) hideSplash(); return true; } // a press skips the intro
        if (ambient) { if (e.getAction() == KeyEvent.ACTION_UP) leaveAmbient(); return true; } // the first press only wakes the screen
        if (code == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) closePanels();
            return true; // the home screen never closes itself
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0 && !anyPanelOpen()) secretKey(code);
        if (pinPanel.getVisibility() == View.VISIBLE && e.getAction() == KeyEvent.ACTION_DOWN) {
            if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) { pinPress(String.valueOf(code - KeyEvent.KEYCODE_0)); return true; }
            if (code == KeyEvent.KEYCODE_DEL) { pinPress("⌫"); return true; }
        }
        return super.dispatchKeyEvent(e);
    }

    // ================================================================== helpers

    private TextView text(String s, float px, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (px >= 100) t.setShadowLayer(24, 0, 3, 0x4D000000);
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
        ui.removeCallbacks(clearOverride);
        ui.postDelayed(clearOverride, 3500);
        updatePill();
    }
    private String pillTextOverride;
    private final Runnable clearOverride = () -> { pillTextOverride = null; updatePill(); };

    // ================================================================== layout

    private TextView adhanName, adhanAr, adhanCountdown, driveText, dayText;
    private volatile String hiLo = "";

    static final int CARD_W = 560, CARD_H = 330;
    // Light dark wash behind the glass so text stays readable on busy wallpapers
    static final int CARD_TINT = 0x3A000000;

    private static TextView kicker(TextView t) { t.setLetterSpacing(0.16f); return t; }

    /** Two matching cards, mirrored: weather and dates top left, the next adhan top right. */
    private void buildTop() {
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(36, 28, 36, 24);
        LinearLayout kRow = new LinearLayout(this);
        kRow.setGravity(Gravity.CENTER_VERTICAL);
        kRow.addView(kicker(text("KUWAIT  ·  الكويت", 20, fSemi, DIM)), new LinearLayout.LayoutParams(0, -2, 1));
        driveText = text("", 20, fSemi, DIM);
        driveText.setVisibility(View.GONE);
        kRow.addView(driveText);
        left.addView(kRow, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout wx = new LinearLayout(this);
        wx.setGravity(Gravity.BOTTOM);
        wx.setPadding(0, 12, 0, 0);
        tempText = text("--°", 60, fSemi, WHITE);
        skyText = text("", 24, fMed, SOFT);
        skyText.setPadding(18, 0, 0, 10);
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
        adhanAr = text("", 26, fArSemi, SOFT);
        adhanAr.setIncludeFontPadding(true);
        names.addView(adhanName, new LinearLayout.LayoutParams(-2, -2));
        names.addView(adhanAr, new LinearLayout.LayoutParams(-2, -2));
        adhanCountdown = text("--:--:--", 56, fSemi, WHITE);
        adhanCountdown.setFontFeatureSettings("tnum");
        adhanCountdown.setGravity(Gravity.END);
        nameRow.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
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
            TextView ar = text(PrayerTimes.AR[k], 20, fAr, DIM);
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
        View rightFrame = card(card, CARD_W, CARD_H, Gravity.TOP | Gravity.END, 80, 56);

        topParts = new View[]{leftFrame, rightFrame};
    }

    private View[] topParts;
    private TextView eventText;
    private View eventChip;

    private void drawEventChip(Calendar now) {
        String[] b = Events.badge(now);
        if (b == null) { eventChip.setVisibility(View.GONE); return; }
        eventText.setText("●  " + b[0] + "  ·  " + b[1]);
        eventChip.setVisibility(View.VISIBLE);
    }
    private ThinBar adhanBar, lineBar, nowBar;
    private ValueAnimator lineTimer;
    static final long LINE_SECONDS = 30;
    private LinearLayout leftCard;
    private View lineFrame;

    /** Middle: a large, slightly see-through clock and the rotating verse / hadith line. */
    private void buildCenter() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clockText = text("--:--", 210, fSemi, 0xD9FFFFFF);
        clockText.setFontFeatureSettings("tnum");
        clockText.setLetterSpacing(-0.03f);
        clockText.setShadowLayer(40, 0, 4, 0x40000000);
        ampmText = text("", 46, fSemi, 0xA6FFFFFF);
        ampmText.setPadding(14, 0, 0, 38);
        clockRow.addView(clockText);
        clockRow.addView(ampmText);
        c.addView(clockRow, new LinearLayout.LayoutParams(-1, -2));

        stage.addView(c, at(1920, -2, Gravity.TOP | Gravity.START, 0, 296));
        centerGroup = c;

        // The verse / hadith card, low on the screen just above the dock: Amiri for the Arabic, an italic serif for the English.
        lineBox = new LinearLayout(this);
        lineBox.setOrientation(LinearLayout.VERTICAL);
        lineBox.setGravity(Gravity.CENTER_HORIZONTAL);
        lineBox.setPadding(56, 22, 56, 24);
        lineKicker = text("", 20, fSemi, DIM);
        lineKicker.setLetterSpacing(0.16f);
        lineAr = text("", 38, fQuran, WHITE);
        lineAr.setIncludeFontPadding(true);
        lineAr.setGravity(Gravity.CENTER);
        lineAr.setMaxWidth(1360);
        lineAr.setPadding(0, 6, 0, 0);
        lineEn = text("", 32, fSerif, SOFT);
        lineEn.setGravity(Gravity.CENTER);
        lineEn.setMaxWidth(1360);
        lineEn.setLineSpacing(0, 1.05f);
        lineEn.setPadding(0, 2, 0, 0);
        lineBox.addView(lineKicker);
        lineBox.addView(lineAr);
        lineBox.addView(lineEn);
        // Fills over the 30 seconds each line stays up.
        lineBar = new ThinBar(this);
        lineBar.setColor(0x66FFFFFF);
        LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(220, 2);
        lbp.topMargin = 14;
        lineBox.addView(lineBar, lbp);
        lineFrame = card(lineBox, -1, -1, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 326);

        // A small message pill, used for short notices like "PIN saved".
        pillText = text("", 28, fMed, WHITE);
        pillText.setGravity(Gravity.CENTER);
        pillText.setPadding(34, 14, 34, 16);
        pillText.setBackground(new Glass(40, 0, 0, 0x4D000000));
        pillText.setAlpha(0);
        stage.addView(pillText, at(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 60));
    }

    static final int TILE = 132, DOCK_H = 188, DOCK_Y = 36;

    /** Bottom: the app dock, centred. Moving across it magnifies the focused app and, a little, its neighbours. */
    private void buildBottom() {
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(30, 0, 30, 0);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        ViewOutlineProvider rounded = new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 30); }
        };
        for (Dock d : DOCK) {
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setClickable(true);
            tile.setTag(d);
            tile.setClipChildren(false);
            // The icon fills the tile edge to edge, with rounded corners, like Apple TV.
            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_XY);
            icon.setOutlineProvider(rounded);
            icon.setClipToOutline(true);
            tile.addView(icon, new FrameLayout.LayoutParams(-1, -1));
            TextView mark;
            if (d.id.equals("quran")) {
                mark = text("القرآن", 44, fQuran, 0xFFF1D58A);
                mark.setIncludeFontPadding(true);
                mark.setBackground(tileFace(0xFF14504A, 0xFF0A2A27));
            } else if (d.id.equals("apps")) {
                mark = text("⋯", 60, fBold, WHITE);
                mark.setBackground(new Glass(30, 0, 0, 0x33000000));
            } else {
                mark = text(d.name.substring(0, 1), 64, fBold, WHITE);
                mark.setBackground(tileFace(d.color, darker(d.color)));
            }
            mark.setGravity(Gravity.CENTER);
            tile.addView(mark, new FrameLayout.LayoutParams(-1, -1));
            tile.setPivotY(TILE);
            if (d.id.equals("yt")) tile.setOnLongClickListener(v -> { openPin(true); return true; });
            tile.setOnClickListener(v -> openDock(d));
            tile.setOnFocusChangeListener((v, has) -> {
                if (has) { lastDock = dockTiles.indexOf(v); magnify(lastDock); showDockLabel(v, d.name); }
                else if (!anyDockFocused()) { magnify(-1); dockLabel.animate().alpha(0).setDuration(150).start(); }
            });
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(TILE, TILE);
            tp.leftMargin = 11; tp.rightMargin = 11;
            dock.addView(tile, tp);
            dockTiles.add(tile);
            tileFocus.add(new Drawable[]{new Glass(30, 30, 0xB3FFFFFF, 0), ring(30, 3, 0xE6FFFFFF)});
        }
        bottomGroup = glassCard(stage, dock, -1, DOCK_H, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, DOCK_Y,
                48, 34, 0x40FFFFFF, CARD_TINT, false);

        dockLabel = text("", 24, fSemi, WHITE);
        dockLabel.setPadding(22, 8, 22, 10);
        dockLabel.setBackground(new Glass(24, 0, 0, 0x4D000000));
        dockLabel.setAlpha(0);
        stage.addView(dockLabel, at(-2, -2, Gravity.TOP | Gravity.START, 0, 0));
    }

    private static GradientDrawable tileFace(int top, int bottom) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(30);
        return g;
    }

    private static int darker(int c) {
        return Color.rgb(Color.red(c) * 6 / 10, Color.green(c) * 6 / 10, Color.blue(c) * 6 / 10);
    }

    private boolean anyDockFocused() {
        for (View t : dockTiles) if (t.hasFocus() || t.isFocused()) return true;
        return false;
    }

    /** Dock magnification: focused 1.3x and lifted, neighbours 1.1x, the rest at rest. */
    private void magnify(int focused) {
        for (int i = 0; i < dockTiles.size(); i++) {
            View t = dockTiles.get(i);
            int dist = focused < 0 ? 9 : Math.abs(i - focused);
            float s = dist == 0 ? 1.22f : dist == 1 ? 1.07f : 1f;
            float lift = dist == 0 ? -20 : dist == 1 ? -5 : 0;
            t.animate().scaleX(s).scaleY(s).translationY(lift).setDuration(240).setInterpolator(EASE).withLayer().start();
            t.setBackground(dist == 0 ? tileFocus.get(i)[0] : null);
            t.setForeground(dist == 0 ? tileFocus.get(i)[1] : null);
        }
    }

    private void showDockLabel(View tile, String name) {
        if (tile.getWidth() == 0 || !tile.isLaidOut()) { tile.post(() -> { if (tile.hasFocus()) showDockLabel(tile, name); }); return; }
        dockLabel.setText(name);
        dockLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] a = new int[2], s = new int[2];
        tile.getLocationOnScreen(a);
        stage.getLocationOnScreen(s);
        float sc = stage.getScaleX();
        float cx = (a[0] - s[0]) / sc + tile.getWidth() * tile.getScaleX() / 2f;
        dockLabel.animate().cancel();
        dockLabel.setX(cx - dockLabel.getMeasuredWidth() / 2f);
        float y = 1080 - DOCK_Y - DOCK_H / 2f + TILE / 2f - TILE * 1.22f - 20 - 62; // just above the magnified tile
        dockLabel.setAlpha(0);
        dockLabel.setTranslationY(y + 10);
        dockLabel.animate().alpha(1).translationY(y).setDuration(220).setInterpolator(EASE).start();
    }

    private int clipRetries;
    private final Runnable retryClips = () -> { if (resumed) playRandomClip(); };

    // ================================================================== start-up intro

    /** True only for the first start after the box turns on (or after the app is updated). */
    private static boolean coldStart = true;
    private FrameLayout splash;
    private long splashSince;

    /** An animated intro while the first wallpaper loads; it fades into the home screen once both are done. */
    private void showSplash() {
        coldStart = false;
        splashSince = System.currentTimeMillis();
        for (View v : new View[]{topParts[0], topParts[1], centerGroup, lineFrame, bottomGroup}) v.setAlpha(0);
        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.BLACK);
        splash.setElevation(60);
        // One of ten animated intros, picked at random and never the same one twice in a row.
        int last = prefs.getInt("last_intro", -1), pick = random.nextInt(IntroView.COUNT);
        if (pick == last) pick = (pick + 1 + random.nextInt(IntroView.COUNT - 1)) % IntroView.COUNT;
        prefs.edit().putInt("last_intro", pick).apply();
        splash.addView(new IntroView(this, pick, fQuran, fSemi), new FrameLayout.LayoutParams(1920, 1080));
        stage.addView(splash, new FrameLayout.LayoutParams(1920, 1080));
        ui.postDelayed(this::hideSplash, SPLASH_MAX); // never wait longer than this, even without a video
    }

    static final long SPLASH_MIN = IntroView.LENGTH + 300, SPLASH_MAX = 14000;

    private void splashReady() {
        if (splash == null) return;
        long wait = SPLASH_MIN - (System.currentTimeMillis() - splashSince);
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
            player.setMediaItem(MediaItem.fromUri(chosen));
            player.prepare();
            player.play();
        }).start();
    }

    // ================================================================== clock, adhan, widgets

    private void tick() {
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
            clockText.setText(clock);
            ampmText.setText(h24 ? "" : h < 12 ? "AM" : "PM");
        }
        updatePill();

        int adhkar = Widgets.adhkarMode(now, today);
        if (adhkar != shownAdhkar) {
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
        adhanBar.setProgress((nowSec - prev) / (float) Math.max(1, target - prev));
        adhanBar.setColor(left <= 15 * 60 ? 0xB3F1D58A : 0x8CFFFFFF);
        String cd = String.format(Locale.US, "%d:%02d:%02d", left / 3600, left % 3600 / 60, left % 60);
        adhanCountdown.setText(cd);
        adhanCountdown.setTextColor(left <= 15 * 60 ? 0xFFF1D58A : WHITE); // gold in the last 15 minutes
        if (!ambient && resumed && !anyPanelOpen() && System.currentTimeMillis() - lastInput > AMBIENT_AFTER) enterAmbient();
        if (ambient && !nightDim && System.currentTimeMillis() - lastInput > DIM_AFTER) enterNightDim();
        boolean ramadan = Events.isRamadan(now);
        if (ramadan && next == 4) { adhanName.setText("Iftar"); adhanAr.setText("الإفطار · المغرب"); }
        else if (ramadan && next == 0) { adhanName.setText("Suhoor ends"); adhanAr.setText("الإمساك · الفجر"); }
        else { adhanName.setText(PrayerTimes.EN[next]); adhanAr.setText(PrayerTimes.AR[next]); }
        if (pillTextOverride != null) {
            pillText.setText(pillTextOverride);
            pillText.animate().alpha(1).setDuration(200).start();
            if (nowWidget != null && nowWidget.getVisibility() == View.VISIBLE) nowWidget.animate().alpha(0).setDuration(150).start();
        } else if (pillText.getAlpha() > 0) {
            pillText.animate().alpha(0).setDuration(300).start();
            if (nowWidget != null && nowWidget.getVisibility() == View.VISIBLE) nowWidget.animate().alpha(1).setDuration(300).start();
        }
        int passedMask = 0;
        for (int i = 0; i < 5; i++) if (BAR_PRAYERS[i] != next && today[BAR_PRAYERS[i]] * 60 <= nowSec) passedMask |= 1 << i;
        if (next == shownNext && passedMask == shownPassedMask) return; // only redraw the bar when it changes
        if (shownNext != -2 && quran != null && quran.getPlayWhenReady()) { quran.pause(); drawQuranState(); toast("Paused for the adhan · توقف للأذان"); }
        shownNext = next;
        shownPassedMask = passedMask;
        for (int i = 0; i < 5; i++) {
            boolean isNext = BAR_PRAYERS[i] == next;
            prayerCells[i].setBackground(isNext ? glass(14, GLASS_HI, 0) : null);
            prayerCells[i].setAlpha((passedMask & (1 << i)) != 0 ? 0.55f : 1f);
        }
    }

    private void nextLine(Calendar now) {
        lastLineSwitch = System.currentTimeMillis();
        List<String[]> items = Widgets.items(this, now, today, ayahToday, tempNow, dustNow);
        if (items.isEmpty()) return;
        lineIndex = (lineIndex + 1) % items.size();
        String[] it = items.get(lineIndex); // {kicker, ar, en, ref}
        lineBox.animate().alpha(0).translationY(8).setDuration(400).withEndAction(() -> {
            lineKicker.setText(it[0] + (it[3].isEmpty() ? "" : "   ·   " + it[3]));
            lineAr.setText(it[1]);
            lineAr.setTextSize(TypedValue.COMPLEX_UNIT_PX, it[1].length() > 110 ? 32 : 38);
            lineEn.setTextSize(TypedValue.COMPLEX_UNIT_PX, it[2].length() > 150 ? 28 : 32);
            lineAr.setVisibility(it[1].isEmpty() ? View.GONE : View.VISIBLE);
            lineEn.setText(it[2]);
            lineBox.setTranslationY(-8);
            lineBox.animate().alpha(1).translationY(0).setDuration(600).start();
            if (lineTimer != null) lineTimer.cancel();
            lineTimer = ValueAnimator.ofFloat(0, 1);
            lineTimer.setDuration(LINE_SECONDS * 1000 - 1000);
            lineTimer.setInterpolator(null);
            lineTimer.addUpdateListener(v -> lineBar.setProgress((float) v.getAnimatedValue()));
            lineTimer.start();
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
        bg.execute(() -> {
            try {
                JSONObject all = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=29.3759&longitude=47.9774&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=Asia%2FKuwait"));
                try {
                    JSONObject daily = all.getJSONObject("daily");
                    hiLo = "↑ " + Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)) + "°   ↓ "
                            + Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)) + "°";
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
                ui.post(() -> { tempText.setText(t + "°"); skyText.setText(sky); drawDayLine(); });
            } catch (Exception e) {
                lastWeather = System.currentTimeMillis() - 25 * 60 * 1000; // try again in 5 minutes
            }
        });
    }

    /** Bottom of the weather card: today's high and low, and sunrise (الشروق). */
    private void drawDayLine() {
        if (today == null) return;
        String sun = "Sunrise " + PrayerTimes.clock(today[1]) + "  ·  الشروق";
        dayText.setText(hiLo.isEmpty() ? sun : hiLo + "      " + sun);
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
        bg.execute(() -> {
            for (int attempt = 0; attempt < 12; attempt++) {
                int n = (int) (((long) (seed + attempt) * 7919L) % 6236) + 1;
                try {
                    JSONObject d = new JSONObject(get("https://api.alquran.cloud/v1/ayah/" + n + "/editions/quran-uthmani,en.sahih"));
                    JSONArray arr = d.getJSONArray("data");
                    JSONObject ar = arr.getJSONObject(0), en = arr.getJSONObject(1);
                    String text = ar.getString("text");
                    if (text.length() > 120 && attempt < 11) continue;
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

    // ================================================================== dock

    private void loadDockIcons() {
        final PackageManager pm = getPackageManager();
        bg.execute(() -> {
            for (FrameLayout tile : dockTiles) {
                Dock d = (Dock) tile.getTag();
                Drawable icon = null;
                for (String p : d.pkgs) {
                    try { icon = pm.getApplicationIcon(p); break; } catch (Exception ignored) { }
                }
                if (icon == null && d.pkgs.length > 0) {
                    String pkg = findByName(dockSearchName(d));
                    if (pkg != null) try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) { }
                }
                final Drawable ic = icon == null ? null : fullBleed(icon);
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
        return new BitmapDrawable(getResources(), b);
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

    private FrameLayout overlay() {
        FrameLayout o = new FrameLayout(this);
        o.setBackgroundColor(0x99000000);
        o.setVisibility(View.GONE);
        o.setElevation(30);
        stage.addView(o, new FrameLayout.LayoutParams(1920, 1080));
        return o;
    }

    private void show(View v) {
        v.setAlpha(0);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1).setDuration(220).start();
        for (View p : new View[]{centerGroup, bottomGroup, lineFrame, topParts[0], topParts[1]}) p.animate().alpha(0.12f).setDuration(220).start();
    }

    private void closePanels() {
        boolean wasOpen = pinPanel.getVisibility() == View.VISIBLE || appsPanel.getVisibility() == View.VISIBLE
                || quranPanel.getVisibility() == View.VISIBLE || parentPanel.getVisibility() == View.VISIBLE;
        View back = dockTiles.get(dockIndex(quranPanel.getVisibility() == View.VISIBLE ? "quran"
                : pinPanel.getVisibility() == View.VISIBLE ? "yt" : appsPanel.getVisibility() == View.VISIBLE ? "apps" : DOCK[lastDock].id));
        for (View v : new View[]{pinPanel, appsPanel, quranPanel, parentPanel}) {
            if (v.getVisibility() == View.VISIBLE)
                v.animate().alpha(0).setDuration(180).withEndAction(() -> v.setVisibility(View.GONE)).start();
        }
        for (View p : new View[]{centerGroup, bottomGroup, lineFrame, topParts[0], topParts[1]}) p.animate().alpha(1).setDuration(250).start();
        if (wasOpen) ui.postDelayed(back::requestFocus, 200);
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
        box.addView(k);
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
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(22, 22);
            p.leftMargin = 16; p.rightMargin = 16;
            dots.addView(dot, p);
            pinDots[i] = dot;
        }
        box.addView(dots, new LinearLayout.LayoutParams(-1, 30));

        GridLayout keys = new GridLayout(this);
        keys.setColumnCount(3);
        keys.setPadding(0, 30, 0, 0);
        keys.setClipChildren(false);
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "✕", "0", "⌫"};
        for (String label : labels) {
            boolean digit = Character.isDigit(label.charAt(0));
            TextView key = text(label, digit ? 40 : 32, fSemi, digit ? WHITE : SOFT);
            key.setGravity(Gravity.CENTER);
            key.setFocusable(true);
            key.setClickable(true);
            GradientDrawable off = new GradientDrawable();
            off.setShape(GradientDrawable.OVAL);
            off.setColor(digit ? 0x1FFFFFFF : 0x0DFFFFFF);
            off.setStroke(2, 0x2EFFFFFF);
            GradientDrawable on = new GradientDrawable();
            on.setShape(GradientDrawable.OVAL);
            on.setColor(0x4DFFFFFF);
            on.setStroke(3, 0xE6FFFFFF);
            key.setBackground(off);
            key.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? on : off);
                v.animate().scaleX(has ? 1.12f : 1f).scaleY(has ? 1.12f : 1f).setDuration(200).setInterpolator(EASE).withLayer().start();
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
            if (label.equals("1")) firstPinKey = key;
        }
        box.addView(keys);
        pinBox = glassCard(pinPanel, box, 600, -1, Gravity.CENTER, 0, 0, 44, 40, 0x59FFFFFF, 0x59000000, false);
    }

    private String savedPin() { return prefs.getString("pin", ""); }

    private void openPin(boolean change) {
        pinLaunchAfter = !change;
        pinThen = null;
        try { pinIcon.setImageDrawable(fullBleed(getPackageManager().getApplicationIcon(DOCK[dockIndex("yt")].pkgs[0]))); }
        catch (Exception e) { pinIcon.setImageDrawable(tileFace(0xFFE62117, 0xFF8A140E)); }
        show(pinPanel);
        pinBox.setScaleX(0.92f); pinBox.setScaleY(0.92f); pinBox.setAlpha(0); pinBox.setTranslationY(24);
        pinBox.animate().scaleX(1).scaleY(1).alpha(1).translationY(0).setDuration(380).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        setPinMode(savedPin().isEmpty() ? "set" : change ? "old" : "check");
        firstPinKey.requestFocus();
    }

    private void setPinMode(String m) {
        pinMode = m;
        pinEntry = "";
        switch (m) {
            case "old": pinTitle.setText("Change the PIN"); pinSub.setText("Enter the current PIN first"); break;
            case "set": pinTitle.setText("Choose a parent PIN"); pinSub.setText("4 digits to open YouTube.\nHold OK on YouTube to change it later."); break;
            case "confirm": pinTitle.setText("Choose a parent PIN"); pinSub.setText("Enter the same 4 digits again"); break;
            default: pinTitle.setText("YouTube is locked"); pinSub.setText("Enter the parent PIN");
        }
        drawDots();
    }

    private int dotColor = WHITE;

    private void drawDots() {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            boolean f = i < pinEntry.length();
            d.setColor(f ? dotColor : 0x14FFFFFF);
            d.setStroke(2, f ? dotColor : 0x80FFFFFF);
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
        ui.postDelayed(() -> { dotColor = WHITE; if (after != null) after.run(); }, 380);
    }

    private void pinPress(String k) {
        if (k.equals("✕")) { closePanels(); return; }
        if (k.equals("⌫")) { if (!pinEntry.isEmpty()) pinEntry = pinEntry.substring(0, pinEntry.length() - 1); drawDots(); return; }
        if (pinEntry.length() >= 4) return;
        pinEntry += k;
        drawDots();
        if (pinEntry.length() < 4) return;
        switch (pinMode) {
            case "check": if (pinEntry.equals(savedPin())) pinDone(); else pinWrong(); break;
            case "old": if (pinEntry.equals(savedPin())) ui.postDelayed(() -> setPinMode("set"), 150); else pinWrong(); break;
            case "set": newPin = pinEntry; ui.postDelayed(() -> setPinMode("confirm"), 150); break;
            case "confirm":
                if (pinEntry.equals(newPin)) { prefs.edit().putString("pin", newPin).apply(); toast("PIN saved"); pinDone(); }
                else { pinWrong(); ui.postDelayed(() -> setPinMode("set"), 400); }
                break;
        }
    }

    private void pinWrong() {
        ValueAnimator a = ValueAnimator.ofFloat(0, -18, 16, -12, 8, -4, 0);
        a.setDuration(420);
        a.addUpdateListener(v -> pinBox.setTranslationX((float) v.getAnimatedValue()));
        a.start();
        flashDots(0xFFFF6B6B, () -> { pinEntry = ""; drawDots(); });
    }

    private void pinDone() {
        flashDots(0xFF5BE38A, () -> {
            Runnable then = pinThen;
            pinThen = null;
            closePanels();
            if (then != null) { ui.postDelayed(then, 260); return; }
            if (pinLaunchAfter && !launchAny(DOCK[dockIndex("yt")].pkgs, "YouTube")) toast("YouTube isn't installed.");
        });
    }

    // ================================================================== All apps / Kids

    private void buildApps() {
        appsPanel = overlay();
        appsTitle = text("All apps", 48, fSemi, WHITE);
        appsPanel.addView(appsTitle, at(-2, -2, Gravity.TOP | Gravity.START, 110, 70));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, 20, 0, 60);
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(8);
        appGrid.setClipChildren(false);
        sv.addView(appGrid);
        appsPanel.addView(sv, at(1920 - 200, 1080 - 160, Gravity.TOP | Gravity.START, 100, 150));
    }

    private void refreshApps() {
        final PackageManager pm = getPackageManager();
        bg.execute(() -> {
            List<AppEntry> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            seen.add(getPackageName());
            for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                    String pkg = r.activityInfo.packageName;
                    if (!seen.add(pkg)) continue;
                    AppEntry a = new AppEntry();
                    a.pkg = pkg;
                    a.label = r.loadLabel(pm).toString();
                    a.icon = r.loadIcon(pm);
                    out.add(a);
                }
            }
            Collections.sort(out, (x, y) -> x.label.compareToIgnoreCase(y.label));
            AppEntry settings = new AppEntry();
            settings.pkg = SETTINGS;
            settings.label = "Settings";
            try { settings.icon = pm.getApplicationIcon("com.android.tv.settings"); } catch (Exception e) { settings.icon = null; }
            out.add(0, settings);
            appCache = out;
        });
    }

    private Set<String> kidPicks() { return new HashSet<>(prefs.getStringSet("kids", new HashSet<>())); }

    private void openApps(String mode) {
        appsMode = mode;
        List<AppEntry> all = appCache;
        if (all == null) { refreshApps(); ui.postDelayed(() -> openApps(mode), 600); return; }
        Set<String> kids = kidPicks();
        appsTitle.setText("All apps   ·   hold OK to add an app to Kids ⭐");
        appGrid.removeAllViews();
        View first = null;
        // Kids picks first, marked with a star.
        List<AppEntry> shown = new ArrayList<>();
        for (AppEntry a : all) if (!HIDDEN_FROM_LIST.contains(a.pkg) && kids.contains(a.pkg)) shown.add(a);
        for (AppEntry a : all) if (!HIDDEN_FROM_LIST.contains(a.pkg) && !kids.contains(a.pkg)) shown.add(a);
        for (AppEntry a : shown) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(10, 20, 10, 14);
            item.setFocusable(true);
            item.setClickable(true);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(a.icon);
            item.addView(icon, new LinearLayout.LayoutParams(96, 96));
            TextView label = text((kids.contains(a.pkg) ? "⭐ " : "") + a.label, 22, fMed, WHITE);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, 12, 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));
            item.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? glass(28, 0x40FFFFFF, 0x99FFFFFF) : null);
                v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(180)
                        .setInterpolator(new DecelerateInterpolator()).start();
            });
            item.setOnClickListener(v -> { if (a.pkg.equals(SETTINGS)) openSettings(); else if (!launch(a.pkg)) toast("Couldn't open " + a.label); });
            if (!a.pkg.equals(SETTINGS)) item.setOnLongClickListener(v -> { toggleKid(a.pkg); return true; });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 196; p.height = 196;
            p.setMargins(8, 8, 8, 8);
            appGrid.addView(item, p);
            if (first == null) first = item;
        }
        show(appsPanel);
        if (first != null) first.requestFocus();
    }

    private void toggleKid(String pkg) {
        Set<String> kids = kidPicks();
        boolean added = kids.add(pkg);
        if (!added) kids.remove(pkg);
        prefs.edit().putStringSet("kids", kids).apply();
        appsTitle.setText(added ? "Added to Kids ⭐" : "Removed from Kids");
        ui.postDelayed(() -> openApps(appsMode), 700);
    }

    static final String SETTINGS = "__settings";

    private void openSettings() {
        Intent i = new Intent(Settings.ACTION_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    // ================================================================== Quran player

    private ExoPlayer quran;
    private int quranSurah, quranErrors;
    private Quran.Reciter quranReciter;
    private TextView quranNow, quranPlayBtn, nowSurah, nowReciter;
    private View nowWidget;
    private final View[] nowBars = new View[3];
    private final ValueAnimator[] barAnims = new ValueAnimator[3];
    private LinearLayout quranControls;
    private GridLayout surahGrid;

    private void buildQuran() {
        quranPanel = overlay();
        TextView title = text("القرآن الكريم   ·   The Holy Quran", 46, fArSemi, WHITE);
        quranPanel.addView(title, at(-2, -2, Gravity.TOP | Gravity.START, 110, 62));
        TextView sub = text("Pick a surah · a different reciter every time", 22, fAr, DIM);
        quranPanel.addView(sub, at(-2, -2, Gravity.TOP | Gravity.START, 112, 126));

        quranControls = new LinearLayout(this);
        quranControls.setGravity(Gravity.CENTER_VERTICAL);
        quranNow = text("", 24, fAr, SOFT);
        quranNow.setPadding(0, 0, 24, 0);
        quranControls.addView(quranNow, new LinearLayout.LayoutParams(440, -2));
        quranPlayBtn = quranButton("Pause", () -> {
            if (quran == null) return;
            quran.setPlayWhenReady(!quran.getPlayWhenReady());
            drawQuranState();
        });
        quranButton("Another reciter", () -> playSurah(quranSurah, true));
        quranButton("Stop", this::stopQuran);
        quranPanel.addView(quranControls, at(-2, -2, Gravity.TOP | Gravity.END, 110, 58));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, 16, 0, 60);
        surahGrid = new GridLayout(this);
        surahGrid.setColumnCount(6);
        surahGrid.setClipChildren(false);
        for (int i = 0; i < 114; i++) {
            final int n = i + 1;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setFocusable(true);
            cell.setClickable(true);
            cell.setBackground(glass(24, 0x1AFFFFFF, 0));
            TextView ar = text(Quran.AR[i], 34, fArSemi, WHITE);
            ar.setGravity(Gravity.CENTER);
            TextView en = text(n + "  ·  " + Quran.EN[i], 22, fMed, DIM);
            en.setPadding(0, 8, 0, 0);
            cell.addView(ar);
            cell.addView(en);
            cell.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? glass(24, 0x4DFFFFFF, 0x99FFFFFF) : glass(24, 0x1AFFFFFF, 0));
                v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(160).setInterpolator(EASE).start();
            });
            cell.setOnClickListener(v -> { quranQuiet = false; playSurah(n, false); });
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 268; gp.height = 118;
            gp.setMargins(9, 9, 9, 9);
            surahGrid.addView(cell, gp);
        }
        sv.addView(surahGrid);
        quranPanel.addView(sv, at(1920 - 200, 1080 - 190, Gravity.TOP | Gravity.START, 100, 180));

        // On the home screen: what is playing, at the bottom of the weather card.
        buildNowReciting();
        drawQuranState();
    }

    private TextView quranButton(String label, Runnable onClick) {
        TextView b = text(label, 24, fSemi, WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(28, 14, 28, 16);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackground(glass(99, 0x1AFFFFFF, GLASS_EDGE));
        b.setOnFocusChangeListener((v, has) -> v.setBackground(glass(99, has ? 0x59FFFFFF : 0x1AFFFFFF, has ? 0xB3FFFFFF : GLASS_EDGE)));
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.leftMargin = 14;
        quranControls.addView(b, p);
        return b;
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
            if (quranQuiet && !quran.getPlayWhenReady()) quran.play(); // carry on where it paused
            return;
        }
        quranQuiet = true;
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
        quranNow.setText("Loading · جارٍ التحميل…");
        bg.execute(() -> {
            // Recitations stored on the box play first (no internet needed); otherwise stream one.
            Quran.cacheDir = getCacheDir();
            List<Quran.Reciter> all = Quran.local(n);
            if (all.isEmpty() || (all.size() == 1 && not != null && all.get(0).server.equals(not.server))) all = Quran.reciters(getCacheDir());
            if (taraweeh && quranQuiet) {
                List<Quran.Reciter> imams = new ArrayList<>();
                for (Quran.Reciter x : all) if (IMAMS.matcher(x.en + " " + x.ar).find() && x.has(n)) imams.add(x);
                if (!imams.isEmpty() && (not == null || imams.size() > 1)) all = imams;
            }
            Quran.Reciter r = Quran.pick(all, n, not, random);
            ui.post(() -> startQuran(n, r));
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
        quran.setVolume(quranQuiet ? QUIET_VOLUME : 1f);
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
        for (int i = 1; i < quranControls.getChildCount(); i++) quranControls.getChildAt(i).setVisibility(active ? View.VISIBLE : View.GONE);
        if (!active) {
            quranNow.setText("");
            showNowReciting(false);
            return;
        }
        String reciter = quranReciter.ar.isEmpty() ? quranReciter.en : quranReciter.ar + "  ·  " + quranReciter.en;
        quranNow.setText("سورة " + Quran.AR[quranSurah - 1] + "  ·  " + Quran.EN[quranSurah - 1] + "\n" + reciter);
        quranPlayBtn.setText(quran.getPlayWhenReady() ? "Pause" : "Play");
        nowSurah.setText("سورة " + Quran.AR[quranSurah - 1] + "   ·   " + Quran.EN[quranSurah - 1]);
        nowReciter.setText(reciter);
        showNowReciting(true);
        animateBars(quran.getPlayWhenReady());
    }

    /** Top centre, between the two cards: which surah is playing and who is reciting, with three softly moving bars. */
    private void buildNowReciting() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(30, 18, 34, 18);
        LinearLayout bars = new LinearLayout(this);
        bars.setGravity(Gravity.BOTTOM);
        for (int i = 0; i < 3; i++) {
            View b = new View(this);
            b.setBackground(glass(3, 0xFFF1D58A, 0));
            b.setPivotY(30);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(6, 30);
            p.rightMargin = 5;
            bars.addView(b, p);
            nowBars[i] = b;
        }
        row.addView(bars, new LinearLayout.LayoutParams(-2, 30));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(20, 0, 0, 0);
        TextView k = kicker(text("NOW RECITING  ·  يُتلى الآن", 20, fSemi, DIM));
        nowSurah = text("", 26, fQuran, WHITE);
        nowSurah.setIncludeFontPadding(true);
        nowReciter = text("", 24, fAr, SOFT);
        nowReciter.setIncludeFontPadding(true);
        nowReciter.setMaxWidth(620); // long names wrap onto a second line instead of being cut
        texts.addView(k);
        texts.addView(nowSurah);
        texts.addView(nowReciter);
        nowBar = new ThinBar(this);
        nowBar.setColor(0x99F1D58A);
        LinearLayout.LayoutParams nbp = new LinearLayout.LayoutParams(-1, 3);
        nbp.topMargin = 8;
        texts.addView(nowBar, nbp);
        row.addView(texts);
        nowWidget = glassCard(stage, row, -1, -1, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 56, 30, 22, 0x33FFFFFF, CARD_TINT, false);
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
    private void animateBars(boolean playing) {
        for (int i = 0; i < 3; i++) {
            View b = nowBars[i];
            if (barAnims[i] != null) { barAnims[i].cancel(); barAnims[i] = null; }
            if (!playing) { b.animate().scaleY(0.3f).setDuration(200).start(); continue; }
            ValueAnimator a = ValueAnimator.ofFloat(0.3f, 1f);
            a.setDuration(520 + i * 170L);
            a.setStartDelay(i * 90L);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.addUpdateListener(v -> b.setScaleY((float) v.getAnimatedValue()));
            a.start();
            barAnims[i] = a;
        }
    }

    // ================================================================== frosted glass backdrop

    /** Every 1.5 s, a tiny copy of the video frame is blurred and shown through the glass panels. */
    private final Runnable grabBackdrop = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            if (videoCover.getAlpha() < 0.5f && player.isPlaying() && video.getHolder().getSurface().isValid()) {
                final Bitmap frame = Bitmap.createBitmap(192, 108, Bitmap.Config.ARGB_8888);
                try {
                    PixelCopy.request(video, frame, result -> {
                        if (result != PixelCopy.SUCCESS) return;
                        blurThread.execute(() -> {
                            Bitmap b = Blur.blur(frame, 5);
                            ui.post(() -> { if (resumed) GlassFrame.setBackdrop(b); });
                        });
                    }, ui);
                } catch (Exception ignored) { }
            } else if (videoCover.getAlpha() >= 0.5f) {
                GlassFrame.setBackdrop(null);
            }
            ui.postDelayed(this, 1500);
        }
    };

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
        bg.execute(() -> {
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
                    driveText.setText("●  " + found);
                    driveText.setTextColor(0xCC9BE7B4);
                    driveText.setVisibility(View.VISIBLE);
                } else if (prefs.getBoolean("drive_seen", false)) {
                    driveText.setText("●  Drive unplugged");
                    driveText.setTextColor(0xFFFF9A8F);
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
        for (View v : new View[]{pinPanel, appsPanel, quranPanel, parentPanel}) if (v != null && v.getVisibility() == View.VISIBLE) return true;
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
            if (savedPin().isEmpty()) { openParent(); return; }
            openPin(false);
            pinLaunchAfter = false;
            pinThen = this::openParent;
            pinTitle.setText("Parent settings");
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
        box.addView(parentList, new LinearLayout.LayoutParams(-1, -2));
        glassCard(parentPanel, box, 900, -1, Gravity.CENTER, 0, 0, 44, 40, 0x59FFFFFF, 0x59000000, false);
    }

    private void openParent() {
        drawParent(0);
        show(parentPanel);
        View box = parentPanel.getChildAt(0);
        box.setScaleX(0.94f); box.setScaleY(0.94f); box.setAlpha(0);
        box.animate().scaleX(1).scaleY(1).alpha(1).setDuration(340).setInterpolator(new DecelerateInterpolator(2.2f)).start();
    }

    private void drawParent(int focus) {
        parentList.removeAllViews();
        int bright = prefs.getInt("brightness", 100);
        parentRow("This week's family stats", "›", this::drawStats);
        parentRow("Change the YouTube PIN", "›", () -> openPin(true));
        parentRow("Play a different wallpaper", "›", () -> { closePanels(); playRandomClip(); });
        parentRow("Wallpaper brightness", bright + "%", () -> {
            int next = bright == 100 ? 85 : bright == 85 ? 70 : 100;
            prefs.edit().putInt("brightness", next).apply();
            dimView.animate().alpha(1 - next / 100f).setDuration(300).start();
            drawParent(3);
        });
        boolean verse = prefs.getBoolean("verse", true);
        parentRow("Verse and hadith card", verse ? "Shown" : "Hidden", () -> {
            prefs.edit().putBoolean("verse", !verse).apply();
            lineFrame.setVisibility(!verse ? View.VISIBLE : View.GONE);
            drawParent(4);
        });
        boolean h24 = prefs.getBoolean("clock24", false);
        parentRow("Clock", h24 ? "24-hour" : "12-hour", () -> {
            prefs.edit().putBoolean("clock24", !h24).apply();
            shownClock = "";
            drawParent(5);
        });
        boolean quiet = prefs.getBoolean("quiet_quran", true);
        parentRow("Soft Quran on the home screen", quiet ? "On" : "Off", () -> {
            prefs.edit().putBoolean("quiet_quran", !quiet).apply();
            if (quiet && quranQuiet) stopQuran(); else if (!quiet) { quietStopped = false; backgroundQuran(); }
            drawParent(6);
        });
        parentRow("Android settings", "›", () -> { closePanels(); openSettings(); });
        View f = parentList.getChildAt(Math.min(focus, parentList.getChildCount() - 1));
        f.post(f::requestFocus);
    }

    /** The weekly family stats page, inside the parent menu: screen time from Kids Corner and the Quran heard this week. */
    private void drawStats() {
        parentList.removeAllViews();
        statHeading("SCREEN TIME · LAST 7 DAYS  ·  وقت الشاشة");
        List<String[]> kids = Stats.kidsWeek(this);
        if (kids == null) statLine("Kids Corner will share its numbers after its next update.", DIM);
        else if (kids.isEmpty()) statLine("No time in Kids Corner this week.", DIM);
        else for (String[] k : kids) {
            String extra = (k[2].isEmpty() ? "" : "  ·  most: " + k[2]) + ("0".equals(k[3]) ? "" : "  ·  ⭐ " + k[3]);
            statLine(k[0] + "   " + Stats.hm(Integer.parseInt(k[1])) + extra, WHITE);
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
            v.setBackground(has ? glass(22, 0x33FFFFFF, 0x80FFFFFF) : null);
            v.animate().scaleX(has ? 1.02f : 1f).scaleY(has ? 1.02f : 1f).setDuration(160).setInterpolator(EASE).start();
        });
        r.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 76);
        p.topMargin = 6;
        parentList.addView(r, p);
    }

    // ================================================================== ambient mode

    /** After two quiet minutes the dock fades away, leaving the clock, the cards, the verse and the video. */
    static final long AMBIENT_AFTER = 2 * 60 * 1000;
    private boolean ambient;
    private long lastInput = System.currentTimeMillis();

    private void enterAmbient() {
        ambient = true;
        bottomGroup.animate().alpha(0).translationY(40).setDuration(1200).setInterpolator(EASE).start();
        dockLabel.animate().alpha(0).setDuration(400).start();
        for (View t : topParts) t.animate().alpha(0.7f).setDuration(1500).start();
        centerGroup.animate().translationY(90).setDuration(1500).setInterpolator(EASE).start();
    }

    /** After fifteen quiet minutes the whole screen dims; the video and the soft Quran keep playing. */
    static final long DIM_AFTER = 15 * 60 * 1000;
    static final float DIM_LEVEL = 0.6f;
    private boolean nightDim;

    private void enterNightDim() {
        nightDim = true;
        float a = Math.max(DIM_LEVEL, 1 - prefs.getInt("brightness", 100) / 100f);
        dimView.animate().alpha(a).setDuration(4000).start();
    }

    private void leaveAmbient() {
        ambient = false;
        if (nightDim) {
            nightDim = false;
            dimView.animate().alpha(1 - prefs.getInt("brightness", 100) / 100f).setDuration(450).start();
        }
        lastInput = System.currentTimeMillis();
        bottomGroup.animate().alpha(1).translationY(0).setDuration(450).setInterpolator(EASE).start();
        lineFrame.animate().alpha(1).setDuration(450).start();
        for (View t : topParts) t.animate().alpha(1).setDuration(450).start();
        centerGroup.animate().translationY(0).setDuration(450).setInterpolator(EASE).start();
        View f = dockTiles.get(lastDock);
        if (f.hasFocus()) showDockLabel(f, DOCK[lastDock].name); else f.requestFocus();
    }
}
