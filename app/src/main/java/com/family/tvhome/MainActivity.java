package com.family.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
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
    static final int WHITE = 0xFFFFFFFF, SOFT = 0xD9FFFFFF, DIM = 0xA6FFFFFF;
    static final int GLASS = 0x29FFFFFF, GLASS_EDGE = 0x47FFFFFF, GLASS_HI = 0x4DFFFFFF, SHADE = 0x8C000000;

    /** The apps in the dock. pkgs are tried in order, then any installed app whose name matches, then the Play Store. */
    static final class Dock {
        final String id, name; final int color; final String[] pkgs;
        Dock(String id, String name, int color, String... pkgs) { this.id = id; this.name = name; this.color = color; this.pkgs = pkgs; }
    }
    static final Dock[] DOCK = {
            new Dock("yt", "YouTube", 0xFFE62117, "com.google.android.youtube.tv", "com.google.android.youtube"),
            new Dock("netflix", "Netflix", 0xFFE50914, "com.netflix.ninja"),
            new Dock("crunchy", "Crunchyroll", 0xFFF47521, "com.crunchyroll.crunchyroid"),
            new Dock("stremio", "Stremio", 0xFF7B5BF5, "com.stremio.one"),
            new Dock("prime", "Prime Video", 0xFF1AA3E8, "com.amazon.amazonvideo.livingroom"),
            new Dock("shahid", "Shahid", 0xFF1FD3A5, "net.mbc.shahidTV", "net.mbc.shahid"),
            new Dock("kodi", "Movies & Shows", 0xFF17B2E7, "org.xbmc.kodi", "org.videolan.vlc"),
            new Dock("browser", "Browser", 0xFF4A8CFF, "com.phlox.tvwebbrowser", "org.mozilla.firefox", "com.android.chrome"),
            new Dock("quran", "Quran · القرآن", 0xFFE8C97A),
            new Dock("apps", "All apps", 0xFF8A8FA8),
    };
    static final Set<String> HIDDEN_FROM_LIST = new HashSet<>(Arrays.asList("com.google.android.youtube.tv", "com.google.android.youtube"));

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private SharedPreferences prefs;
    private Typeface fMed, fSemi, fBold, fAr, fArSemi, fQuran, fSerif;

    private FrameLayout root, stage;
    private SurfaceView video;
    private View videoCover;
    private ExoPlayer player;
    private String currentClip = "";

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
    private int lineIndex = -1;

    private FrameLayout pinPanel, appsPanel, quranPanel;
    private TextView pinTitle, pinSub;
    private final View[] pinDots = new View[4];
    private View firstPinKey;
    private String pinEntry = "", pinMode = "check", newPin = "";
    private boolean pinLaunchAfter;
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

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new SurfaceView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        videoCover = new View(this);
        videoCover.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF1B2A4A, 0xFF0E1426, 0xFF241A3A}));
        root.addView(videoCover, new FrameLayout.LayoutParams(-1, -1));
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

        player = new ExoPlayer.Builder(this).build();
        player.setVideoSurfaceView(video);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(0f);
        player.addListener(new Player.Listener() {
            @Override public void onRenderedFirstFrame() { videoCover.animate().alpha(0).setDuration(700).start(); }
        });

        dockTiles.get(0).requestFocus();
        tick();
        introAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
        playRandomClip();
        ui.postDelayed(() -> { loadDockIcons(); refreshApps(); }, 600);
    }

    @Override
    protected void onPause() {
        player.pause();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        closePanels();
        dockTiles.get(0).requestFocus();
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
        bg.shutdownNow();
        super.onDestroy();
    }

    private void hideBars() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) closePanels();
            return true; // the home screen never closes itself
        }
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
        t.setShadowLayer(px > 60 ? 20 : 6, 0, 2, 0x66000000);
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
    private static Glass card() { return new Glass(34, 28, 0x38FFFFFF, 0x1A000000); }

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

    private TextView adhanName, adhanAr, adhanCountdown;

    /** Top left: weather, then the date and Hijri date. */
    private void buildTop() {
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(34, 26, 40, 26);
        left.setMinimumWidth(440);
        left.setBackground(card());
        LinearLayout wx = new LinearLayout(this);
        wx.setGravity(Gravity.BOTTOM);
        tempText = text("--°", 64, fSemi, WHITE);
        skyText = text("Kuwait City", 24, fMed, SOFT);
        skyText.setPadding(18, 0, 0, 10);
        wx.addView(tempText);
        wx.addView(skyText);
        left.addView(wx);
        dateText = text("", 30, fSemi, WHITE);
        dateText.setPadding(0, 16, 0, 0);
        hijriText = text("", 26, fAr, SOFT);
        hijriText.setPadding(0, 6, 0, 0);
        left.addView(dateText);
        left.addView(hijriText);
        stage.addView(left, at(-2, -2, Gravity.TOP | Gravity.START, 96, 56));

        // Top right: the adhan card.
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 24, 30, 20);
        card.setBackground(card());
        TextView kicker = text("NEXT ADHAN  ·  الأذان القادم", 18, fSemi, DIM);
        kicker.setLetterSpacing(0.14f);
        card.addView(kicker);
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        adhanName = text("", 38, fSemi, WHITE);
        adhanAr = text("", 34, fArSemi, SOFT);
        nameRow.addView(adhanName, new LinearLayout.LayoutParams(0, -2, 1));
        nameRow.addView(adhanAr);
        nameRow.setPadding(0, 10, 0, 0);
        card.addView(nameRow);
        adhanCountdown = text("--:--:--", 52, fSemi, WHITE);
        adhanCountdown.setFontFeatureSettings("tnum");
        adhanCountdown.setPadding(0, 4, 0, 14);
        card.addView(adhanCountdown);
        for (int i = 0; i < 5; i++) {
            int k = BAR_PRAYERS[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(14, 0, 14, 0);
            TextView en = text(PrayerTimes.EN[k], 24, fMed, SOFT);
            TextView ar = text(PrayerTimes.AR[k], 22, fAr, DIM);
            TextView tm = text("", 26, fSemi, WHITE);
            tm.setFontFeatureSettings("tnum");
            tm.setGravity(Gravity.END);
            row.addView(en, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(ar);
            row.addView(tm, new LinearLayout.LayoutParams(110, -2));
            prayerTexts[i][0] = en; prayerTexts[i][1] = ar; prayerTexts[i][2] = tm;
            prayerCells[i] = row;
            card.addView(row, new LinearLayout.LayoutParams(-1, 46));
        }
        stage.addView(card, at(440, -2, Gravity.TOP | Gravity.END, 96, 56));

        topParts = new View[]{left, card};
    }

    private View[] topParts;

    /** Middle: a large, slightly see-through clock and the rotating verse / hadith line. */
    private void buildCenter() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clockText = text("--:--", 260, fSemi, 0xD9FFFFFF);
        clockText.setFontFeatureSettings("tnum");
        clockText.setLetterSpacing(-0.03f);
        clockText.setShadowLayer(40, 0, 4, 0x40000000);
        ampmText = text("", 56, fSemi, 0xA6FFFFFF);
        ampmText.setPadding(16, 0, 0, 46);
        clockRow.addView(clockText);
        clockRow.addView(ampmText);
        c.addView(clockRow, new LinearLayout.LayoutParams(-1, -2));

        stage.addView(c, at(1920, -2, Gravity.TOP | Gravity.START, 0, 268));
        centerGroup = c;

        // The verse / hadith card, low on the screen just above the dock: Amiri for the Arabic, an italic serif for the English.
        lineBox = new LinearLayout(this);
        lineBox.setOrientation(LinearLayout.VERTICAL);
        lineBox.setGravity(Gravity.CENTER_HORIZONTAL);
        lineBox.setPadding(56, 22, 56, 24);
        lineBox.setBackground(card());
        lineKicker = text("", 18, fSemi, DIM);
        lineKicker.setLetterSpacing(0.16f);
        lineAr = text("", 38, fQuran, WHITE);
        lineAr.setIncludeFontPadding(true);
        lineAr.setGravity(Gravity.CENTER);
        lineAr.setMaxLines(2);
        lineAr.setEllipsize(TextUtils.TruncateAt.END);
        lineAr.setMaxWidth(1360);
        lineAr.setPadding(0, 6, 0, 0);
        lineEn = text("", 32, fSerif, SOFT);
        lineEn.setGravity(Gravity.CENTER);
        lineEn.setMaxLines(2);
        lineEn.setEllipsize(TextUtils.TruncateAt.END);
        lineEn.setMaxWidth(1360);
        lineEn.setLineSpacing(0, 1.05f);
        lineEn.setPadding(0, 2, 0, 0);
        lineBox.addView(lineKicker);
        lineBox.addView(lineAr);
        lineBox.addView(lineEn);
        stage.addView(lineBox, at(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 318));

        // A small message pill, used for short notices like "PIN saved".
        pillText = text("", 28, fMed, WHITE);
        pillText.setGravity(Gravity.CENTER);
        pillText.setPadding(34, 14, 34, 16);
        pillText.setBackground(new Glass(40, 22, 0x40FFFFFF, 0x33000000));
        pillText.setAlpha(0);
        stage.addView(pillText, at(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 60));
    }

    /** Bottom: the app dock, centred. Moving across it magnifies the focused app and, a little, its neighbours. */
    private void buildBottom() {
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(30, 0, 30, 0);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setBackground(new Glass(48, 34, 0x4DFFFFFF, 0x26000000));
        ViewOutlineProvider rounded = new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 26); }
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
                mark = text("القرآن", 36, fQuran, 0xFFF1D58A);
                mark.setIncludeFontPadding(true);
                mark.setBackground(tileFace(0xFF14504A, 0xFF0A2A27));
            } else if (d.id.equals("apps")) {
                mark = text("⋯", 48, fBold, WHITE);
                mark.setBackground(new Glass(26, 0, 0, 0x33000000));
            } else {
                mark = text(d.name.substring(0, 1), 52, fBold, WHITE);
                mark.setBackground(tileFace(d.color, darker(d.color)));
            }
            mark.setGravity(Gravity.CENTER);
            tile.addView(mark, new FrameLayout.LayoutParams(-1, -1));
            tile.setPivotY(112);
            if (d.id.equals("yt")) tile.setOnLongClickListener(v -> { openPin(true); return true; });
            tile.setOnClickListener(v -> openDock(d));
            tile.setOnFocusChangeListener((v, has) -> {
                if (has) { magnify(dockTiles.indexOf(v)); showDockLabel(v, d.name); }
                else if (!anyDockFocused()) { magnify(-1); dockLabel.animate().alpha(0).setDuration(150).start(); }
            });
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(112, 112);
            tp.leftMargin = 12; tp.rightMargin = 12;
            dock.addView(tile, tp);
            dockTiles.add(tile);
            tileFocus.add(new Drawable[]{new Glass(26, 26, 0xB3FFFFFF, 0), ring(26, 3, 0xE6FFFFFF)});
        }
        stage.addView(dock, at(-2, 156, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 56));
        bottomGroup = dock;

        dockLabel = text("", 24, fSemi, WHITE);
        dockLabel.setPadding(22, 8, 22, 10);
        dockLabel.setBackground(new Glass(24, 18, 0x40FFFFFF, 0x33000000));
        dockLabel.setAlpha(0);
        stage.addView(dockLabel, at(-2, -2, Gravity.TOP | Gravity.START, 0, 0));
    }

    private static GradientDrawable tileFace(int top, int bottom) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(26);
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
            float s = dist == 0 ? 1.3f : dist == 1 ? 1.1f : 1f;
            float lift = dist == 0 ? -22 : dist == 1 ? -6 : 0;
            t.animate().scaleX(s).scaleY(s).translationY(lift).setDuration(240).setInterpolator(EASE).withLayer().start();
            t.setBackground(dist == 0 ? tileFocus.get(i)[0] : null);
            t.setForeground(dist == 0 ? tileFocus.get(i)[1] : null);
        }
    }

    private void showDockLabel(View tile, String name) {
        dockLabel.setText(name);
        dockLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] a = new int[2], s = new int[2];
        tile.getLocationOnScreen(a);
        stage.getLocationOnScreen(s);
        float sc = stage.getScaleX();
        float cx = (a[0] - s[0]) / sc + tile.getWidth() * tile.getScaleX() / 2f;
        dockLabel.animate().cancel();
        dockLabel.setX(cx - dockLabel.getMeasuredWidth() / 2f);
        dockLabel.setY(1080 - 56 - 156 - 96);
        dockLabel.setAlpha(0);
        dockLabel.setTranslationY(10);
        dockLabel.animate().alpha(1).translationY(0).setDuration(220).setInterpolator(EASE).start();
    }

    private void introAnimation() {
        View[] parts = {topParts[0], topParts[1], centerGroup, lineBox, bottomGroup};
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
        if (files != null) for (File f : files) if (f.canRead()) clips.add(Uri.fromFile(f));
        if (clips.isEmpty()) { videoCover.setAlpha(1); player.stop(); return; } // no clips yet: the calm gradient below
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
        String clock = ((h + 11) % 12 + 1) + ":" + (mi < 10 ? "0" : "") + mi;
        if (!clock.equals(shownClock)) {
            shownClock = clock;
            clockText.setText(clock);
            ampmText.setText(h < 12 ? "AM" : "PM");
        }
        updatePill();

        if (System.currentTimeMillis() - lastLineSwitch > 15000) nextLine(now);
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
        String cd = String.format(Locale.US, "%d:%02d:%02d", left / 3600, left % 3600 / 60, left % 60);
        adhanCountdown.setText(cd);
        adhanName.setText(PrayerTimes.EN[next]);
        adhanAr.setText(PrayerTimes.AR[next]);
        if (pillTextOverride != null) { pillText.setText(pillTextOverride); pillText.animate().alpha(1).setDuration(200).start(); }
        else if (pillText.getAlpha() > 0) pillText.animate().alpha(0).setDuration(300).start();
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
            lineAr.setVisibility(it[1].isEmpty() ? View.GONE : View.VISIBLE);
            lineEn.setText(it[2]);
            lineBox.setTranslationY(-8);
            lineBox.animate().alpha(1).translationY(0).setDuration(600).start();
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
                JSONObject cur = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=29.3759&longitude=47.9774&current=temperature_2m,weather_code,is_day&timezone=Asia%2FKuwait")).getJSONObject("current");
                tempNow = cur.getDouble("temperature_2m");
                int code = cur.getInt("weather_code");
                try {
                    dustNow = new JSONObject(get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=29.3759&longitude=47.9774&current=dust"))
                            .getJSONObject("current").getDouble("dust");
                } catch (Exception ignored) { }
                String sky = !Double.isNaN(dustNow) && dustNow > 150 ? "Dusty · غبار" : skyName(code);
                long t = Math.round(tempNow);
                ui.post(() -> { tempText.setText(t + "°"); skyText.setText(sky); });
            } catch (Exception e) {
                lastWeather = System.currentTimeMillis() - 25 * 60 * 1000; // try again in 5 minutes
            }
        });
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
            c.drawColor(0x33FFFFFF);
            int pad = size / 10;
            icon.setBounds(pad, pad, size - pad, size - pad);
            icon.draw(c);
        }
        return new BitmapDrawable(getResources(), b);
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
        for (View p : new View[]{centerGroup, bottomGroup, lineBox, topParts[0], topParts[1]}) p.animate().alpha(0.12f).setDuration(220).start();
    }

    private void closePanels() {
        boolean wasOpen = pinPanel.getVisibility() == View.VISIBLE || appsPanel.getVisibility() == View.VISIBLE
                || quranPanel.getVisibility() == View.VISIBLE;
        View back = quranPanel.getVisibility() == View.VISIBLE ? dockTiles.get(DOCK.length - 2) : dockTiles.get(0);
        for (View v : new View[]{pinPanel, appsPanel, quranPanel}) {
            if (v.getVisibility() == View.VISIBLE)
                v.animate().alpha(0).setDuration(180).withEndAction(() -> v.setVisibility(View.GONE)).start();
        }
        for (View p : new View[]{centerGroup, bottomGroup, lineBox, topParts[0], topParts[1]}) p.animate().alpha(1).setDuration(250).start();
        if (wasOpen) ui.postDelayed(back::requestFocus, 200);
    }

    // ================================================================== YouTube PIN

    private void buildPin() {
        pinPanel = overlay();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(60, 44, 60, 44);
        box.setBackground(new Glass(40, 30, 0x4DFFFFFF, 0x40000000));
        pinTitle = text("", 48, fSemi, WHITE);
        pinTitle.setGravity(Gravity.CENTER);
        box.addView(pinTitle);
        pinSub = text("", 26, fMed, SOFT);
        pinSub.setGravity(Gravity.CENTER);
        pinSub.setPadding(0, 10, 0, 26);
        box.addView(pinSub, new LinearLayout.LayoutParams(520, -2));
        LinearLayout dots = new LinearLayout(this);
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(28, 28);
            p.leftMargin = 14; p.rightMargin = 14;
            dots.addView(dot, p);
            pinDots[i] = dot;
        }
        box.addView(dots);
        GridLayout keys = new GridLayout(this);
        keys.setColumnCount(3);
        keys.setPadding(0, 30, 0, 0);
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "✕", "0", "⌫"};
        for (String k : labels) {
            TextView key = text(k, 40, fSemi, WHITE);
            key.setGravity(Gravity.CENTER);
            key.setFocusable(true);
            key.setClickable(true);
            key.setBackground(glass(99, 0x1AFFFFFF, 0));
            key.setOnFocusChangeListener((v, has) -> {
                v.setBackground(glass(99, has ? 0x59FFFFFF : 0x1AFFFFFF, has ? 0xB3FFFFFF : 0));
                v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).setDuration(150).start();
            });
            key.setOnClickListener(v -> pinPress(k));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 104; p.height = 104;
            p.setMargins(14, 10, 14, 10);
            keys.addView(key, p);
            if (k.equals("1")) firstPinKey = key;
        }
        box.addView(keys);
        pinPanel.addView(box, new FrameLayout.LayoutParams(620, -2, Gravity.CENTER));
    }

    private String savedPin() { return prefs.getString("pin", ""); }

    private void openPin(boolean change) {
        pinLaunchAfter = !change;
        show(pinPanel);
        setPinMode(savedPin().isEmpty() ? "set" : change ? "old" : "check");
        firstPinKey.requestFocus();
    }

    private void setPinMode(String m) {
        pinMode = m;
        pinEntry = "";
        switch (m) {
            case "old": pinTitle.setText("Change the PIN"); pinSub.setText("Enter the current PIN first"); break;
            case "set": pinTitle.setText("Choose a parent PIN"); pinSub.setText("4 digits. You'll need it to open YouTube. Hold OK on YouTube to change it."); break;
            case "confirm": pinTitle.setText("Choose a parent PIN"); pinSub.setText("Enter the same 4 digits again"); break;
            default: pinTitle.setText("YouTube is locked"); pinSub.setText("Enter the parent PIN");
        }
        drawDots();
    }

    private void drawDots() {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            boolean f = i < pinEntry.length();
            d.setColor(f ? WHITE : Color.TRANSPARENT);
            d.setStroke(3, WHITE);
            pinDots[i].setBackground(d);
        }
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
        View box = pinPanel.getChildAt(0);
        ValueAnimator a = ValueAnimator.ofFloat(0, -16, 16, -10, 10, 0);
        a.setDuration(360);
        a.addUpdateListener(v -> box.setTranslationX((float) v.getAnimatedValue()));
        a.start();
        ui.postDelayed(() -> { pinEntry = ""; drawDots(); }, 200);
    }

    private void pinDone() {
        closePanels();
        if (pinLaunchAfter && !launchAny(DOCK[0].pkgs, "YouTube")) toast("YouTube isn't installed.");
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
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
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
    private TextView quranNow, quranNowHome, quranPlayBtn;
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
        quranNow.setMaxLines(2);
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
            TextView en = text(n + "  ·  " + Quran.EN[i], 20, fMed, DIM);
            en.setPadding(0, 8, 0, 0);
            cell.addView(ar);
            cell.addView(en);
            cell.setOnFocusChangeListener((v, has) -> {
                v.setBackground(has ? glass(24, 0x4DFFFFFF, 0x99FFFFFF) : glass(24, 0x1AFFFFFF, 0));
                v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(160).setInterpolator(EASE).start();
            });
            cell.setOnClickListener(v -> playSurah(n, false));
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 268; gp.height = 118;
            gp.setMargins(9, 9, 9, 9);
            surahGrid.addView(cell, gp);
        }
        sv.addView(surahGrid);
        quranPanel.addView(sv, at(1920 - 200, 1080 - 190, Gravity.TOP | Gravity.START, 100, 180));

        // On the home screen: what is playing, at the bottom of the weather card.
        quranNowHome = text("", 22, fAr, 0xFFE8C97A);
        quranNowHome.setPadding(0, 16, 0, 0);
        quranNowHome.setVisibility(View.GONE);
        ((LinearLayout) topParts[0]).addView(quranNowHome);
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
    private void playSurah(int n, boolean anotherReciter) {
        if (n <= 0) return;
        if (!anotherReciter) quranErrors = 0;
        final Quran.Reciter not = anotherReciter ? quranReciter : null;
        quranNow.setText("Loading · جارٍ التحميل…");
        bg.execute(() -> {
            List<Quran.Reciter> all = Quran.reciters(getCacheDir());
            Quran.Reciter r = Quran.pick(all, not, random);
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
                    if (state == Player.STATE_ENDED) { quranSurah = 0; drawQuranState(); }
                }
                @Override public void onPlayerError(PlaybackException e) {
                    if (++quranErrors > 3) { toast("The Quran couldn't load. Check the internet."); stopQuran(); return; }
                    toast("That recitation didn't load. Trying another reciter…");
                    playSurah(quranSurah, true);
                }
            });
        }
        quranSurah = n;
        quranReciter = r;
        quran.setMediaItem(MediaItem.fromUri(r.url(n)));
        quran.prepare();
        quran.play();
        drawQuranState();
    }

    private void stopQuran() {
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
            quranNowHome.setVisibility(View.GONE);
            return;
        }
        String reciter = quranReciter.ar.isEmpty() ? quranReciter.en : quranReciter.ar + "  ·  " + quranReciter.en;
        quranNow.setText("سورة " + Quran.AR[quranSurah - 1] + "  ·  " + Quran.EN[quranSurah - 1] + "\n" + reciter);
        quranPlayBtn.setText(quran.getPlayWhenReady() ? "Pause" : "Play");
        quranNowHome.setText((quran.getPlayWhenReady() ? "▶  " : "❚❚  ") + "سورة " + Quran.AR[quranSurah - 1] + "  ·  " + quranReciter.en);
        quranNowHome.setVisibility(View.VISIBLE);
    }
}
