package com.family.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.icu.util.ULocale;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Family TV Home: a native Android TV home screen.
 * Everything is laid out on a 1920x1080 "stage" that is scaled to the screen, so the numbers below are TV pixels.
 */
public class MainActivity extends Activity {
    // ---- worlds ----
    static final String[] THEME_IDS = {"power", "hills", "loop", "dream", "blocks"};
    static final String[] THEME_NAMES = {"Power Up", "Mushroom Hills", "Speed Loop", "Dream Sky", "Block World"};
    static final int[] THEME_ACCENT = {0xFFFFB22E, 0xFFFFD23A, 0xFFFFD700, 0xFFFF8FC8, 0xFF7DDC4A};
    static final int[] THEME_VIDEO = {R.raw.power, R.raw.hills, R.raw.loop, R.raw.dream, R.raw.blocks};

    // ---- the apps on the top row ----
    static final class Hub {
        final String id, name, sub, mark; final int markColor, c1, c2; final String[] pkgs; final boolean banner;
        Hub(String id, String name, String sub, String mark, int markColor, int c1, int c2, boolean banner, String... pkgs) {
            this.id = id; this.name = name; this.sub = sub; this.mark = mark; this.markColor = markColor;
            this.c1 = c1; this.c2 = c2; this.banner = banner; this.pkgs = pkgs;
        }
    }
    static final Hub[] HUBS = {
            new Hub("yt", "YouTube", "Parent PIN", "▶", 0xFFFFFFFF, 0xFFE62117, 0xFF8A0C0C, true, "com.google.android.youtube.tv", "com.google.android.youtube"),
            new Hub("netflix", "Netflix", "Series & films", "N", 0xFFE50914, 0xFF2A0A0C, 0xFF000000, true, "com.netflix.ninja"),
            new Hub("crunchy", "Crunchyroll", "Anime", "C", 0xFFFFFFFF, 0xFFFF8A1F, 0xFFF15A00, true, "com.crunchyroll.crunchyroid"),
            new Hub("stremio", "Stremio", "Streams", "S", 0xFFFFFFFF, 0xFF7B5BF5, 0xFF3A1F9E, true, "com.stremio.one"),
            new Hub("prime", "Prime Video", "Amazon", "P", 0xFFFFFFFF, 0xFF1AA3E8, 0xFF0A3C66, true, "com.amazon.amazonvideo.livingroom"),
            new Hub("shahid", "Shahid", "MBC · شاهد", "ش", 0xFFFFFFFF, 0xFF1FD3A5, 0xFF0A6B5A, true, "net.mbc.shahidTV", "net.mbc.shahid"),
            new Hub("movies", "Movies", "Hard drive", "🎬", 0xFFFFFFFF, 0xFF3C5BFF, 0xFF1B237A, false, "org.xbmc.kodi", "org.videolan.vlc"),
            new Hub("shows", "TV Shows", "Hard drive", "📺", 0xFFFFFFFF, 0xFF0FB59A, 0xFF065A54, false, "org.xbmc.kodi", "org.videolan.vlc"),
    };
    static final String[] BROWSERS = {"com.phlox.tvwebbrowser", "org.mozilla.firefox", "com.android.chrome"};
    static final Set<String> HIDDEN_FROM_LIST = new HashSet<>(java.util.Arrays.asList("com.google.android.youtube.tv", "com.google.android.youtube"));

    static final TimeZone KUWAIT = TimeZone.getTimeZone("Asia/Kuwait");
    static final int INK = 0xFF0B0D14, TEXT = 0xFFF6F3FF, MUTED = 0xB8F6F3FF, GLASS = 0xC70A0C16;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private Typeface fMedium, fBold, fHeavy, fMono;

    private FrameLayout root, stage;
    private SurfaceView video;
    private View videoCover;
    private MediaPlayer player;
    private boolean surfaceReady;
    private WatcherView watcher;
    private int themeIndex;
    private int accent;

    private TextView timeText, ampmText, dateText, hijriText, nextEn, nextAr, countdown;
    private final TextView[][] prayerRows = new TextView[6][3];
    private final LinearLayout[] prayerRowViews = new LinearLayout[6];
    private int[] today, tomorrow;
    private String cachedDay = "";

    private HorizontalScrollView tileScroll;
    private final List<FrameLayout> tiles = new ArrayList<>();
    private final List<TextView> themeChips = new ArrayList<>();
    private TextView toastView;

