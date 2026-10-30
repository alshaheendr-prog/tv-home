package com.family.kids;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.LruCache;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kids Corner: the children's own home screen. Opening it turns kids mode on; only the parent PIN turns it off.
 * Everything sits on a 1920x1080 stage that is scaled to the screen, so the numbers below are TV pixels.
 */
public class KidsActivity extends Activity {
    static final int WHITE = 0xFFFFFFFF, SOFT = 0xE6FFFFFF, DIM = 0xB3FFFFFF;
    static final String[] VIDEO_EXT = {".mp4", ".mkv", ".m4v", ".webm", ".avi", ".mov", ".ts"};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private Typeface fMed, fBold, fHeavy;

    private FrameLayout root, stage;
    private SurfaceView video;
    private View cover;
    private MediaPlayer player;
    private boolean surfaceReady;
    private Worlds.World world;

    private Mascot mascot;
    private TextView hello, names, worldChip, weatherText, clockText, ampmText, ringBig, ringSmall, ringNote, charName, charFrom, charFact, tileLabel;
    private View labelPill;
    private volatile String weather = "";
    private long lastWeather;
    private ImageView charPhoto;
    private Ring ring;
    private LinearLayout tileRow;
    private View lockTile, setupDot;
    private final List<View> tiles = new ArrayList<>();
    private int lastTile;

    private FrameLayout restPanel, pinPanel, parentPanel, appsPanel, cartoonsPanel;
    private Mascot restMascot;
    private TextView restTitle, restSub;
    private View restButton;

    private List<File> cartoons = new ArrayList<>();
    private final LruCache<String, Bitmap> thumbs = new LruCache<>(48);

    // ================================================================== lifecycle

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        fMed = getResources().getFont(R.font.baloo_medium);
        fBold = getResources().getFont(R.font.baloo_bold);
        fHeavy = getResources().getFont(R.font.baloo_extrabold);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new SurfaceView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        cover = new View(this);
        root.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xA6000000, 0x1A000000, 0x1A000000, 0x73000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
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