    private FrameLayout pinPanel, appsPanel;
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

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        fMedium = getResources().getFont(R.font.baloo_medium);
        fBold = getResources().getFont(R.font.baloo_bold);
        fHeavy = getResources().getFont(R.font.baloo_extrabold);
        fMono = getResources().getFont(R.font.chivo_mono_bold);

        root = new FrameLayout(this);
        root.setBackgroundColor(INK);
        video = new SurfaceView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        videoCover = new View(this);
        videoCover.setBackgroundColor(Color.BLACK);
        root.addView(videoCover, new FrameLayout.LayoutParams(-1, -1));
        watcher = new WatcherView(this);
        root.addView(watcher, new FrameLayout.LayoutParams(-1, -1));

        stage = new FrameLayout(this);
        stage.setClipChildren(false);
        root.addView(stage, new FrameLayout.LayoutParams(1920, 1080));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            float s = Math.min((r - l) / 1920f, (b - t) / 1080f);
            stage.setPivotX(0); stage.setPivotY(0);
            stage.setScaleX(s); stage.setScaleY(s);
            stage.setTranslationX(((r - l) - 1920 * s) / 2);
            stage.setTranslationY(((b - t) - 1080 * s) / 2);
        });
        setContentView(root);

        buildClock();
        buildAdhan();
        buildTiles();
        buildBottomBar();
        buildToast();
        buildPin();
        buildApps();

        video.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder h) { surfaceReady = true; startVideo(); }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { }
            @Override public void surfaceDestroyed(SurfaceHolder h) { surfaceReady = false; stopVideo(); }
        });

        int saved0 = 0;
        String savedTheme = prefs.getString("theme", "power");
        for (int i = 0; i < THEME_IDS.length; i++) if (THEME_IDS[i].equals(savedTheme)) saved0 = i;
        applyTheme(saved0, false);
        tiles.get(0).requestFocus();
        tick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
        watcher.setRunning(true);
        if (player != null) try { player.start(); } catch (IllegalStateException ignored) { }
        ui.postDelayed(() -> { loadBanners(); refreshApps(); }, 800);
    }

    @Override
    protected void onPause() {
        watcher.setRunning(false);
        if (player != null) try { player.pause(); } catch (IllegalStateException ignored) { }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: close panels and go back to the first tile.
        closePanels();
        tileScroll.smoothScrollTo(0, 0);
        tiles.get(0).requestFocus();
    }

    @Override
    protected void onDestroy() {
        stopVideo();
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

    // ------------------------------------------------------------------ helpers

    private TextView text(String s, float px, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private static FrameLayout.LayoutParams at(int w, int h, int left, int top) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h);
        p.leftMargin = left; p.topMargin = top;
        return p;
    }

    private View glassPanel() {
        View v = new View(this);
        GradientDrawable d = rounded(GLASS, 28);
        d.setStroke(1, 0x24FFFFFF);
        v.setBackground(d);
        return v;
    }

    private void focusGrow(View v, float scale) {
        v.setOnFocusChangeListener((view, has) -> {
            view.animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f).translationZ(has ? 24 : 0)
                    .setDuration(170).setInterpolator(new DecelerateInterpolator()).start();
            Object tag = view.getTag(R.id.focus_ring);
            if (tag instanceof View) ((View) tag).setVisibility(has ? View.VISIBLE : View.INVISIBLE);
            if (has && view.getParent() == tilesRow) watcherLook(view);
            else if (has) watcherLook(view);
        });
    }

    private LinearLayout tilesRow;

    private void watcherLook(View v) {
        int[] a = new int[2], s = new int[2];
        v.getLocationOnScreen(a);
        stage.getLocationOnScreen(s);
        float sc = stage.getScaleX();
        float x = (a[0] - s[0] + v.getWidth() * sc / 2) / sc, y = (a[1] - s[1] + v.getHeight() * sc / 2) / sc;
        // Ask the character to look at the screen position, converted to its own 1920x1080 space.
        float wx = (a[0] + v.getWidth() * sc / 2) / (float) watcher.getWidth() * 1920;
        float wy = (a[1] + v.getHeight() * sc / 2) / (float) watcher.getHeight() * 1080;
        watcher.lookAt(Float.isNaN(wx) ? x : wx, Float.isNaN(wy) ? y : wy);
    }

    private void toast(String msg) {
        toastView.setText(msg);
        toastView.setAlpha(0);
        toastView.setVisibility(View.VISIBLE);
        toastView.animate().alpha(1).setDuration(150).start();
        ui.removeCallbacks(hideToast);
        ui.postDelayed(hideToast, 2600);
    }

    private final Runnable hideToast = () -> toastView.animate().alpha(0).setDuration(200)
            .withEndAction(() -> toastView.setVisibility(View.GONE)).start();

    // ------------------------------------------------------------------ clock & adhan

    private void buildClock() {
        View bgv = glassPanel();
        stage.addView(bgv, at(420, 255, 72, 48));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 26, 40, 22);
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.BOTTOM);
        timeText = text("--:--", 132, fHeavy, TEXT);
        ampmText = text("", 44, fBold, MUTED);
        ampmText.setPadding(12, 0, 0, 14);
        timeRow.addView(timeText);
        timeRow.addView(ampmText);
        box.addView(timeRow);
        dateText = text("", 34, fMedium, MUTED);
        dateText.setPadding(0, 10, 0, 0);
        box.addView(dateText);
        hijriText = text("", 34, fBold, accent);
        hijriText.setTextDirection(View.TEXT_DIRECTION_RTL);
        box.addView(hijriText);
        stage.addView(box, at(420, 255, 72, 48));
    }

    private void buildAdhan() {
        View bgv = glassPanel();
        stage.addView(bgv, at(560, 480, 1920 - 72 - 560, 48));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 20, 32, 18);
        TextView label = text("NEXT ADHAN · KUWAIT CITY", 24, fMedium, MUTED);
        label.setLetterSpacing(0.12f);
        box.addView(label);
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nextEn = text("", 52, fHeavy, TEXT);
        nextAr = text("", 54, fHeavy, accent);
        nameRow.addView(nextEn, new LinearLayout.LayoutParams(0, -2, 1));
        nameRow.addView(nextAr);
        nameRow.setPadding(0, 6, 0, 0);
        box.addView(nameRow);
        countdown = text("--:--:--", 66, fMono, TEXT);
        countdown.setPadding(0, 2, 0, 10);
        box.addView(countdown);
        for (int i = 0; i < 6; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(14, 3, 14, 3);
            TextView en = text(PrayerTimes.EN[i], 28, fMedium, TEXT);
            TextView ar = text(PrayerTimes.AR[i], 28, fMedium, MUTED);
            TextView tm = text("", 28, fMono, TEXT);
            tm.setGravity(Gravity.END);
            row.addView(en, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(ar);
            row.addView(tm, new LinearLayout.LayoutParams(130, -2));
            prayerRows[i][0] = en; prayerRows[i][1] = ar; prayerRows[i][2] = tm;
            prayerRowViews[i] = row;
            box.addView(row, new LinearLayout.LayoutParams(-1, 46));
        }
        stage.addView(box, at(560, 480, 1920 - 72 - 560, 48));
    }

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
            for (int i = 0; i < 6; i++) prayerRows[i][2].setText(PrayerTimes.clock(today[i]));
            SimpleDateFormat f = new SimpleDateFormat("EEEE d MMMM", Locale.UK);
            f.setTimeZone(KUWAIT);
            dateText.setText(f.format(now.getTime()));
            try {
                DateFormat hf = DateFormat.getDateInstance(DateFormat.LONG, new ULocale("ar_SA@calendar=islamic-umalqura"));
                hf.setTimeZone(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
                hijriText.setText(hf.format(now.getTime()));
            } catch (Exception ignored) { }
        }
        int h = now.get(Calendar.HOUR_OF_DAY), mi = now.get(Calendar.MINUTE), s = now.get(Calendar.SECOND);
        timeText.setText(((h + 11) % 12 + 1) + ":" + (mi < 10 ? "0" : "") + mi);
        ampmText.setText(h < 12 ? "AM" : "PM");
        int nowSec = h * 3600 + mi * 60 + s;
        int next = -1;
        for (int i = 0; i < 6; i++) if (i != 1 && today[i] * 60 > nowSec) { next = i; break; }
        int target = next >= 0 ? today[next] * 60 : tomorrow[0] * 60 + 86400;
        if (next < 0) next = 0;
        int left = target - nowSec;
        countdown.setText(String.format(Locale.US, "%02d:%02d:%02d", left / 3600, left % 3600 / 60, left % 60));
        nextEn.setText(PrayerTimes.EN[next]);
        nextAr.setText(PrayerTimes.AR[next]);
        for (int i = 0; i < 6; i++) {
            boolean isNext = i == next;
            boolean passed = !isNext && today[i] * 60 <= nowSec;
            prayerRowViews[i].setBackground(isNext ? rounded(accent, 12) : null);
            prayerRowViews[i].setAlpha(passed ? 0.5f : 1f);
            prayerRows[i][0].setTextColor(isNext ? INK : TEXT);
            prayerRows[i][1].setTextColor(isNext ? INK : MUTED);
            prayerRows[i][2].setTextColor(isNext ? INK : TEXT);
        }
        ui.postDelayed(this::tick, 1000 - (System.currentTimeMillis() % 1000));
    }

    // ------------------------------------------------------------------ tiles

    private void buildTiles() {
        tileScroll = new HorizontalScrollView(this);
        tileScroll.setHorizontalScrollBarEnabled(false);
        tileScroll.setClipChildren(false);
        tileScroll.setClipToPadding(false);
        tileScroll.setSmoothScrollingEnabled(true);
        tileScroll.setFadingEdgeLength(0);
        tilesRow = new LinearLayout(this);
        tilesRow.setClipChildren(false);
        tilesRow.setClipToPadding(false);
        tilesRow.setPadding(72, 26, 72, 26);
        for (Hub hub : HUBS) {
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setClickable(true);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{hub.c1, hub.c2});
            g.setCornerRadius(22);
            tile.setBackground(g);
            tile.setClipToOutline(true);
            tile.setTag(hub);

            TextView mark = text(hub.mark, 52, fHeavy, hub.markColor);
            tile.addView(mark, at(-2, -2, 20, 12));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(hub.name, 30, fHeavy, Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(name);
            labels.addView(text(hub.sub, 20, fMedium, 0xD9FFFFFF));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            lp.leftMargin = 20; lp.rightMargin = 20; lp.bottomMargin = 16;
            tile.addView(labels, lp);

            ImageView banner = new ImageView(this);
            banner.setScaleType(ImageView.ScaleType.FIT_CENTER);
            banner.setBackgroundColor(Color.BLACK);
            banner.setVisibility(View.GONE);
            tile.addView(banner, new FrameLayout.LayoutParams(-1, -1));

            if (hub.id.equals("yt")) {
                TextView lock = text("🔒 PIN", 20, fBold, Color.WHITE);
                lock.setBackground(rounded(0x59000000, 99));
                lock.setPadding(14, 4, 14, 4);
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.TOP);
                p.topMargin = 14; p.rightMargin = 14;
                tile.addView(lock, p);
                tile.setOnLongClickListener(v -> { openPin(true); return true; });
            }

            View ring = new View(this);
            GradientDrawable rd = new GradientDrawable();
            rd.setCornerRadius(22);
            rd.setStroke(6, Color.WHITE);
            ring.setBackground(rd);
            ring.setVisibility(View.INVISIBLE);
            tile.addView(ring, new FrameLayout.LayoutParams(-1, -1));
            tile.setTag(R.id.focus_ring, ring);

            tile.setOnClickListener(v -> openHub(hub));
            focusGrow(tile, 1.1f);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(320, 180);
            tp.rightMargin = 24;
            tilesRow.addView(tile, tp);
            tiles.add(tile);
        }
        tileScroll.addView(tilesRow);
        stage.addView(tileScroll, at(1920, 232, 0, 1080 - 104 - 232));
    }

    private void loadBanners() {
        final PackageManager pm = getPackageManager();
        bg.execute(() -> {
            for (FrameLayout tile : tiles) {
                Hub hub = (Hub) tile.getTag();
                if (!hub.banner) continue;
                Drawable found = null;
                for (String pkg : hub.pkgs) {
                    try {
                        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
                        if (i != null) found = pm.getActivityBanner(i);
                        if (found == null) found = pm.getApplicationBanner(pkg);
                    } catch (Exception ignored) { }
                    if (found != null) break;
                }
                final Drawable d = found;
                ui.post(() -> {
                    ImageView banner = (ImageView) tile.getChildAt(2);
                    if (d == null) { banner.setVisibility(View.GONE); return; }
                    banner.setImageDrawable(d);
                    banner.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void openHub(Hub hub) {
        if (hub.id.equals("yt")) { openPin(false); return; }
        if (launchAny(hub.pkgs, hub.name)) return;
        toast(hub.name + " is not installed yet. Opening the Play Store…");
        ui.postDelayed(() -> openStore(hub.pkgs[0]), 1200);
    }

    private boolean launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return true; } catch (ActivityNotFoundException e) { return false; }
    }

    /** Tries the known package names, then any installed app whose name matches. */
    private boolean launchAny(String[] pkgs, String name) {
        for (String p : pkgs) if (launch(p)) return true;
        List<AppEntry> list = appCache;
        if (list != null) {
            String w = name.toLowerCase(Locale.ROOT);
            for (AppEntry a : list) if (a.label.toLowerCase(Locale.ROOT).contains(w) && launch(a.pkg)) return true;
        }
        return false;
    }

    private void openStore(String pkg) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ bottom bar

    private TextView chip(String label, boolean util) {
        TextView c = text(label, 25, fBold, TEXT);
        c.setPadding(24, 10, 24, 10);
        c.setFocusable(true);
        c.setClickable(true);
        c.setBackground(rounded(util ? 0x33FFFFFF : GLASS, 99));
        View dummy = new View(this);
        c.setTag(R.id.focus_ring, dummy);
        c.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).setDuration(150).setInterpolator(new DecelerateInterpolator()).start();
            restyleChip((TextView) v, has);
            if (has) watcherLook(v);
        });
        return c;
    }

    private void restyleChip(TextView c, boolean focused) {
        Object t = c.getTag();
        boolean on = t instanceof Integer && (Integer) t == themeIndex;
        boolean util = t instanceof String;
        GradientDrawable d = rounded(on ? accent : util ? 0x33FFFFFF : GLASS, 99);
        if (focused) d.setStroke(4, Color.WHITE);
        c.setBackground(d);
        c.setTextColor(on ? INK : TEXT);
    }

    private void buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setClipChildren(false);
        String[][] utils = {{"kids", "⭐ Kids"}, {"browser", "🌐 Browser"}, {"apps", "All apps"}, {"settings", "Settings"}};
        for (String[] u : utils) {
            TextView c = chip(u[1], true);
            c.setTag(u[0]);
            c.setOnClickListener(v -> openUtil(u[0]));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.rightMargin = 18;
            bar.addView(c, p);
        }
        TextView world = text("World", 24, fMedium, MUTED);
        world.setPadding(10, 0, 18, 0);
        bar.addView(world);
        for (int i = 0; i < THEME_IDS.length; i++) {
            final int idx = i;
            TextView c = chip(THEME_NAMES[i], false);
            c.setTag(i);
            c.setOnClickListener(v -> applyTheme(idx, true));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.rightMargin = 18;
            bar.addView(c, p);
            themeChips.add(c);
        }
        stage.addView(bar, at(1920 - 144, 70, 72, 1080 - 44 - 66));
    }

    private void openUtil(String id) {
        switch (id) {
            case "settings": {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { startActivity(i); } catch (Exception ignored) { }
                break;
            }
            case "browser":
                if (!launchAny(BROWSERS, "browser")) {
                    toast("Installing TV Bro, a browser that works with the remote…");
                    ui.postDelayed(() -> openStore(BROWSERS[0]), 1200);
                }
                break;
            default:
                openApps(id);
        }
    }

    // ------------------------------------------------------------------ worlds

    private void applyTheme(int idx, boolean user) {
        themeIndex = idx;
        accent = THEME_ACCENT[idx];
        prefs.edit().putString("theme", THEME_IDS[idx]).apply();
        watcher.setTheme(THEME_IDS[idx]);
        hijriText.setTextColor(accent);
        nextAr.setTextColor(accent);
        for (TextView c : themeChips) restyleChip(c, c.isFocused());
        for (FrameLayout t : tiles) {
            View ring = (View) t.getTag(R.id.focus_ring);
            GradientDrawable rd = new GradientDrawable();
            rd.setCornerRadius(22);
            rd.setStroke(6, accent);
            ring.setBackground(rd);
        }
        if (user) {
            videoCover.animate().alpha(1).setDuration(180).withEndAction(this::startVideo).start();
        }
        if (today != null) tick();
    }

    private void startVideo() {
        stopVideo();
        if (!surfaceReady) return;
        try {
            AssetFileDescriptor fd = getResources().openRawResourceFd(THEME_VIDEO[themeIndex]);
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mp.setDisplay(video.getHolder());
            mp.setLooping(true);
            mp.setVolume(0, 0);
            mp.setOnPreparedListener(p -> p.start());
            mp.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) videoCover.animate().alpha(0).setDuration(300).start();
                return false;
            });
            mp.prepareAsync();
            player = mp;
        } catch (Exception e) {
            videoCover.setAlpha(0);
        }
    }

    private void stopVideo() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    // ------------------------------------------------------------------ toast

    private void buildToast() {
        toastView = text("", 36, fMedium, TEXT);
        toastView.setGravity(Gravity.CENTER);
        toastView.setPadding(46, 26, 46, 30);
        toastView.setMaxWidth(1100);
        GradientDrawable d = rounded(0xE60A0C16, 28);
        d.setStroke(1, 0x24FFFFFF);
        toastView.setBackground(d);
        toastView.setVisibility(View.GONE);
        toastView.setElevation(40);
        stage.addView(toastView, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
    }

    // ------------------------------------------------------------------ YouTube PIN

    private void buildPin() {
        pinPanel = new FrameLayout(this);
        pinPanel.setBackgroundColor(0xC705060C);
        pinPanel.setVisibility(View.GONE);
        pinPanel.setElevation(30);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(60, 44, 60, 44);
        GradientDrawable d = rounded(0xF20E1020, 28);
        d.setStroke(1, 0x24FFFFFF);
        box.setBackground(d);
        pinTitle = text("", 52, fHeavy, TEXT);
        pinTitle.setGravity(Gravity.CENTER);
        box.addView(pinTitle);
        pinSub = text("", 28, fMedium, MUTED);
        pinSub.setGravity(Gravity.CENTER);
        pinSub.setPadding(0, 8, 0, 24);
        box.addView(pinSub, new LinearLayout.LayoutParams(520, -2));
        LinearLayout dots = new LinearLayout(this);
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(34, 34);
            p.leftMargin = 12; p.rightMargin = 12;
            dots.addView(dot, p);
            pinDots[i] = dot;
        }
        box.addView(dots);
        GridLayout keys = new GridLayout(this);
        keys.setColumnCount(3);
        keys.setPadding(0, 26, 0, 0);
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "✕", "0", "⌫"};
        for (String k : labels) {
            TextView key = text(k, 44, fMono, TEXT);
            key.setGravity(Gravity.CENTER);
            key.setFocusable(true);
            key.setClickable(true);
            key.setBackground(keyBg(false));
            key.setOnFocusChangeListener((v, has) -> v.setBackground(keyBg(has)));
            key.setOnClickListener(v -> pinPress(k));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 160; p.height = 76;
            p.setMargins(7, 7, 7, 7);
            keys.addView(key, p);
            if (k.equals("1")) firstPinKey = key;
        }
        box.addView(keys);
        pinPanel.addView(box, new FrameLayout.LayoutParams(640, -2, Gravity.CENTER));
        stage.addView(pinPanel, new FrameLayout.LayoutParams(1920, 1080));
    }

    private GradientDrawable keyBg(boolean focused) {
        GradientDrawable d = rounded(focused ? 0x33FFFFFF : 0x14FFFFFF, 18);
        if (focused) d.setStroke(4, accent);
        return d;
    }

    private String savedPin() { return prefs.getString("pin", ""); }

    private void openPin(boolean change) {
        pinLaunchAfter = !change;
        pinPanel.setVisibility(View.VISIBLE);
        watcher.lookAt(960, 540);
        setPinMode(savedPin().isEmpty() ? "set" : change ? "old" : "check");
        firstPinKey.requestFocus();
    }

    private void setPinMode(String m) {
        pinMode = m;
        pinEntry = "";
        switch (m) {
            case "old": pinTitle.setText("Change the PIN"); pinSub.setText("Enter the current PIN first"); break;
            case "set": pinTitle.setText("Choose a parent PIN"); pinSub.setText("4 digits. You'll need it to open YouTube. Hold OK on the YouTube tile to change it."); break;
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
            d.setColor(f ? accent : Color.TRANSPARENT);
            d.setStroke(4, f ? accent : TEXT);
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
        if (pinLaunchAfter && !launchAny(HUBS[0].pkgs, "YouTube")) toast("YouTube is not installed.");
    }

    // ------------------------------------------------------------------ All apps / Kids

    private void buildApps() {
        appsPanel = new FrameLayout(this);
        appsPanel.setBackgroundColor(0xE605060C);
        appsPanel.setVisibility(View.GONE);
        appsPanel.setElevation(30);
        appsTitle = text("All apps", 52, fHeavy, TEXT);
        appsPanel.addView(appsTitle, at(-2, -2, 90, 60));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, 20, 0, 40);
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(7);
        appGrid.setClipChildren(false);
        sv.addView(appGrid);
        appsPanel.addView(sv, at(1920 - 160, 1080 - 150, 80, 140));
        stage.addView(appsPanel, new FrameLayout.LayoutParams(1920, 1080));
    }

    private void refreshApps() {
        final PackageManager pm = getPackageManager();
        bg.execute(() -> {
            List<AppEntry> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            seen.add(getPackageName());
            for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                List<ResolveInfo> list = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0);
                for (ResolveInfo r : list) {
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
            appCache = out;
        });
    }

    private Set<String> kidPicks() { return new HashSet<>(prefs.getStringSet("kids", new HashSet<>())); }

    private void openApps(String mode) {
        appsMode = mode;
        List<AppEntry> all = appCache;
        if (all == null) { toast("Loading apps…"); refreshApps(); ui.postDelayed(() -> openApps(mode), 700); return; }
        Set<String> kids = kidPicks();
        List<AppEntry> shown = new ArrayList<>();
        for (AppEntry a : all) {
            if (HIDDEN_FROM_LIST.contains(a.pkg)) continue; // YouTube only through the PIN tile
            if (mode.equals("kids") && !kids.contains(a.pkg)) continue;
            shown.add(a);
        }
        if (mode.equals("kids") && shown.isEmpty()) { toast("No Kids apps yet. Open All apps and hold OK on an app to add it."); return; }
        appsTitle.setText(mode.equals("kids") ? "Kids" : "All apps  ·  hold OK to add an app to Kids");
        appGrid.removeAllViews();
        View first = null;
        for (AppEntry a : shown) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(10, 18, 10, 14);
            item.setFocusable(true);
            item.setClickable(true);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(a.icon);
            item.addView(icon, new LinearLayout.LayoutParams(110, 110));
            TextView label = text(a.label + (mode.equals("apps") && kids.contains(a.pkg) ? " ⭐" : ""), 24, fMedium, TEXT);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setPadding(0, 10, 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));
            item.setOnFocusChangeListener((v, has) -> {
                GradientDrawable d = rounded(has ? 0x1FFFFFFF : Color.TRANSPARENT, 22);
                if (has) d.setStroke(4, accent);
                v.setBackground(d);
                v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(140).start();
            });
            item.setOnClickListener(v -> { if (!launch(a.pkg)) toast("Couldn't open " + a.label); });
            if (mode.equals("apps")) item.setOnLongClickListener(v -> { toggleKid(a.pkg); return true; });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 230; p.height = 210;
            p.setMargins(11, 11, 11, 11);
            appGrid.addView(item, p);
            if (first == null) first = item;
        }
        appsPanel.setVisibility(View.VISIBLE);
        watcher.setRunning(false);
        if (first != null) first.requestFocus();
    }

    private void toggleKid(String pkg) {
        Set<String> kids = kidPicks();
        boolean added = kids.add(pkg);
        if (!added) kids.remove(pkg);
        prefs.edit().putStringSet("kids", kids).apply();
        toast(added ? "Added to Kids" : "Removed from Kids");
        View focused = getCurrentFocus();
        int index = focused == null ? 0 : appGrid.indexOfChild(focused);
        openApps(appsMode);
        if (index >= 0 && index < appGrid.getChildCount()) appGrid.getChildAt(index).requestFocus();
    }

    // ------------------------------------------------------------------

    private void closePanels() {
        boolean wasOpen = pinPanel.getVisibility() == View.VISIBLE || appsPanel.getVisibility() == View.VISIBLE;
        pinPanel.setVisibility(View.GONE);
        appsPanel.setVisibility(View.GONE);
        watcher.setRunning(true);
        if (wasOpen) tiles.get(0).requestFocus();
    }

    @SuppressWarnings("unused")
    private static ViewGroup.LayoutParams wrap() { return new ViewGroup.LayoutParams(-2, -2); }
}