        video.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder h) { surfaceReady = true; playClip(); }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { }
            @Override public void surfaceDestroyed(SurfaceHolder h) { surfaceReady = false; stopClip(); }
        });

        buildTop();
        buildMiddle();
        buildTiles();
        buildRest();
        buildCartoons();
        buildApps();
        buildParent();
        buildPin();

        if (Rules.pin(this).isEmpty()) {
            // First time: the parent picks a PIN, then kids mode starts.
            askPin("set", () -> { Rules.setOn(this, true); Guard.start(this); closePanels(); });
        } else {
            Rules.setOn(this, true);
            Guard.start(this);
        }
        if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES}, 1);
        tick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
        applyWorld();
        refreshTiles();
        loadCartoons();
        if (Rules.on(this)) Guard.start(this);
        tick();
    }

    @Override
    protected void onPause() {
        if (player != null) player.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        // Another app (Netflix, a cartoon) needs the video decoder: let ours go until we come back.
        stopClip();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        video.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        stopClip();
        ui.removeCallbacksAndMessages(null);
        bg.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        applyWorld();
        loadCartoons();
    }

    private void hideBars() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) back();
            return true; // Kids Corner never closes itself
        }
        if (pinPanel.getVisibility() == View.VISIBLE && e.getAction() == KeyEvent.ACTION_DOWN) {
            if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) { pinPress(String.valueOf(code - KeyEvent.KEYCODE_0)); return true; }
            if (code == KeyEvent.KEYCODE_DEL) { pinPress("⌫"); return true; }
        }
        return super.dispatchKeyEvent(e);
    }

    private void back() {
        if (pinPanel.getVisibility() == View.VISIBLE) {
            if (Rules.pin(this).isEmpty()) return; // the first PIN must be chosen
            hide(pinPanel);
            focusAfterPanel();
        } else if (appsPanel.getVisibility() == View.VISIBLE) {
            hide(appsPanel);
            refreshTiles();
            parentFirst.requestFocus();
        } else if (parentPanel.getVisibility() == View.VISIBLE) {
            hide(parentPanel);
            focusAfterPanel();
        } else if (cartoonsPanel.getVisibility() == View.VISIBLE) {
            hide(cartoonsPanel);
            focusAfterPanel();
        }
    }

    private void focusAfterPanel() {
        if (parentPanel.getVisibility() == View.VISIBLE) parentFirst.requestFocus();
        else if (restPanel.getVisibility() == View.VISIBLE) restButton.requestFocus();
        else if (!tiles.isEmpty()) tiles.get(Math.min(lastTile, tiles.size() - 1)).requestFocus();
    }

    /** Once a second: the time ring, the greeting, and the rest screen when time is up. */
    private void tick() {
        ui.removeCallbacks(tickRunnable);
        updateRing();
        String rest = Rules.resting(this);
        if (!rest.isEmpty() && restPanel.getVisibility() != View.VISIBLE) showRest(rest);
        else if (rest.isEmpty() && restPanel.getVisibility() == View.VISIBLE) hideRest();
        ui.postDelayed(tickRunnable, 1000);
    }
    private final Runnable tickRunnable = this::tick;

    // ================================================================== world + background clip

    private void applyWorld() {
        Worlds.World w = Worlds.today(Rules.prefs(this).getInt("world", -1));
        boolean changed = w != world;
        world = w;
        cover.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, w.sky));
        mascot.setWorld(w);
        restMascot.setWorld(w);
        worldChip.setText("★  " + w.name.toUpperCase(Locale.ROOT));
        worldChip.setTextColor(w.accent);
        names.setText(Rules.prefs(this).getString("names", "Ghanim & Noha"));
        drawGreeting();

        boolean pinned = Rules.prefs(this).getInt("world", -1) >= 0;
        Worlds.Character ch = Worlds.characterOfTheDay(w, pinned);
        charName.setText(ch.name);
        charFrom.setText(ch.from);
        charFrom.setTextColor(w.accent);
        charFact.setText(ch.fact);
        bg.execute(() -> {
            Bitmap b = characterPhoto(ch.name);
            ui.post(() -> {
                charPhoto.setImageBitmap(b);
                charPhoto.setVisibility(b == null ? View.GONE : View.VISIBLE);
            });
        });
        if (changed) { stopClip(); playClip(); }
        else if (player != null) try { player.start(); } catch (Exception ignored) { }
    }

    /** An optional picture the parent put in Pictures/KidsCorner, named after the character (Goku.png). */
    private Bitmap characterPhoto(String name) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KidsCorner");
        File[] files = dir.listFiles();
        if (files == null) return null;
        String want = name.toLowerCase(Locale.ROOT).replace(" ", "");
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            int dot = n.lastIndexOf('.');
            if (dot > 0 && n.substring(0, dot).replace(" ", "").replace("-", "").replace("_", "").equals(want)) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getPath(), o);
                o.inSampleSize = Math.max(1, Math.max(o.outWidth, o.outHeight) / 600);
                o.inJustDecodeBounds = false;
                return BitmapFactory.decodeFile(f.getPath(), o);
            }
        }
        return null;
    }

    /** Words in a wallpaper's file name that fit each world (Goku Sunset fits Power Up). */
    static String[] clipWords(String world) {
        switch (world) {
            case "power": return new String[]{"goku", "gohan", "vegeta", "dragon", "dbz", "saiyan"};
            case "mushroom": return new String[]{"mario", "yoshi", "luigi", "pinna", "sunshine", "peach"};
            case "speed": return new String[]{"sonic", "tails", "knuckles"};
            case "dream": return new String[]{"kirby", "dedede"};
            default: return new String[]{"minecraft", "creeper"};
        }
    }

    /**
     * The same video wallpapers as the family home screen (Movies/TVWallpapers): one that fits today's world if there is one.
     * Otherwise the world's own clip in Movies/KidsCorner, otherwise any of the home screen's videos.
     */
    private File clipFor(Worlds.World w) {
        File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        List<File> home = videosIn(new File(movies, "TVWallpapers")), fits = new ArrayList<>(), own = new ArrayList<>();
        for (File f : home) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            for (String word : clipWords(w.id)) if (n.contains(word)) { fits.add(f); break; }
        }
        for (File f : videosIn(new File(movies, "KidsCorner"))) if (f.getName().toLowerCase(Locale.ROOT).startsWith(w.clip)) own.add(f);
        List<File> pick = !fits.isEmpty() ? fits : !own.isEmpty() ? own : home;
        return pick.isEmpty() ? null : pick.get(random.nextInt(pick.size()));
    }

    private static List<File> videosIn(File dir) {
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) if (f.isFile() && isVideo(f.getName())) out.add(f);
        return out;
    }

    private void playClip() {
        if (!surfaceReady || player != null || world == null) return;
        File f = clipFor(world);
        if (f == null) { cover.setAlpha(1); return; }
        try {
            MediaPlayer p = new MediaPlayer();
            p.setDataSource(f.getPath());
            p.setDisplay(video.getHolder());
            p.setLooping(true);
            p.setVolume(0, 0);
            p.setOnPreparedListener(mp -> { mp.start(); cover.animate().alpha(0).setDuration(700).start(); });
            p.setOnErrorListener((mp, a, b) -> { cover.setAlpha(1); return true; });
            p.prepareAsync();
            player = p;
        } catch (Exception e) { cover.setAlpha(1); }
    }

    private void stopClip() {
        if (player == null) return;
        try { player.release(); } catch (Exception ignored) { }
        player = null;
        cover.setAlpha(1);
    }

    // ================================================================== layout

    private void buildTop() {
        // welcome card, top left
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(48, 34, 56, 38);
        left.setBackground(glass(48));
        LinearLayout kick = new LinearLayout(this);
        kick.setGravity(Gravity.CENTER_VERTICAL);
        TextView welcome = text("WELCOME", 26, fBold, DIM);
        welcome.setLetterSpacing(0.2f);
        kick.addView(welcome);
        worldChip = text("", 24, fBold, WHITE);
        worldChip.setLetterSpacing(0.12f);
        worldChip.setPadding(26, 0, 0, 0);
        kick.addView(worldChip);
        left.addView(kick);
        names = text("", 92, fHeavy, WHITE);
        names.setIncludeFontPadding(false);
        names.setPadding(0, 14, 0, 4);
        left.addView(names);
        hello = text("", 34, fBold, SOFT);
        left.addView(hello);
        weatherText = text("", 34, fBold, WHITE);
        weatherText.setPadding(0, 2, 0, 0);
        left.addView(weatherText);
        place(left, 80, 56, -2, -2);

        // clock and time left, top right
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(34, 30, 52, 30);
        card.setBackground(glass(48));
        FrameLayout ringBox = new FrameLayout(this);
        ring = new Ring(this);
        ringBox.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout inRing = new LinearLayout(this);
        inRing.setOrientation(LinearLayout.VERTICAL);
        inRing.setGravity(Gravity.CENTER);
        ringBig = text("", 40, fHeavy, WHITE);
        ringBig.setIncludeFontPadding(false);
        ringBig.setGravity(Gravity.CENTER);
        ringSmall = text("", 22, fBold, DIM);
        ringSmall.setIncludeFontPadding(false);
        ringSmall.setGravity(Gravity.CENTER);
        inRing.addView(ringBig);
        inRing.addView(ringSmall);
        ringBox.addView(inRing, new FrameLayout.LayoutParams(-1, -1));
        card.addView(ringBox, new LinearLayout.LayoutParams(184, 184));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(40, 0, 0, 0);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM);
        clockText = text("", 112, fHeavy, WHITE);
        clockText.setIncludeFontPadding(false);
        clockRow.addView(clockText);
        ampmText = text("", 38, fHeavy, DIM);
        ampmText.setPadding(12, 0, 0, 14);
        clockRow.addView(ampmText);
        right.addView(clockRow);
        ringNote = text("", 30, fBold, SOFT);
        right.addView(ringNote);
        card.addView(right);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        fl.topMargin = 56; fl.rightMargin = 80;
        stage.addView(card, fl);
    }

    private void buildMiddle() {
        mascot = new Mascot(this, fHeavy);
        place(mascot, 210, 390, 360, 360);
        bob(mascot);

        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(52, 36, 52, 40);
        card.setBackground(glass(48));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView kick = text("CHARACTER OF THE DAY", 24, fBold, DIM);
        kick.setLetterSpacing(0.2f);
        t.addView(kick);
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.BOTTOM);
        charName = text("", 68, fHeavy, WHITE);
        charName.setIncludeFontPadding(false);
        nameRow.addView(charName);
        charFrom = text("", 30, fBold, WHITE);
        charFrom.setPadding(22, 0, 0, 8);
        nameRow.addView(charFrom);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-2, -2);
        nl.topMargin = 10;
        t.addView(nameRow, nl);
        charFact = text("", 34, fMed, SOFT);
        charFact.setMaxLines(3);
        charFact.setLineSpacing(0, 1.05f);
        charFact.setPadding(0, 10, 0, 0);
        t.addView(charFact);
        card.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        charPhoto = new ImageView(this);
        charPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        charPhoto.setOutlineProvider(rounded(32));
        charPhoto.setClipToOutline(true);
        charPhoto.setVisibility(View.GONE);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(220, 220);
        pl.leftMargin = 36;
        card.addView(charPhoto, pl);
        place(card, 610, 420, 1110, -2);
    }

    private void buildTiles() {
        // the glass dock along the bottom, like the family home screen
        View dock = new View(this);
        dock.setBackground(glass(60));
        place(dock, 80, 866, 1760, 178);

        tileRow = new LinearLayout(this);
        tileRow.setGravity(Gravity.CENTER);
        tileRow.setClipChildren(false);
        tileRow.setClipToPadding(false);
        place(tileRow, 80, 866, 1760, 178);

        tileLabel = text("", 32, fHeavy, WHITE);
        tileLabel.setGravity(Gravity.CENTER);
        tileLabel.setPadding(30, 4, 30, 6);
        tileLabel.setBackground(glass(40));
        labelPill = tileLabel;
        place(tileLabel, 0, 776, -2, -2);
    }

    private void drawGreeting() {
        Calendar now = Calendar.getInstance(Rules.KUWAIT);
        int h = now.get(Calendar.HOUR_OF_DAY);
        java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("EEEE d MMMM", Locale.ENGLISH);
        day.setTimeZone(Rules.KUWAIT);
        hello.setText((h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening") + "  ·  " + day.format(now.getTime()));
        weatherText.setText(weather);
        weatherText.setVisibility(weather.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Puts the name pill just above the focused tile. */
    private void moveLabel(View tile, String label) {
        tileLabel.setText(label);
        tileLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float cx = 80 + tile.getLeft() + tile.getWidth() / 2f; // the dock starts 80 px in
        float x = Math.max(80, Math.min(1840 - tileLabel.getMeasuredWidth(), cx - tileLabel.getMeasuredWidth() / 2f));
        tileLabel.animate().translationX(x).alpha(1).setDuration(260).setInterpolator(EASE).start();
    }
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.8f);

    /** Kuwait City weather from Open-Meteo, every half hour. */
    private void fetchWeather() {
        if (System.currentTimeMillis() - lastWeather < 30 * 60_000L) return;
        lastWeather = System.currentTimeMillis();
        bg.execute(() -> {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(
                        "https://api.open-meteo.com/v1/forecast?latitude=29.3759&longitude=47.9774&current=temperature_2m,weather_code,is_day&timezone=Asia%2FKuwait").openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                org.json.JSONObject cur = new org.json.JSONObject(sb.toString()).getJSONObject("current");
                long temp = Math.round(cur.getDouble("temperature_2m"));
                weather = sky(cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1) + "  " + temp + "°  in Kuwait";
                ui.post(this::drawGreeting);
            } catch (Exception e) { lastWeather -= 25 * 60_000L; } // try again in five minutes
        });
    }

    static String sky(int code, boolean day) {
        if (code == 0) return day ? "☀️ Sunny" : "🌙 Clear night";
        if (code <= 3) return day ? "⛅ A few clouds" : "☁️ Cloudy night";
        if (code <= 48) return "🌫️ Foggy";
        if (code <= 67 || (code >= 80 && code <= 82)) return "🌧️ Rainy";
        if (code <= 77 || code == 85 || code == 86) return "❄️ Snowy";
        return "⛈️ Stormy";
    }

    /** Their tiles: My cartoons, then each allowed app that is installed, then the small parent lock. */
    private void refreshTiles() {
        View focused = getCurrentFocus();
        boolean hadFocus = focused != null && tiles.contains(focused);
        tileRow.removeAllViews();
        tiles.clear();
        PackageManager pm = getPackageManager();
        List<String[]> items = new ArrayList<>(); // {pkg or "", label}
        items.add(new String[]{"", "My cartoons"});
        Set<String> allowed = Rules.apps(this);
        for (ResolveInfo r : launchables()) {
            String p = r.activityInfo.packageName;
            if (allowed.contains(p) && !Rules.NEVER.contains(p)) items.add(new String[]{p, r.loadLabel(pm).toString()});
        }
        int n = items.size();
        int h = 136, w = Math.min(242, (1560 - 28 * n) / n);
        for (int i = 0; i < n; i++) {
            String pkg = items.get(i)[0], label = items.get(i)[1];
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setClipToOutline(true);
            tile.setOutlineProvider(rounded(26));
            tile.setOutlineAmbientShadowColor(0);
            tile.setOutlineSpotShadowColor(0);
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (pkg.isEmpty()) {
                tile.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{world.body, world.bodyDark}));
                TextView t = text("▶  Cartoons", Math.max(26, w / 8.5f), fHeavy, WHITE);
                t.setGravity(Gravity.CENTER);
                tile.addView(t, new FrameLayout.LayoutParams(-1, -1));
            } else {
                Drawable art = banner(pkg);
                if (art != null) {
                    img.setImageDrawable(art);
                } else {
                    tile.setBackgroundColor(0xFF20263A);
                    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    img.setPadding(h / 5, h / 5, h / 5, h / 5);
                    try { img.setImageDrawable(pm.getApplicationIcon(pkg)); } catch (Exception ignored) { }
                }
                tile.addView(img, new FrameLayout.LayoutParams(-1, -1));
            }
            View ringView = new View(this);
            GradientDrawable edge = new GradientDrawable();
            edge.setCornerRadius(26);
            edge.setStroke(6, WHITE);
            ringView.setBackground(edge);
            ringView.setAlpha(0);
            tile.addView(ringView, new FrameLayout.LayoutParams(-1, -1));

            final int index = i;
            tile.setOnFocusChangeListener((v, has) -> {
                v.animate().scaleX(has ? 1.16f : 1f).scaleY(has ? 1.16f : 1f).translationY(has ? -14 : 0)
                        .setDuration(has ? 420 : 220).setInterpolator(has ? new OvershootInterpolator(2.2f) : new DecelerateInterpolator(1.6f)).start();
                v.setTranslationZ(has ? 10 : 0); // on top of its neighbours, with no shadow drawn outside the tile
                ringView.animate().alpha(has ? 1 : 0).setDuration(200).start();
                if (has) {
                    lastTile = index;
                    v.post(() -> {
                        moveLabel(v, label);
                        float cx = 80 + v.getLeft() + v.getWidth() / 2f;
                        mascot.lookAt((cx - 390) / 900f, 0.9f);
                    });
                    hop(mascot);
                }
            });
            tile.setOnClickListener(v -> {
                if (pkg.isEmpty()) openCartoons(); else launch(pkg);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.leftMargin = lp.rightMargin = 14;
            tileRow.addView(tile, lp);
            tiles.add(tile);
        }

        // the parent lock: small and quiet at the end of the row
        FrameLayout lock = new FrameLayout(this);
        lock.setFocusable(true);
        lock.setBackground(glass(60));
        TextView l = text("🔒", 40, fBold, WHITE);
        l.setGravity(Gravity.CENTER);
        lock.addView(l, new FrameLayout.LayoutParams(-1, -1));
        setupDot = new View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFFF4D4D);
        setupDot.setBackground(dot);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(22, 22, Gravity.TOP | Gravity.END);
        dl.topMargin = dl.rightMargin = 10;
        lock.addView(setupDot, dl);
        setupDot.setVisibility(setupComplete() ? View.GONE : View.VISIBLE);
        lock.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.15f : 1f).scaleY(has ? 1.15f : 1f).setDuration(200).start();
            if (has) { moveLabel(v, "Grown-ups"); mascot.lookAt(1, 0.9f); }
        });
        lock.setOnClickListener(v -> askPin("check", this::openParent));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(88, 88);
        ll.leftMargin = 30;
        tileRow.addView(lock, ll);
        lockTile = lock;

        if (hadFocus || getCurrentFocus() == null) {
            if (anyPanelOpen()) return;
            tiles.get(Math.min(lastTile, tiles.size() - 1)).requestFocus();
        }
    }

    private List<ResolveInfo> launchables() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> out = new ArrayList<>(pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        Set<String> seen = new java.util.HashSet<>();
        for (ResolveInfo r : out) seen.add(r.activityInfo.packageName);
        for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
            if (seen.add(r.activityInfo.packageName)) out.add(r);
        List<ResolveInfo> unique = new ArrayList<>();
        Set<String> once = new java.util.HashSet<>();
        for (ResolveInfo r : out) {
            String p = r.activityInfo.packageName;
            if (!p.equals(getPackageName()) && once.add(p)) unique.add(r);
        }
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        for (ResolveInfo r : unique) labels.put(r.activityInfo.packageName, r.loadLabel(pm).toString());
        Collections.sort(unique, (a, b) -> labels.get(a.activityInfo.packageName).compareToIgnoreCase(labels.get(b.activityInfo.packageName)));
        return unique;
    }

    private Drawable banner(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            Intent li = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (li != null) { Drawable d = pm.getActivityBanner(li); if (d != null) return d; }
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationBanner(ai);
        } catch (Exception e) { return null; }
    }

    private void launch(String pkg) {
        if (!Rules.resting(this).isEmpty()) return;
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return;
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
    }

    private void updateRing() {
        if (ring == null) return;
        long left = Rules.left(this);
        long extra = Rules.extraUntil(this) - System.currentTimeMillis();
        int limit = Rules.limitToday(this);
        if (extra > 0) {
            ring.set(1, 0xFF7CFC6A);
            ringBig.setText(shortMinutes(extra / 1000));
            ringSmall.setText("bonus");
            ringNote.setText("Bonus time from a grown-up");
        } else if (left == Long.MAX_VALUE) {
            ring.set(1, world == null ? WHITE : world.accent);
            ringBig.setText("∞");
            ringSmall.setText("free play");
            ringNote.setText("No time limit today");
        } else {
            float f = limit <= 0 ? 0 : left / (limit * 60f);
            ring.set(f, f > 0.25f ? (world == null ? WHITE : world.accent) : 0xFFFF6B5B);
            ringBig.setText(shortMinutes(left));
            ringSmall.setText("left today");
            int bed = Rules.bedtime(this);
            ringNote.setText(bed < 0 ? "Enjoy your shows!" : "Bedtime " + bedLabel(bed));
        }
        Calendar now = Calendar.getInstance(Rules.KUWAIT);
        int hr = now.get(Calendar.HOUR), min = now.get(Calendar.MINUTE);
        clockText.setText((hr == 0 ? 12 : hr) + ":" + (min < 10 ? "0" : "") + min);
        ampmText.setText(now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");
        if (min == 0 && now.get(Calendar.SECOND) < 2) drawGreeting();
        fetchWeather();
    }

    /** "1:20" or "45m" for inside the ring. */
    static String shortMinutes(long sec) {
        long m = (sec + 59) / 60;
        return m >= 60 ? (m / 60) + ":" + (m % 60 < 10 ? "0" : "") + (m % 60) : m + "m";
    }

    static String minutes(long sec) {
        long m = (sec + 59) / 60;
        if (m >= 60) return (m / 60) + " h" + (m % 60 == 0 ? "" : " " + (m % 60) + " min");
        return m + " min";
    }

    // ================================================================== rest screen

    private void buildRest() {
        restPanel = new FrameLayout(this);
        restPanel.setBackgroundColor(0xF20B0820);
        restPanel.setVisibility(View.GONE);
        restPanel.setClickable(true);
        stage.addView(restPanel, new FrameLayout.LayoutParams(1920, 1080));
        restMascot = new Mascot(this, fHeavy);
        restMascot.setAsleep(true);
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(460, 460, Gravity.CENTER_HORIZONTAL);
        ml.topMargin = 110;
        restPanel.addView(restMascot, ml);
        restTitle = text("", 92, fHeavy, WHITE);
        restTitle.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-1, -2);
        tl.topMargin = 590;
        restPanel.addView(restTitle, tl);
        restSub = text("", 42, fMed, SOFT);
        restSub.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-1, -2);
        sl.topMargin = 720;
        restPanel.addView(restSub, sl);
        TextView b = button("Grown-ups", 30);
        b.setOnClickListener(v -> askPin("check", this::openParent));
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL);
        bl.topMargin = 880;
        restPanel.addView(b, bl);
        restButton = b;
    }

    private void showRest(String why) {
        boolean bed = why.equals("bed");
        restTitle.setText(bed ? "Time for bed!" : "That's all for today!");
        restSub.setText(bed ? "Sleep well. Your shows will be waiting in the morning." : "Great watching! Let's play something else. See you tomorrow!");
        hide(cartoonsPanel);
        restPanel.setVisibility(View.VISIBLE);
        syncFocus();
        restPanel.setAlpha(0);
        restPanel.animate().alpha(1).setDuration(600).start();
        bob(restMascot);
        if (pinPanel.getVisibility() != View.VISIBLE && parentPanel.getVisibility() != View.VISIBLE) restButton.requestFocus();
        if (player != null) player.pause();
    }

    private void hideRest() {
        restPanel.animate().alpha(0).setDuration(400).withEndAction(() -> {
            restPanel.setVisibility(View.GONE);
            syncFocus();
            if (!anyPanelOpen()) focusAfterPanel();
        }).start();
        if (player != null) try { player.start(); } catch (Exception ignored) { }
    }

    // ================================================================== my cartoons

    private GridLayout cartoonGrid;
    private TextView cartoonsEmpty;

    private void buildCartoons() {
        cartoonsPanel = panel();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(110, 70, 110, 0);
        col.addView(text("My cartoons", 64, fHeavy, WHITE));
        cartoonsEmpty = text("No cartoons yet.\nGrown-ups: put videos in Movies/Kids on the box, or a Kids folder on the USB drive.", 38, fMed, SOFT);
        cartoonsEmpty.setPadding(0, 30, 0, 0);
        col.addView(cartoonsEmpty);
        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(0, 30, 0, 60);
        sv.setVerticalScrollBarEnabled(false);
        cartoonGrid = new GridLayout(this);
        cartoonGrid.setColumnCount(4);
        cartoonGrid.setClipChildren(false);
        sv.addView(cartoonGrid);
        col.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        cartoonsPanel.addView(col, new FrameLayout.LayoutParams(-1, -1));
    }

    /** Videos in Movies/Kids, Movies/Cartoons, Download/Kids, and Kids or Cartoons on any USB drive. */
    private void loadCartoons() {
        bg.execute(() -> {
            List<File> roots = new ArrayList<>();
            File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            roots.add(new File(movies, "Kids"));
            roots.add(new File(movies, "Cartoons"));
            roots.add(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kids"));
            File[] vols = new File("/storage").listFiles();
            if (vols != null) for (File v : vols) {
                if (v.getName().equals("emulated") || v.getName().equals("self")) continue;
                roots.add(new File(v, "Kids"));
                roots.add(new File(v, "Cartoons"));
                roots.add(new File(v, "Movies/Kids"));
            }
            List<File> found = new ArrayList<>();
            for (File r : roots) collect(r, 0, found);
            Collections.sort(found, (a, b) -> a.getPath().compareToIgnoreCase(b.getPath()));
            ui.post(() -> cartoons = found);
        });
    }

    private static void collect(File dir, int depth, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null || out.size() > 600) return;
        for (File f : files) {
            if (f.isDirectory()) { if (depth < 3 && !f.getName().startsWith(".")) collect(f, depth + 1, out); }
            else if (isVideo(f.getName())) out.add(f);
        }
    }

    static boolean isVideo(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.startsWith(".")) return false;
        for (String e : VIDEO_EXT) if (n.endsWith(e)) return true;
        return false;
    }

    static String title(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        return n.replace('_', ' ').replace('.', ' ').trim();
    }

    private void openCartoons() {
        if (!Rules.resting(this).isEmpty()) return;
        cartoonGrid.removeAllViews();
        cartoonsEmpty.setVisibility(cartoons.isEmpty() ? View.VISIBLE : View.GONE);
        show(cartoonsPanel);
        for (int i = 0; i < cartoons.size(); i++) {
            File f = cartoons.get(i);
            FrameLayout card = new FrameLayout(this);
            card.setFocusable(true);
            card.setBackground(glass(26));
            card.setClipToOutline(true);
            card.setOutlineProvider(rounded(26));
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(-1, 216));
            TextView t = text(title(f), 28, fBold, WHITE);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setPadding(20, 0, 20, 0);
            t.setGravity(Gravity.CENTER_VERTICAL);
            FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-1, 64, Gravity.BOTTOM);
            card.addView(t, tl);
            card.setOnFocusChangeListener((v, has) -> {
                v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(220).start();
                v.setBackground(has ? solid(WHITE, 26, 0x33) : glass(26));
                t.setTextColor(has ? 0xFF1A1033 : WHITE);
            });
            final int index = i;
            card.setOnClickListener(v -> play(index));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 384; lp.height = 280;
            lp.setMargins(0, 0, 40, 40);
            cartoonGrid.addView(card, lp);
            thumb(f, img);
        }
        if (cartoonGrid.getChildCount() > 0) cartoonGrid.getChildAt(0).requestFocus();
    }

    private void thumb(File f, ImageView into) {
        Bitmap cached = thumbs.get(f.getPath());
        if (cached != null) { into.setImageBitmap(cached); return; }
        bg.execute(() -> {
            Bitmap b = null;
            try { b = ThumbnailUtils.createVideoThumbnail(f, new Size(384, 216), new CancellationSignal()); } catch (Exception ignored) { }
            if (b == null) return;
            thumbs.put(f.getPath(), b);
            Bitmap fb = b;
            ui.post(() -> into.setImageBitmap(fb));
        });
    }

    private void play(int index) {
        if (!Rules.resting(this).isEmpty()) return;
        String[] paths = new String[cartoons.size()];
        for (int i = 0; i < paths.length; i++) paths[i] = cartoons.get(i).getPath();
        startActivity(new Intent(this, PlayerActivity.class).putExtra("paths", paths).putExtra("index", index));
    }

    // ================================================================== PIN

    private TextView pinTitle, pinSub;
    private final View[] pinDots = new View[4];
    private View firstPinKey;
    private String pinEntry = "", pinMode = "check", firstPin = "";
    private Runnable pinThen;

    private void buildPin() {
        pinPanel = panel();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(60, 50, 60, 50);
        box.setBackground(solid(0xFF161028, 44, 0));
        pinTitle = text("", 50, fHeavy, WHITE);
        pinTitle.setGravity(Gravity.CENTER);
        box.addView(pinTitle);
        pinSub = text("", 28, fMed, DIM);
        pinSub.setGravity(Gravity.CENTER);
        pinSub.setPadding(0, 4, 0, 26);
        box.addView(pinSub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(30, 30);
            dl.leftMargin = dl.rightMargin = 14;
            dots.addView(d, dl);
            pinDots[i] = d;
        }
        box.addView(dots);
        GridLayout pad = new GridLayout(this);
        pad.setColumnCount(3);
        pad.setPadding(0, 30, 0, 0);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"};
        for (String k : keys) {
            TextView key = text(k, 44, fBold, WHITE);
            key.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 130; lp.height = 96;
            lp.setMargins(10, 10, 10, 10);
            if (k.isEmpty()) { pad.addView(new View(this), lp); continue; }
            key.setFocusable(true);
            key.setBackground(solid(0x1FFFFFFF, 24, 0));
            key.setOnFocusChangeListener((v, has) -> {
                key.setBackground(solid(has ? WHITE : 0x1FFFFFFF, 24, 0));
                key.setTextColor(has ? 0xFF161028 : WHITE);
            });
            key.setOnClickListener(v -> pinPress(k));
            pad.addView(key, lp);
            if (k.equals("1")) firstPinKey = key;
        }
        box.addView(pad);
        pinPanel.addView(box, new FrameLayout.LayoutParams(560, -2, Gravity.CENTER));
    }

    private void askPin(String mode, Runnable then) {
        pinThen = then;
        firstPin = "";
        setPinMode(mode);
        show(pinPanel);
        firstPinKey.requestFocus();
    }

    private void setPinMode(String m) {
        pinMode = m;
        pinEntry = "";
        switch (m) {
            case "set": pinTitle.setText("Choose a parent PIN"); pinSub.setText("4 numbers. You'll need it to leave Kids Corner."); break;
            case "again": pinTitle.setText("Once more"); pinSub.setText("Type the same PIN again"); break;
            default: pinTitle.setText("Grown-ups only"); pinSub.setText("Enter the parent PIN"); break;
        }
        drawDots(0);
    }

    private void drawDots(int filledColorOverride) {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            boolean filled = i < pinEntry.length();
            d.setColor(filledColorOverride != 0 ? filledColorOverride : filled ? WHITE : 0x00000000);
            d.setStroke(3, filledColorOverride != 0 ? filledColorOverride : 0x99FFFFFF);
            pinDots[i].setBackground(d);
        }
    }

    private void pinPress(String k) {
        if (k.equals("⌫")) { if (!pinEntry.isEmpty()) pinEntry = pinEntry.substring(0, pinEntry.length() - 1); drawDots(0); return; }
        if (pinEntry.length() >= 4) return;
        pinEntry += k;
        drawDots(0);
        if (pinEntry.length() < 4) return;
        String entered = pinEntry;
        ui.postDelayed(() -> {
            switch (pinMode) {
                case "set": firstPin = entered; setPinMode("again"); return;
                case "again":
                    if (entered.equals(firstPin)) { Rules.setPin(this, entered); pinDone(); }
                    else { wrong(); setPinModeLater("set"); }
                    return;
                default:
                    if (entered.equals(Rules.pin(this))) pinDone();
                    else { wrong(); setPinModeLater("check"); }
            }
        }, 160);
    }

    private void setPinModeLater(String m) { ui.postDelayed(() -> setPinMode(m), 650); }

    private void wrong() {
        drawDots(0xFFFF5A5A);
        View box = (View) pinTitle.getParent();
        box.animate().translationX(24).setDuration(60).withEndAction(() ->
                box.animate().translationX(-24).setDuration(60).withEndAction(() ->
                        box.animate().translationX(0).setDuration(60).start()).start()).start();
    }

    private void pinDone() {
        hide(pinPanel);
        Runnable r = pinThen;
        pinThen = null;
        if (r != null) r.run();
    }

    // ================================================================== parent menu

    static final int[] LIMITS = {0, 15, 30, 45, 60, 75, 90, 105, 120, 150, 180, 240};
    static final int[] BEDTIMES = {-1, 19 * 60, 19 * 60 + 30, 20 * 60, 20 * 60 + 30, 21 * 60, 21 * 60 + 30, 22 * 60};
    private LinearLayout parentList;
    private View parentFirst;
    private TextView setupText;

    private void buildParent() {
        parentPanel = panel();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(56, 44, 56, 44);
        col.setBackground(solid(0xFF161028, 44, 0));
        col.addView(text("Kids Corner settings", 52, fHeavy, WHITE));
        TextView hint = text("Left and right change a setting. Back closes.", 26, fMed, DIM);
        hint.setPadding(0, 0, 0, 18);
        col.addView(hint);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        parentList = new LinearLayout(this);
        parentList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(parentList);
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        parentPanel.addView(col, new FrameLayout.LayoutParams(1100, 960, Gravity.CENTER));
    }

    private void openParent() {
        parentList.removeAllViews();
        parentFirst = choiceRow("School days (Sun to Thu)", LIMITS, "limit_school", 90, KidsActivity::limitLabel);
        choiceRow("Weekend (Fri and Sat)", LIMITS, "limit_weekend", 120, KidsActivity::limitLabel);
        choiceRow("Bedtime (until 7 am)", BEDTIMES, "bedtime", 20 * 60 + 30, KidsActivity::bedLabel);
        int[] worlds = new int[Worlds.ALL.length + 1];
        for (int i = 0; i < worlds.length; i++) worlds[i] = i - 1;
        choiceRow("World", worlds, "world", -1, v -> v < 0 ? "A new one every day" : Worlds.ALL[v].name);

        LinearLayout extra = new LinearLayout(this);
        extra.setPadding(0, 18, 0, 6);
        extra.addView(text("Extra time now", 32, fBold, DIM), new LinearLayout.LayoutParams(330, -2));
        int[][] gifts = {{15}, {30}, {60}};
        for (int[] g : gifts) {
            TextView b = button(g[0] == 60 ? "+1 hour" : "+" + g[0] + " min", 26);
            b.setOnClickListener(v -> { Rules.giveExtra(this, g[0]); toastIn(b, "Added"); tick(); });
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
            bl.rightMargin = 16;
            extra.addView(b, bl);
        }
        TextView reset = button("Reset today", 26);
        reset.setOnClickListener(v -> { Rules.setUsed(this, 0); Rules.prefs(this).edit().remove("extra_until").apply(); toastIn(reset, "Reset"); tick(); });
        extra.addView(reset);
        parentList.addView(extra);

        TextView watched = text("Watched today: " + minutes(Rules.used(this)), 28, fMed, DIM);
        watched.setPadding(330, 0, 0, 12);
        parentList.addView(watched);

        actionRow("Their apps", "Choose which apps they can open", this::openApps);
        actionRow("Change PIN", "", () -> askPin("set", this::openParent));
        setupText = text(setupSummary(), 28, fMed, DIM);
        setupText.setPadding(0, 18, 0, 18);
        setupText.setLineSpacing(0, 1.1f);
        parentList.addView(setupText);
        actionRow("Leave Kids Corner", "Turns kids mode off until it's opened again", this::leave);

        show(parentPanel);
        parentFirst.requestFocus();
    }

    interface Label { String of(int v); }

    static String limitLabel(int m) { return m == 0 ? "No limit" : minutes(m * 60L); }

    static String bedLabel(int m) {
        if (m < 0) return "Off";
        int h = m / 60, min = m % 60;
        return (h > 12 ? h - 12 : h) + ":" + (min < 10 ? "0" : "") + min + (h >= 12 ? " pm" : " am");
    }

    private View choiceRow(String name, int[] values, String key, int def, Label label) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setPadding(28, 14, 28, 14);
        row.addView(text(name, 32, fBold, WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text("", 34, fHeavy, WHITE);
        row.addView(value);
        Runnable draw = () -> value.setText("◀  " + label.of(Rules.prefs(this).getInt(key, def)) + "  ▶");
        draw.run();
        row.setOnFocusChangeListener((v, has) -> row.setBackground(has ? solid(0x26FFFFFF, 22, 0) : null));
        row.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            int dir = code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 0;
            if (dir == 0) return false;
            int cur = Rules.prefs(this).getInt(key, def), at = 0;
            for (int i = 0; i < values.length; i++) if (values[i] == cur) at = i;
            at = Math.max(0, Math.min(values.length - 1, at + dir));
            Rules.prefs(this).edit().putInt(key, values[at]).apply();
            draw.run();
            if (key.equals("world")) { applyWorld(); refreshTiles(); }
            tick();
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 6;
        parentList.addView(row, lp);
        return row;
    }

    private void actionRow(String name, String sub, Runnable run) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setFocusable(true);
        row.setPadding(28, 14, 28, 14);
        TextView t = text(name, 32, fBold, WHITE);
        row.addView(t);
        if (!sub.isEmpty()) row.addView(text(sub, 24, fMed, DIM));
        row.setOnFocusChangeListener((v, has) -> row.setBackground(has ? solid(0x26FFFFFF, 22, 0) : null));
        row.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 6;
        parentList.addView(row, lp);
    }

    private void toastIn(TextView b, String msg) {
        CharSequence was = b.getText();
        b.setText(msg + " ✓");
        ui.postDelayed(() -> b.setText(was), 1200);
    }

    private boolean usageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private boolean setupComplete() { return usageAccess() && Guard.canOverlay(this); }

    private String setupSummary() {
        boolean u = usageAccess(), o = Guard.canOverlay(this);
        boolean v = checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        return "Setup:  " + (u ? "✓" : "✗") + " app watching   " + (o ? "✓" : "✗") + " pop-ups   " + (v ? "✓" : "✗") + " videos   ·   "
                + cartoons.size() + " cartoons found"
                + (u && o ? "" : "\nRun \"Kids Corner setup\" on the PC once to switch the ✗ items on.");
    }

    private void leave() {
        Rules.setOn(this, false);
        stopService(new Intent(this, Guard.class));
        hide(parentPanel);
        hideRestNow();
        // Free the video decoder before the family home starts its own video: two at once make the box stall.
        stopClip();
        video.setVisibility(View.GONE);
        ui.postDelayed(() -> {
            try {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) { }
            finish();
        }, 250);
    }

    private void hideRestNow() { restPanel.setVisibility(View.GONE); syncFocus(); }

    // ---------------------------------------------------------------- their apps

    private GridLayout appGrid;

    private void buildApps() {
        appsPanel = panel();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(110, 70, 110, 0);
        col.addView(text("Their apps", 64, fHeavy, WHITE));
        col.addView(text("OK ticks or unticks an app. YouTube, the Play Store, Settings and browsers are never allowed.", 28, fMed, DIM));
        ScrollView sv = new ScrollView(this);
        sv.setPadding(0, 30, 0, 60);
        sv.setClipToPadding(false);
        sv.setVerticalScrollBarEnabled(false);
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(5);
        sv.addView(appGrid);
        col.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        appsPanel.addView(col, new FrameLayout.LayoutParams(-1, -1));
    }

    private void openApps() {
        appGrid.removeAllViews();
        PackageManager pm = getPackageManager();
        for (ResolveInfo r : launchables()) {
            String pkg = r.activityInfo.packageName;
            if (Rules.NEVER.contains(pkg)) continue;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setFocusable(true);
            cell.setPadding(14, 18, 14, 14);
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(r.loadIcon(pm)); } catch (Exception ignored) { }
            cell.addView(icon, new LinearLayout.LayoutParams(110, 110));
            TextView name = text(r.loadLabel(pm).toString(), 26, fBold, WHITE);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cell.addView(name, new LinearLayout.LayoutParams(-1, -2));
            TextView mark = text("", 24, fBold, WHITE);
            cell.addView(mark);
            Runnable draw = () -> {
                boolean on = Rules.apps(this).contains(pkg);
                mark.setText(on ? "✓ allowed" : "not allowed");
                mark.setTextColor(on ? 0xFF7CFC6A : DIM);
                cell.setAlpha(on ? 1f : 0.7f);
            };
            draw.run();
            cell.setOnFocusChangeListener((v, has) -> cell.setBackground(has ? solid(0x26FFFFFF, 24, 0) : null));
            cell.setOnClickListener(v -> { Rules.toggleApp(this, pkg); draw.run(); });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 300; lp.height = 230;
            lp.setMargins(0, 0, 40, 30);
            appGrid.addView(cell, lp);
        }
        show(appsPanel);
        if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
    }

    // ================================================================== helpers

    private boolean anyPanelOpen() {
        for (View v : new View[]{pinPanel, parentPanel, appsPanel, cartoonsPanel})
            if (v != null && v.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    private void closePanels() {
        for (View v : new View[]{pinPanel, parentPanel, appsPanel, cartoonsPanel}) hide(v);
        focusAfterPanel();
    }

    private FrameLayout panel() {
        FrameLayout p = new FrameLayout(this);
        p.setBackgroundColor(0xE60B0820);
        p.setVisibility(View.GONE);
        p.setClickable(true);
        stage.addView(p, new FrameLayout.LayoutParams(1920, 1080));
        return p;
    }

    private void show(View p) {
        p.bringToFront();
        p.setVisibility(View.VISIBLE);
        p.setAlpha(0);
        p.animate().alpha(1).setDuration(220).start();
        syncFocus();
    }

    private void hide(View p) { if (p != null) { p.setVisibility(View.GONE); syncFocus(); } }

    /** Only the top layer can take focus, so the remote never wanders onto tiles hidden behind a panel. */
    private void syncFocus() {
        if (appsPanel == null || pinPanel == null || restPanel == null) return;
        boolean pin = vis(pinPanel), apps = vis(appsPanel), parent = vis(parentPanel), rest = vis(restPanel), cart = vis(cartoonsPanel);
        block(tileRow, pin || apps || parent || rest || cart);
        block(restPanel, pin || apps || parent);
        block(parentPanel, pin || apps);
    }

    private static boolean vis(View v) { return v != null && v.getVisibility() == View.VISIBLE; }

    private static void block(ViewGroup g, boolean blocked) {
        if (g != null) g.setDescendantFocusability(blocked ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    private void place(View v, int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x; lp.topMargin = y;
        stage.addView(v, lp);
    }

    private TextView text(String s, float size, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        t.setTypeface(tf);
        t.setTextColor(color);
        return t;
    }

    private TextView button(String s, float size) {
        TextView b = text(s, size, fBold, WHITE);
        b.setFocusable(true);
        b.setPadding(34, 12, 34, 12);
        b.setGravity(Gravity.CENTER);
        b.setBackground(solid(0x26FFFFFF, 40, 0));
        b.setOnFocusChangeListener((v, has) -> {
            b.setBackground(solid(has ? WHITE : 0x26FFFFFF, 40, 0));
            b.setTextColor(has ? 0xFF161028 : WHITE);
        });
        return b;
    }

    private static GradientDrawable glass(float radius) {
        // light at the top, clearer below, with a fine bright edge: the video shows through
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x40FFFFFF, 0x1FFFFFFF});
        d.setStroke(2, 0x52FFFFFF);
        d.setCornerRadius(radius);
        return d;
    }

    private static GradientDrawable solid(int color, float radius, int strokeAlpha) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeAlpha > 0) d.setStroke(3, (strokeAlpha << 24) | 0xFFFFFF);
        return d;
    }

    private static ViewOutlineProvider rounded(float radius) {
        return new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
        };
    }

    /** A gentle float up and down, forever. */
    private static void bob(View v) {
        v.animate().cancel();
        v.animate().translationY(-14).setDuration(1600).setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> v.animate().translationY(0).setDuration(1600).withEndAction(() -> bob(v)).start()).start();
    }

    /** A happy little hop when focus moves. */
    private static void hop(View v) {
        v.setScaleX(1.06f); v.setScaleY(0.94f);
        v.animate().scaleX(1).scaleY(1).setDuration(380).setInterpolator(new OvershootInterpolator(3f)).start();
    }

    /** The time-left ring. */
    static final class Ring extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float fraction = 1; private int color = WHITE;
        Ring(android.content.Context c) { super(c); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); }
        void set(float f, int c) { if (f != fraction || c != color) { fraction = f; color = c; invalidate(); } }
        @Override protected void onDraw(Canvas c) {
            float s = 14;
            r.set(s, s, getWidth() - s, getHeight() - s);
            p.setStrokeWidth(s);
            p.setColor(0x33FFFFFF);
            c.drawArc(r, 0, 360, false, p);
            p.setColor(color);
            c.drawArc(r, -90, 360 * Math.max(0, Math.min(1, fraction)), false, p);
        }
    }
}
