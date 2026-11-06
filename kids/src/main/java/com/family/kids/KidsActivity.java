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
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextUtils;

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
    // a list end that can't go further: dimmer than hint text, like the resting PIN keys
    private static final int ENDED = 0x59FFFFFF;
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

    private TextView hello, names, worldChip, weatherText, clockText, ampmText, ringBig, ringSmall, ringNote, charName, charFrom, charFact, tileLabel;
    private View labelPill;
    private TextView starsText, restSurah;
    private LinearLayout restStars;
    private MediaPlayer surah;
    private volatile String weather = "";
    private long lastWeather;
    private ImageView charPhoto;
    private TextView wordKick, wordPic, wordArabic, wordName, wordMeaning, wordLine;
    private View charCard, wordCard, welcomeCard, clockCard, topShade, midShade, bottomShade;
    /** Each app's icon drawn edge to edge, made once per app on a background thread. */
    private final java.util.Map<String, Drawable> iconCache = new java.util.HashMap<>();
    /** The tile being moved after a grown-up held OK on it, or null. */
    private View arrangeTile;
    private Ring ring;
    private LinearLayout tileRow;
    private View lockTile, setupDot;
    private final List<View> tiles = new ArrayList<>();
    private int lastTile;

    private FrameLayout restPanel, pinPanel, parentPanel, appsPanel, cartoonsPanel;
    private TextView restIcon;
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
        // Top and bottom shades over their thirds, and the wallpaper dimming in the middle third:
        // three bands that never overlap, so the darkening costs one fill of the screen at most.
        topShade = new View(this);
        midShade = new View(this);
        bottomShade = new View(this);
        for (View sh : new View[]{topShade, midShade, bottomShade}) root.addView(sh, new FrameLayout.LayoutParams(-1, 0));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int third = (b - t) / 3;
            View[] bands = {topShade, midShade, bottomShade};
            for (int i = 0; i < 3; i++) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bands[i].getLayoutParams();
                int h = i == 1 ? (b - t) - 2 * third : third, top = i * third;
                if (lp.height != h || lp.topMargin != top) { lp.height = h; lp.topMargin = top; lp.gravity = Gravity.TOP; bands[i].setLayoutParams(lp); }
            }
        });
        applyShade();
        stage = new FrameLayout(this);
        stage.setClipChildren(false);
        root.addView(stage, new FrameLayout.LayoutParams(1920, 1080));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitStage());
        setContentView(root);
        preferSixtyHz();

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
        applyLook();
        noAlphaLayers((View) stage.getParent());

        if (!PinLock.isSet(Rules.prefs(this))) {
            // First time: the parent picks a PIN, then kids mode starts.
            askPin("set", () -> { Rules.setOn(this, true); Guard.start(this); closePanels(); });
        } else {
            Rules.setOn(this, true);
            Guard.start(this);
        }
        if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO}, 1);
        tick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        clipTries = 0;
        hideBars();
        applyWorld();
        if (player == null) playClip(); // a short pause cancelled the retry; the world may not have changed to start one
        refreshTiles(true); // coming back from a cartoon or an app: the same tiles stay, nothing freezes
        loadCartoons();
        if (Rules.on(this)) Guard.start(this);
        tick();
    }

    @Override
    protected void onPause() {
        resumed = false;
        ui.removeCallbacks(clipAgain);
        if (player != null) player.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        // Another app (Netflix, a cartoon) needs the video decoder: let ours go until we come back.
        stopClip();
        stopSurah();
        closeParentMenus(); // a grown-up's menu never stays open behind another app
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

    /** Screen size in percent, as on the family home: TVs that crop the picture's edges can shrink the layout to fit. */
    static final int[] FITS = {100, 97, 94, 91, 88};

    private void fitStage() {
        int w = root.getWidth(), h = root.getHeight();
        if (w == 0 || h == 0) return;
        float s = Math.min(w / 1920f, h / 1080f) * Rules.prefs(this).getInt("fit", 100) / 100f;
        stage.setPivotX(0); stage.setPivotY(0);
        stage.setScaleX(s); stage.setScaleY(s);
        stage.setTranslationX((w - 1920 * s) / 2);
        stage.setTranslationY((h - 1080 * s) / 2);
    }

    /** Ask for a 60 Hz display mode at the current resolution, so every animation can run at 60 fps. */
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
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = best.getModeId();
            getWindow().setAttributes(lp);
        } catch (Exception ignored) { }
    }

    private void hideBars() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (vis(parentPanel) || vis(appsPanel)) { ui.removeCallbacks(parentIdle); ui.postDelayed(parentIdle, PARENT_IDLE_MS); }
        if (arrangeTile != null && !vis(pinPanel)) return arrangeKey(e);
        pressDip(e, getCurrentFocus());
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
            if (!PinLock.isSet(Rules.prefs(this))) return; // the first PIN must be chosen
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

    /** The grown-ups' menu closes by itself after 90 seconds with no key press, so a child handed the remote can't change the rules. */
    static final long PARENT_IDLE_MS = 90_000;
    private final Runnable parentIdle = this::closeParentMenus;

    private void closeParentMenus() {
        ui.removeCallbacks(parentIdle);
        if (!vis(parentPanel) && !vis(appsPanel)) return;
        if (vis(pinPanel) && PinLock.isSet(Rules.prefs(this))) { hide(pinPanel); pinThen = null; }
        hide(appsPanel);
        hide(parentPanel);
        refreshTiles();
        focusAfterPanel();
    }

    private void focusAfterPanel() {
        if (parentPanel.getVisibility() == View.VISIBLE) parentFirst.requestFocus();
        else if (restPanel.getVisibility() == View.VISIBLE) (restStars.getChildCount() > 0 ? restStars.getChildAt(0) : restButton).requestFocus();
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
        cover.setBackgroundColor(0xFF0B0B12); // shown only until the wallpaper video starts
        worldChip.setText("★  " + w.name.toUpperCase(Locale.ROOT));
        worldChip.setTextColor(w.accent);
        names.setText(Rules.prefs(this).getString("names", "Ghanim & Noha"));
        drawGreeting();

        boolean pinned = Rules.prefs(this).getInt("world", -1) >= 0;
        Worlds.Character ch = Worlds.characterOfTheDay(w, pinned);
        charName.setText(ch.name);
        charFrom.setText(ch.from.toUpperCase(Locale.ROOT));
        charFrom.setTextColor(w.accent);
        charFact.setText(ch.fact);
        drawGoodWord();
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
     * Only the family home screen's own video wallpapers (Movies/TVWallpapers): one that fits today's world if there is one,
     * otherwise any of them.
     */
    private File clipFor(Worlds.World w) {
        List<File> home = wallpapers(), fits = new ArrayList<>();
        int pick0 = wallpaperPick(home);
        if (pick0 >= 0 && pick0 < home.size()) return home.get(pick0); // the grown-up chose this one
        if (pick0 == -2 && !home.isEmpty()) return home.get(random.nextInt(home.size()));
        for (File f : home) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            for (String word : clipWords(w.id)) if (n.contains(word)) { fits.add(f); break; }
        }
        List<File> pick = !fits.isEmpty() ? fits : home;
        return pick.isEmpty() ? null : pick.get(random.nextInt(pick.size()));
    }

    /**
     * The grown-up's wallpaper choice: -1 matches the world, -2 any, or the chosen clip's place in today's list, found by
     * its file name. A clip that has gone falls back to matching the world. An older choice kept only by place is
     * turned into a name once.
     */
    private int wallpaperPick(List<File> clips) {
        android.content.SharedPreferences p = Rules.prefs(this);
        int v = p.getInt("wallpaper", -1);
        if (v < 0) return v;
        if (clips.isEmpty()) return -1; // storage not ready yet (just after boot): keep the choice for later
        String name = p.getString("wallpaper_name", null);
        if (name == null) {
            if (v >= clips.size()) return -1;
            p.edit().putString("wallpaper_name", clips.get(v).getName()).apply();
            return v;
        }
        for (int i = 0; i < clips.size(); i++)
            if (clips.get(i).getName().equals(name)) {
                if (i != v) p.edit().putInt("wallpaper", i).apply();
                return i;
            }
        p.edit().putInt("wallpaper", -1).remove("wallpaper_name").apply();
        return -1;
    }

    /** The family home's own video wallpapers, by name, so a chosen one keeps its place in the list. */
    private static List<File> wallpapers() {
        File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        List<File> out = videosIn(new File(movies, "TVWallpapers"));
        Collections.sort(out, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return out;
    }

    private static String clipName(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return (dot > 0 ? n.substring(0, dot) : n).replace('_', ' ').replace('-', ' ');
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
        if (f == null) { cover.setAlpha(1); retryClip(); return; }
        MediaPlayer p = new MediaPlayer();
        try {
            p.setDataSource(f.getPath());
            p.setDisplay(video.getHolder());
            p.setLooping(true);
            p.setVolume(0, 0);
            p.setOnPreparedListener(mp -> { clipTries = 0; mp.start(); cover.animate().alpha(0).setDuration(700).start(); });
            p.setOnErrorListener((mp, a, b) -> { stopClip(); retryClip(); return true; });
            p.prepareAsync();
            player = p;
        } catch (Exception e) { p.release(); cover.setAlpha(1); retryClip(); }
    }

    /** Right after the box turns on the files may not be readable yet: look again every few seconds, as the family home does. */
    private int clipTries;
    private boolean resumed;
    private final Runnable clipAgain = this::playClip;

    private void retryClip() {
        ui.removeCallbacks(clipAgain);
        if (resumed && ++clipTries < 40) ui.postDelayed(clipAgain, 3000);
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
        TextView welcome = text("WELCOME", 30, fBold, DIM);
        welcome.setLetterSpacing(0.12f);
        kick.addView(welcome);
        worldChip = text("", 30, fBold, WHITE);
        worldChip.setLetterSpacing(0.12f);
        worldChip.setPadding(26, 0, 0, 0);
        kick.addView(worldChip);
        left.addView(kick);
        names = text("", 92, fHeavy, WHITE);
        names.setIncludeFontPadding(false);
        names.setPadding(0, 6, 0, -10);
        left.addView(names);
        hello = text("", 34, fBold, SOFT);
        left.addView(hello);
        starsText = text("", 34, fBold, 0xFFFFD45A);
        // match_parent like its neighbours: a wrap_content child makes LinearLayout ignore their widths
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.topMargin = 6;
        left.addView(starsText, sl);
        place(left, 80, 56, -2, -2);
        welcomeCard = left;

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
        ringBig = text("", 46, fHeavy, WHITE);
        ringBig.setIncludeFontPadding(false);
        ringBig.setGravity(Gravity.CENTER);
        ringSmall = text("", 30, fBold, DIM);
        ringSmall.setIncludeFontPadding(false);
        ringSmall.setGravity(Gravity.CENTER);
        inRing.addView(ringBig);
        inRing.addView(ringSmall);
        ringBox.addView(inRing, new FrameLayout.LayoutParams(-1, -1));
        card.addView(ringBox, new LinearLayout.LayoutParams(200, 200));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(40, 0, 0, 0);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM);
        clockText = text("", 112, fHeavy, WHITE);
        clockText.setFontFeatureSettings("tnum"); // even-width digits: the time never jumps sideways each minute
        clockText.setIncludeFontPadding(false);
        clockRow.addView(clockText);
        ampmText = text("", 38, fHeavy, DIM);
        ampmText.setPadding(12, 0, 0, 14);
        clockRow.addView(ampmText);
        right.addView(clockRow);
        ringNote = text("", 30, fBold, SOFT);
        right.addView(ringNote);
        weatherText = text("", 30, fBold, WHITE);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-1, -2);
        wl.topMargin = 8;
        right.addView(weatherText, wl);
        card.addView(right);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        fl.topMargin = 56; fl.rightMargin = 80;
        stage.addView(card, fl);
        clockCard = card;
    }

    private void buildMiddle() {

        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(48, 32, 48, 36);
        card.setBackground(glass(48));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        // the show's name is the heading ("SONIC THE HEDGEHOG"), the character's name is big below it
        charFrom = text("", 30, fBold, WHITE);
        charFrom.setLetterSpacing(0.12f);
        t.addView(charFrom);
        charName = text("", 64, fHeavy, WHITE);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, -2);
        nl.topMargin = 6;
        t.addView(charName, nl);
        charFact = text("", 32, fMed, SOFT);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, -2);
        fl.topMargin = 8;
        t.addView(charFact, fl);
        card.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        charPhoto = new ImageView(this);
        charPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        charPhoto.setOutlineProvider(rounded(32));
        charPhoto.setClipToOutline(true);
        charPhoto.setVisibility(View.GONE);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(160, 160);
        pl.leftMargin = 30;
        card.addView(charPhoto, pl);
        place(card, 80, 430, MID_SPLIT - 80 - 30, -2);
        charCard = card;
        buildGoodWord();
        // Both cards grow to fit their text; if they ever reach the dock label, lift them a little.
        stage.getViewTreeObserver().addOnGlobalLayoutListener(this::fitMiddle);
    }

    /** Where the character card ends and the good-word card begins, on the 1920 stage. */
    private static final int MID_SPLIT = 890, MID_GAP = 28, MID_BOTTOM = KidsActivity.DOCK_Y - 30;

    /** The middle cards sit just under the welcome card, and never run into the dock label. */
    private void fitMiddle() {
        if (charCard == null || wordCard == null || welcomeCard == null) return;
        boolean c = charCard.getVisibility() == View.VISIBLE, w = wordCard.getVisibility() == View.VISIBLE;
        int h = Math.max(c ? charCard.getHeight() : 0, w ? wordCard.getHeight() : 0);
        if (h == 0) return;
        // both cards take the taller one's height, so the row looks like one piece
        if (charCard.getMinimumHeight() != h || wordCard.getMinimumHeight() != h) { charCard.setMinimumHeight(h); wordCard.setMinimumHeight(h); }
        int top = Math.max(welcomeCard.getBottom() + 16, Math.min(welcomeCard.getBottom() + MID_GAP, MID_BOTTOM - h));
        for (View v : new View[]{charCard, wordCard}) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            if (lp.topMargin != top) { lp.topMargin = top; v.setLayoutParams(lp); }
        }
    }

    /** A beautiful name of Allah or a short hadith, a new one each day, with a simple picture. */
    private void buildGoodWord() {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(44, 32, 40, 36);
        card.setBackground(glass(48));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        wordKick = text("", 30, fBold, DIM);
        wordKick.setLetterSpacing(0.12f);
        t.addView(wordKick);
        wordArabic = text("", 42, fBold, 0xFFFFD45A);
        // no trimmed top on the Arabic: its stacked marks (shadda with damma) reach 1.15 em, above Baloo's line top
        wordArabic.setPadding(0, Math.round(42 * 0.1f), 0, 0);
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-2, -2);
        al.topMargin = 8;
        t.addView(wordArabic, al);
        wordName = text("", 30, fBold, WHITE);
        t.addView(wordName);
        wordMeaning = text("", 34, fHeavy, WHITE);
        t.addView(wordMeaning);
        wordLine = text("", 30, fMed, SOFT);
        wordLine.setPadding(0, 6, 0, 0);
        t.addView(wordLine);
        card.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        wordPic = text("", 72, fMed, WHITE);
        wordPic.setGravity(Gravity.CENTER);
        wordPic.setIncludeFontPadding(false);
        wordPic.setBackground(solid(0x26FFFFFF, 64, 60));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(128, 128);
        pl.leftMargin = 24;
        card.addView(wordPic, pl);
        place(card, MID_SPLIT, 430, 1840 - MID_SPLIT, -2);
        wordCard = card;
    }

    private void drawGoodWord() {
        if (wordCard == null) return;
        charCard.setMinimumHeight(0); // fitMiddle evens them out again after the next layout
        wordCard.setMinimumHeight(0);
        if (GoodWords.nameDay()) {
            GoodWords.Name n = GoodWords.name();
            wordKick.setText("A NAME OF ALLAH");
            wordPic.setText(n.pic);
            wordArabic.setText(n.arabic);
            wordName.setText(n.name);
            wordName.setVisibility(View.VISIBLE);
            wordMeaning.setText(n.meaning);
            wordMeaning.setTextColor(world == null ? WHITE : world.accent);
            wordLine.setText(n.forKids);
        } else {
            GoodWords.Hadith h = GoodWords.hadith();
            wordKick.setText("HADITH OF THE DAY");
            wordPic.setText(h.pic);
            wordArabic.setText(h.arabic);
            wordName.setVisibility(View.GONE);
            wordMeaning.setText(h.english);
            wordMeaning.setTextColor(WHITE);
            boolean src = Rules.prefs(this).getInt("word_source", 1) == 1; // Parent settings can hide the reference
            wordLine.setText(src ? "The Prophet ﷺ  ·  " + h.source : "The Prophet ﷺ");
        }
    }

    private void buildTiles() {
        // just the apps floating along the bottom, no box around them, like the family home's row:
        // the bottom shade keeps them and their names readable

        tileRow = new LinearLayout(this);
        tileRow.setGravity(Gravity.CENTER);
        tileRow.setClipChildren(false);
        tileRow.setClipToPadding(false);
        tileRow.setPadding(48, 0, 48, DOCK_NAME); // the bottom strip is where the focused app's name appears
        tileRow.setBackground(glow); // the focused app's colour, glowing softly behind its tile
        place(tileRow, 80, DOCK_Y, 1760, DOCK_H);

        // the focused app's name, under its tile inside the dock (no box)
        tileLabel = text("", 30, fBold, WHITE);
        tileLabel.setGravity(Gravity.CENTER);
        labelPill = tileLabel;
        tileLabel.setAlpha(0); // shown once a tile has focus
        place(tileLabel, 0, DOCK_Y + DOCK_H - DOCK_NAME - 4, -2, -2);
    }

    /** Each new text re-measures the whole screen, so the once-a-second clock only sets what really changed. */
    private static void setIfChanged(TextView t, CharSequence text) {
        if (!TextUtils.equals(t.getText(), text)) t.setText(text);
    }

    private void drawGreeting() {
        Calendar now = Calendar.getInstance(Rules.KUWAIT);
        int h = now.get(Calendar.HOUR_OF_DAY);
        java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("EEEE d MMMM", Locale.ENGLISH);
        day.setTimeZone(Rules.KUWAIT);
        setIfChanged(hello, (h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening") + "  ·  " + day.format(now.getTime()));
        setIfChanged(weatherText, weather);
        int wv = weather.isEmpty() ? View.GONE : View.VISIBLE;
        if (weatherText.getVisibility() != wv) weatherText.setVisibility(wv);
        drawStars();
    }

    /** "★ 3 Ghanim   ★ 2 Noha": the reward stars each child has saved up. */
    private void drawStars() {
        String[] kids = Rules.kids(this);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < kids.length; i++) {
            if (i > 0) b.append("     ");
            b.append("★ ").append(Rules.stars(this, i)).append("  ").append(kids[i]);
        }
        starsText.setText(b.toString());
    }

    static final int DOCK_Y = 836, DOCK_H = 208, DOCK_NAME = 52;
    private final BrandGlow glow = new BrandGlow();
    private final List<Integer> tileColors = new ArrayList<>();

    /** Puts the focused app's name just under its tile, rising in softly. */
    private void moveLabel(View tile, String label) {
        tileLabel.setText(label);
        tileLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float cx = 80 + tile.getLeft() + tile.getWidth() / 2f; // the dock starts 80 px in
        float x = Math.max(80, Math.min(1840 - tileLabel.getMeasuredWidth(), cx - tileLabel.getMeasuredWidth() / 2f));
        tileLabel.animate().cancel();
        tileLabel.setTranslationX(x);
        tileLabel.setAlpha(0);
        tileLabel.setTranslationY(8);
        tileLabel.animate().alpha(1).translationY(0).setDuration(250).setInterpolator(EASE).start();
    }

    /**
     * Focus in the dock: the focused tile grows a little and lifts, its neighbours slide aside so nothing
     * overlaps, and the app's own colour glows behind it. No white box or ring.
     */
    private void magnify(int focused) {
        for (int i = 0; i < tileRow.getChildCount(); i++) {
            View t = tileRow.getChildAt(i);
            int dist = focused < 0 ? 9 : Math.abs(i - focused);
            float sc = dist == 0 ? 1.12f : dist == 1 ? 1.03f : 1f;
            float lift = dist == 0 ? -12 : dist == 1 ? -3 : 0;
            float side = focused < 0 || dist == 0 ? 0 : Math.signum(i - focused) * (dist == 1 ? 16 : 20);
            rest(t, sc).animate().scaleX(sc).scaleY(sc).translationX(side).translationY(lift).setDuration(250).setInterpolator(GROW).start();
        }
        if (focused < 0 || focused >= tileColors.size()) { glow.moveTo(-1, 0); return; }
        View f = tileRow.getChildAt(focused);
        int c = tileColors.get(focused);
        if (f.getWidth() == 0) f.post(() -> glow.moveTo(f.getLeft() + f.getWidth() / 2f, c));
        else glow.moveTo(f.getLeft() + f.getWidth() / 2f, c);
    }
    private static final DecelerateInterpolator GROW = new DecelerateInterpolator(2f);

    /** The colour each app is known by, for its glow in the dock (the same colours as the family home). */
    private int appColor(String pkg) {
        switch (pkg) {
            case "com.netflix.ninja": return 0xFFE50914;
            case "com.crunchyroll.crunchyroid": return 0xFFF47521;
            case "com.amazon.amazonvideo.livingroom": return 0xFF1AA3E8;
            case "net.mbc.shahidTV": case "net.mbc.shahid": return 0xFF1FD3A5;
            case "org.xbmc.kodi": case "org.videolan.vlc": return 0xFF17B2E7;
            case "com.disney.disneyplus": return 0xFF1F80E0;
            default: return 0xFFDDE6FF;
        }
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
    private void refreshTiles() { refreshTiles(false); }

    /** What the row was last built from: the apps in order, icons or banners, and the world. */
    private String tilesKey;

    private void refreshTiles(boolean onlyIfChanged) {
        PackageManager pm = getPackageManager();
        List<String[]> items = new ArrayList<>(); // {pkg or "", label}
        items.add(new String[]{"", "My cartoons"});
        Set<String> allowed = Rules.apps(this);
        for (ResolveInfo r : launchables()) {
            String p = r.activityInfo.packageName;
            if (allowed.contains(p) && !Rules.NEVER.contains(p)) items.add(new String[]{p, r.loadLabel(pm).toString()});
        }
        sortByOrder(items);
        int n = items.size();
        boolean icons = Rules.prefs(this).getInt("icons", 0) == 0;
        // the lock tile's red setup dot is part of the row too: it must come and go with the permissions
        StringBuilder key = new StringBuilder(icons + "|" + System.identityHashCode(world) + "|" + setupComplete() + "|");
        for (String[] it : items) key.append(it[0]).append('=').append(it[1]).append(';');
        if (onlyIfChanged && key.toString().equals(tilesKey) && tiles.size() == n) return;
        tilesKey = key.toString();
        View focused = getCurrentFocus();
        boolean hadFocus = focused != null && tiles.contains(focused);
        tileRow.removeAllViews();
        tiles.clear();
        tileColors.clear();
        // app icons are square like on the family home, a little bigger for small hands on the remote;
        // banners are 16:9 like the TV's own, so nothing gets cropped
        int w = Math.max(60, icons ? Math.min(136, 1340 / n - 44) : Math.min(250, 1460 / n - 52)), h = icons ? w : w * 9 / 16;
        float corner = icons ? w * 0.24f : 26;
        for (int i = 0; i < n; i++) {
            String pkg = items.get(i)[0], label = items.get(i)[1];
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setTag(pkg.isEmpty() ? "cartoons" : pkg);
            ImageView img = new ImageView(this);
            if (pkg.isEmpty()) {
                tile.setBackground(roundGrad(new int[]{world.body, world.bodyDark}, corner));
                TextView t = icons ? text("🎬", w * 0.46f, fMed, WHITE) : text("▶ Cartoons", 32, fHeavy, WHITE);
                t.setGravity(Gravity.CENTER);
                t.setIncludeFontPadding(false);
                if (icons) t.setPadding(0, 0, 0, 0);
                tile.addView(t, new FrameLayout.LayoutParams(-1, -1));
            } else if (icons) {
                // the icon bitmap has its corners rounded already, so nothing is clipped while it animates
                img.setScaleType(ImageView.ScaleType.FIT_XY);
                tile.addView(img, new FrameLayout.LayoutParams(-1, -1));
                Drawable cached = iconCache.get(pkg);
                if (cached != null) img.setImageDrawable(cached);
                else {
                    tile.setBackground(roundGrad(new int[]{appColor(pkg) == 0xFFDDE6FF ? 0xFF3A4766 : appColor(pkg), 0xFF141A2A}, corner));
                    bg.execute(() -> {
                        Drawable d;
                        try { d = Icons.fullBleed(getResources(), getPackageManager().getApplicationIcon(pkg), 0.24f); } catch (Exception e) { return; }
                        ui.post(() -> { iconCache.put(pkg, d); img.setImageDrawable(d); tile.setBackground(null); });
                    });
                }
            } else {
                tile.setClipToOutline(true);
                tile.setOutlineProvider(rounded(26));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                tile.setBackgroundColor(0xFF20263A); // a plain card until the banner is read, off the main thread
                tile.addView(img, new FrameLayout.LayoutParams(-1, -1));
                Drawable cachedArt = iconCache.get("banner:" + pkg);
                if (cachedArt != null) showBanner(tile, img, cachedArt, pkg, h);
                else bg.execute(() -> {
                    Drawable art = banner(pkg);
                    if (art == null) {
                        // no banner: the app's own icon, made full-bleed like the icon tiles (marked by the inset)
                        try { art = Icons.fullBleed(getResources(), getPackageManager().getApplicationIcon(pkg), 0.24f); } catch (Exception e) { return; }
                        art = new android.graphics.drawable.InsetDrawable(art, 0);
                    }
                    Drawable d = art;
                    ui.post(() -> { iconCache.put("banner:" + pkg, d); showBanner(tile, img, d, pkg, h); });
                });
            }
            tile.setOutlineAmbientShadowColor(0);
            tile.setOutlineSpotShadowColor(0);
            // a fine light edge on every tile, as on the family home's row
            tile.setForeground(edge(corner, 2, 0x4DFFFFFF));
            tile.setPivotX(w / 2f);
            tile.setPivotY(h);

            final int index = i;
            tileColors.add(pkg.isEmpty() ? world.accent : appColor(pkg));
            tile.setOnFocusChangeListener((v, has) -> {
                if (has) {
                    lastTile = index;
                    magnify(index);
                    v.post(() -> moveLabel(v, arrangeTile == v ? ARRANGE_HINT : label));
                } else {
                    v.post(() -> { if (!dockFocused()) { magnify(-1); tileLabel.animate().alpha(0).setDuration(150).start(); } });
                }
            });
            tile.setOnClickListener(v -> {
                if (arrangeTile != null) return;
                if (pkg.isEmpty()) openCartoons(); else launch(pkg);
            });
            // a grown-up holds OK on an app to move it along the row, or take it off (after the PIN)
            tile.setOnLongClickListener(v -> {
                if (arrangeTile == null) askPin("check", () -> startArrange(v));
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.leftMargin = lp.rightMargin = icons ? 22 : 16; // neighbours slide aside when one grows, so tiles never overlap
            tileRow.addView(tile, lp);
            tiles.add(tile);
        }

        // the parent lock: small and quiet at the end of the row
        FrameLayout lock = new FrameLayout(this);
        lock.setFocusable(true);
        lock.setBackground(new Glass(44, 0x40000000));
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
        tileColors.add(0xFFFFD45A);
        final int lockIndex = tiles.size();
        lock.setOnFocusChangeListener((v, has) -> {
            if (has) { magnify(lockIndex); v.post(() -> moveLabel(v, "Grown-ups")); }
            else v.post(() -> { if (!dockFocused()) { magnify(-1); tileLabel.animate().alpha(0).setDuration(150).start(); } });
        });
        lock.setOnClickListener(v -> askPin("check", this::openParent));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(88, 88);
        ll.leftMargin = 30;
        tileRow.addView(lock, ll);
        noAlphaLayers(tileRow);
        lockTile = lock;

        if (hadFocus || getCurrentFocus() == null) {
            if (anyPanelOpen()) return;
            tiles.get(Math.min(lastTile, tiles.size() - 1)).requestFocus();
        }
    }

    // ================================================================== the parent's own look

    /** Card style: 0 smoky glass like the family home, 1 bright glass, 2 no boxes at all. */
    private Drawable cardBg(float radius) {
        switch (Rules.prefs(this).getInt("style", 0)) {
            case 1: return glass(radius);
            case 2: return null;
            default: return new Glass(radius, 0x38000000);
        }
    }

    /** Applies the grown-up's choices from the Look section: card style, which cards show, dimming and the app row. */
    private void applyLook() {
        for (View v : new View[]{welcomeCard, clockCard, charCard, wordCard}) if (v != null) v.setBackground(cardBg(48));
        boolean c = Rules.prefs(this).getInt("char_card", 1) == 1, w = Rules.prefs(this).getInt("word_card", 1) == 1;
        charCard.setVisibility(c ? View.VISIBLE : View.GONE);
        wordCard.setVisibility(w ? View.VISIBLE : View.GONE);
        // one card alone takes the whole width
        FrameLayout.LayoutParams cl = (FrameLayout.LayoutParams) charCard.getLayoutParams();
        FrameLayout.LayoutParams wl = (FrameLayout.LayoutParams) wordCard.getLayoutParams();
        cl.width = w ? MID_SPLIT - 80 - 30 : 1760;
        wl.leftMargin = c ? MID_SPLIT : 80;
        wl.width = c ? 1840 - MID_SPLIT : 1760;
        charCard.setLayoutParams(cl);
        wordCard.setLayoutParams(wl);
        charCard.setMinimumHeight(0);
        wordCard.setMinimumHeight(0);
        applyShade();
        if (tileRow != null && world != null) refreshTiles();
        noAlphaLayers(stage);
    }

    /** Wallpaper dimming (default 8%, like the family home) folded into the top and bottom shades. */
    private void applyShade() {
        if (topShade == null) return;
        float dim = Rules.prefs(this).getInt("dim", 8) / 100f;
        int mid = withDim(0, dim); // the shades fade into exactly the middle band's dimming, so there is no seam
        topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{withDim(0x73, dim), mid}));
        if (mid == 0) midShade.setBackground(null); else midShade.setBackgroundColor(mid);
        bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{withDim(0xA6, dim), mid}));
    }

    /** A black of alpha a, with the dimming added on top: 1 - (1 - a)(1 - dim). */
    private static int withDim(int a, float dim) {
        float t = 1 - (1 - a / 255f) * (1 - dim);
        return Math.round(t * 255) << 24;
    }

    private static GradientDrawable roundGrad(int[] colors, float radius) {
        GradientDrawable d = grad(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radius);
        return d;
    }

    private static GradientDrawable edge(float radius, int width, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radius);
        d.setStroke(width, color);
        return d;
    }

    /** The grown-up's order for the row ("cartoons" and package names); apps not in it yet go at the end. */
    private void sortByOrder(List<String[]> items) {
        List<String> order = java.util.Arrays.asList(Rules.prefs(this).getString("order", "").split(","));
        List<String[]> out = new ArrayList<>(), rest = new ArrayList<>();
        for (String id : order) for (String[] it : items) if ((it[0].isEmpty() ? "cartoons" : it[0]).equals(id)) out.add(it);
        for (String[] it : items) if (!out.contains(it)) rest.add(it);
        out.addAll(rest);
        items.clear();
        items.addAll(out);
    }

    private void saveOrder() {
        StringBuilder b = new StringBuilder();
        for (View t : tiles) b.append(t.getTag()).append(',');
        Rules.prefs(this).edit().putString("order", b.toString()).apply();
    }

    static final String ARRANGE_HINT = "◀ ▶  move     ▼  take off     OK  done";

    /** After the PIN: the app lifts with a gold edge, the arrows move it, down takes it off the row, OK finishes. */
    private void startArrange(View tile) {
        if (tile.getParent() != tileRow) return;
        arrangeTile = tile;
        tile.setForeground(edge(cornerOf(tile), 5, 0xFFFFD45A));
        tile.requestFocus();
        tile.post(() -> moveLabel(tile, ARRANGE_HINT));
    }

    private float cornerOf(View tile) {
        return Rules.prefs(this).getInt("icons", 0) == 0 ? tile.getWidth() * 0.24f : 26;
    }

    private void endArrange() {
        View t = arrangeTile;
        if (t == null) return;
        arrangeTile = null;
        t.setForeground(edge(cornerOf(t), 2, 0x4DFFFFFF));
        saveOrder();
        refreshTiles();
    }

    private boolean arrangeKey(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (ok || code == KeyEvent.KEYCODE_BACK) endArrange();
            return true;
        }
        int i = tiles.indexOf(arrangeTile);
        if (i < 0) { arrangeTile = null; return true; }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int to = i + (code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
            if (to < 0 || to >= tiles.size()) return true;
            View t = arrangeTile;
            ViewGroup.LayoutParams lp = t.getLayoutParams();
            tileRow.removeView(t);
            tileRow.addView(t, to, lp);
            tiles.remove(i);
            tiles.add(to, t);
            tileColors.add(to, tileColors.remove(i));
            t.requestFocus();
            lastTile = to;
            ui.post(() -> { magnify(to); if (arrangeTile != null) moveLabel(arrangeTile, ARRANGE_HINT); });
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            String id = String.valueOf(arrangeTile.getTag());
            if (id.equals("cartoons")) { moveLabel(arrangeTile, "My cartoons always stays"); return true; }
            Rules.toggleApp(this, id); // no longer allowed: it can be ticked again in Their apps
            arrangeTile = null;
            saveOrder();
            lastTile = Math.max(0, i - 1);
            refreshTiles();
            if (!tiles.isEmpty()) tiles.get(Math.min(lastTile, tiles.size() - 1)).requestFocus();
            return true;
        }
        return true; // up and anything else: stay in arranging
    }

    private boolean dockFocused() {
        View f = getCurrentFocus();
        return f != null && f.getParent() == tileRow;
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

    /** A real banner fills the tile; an app with none shows its rounded icon on its own colour, like the icon tiles. */
    private void showBanner(FrameLayout tile, ImageView img, Drawable d, String pkg, int h) {
        if (d instanceof android.graphics.drawable.InsetDrawable) {
            int c = appColor(pkg);
            tile.setBackground(roundGrad(new int[]{c == 0xFFDDE6FF ? 0xFF3A4766 : c, 0xFF141A2A}, 26));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = Math.round(h * 0.19f); // the icon stands 62% of the tile's height
            img.setPadding(pad, pad, pad, pad);
        } else {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.setBackground(null);
        }
        img.setImageDrawable(d);
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
            setIfChanged(ringBig, shortMinutes(extra / 1000));
            setIfChanged(ringSmall, "bonus");
            setIfChanged(ringNote, "Bonus time from a grown-up");
        } else if (left == Long.MAX_VALUE) {
            ring.set(1, world == null ? WHITE : world.accent);
            setIfChanged(ringBig, "∞");
            setIfChanged(ringSmall, "no limit");
            setIfChanged(ringNote, "No time limit today");
        } else {
            float f = limit <= 0 ? 0 : left / (limit * 60f);
            ring.set(f, f > 0.25f ? (world == null ? WHITE : world.accent) : 0xFFFF6B5B);
            setIfChanged(ringBig, shortMinutes(left));
            setIfChanged(ringSmall, "left");
            int bed = Rules.bedtime(this);
            setIfChanged(ringNote, bed < 0 ? "Enjoy your shows!" : "Bedtime " + bedLabel(bed));
        }
        Calendar now = Calendar.getInstance(Rules.KUWAIT);
        int hr = now.get(Calendar.HOUR), min = now.get(Calendar.MINUTE);
        setIfChanged(clockText, (hr == 0 ? 12 : hr) + ":" + (min < 10 ? "0" : "") + min);
        setIfChanged(ampmText, now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM");
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
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        restIcon = text("🌙", 200, fHeavy, WHITE);
        restIcon.setGravity(Gravity.CENTER);
        col.addView(restIcon, new LinearLayout.LayoutParams(-2, -2));
        restTitle = text("", 88, fHeavy, WHITE);
        restTitle.setGravity(Gravity.CENTER);
        col.addView(restTitle, new LinearLayout.LayoutParams(-1, -2));
        restSub = text("", 40, fMed, SOFT);
        restSub.setGravity(Gravity.CENTER);
        col.addView(restSub, new LinearLayout.LayoutParams(-1, -2));
        restSurah = text("", 34, fBold, 0xFFFFD45A);
        restSurah.setGravity(Gravity.CENTER);
        restSurah.setPadding(0, 14, 0, 0);
        col.addView(restSurah, new LinearLayout.LayoutParams(-1, -2));
        restStars = new LinearLayout(this);
        restStars.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.topMargin = 30;
        col.addView(restStars, sl);
        TextView b = button("Grown-ups", 30);
        b.setOnClickListener(v -> askPin("check", this::openParent));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
        bl.topMargin = 30;
        col.addView(b, bl);
        restButton = b;
        restPanel.addView(col, new FrameLayout.LayoutParams(1700, -2, Gravity.CENTER));
    }

    /** On the time's-up screen, each child can spend a reward star for 15 more minutes. */
    private void drawRestStars(boolean bed) {
        restStars.removeAllViews();
        if (bed) return;
        String[] kids = Rules.kids(this);
        View first = null;
        for (int i = 0; i < kids.length; i++) {
            int n = Rules.stars(this, i);
            if (n <= 0) continue;
            final int kid = i;
            TextView b = goldButton("★  " + kids[i] + ": " + Rules.STAR_MINUTES + " more minutes  (" + n + " left)", 32);
            b.setOnClickListener(v -> {
                if (Rules.stars(this, kid) <= 0) return;
                Rules.setStars(this, kid, Rules.stars(this, kid) - 1);
                Stats.starSpent(this, kid);
                Rules.giveExtra(this, Rules.STAR_MINUTES);
                drawStars();
                tick();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.leftMargin = lp.rightMargin = 14;
            restStars.addView(b, lp);
            if (first == null) first = b;
        }
        restStars.setVisibility(first == null ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------- bedtime surah

    /** Short surahs for bedtime: Al-Fatihah, Al-Qadr, Al-Asr, Al-Kawthar, Al-Ikhlas, Al-Falaq, An-Nas. */
    static final int[] BED_SURAHS = {1, 97, 103, 108, 112, 113, 114};
    static final String[] BED_NAMES = {"Al-Fatihah", "Al-Qadr", "Al-Asr", "Al-Kawthar", "Al-Ikhlas", "Al-Falaq", "An-Nas"};
    /** Hafs murattal reciters (never Mishary Alafasy), used when the box has no recitations of its own. */
    static final String[][] RECITERS = {
            {"Mahmoud Khalil Al-Hussary", "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/"},
            {"Mohammed Siddiq Al-Minshawi", "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/"},
            {"Maher Al-Muaiqly", "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/"},
            {"Idrees Abkar", "https://cdn.mp3quran.net/audio/idrees-abkar/r1/"},
            {"Mohammed Ayyub", "https://cdn.mp3quran.net/audio/muhammad-ayyub/r1/"},
    };

    /** Once a night, when bedtime starts: a random short surah at the softened volume. */
    private void playBedSurah() {
        if (!Rules.windDownOn(this) || surah != null) return;
        Calendar night = Calendar.getInstance(Rules.KUWAIT);
        night.add(Calendar.HOUR_OF_DAY, -12); // the same "night" from evening until the morning
        String key = night.get(Calendar.YEAR) + "-" + night.get(Calendar.DAY_OF_YEAR);
        if (key.equals(Rules.prefs(this).getString("surah_night", ""))) return;
        Rules.prefs(this).edit().putString("surah_night", key).apply();
        int pick = random.nextInt(BED_SURAHS.length), number = BED_SURAHS[pick];
        String file = String.format(Locale.US, "%03d.mp3", number), source = null, reciter = null;
        // recitations the PC copied to the box (Music/Quran/<reciter>/001.mp3) come first
        File[] dirs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Quran").listFiles(File::isDirectory);
        List<File> local = new ArrayList<>();
        if (dirs != null) for (File d : dirs) {
            String n = d.getName().toLowerCase(Locale.ROOT);
            if (n.contains("afasy") || n.contains("afasi") || n.contains("عفاسي")) continue;
            if (new File(d, file).canRead()) local.add(d);
        }
        if (!local.isEmpty()) {
            File d = local.get(random.nextInt(local.size()));
            source = new File(d, file).getPath();
            reciter = d.getName().replaceAll("\\s*\\(\\d+\\)\\s*$", "");
        } else {
            String[] r = RECITERS[random.nextInt(RECITERS.length)];
            source = r[1] + file;
            reciter = r[0];
        }
        try {
            MediaPlayer p = new MediaPlayer();
            p.setDataSource(source);
            p.setOnPreparedListener(MediaPlayer::start);
            p.setOnCompletionListener(mp -> stopSurah());
            p.setOnErrorListener((mp, a, b) -> { stopSurah(); return true; });
            p.prepareAsync();
            surah = p;
            restSurah.setText("Surah " + BED_NAMES[pick] + "  ·  " + reciter);
        } catch (Exception e) { stopSurah(); }
    }

    private void stopSurah() {
        if (surah != null) try { surah.release(); } catch (Exception ignored) { }
        surah = null;
        if (restSurah != null) restSurah.setText("");
    }

    private void showRest(String why) {
        boolean bed = why.equals("bed");
        restTitle.setText(bed ? "Time for bed!" : "That's all for today!");
        restSub.setText(bed ? "Sleep well. Your shows will be waiting in the morning." : "Great watching! Let's play something else. See you tomorrow!");
        drawRestStars(bed);
        if (bed) playBedSurah();
        hide(cartoonsPanel);
        restPanel.setVisibility(View.VISIBLE);
        syncFocus();
        restPanel.setAlpha(0);
        restPanel.animate().alpha(1).setDuration(600).start();
        restIcon.setText(bed ? "🌙" : "⭐");
        if (pinPanel.getVisibility() != View.VISIBLE && parentPanel.getVisibility() != View.VISIBLE)
            (restStars.getChildCount() > 0 ? restStars.getChildAt(0) : restButton).requestFocus();
        if (player != null) player.pause();
    }

    private void hideRest() {
        stopSurah();
        restPanel.animate().alpha(0).setDuration(400).withEndAction(() -> {
            restPanel.setVisibility(View.GONE);
            syncFocus();
            if (!anyPanelOpen()) focusAfterPanel();
        }).start();
        if (player != null) try { player.start(); } catch (Exception ignored) { }
    }

    // ================================================================== my cartoons

    private GridLayout cartoonGrid;
    private LinearLayout cartoonsEmpty;
    private TextView cartoonsCount;

    private void buildCartoons() {
        cartoonsPanel = panel();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(86, 64, 86, 0); // with the scroller's 24 below: tiles 110 px from each edge
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.addView(text("🎬", 60, fMed, WHITE));
        TextView title = text("My cartoons", 64, fHeavy, WHITE);
        title.setPadding(22, 0, 0, -14);
        head.addView(title);
        cartoonsCount = text("", 32, fBold, DIM);
        cartoonsCount.setPadding(26, 0, 0, -2);
        head.addView(cartoonsCount);
        col.addView(head);
        cartoonsEmpty = new LinearLayout(this);
        cartoonsEmpty.setOrientation(LinearLayout.VERTICAL);
        cartoonsEmpty.setGravity(Gravity.CENTER_HORIZONTAL);
        cartoonsEmpty.setPadding(80, 50, 80, 60);
        cartoonsEmpty.setBackground(glass(48));
        TextView tv = text("📺", 140, fMed, WHITE);
        tv.setGravity(Gravity.CENTER);
        cartoonsEmpty.addView(tv);
        TextView none = text("No cartoons yet", 56, fHeavy, WHITE);
        none.setGravity(Gravity.CENTER);
        none.setPadding(0, 10, 0, 0);
        cartoonsEmpty.addView(none);
        TextView how = text("Grown-ups: put videos in Movies/Kids on the box, or in a Kids folder on the USB drive.", 34, fMed, SOFT);
        how.setGravity(Gravity.CENTER);
        how.setPadding(0, 16, 0, 0);
        cartoonsEmpty.addView(how, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(1100, -2);
        el.gravity = Gravity.CENTER_HORIZONTAL;
        el.topMargin = 140;
        col.addView(cartoonsEmpty, el);
        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(24, 30, 24, 60); // room inside the clip for a focused card (1.08 larger)
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
        cartoonsCount.setText(cartoons.isEmpty() ? "" : cartoons.size() == 1 ? "1 video" : cartoons.size() + " videos");
        show(cartoonsPanel);
        for (int i = 0; i < cartoons.size(); i++) {
            File f = cartoons.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setFocusable(true);
            card.setBackground(solid(0x1AFFFFFF, 26, 0x14));
            card.setClipToOutline(true);
            card.setOutlineProvider(rounded(26));
            // the picture: today's world colours with a play mark, until the video's own frame is ready
            FrameLayout pic = new FrameLayout(this);
            pic.setBackground(grad(GradientDrawable.Orientation.TL_BR, new int[]{world.body, world.bodyDark}));
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            pic.addView(img, new FrameLayout.LayoutParams(-1, -1));
            TextView playMark = text("▶", 56, fHeavy, 0xE6FFFFFF);
            playMark.setGravity(Gravity.CENTER);
            pic.addView(playMark, new FrameLayout.LayoutParams(-1, -1));
            card.addView(pic, new LinearLayout.LayoutParams(-1, 222)); // 16:9 of the 395 px card
            // the whole name, on as many lines as it needs
            TextView t = text(title(f), 30, fBold, WHITE);
            t.setPadding(20, 12, 20, 16);
            t.setMinLines(2); // cards in a row stay the same height; long names still get every line they need
            card.addView(t, new LinearLayout.LayoutParams(-1, -2));
            card.setOnFocusChangeListener((v, has) -> {
                rest(v, has ? 1.08f : 1f).animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(220).start();
                v.setBackground(has ? solid(WHITE, 26, 0x33) : solid(0x1AFFFFFF, 26, 0x14));
                ink(t, has ? 0xFF1A1033 : WHITE);
            });
            final int index = i;
            card.setOnClickListener(v -> play(index));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 395; lp.height = GridLayout.LayoutParams.WRAP_CONTENT; // four across, centred
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
        box.setPadding(60, 44, 60, 50);
        box.setBackground(sheet(44));
        TextView lockIcon = text("🔒", 64, fMed, WHITE);
        lockIcon.setGravity(Gravity.CENTER);
        box.addView(lockIcon, new LinearLayout.LayoutParams(-1, -2));
        pinTitle = text("", 50, fHeavy, WHITE);
        pinTitle.setGravity(Gravity.CENTER);
        box.addView(pinTitle);
        pinSub = text("", 30, fMed, DIM);
        pinSub.setGravity(Gravity.CENTER);
        pinSub.setPadding(0, 4, 0, 26);
        box.addView(pinSub, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(36, 36);
            dl.leftMargin = dl.rightMargin = 16;
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
            key.setBackground(solid(0x1AFFFFFF, 28, 0x14));
            key.setOnFocusChangeListener((v, has) -> {
                key.setBackground(solid(has ? WHITE : 0x1AFFFFFF, 28, has ? 0 : 0x14));
                ink(key, has ? 0xFF161028 : WHITE);
                rest(key, has ? 1.06f : 1f).animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(160).start();
            });
            key.setTag("dip"); // has its own press dip
            key.setOnClickListener(v -> {
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(() -> // the same dip as the family home's keys
                        v.animate().scaleX(v.isFocused() ? 1.06f : 1f).scaleY(v.isFocused() ? 1.06f : 1f).setDuration(140).start()).start();
                pinPress(k);
            });
            pad.addView(key, lp);
            if (Character.isDigit(k.charAt(0))) pinKeys.add(key);
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
            case "set": pinTitle.setText("Choose a parent PIN"); break;
            case "again": pinTitle.setText("Once more"); break;
            default: pinTitle.setText("Grown-ups only"); break;
        }
        setPinSub(m);
        if (m.equals("check")) showPinWait(); else restKeys(false);
        drawDots(0);
    }

    private void setPinSub(String m) {
        ui.removeCallbacks(pinNoteEnd);
        pinSub.setText(m.equals("set") ? "4 numbers. You'll need it to leave Kids Corner." : m.equals("again") ? "Type the same PIN again" : "Enter the parent PIN");
    }

    /** After too many wrong tries, the line under the title counts down the rest. */
    private void showPinWait() {
        long left = PinLock.waitLeft(Rules.prefs(this));
        ui.removeCallbacks(pinWaitTick);
        restKeys(left > 0);
        if (left <= 0) return;
        ui.removeCallbacks(pinNoteEnd);
        pinSub.setText(PinLock.waitText(left));
        ui.postDelayed(pinWaitTick, Math.min(left, 1000));
    }

    private final Runnable pinWaitTick = () -> {
        if (pinPanel == null || pinPanel.getVisibility() != View.VISIBLE || !pinMode.equals("check")) { restKeys(false); return; }
        if (PinLock.waitLeft(Rules.prefs(this)) > 0) showPinWait(); else { restKeys(false); pinSub.setText("Enter the parent PIN"); drawDots(0); }
    };

    /** While the pad rests after too many wrong tries, its number keys dim (they stay reachable, the countdown says why). */
    private final List<View> pinKeys = new ArrayList<>();
    private void restKeys(boolean resting) {
        for (View k : pinKeys) k.animate().alpha(resting ? 0.4f : 1f).setDuration(200).start();
    }

    /** A short message under the title for two seconds (wrong PIN, PINs that didn't match), then the usual line again. */
    private void pinNote(String msg) {
        if (PinLock.waitLeft(Rules.prefs(this)) > 0) return; // the rest countdown says more
        pinSub.setText(msg);
        ui.removeCallbacks(pinNoteEnd);
        ui.postDelayed(pinNoteEnd, 2000);
    }
    private final Runnable pinNoteEnd = () -> { if (vis(pinPanel) && PinLock.waitLeft(Rules.prefs(this)) <= 0) setPinSub(pinMode); };

    private void drawDots(int filledColorOverride) {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            boolean filled = i < pinEntry.length();
            d.setColor(filledColorOverride != 0 ? filledColorOverride : filled ? WHITE : 0x00000000);
            d.setStroke(4, filledColorOverride != 0 ? filledColorOverride : 0x99FFFFFF);
            pinDots[i].setBackground(d);
        }
    }

    private void pinPress(String k) {
        if (k.equals("⌫")) { if (!pinEntry.isEmpty()) pinEntry = pinEntry.substring(0, pinEntry.length() - 1); drawDots(0); return; }
        if (pinEntry.length() >= 4) return;
        // resting: a soft dip on the key is the only answer (no shake, no red), and the countdown stays
        if (pinMode.equals("check") && PinLock.waitLeft(Rules.prefs(this)) > 0) { showPinWait(); return; }
        pinEntry += k;
        drawDots(0);
        if (pinEntry.length() < 4) return;
        String entered = pinEntry;
        ui.postDelayed(() -> {
            switch (pinMode) {
                case "set": firstPin = entered; setPinMode("again"); return;
                case "again":
                    if (entered.equals(firstPin)) { PinLock.set(Rules.prefs(this), entered); pinDone(); }
                    else { wrong(); setPinModeLater("set", "The two PINs were different. Choose one again."); }
                    return;
                default:
                    if (PinLock.check(Rules.prefs(this), entered)) pinDone();
                    else { wrong(); setPinModeLater("check", "That PIN didn't match. Try again."); }
            }
        }, 160);
    }

    private void setPinModeLater(String m, String note) {
        pinNote(note);
        ui.postDelayed(() -> { setPinMode(m); pinNote(note); }, 650);
    }

    /** Every control answers OK the moment it goes down: a small dip and back (the PIN keys keep their own, tagged "dip"). */
    private static void pressDip(KeyEvent e, View f) {
        int c = e.getKeyCode();
        if (e.getAction() != KeyEvent.ACTION_DOWN || e.getRepeatCount() != 0 || f == null || !f.isClickable() || "dip".equals(f.getTag())) return;
        if (c != KeyEvent.KEYCODE_DPAD_CENTER && c != KeyEvent.KEYCODE_ENTER && c != KeyEvent.KEYCODE_NUMPAD_ENTER) return;
        // dips from, and comes back to, the size the item rests at (not whatever size it had mid-animation)
        Float r0 = REST.get(f);
        float r = r0 != null ? r0 : 1f;
        f.animate().setStartDelay(0).scaleX(r * 0.96f).scaleY(r * 0.96f).setDuration(70).setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).withEndAction(() -> {
            Float back = REST.get(f); // focus may have moved meanwhile
            float b = back != null ? back : 1f;
            f.animate().scaleX(b).scaleY(b).setDuration(140).setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
        }).start();
    }

    /** The scale each item rests at, set by every focus animation, so a press dip always returns to it. */
    private static final java.util.WeakHashMap<View, Float> REST = new java.util.WeakHashMap<>();

    static View rest(View v, float s) { REST.put(v, s); return v; }

    private void wrong() {
        drawDots(0xFFFF5A5A);
        View box = (View) pinTitle.getParent();
        // the same smooth, damped shake as the family home's PIN pad, one at a time
        if (pinShake != null) pinShake.cancel();
        android.animation.ValueAnimator a = pinShake = android.animation.ValueAnimator.ofFloat(0, -18, 16, -12, 8, -4, 0);
        a.setDuration(420);
        a.addUpdateListener(v -> box.setTranslationX((float) v.getAnimatedValue()));
        a.start();
    }
    private android.animation.ValueAnimator pinShake;

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
        col.setPadding(56, 44, 56, 30);
        col.setClipToPadding(false); // the list reaches 24 px into this padding for its focus room
        col.setBackground(sheet(44));
        col.addView(text("Kids Corner settings", 52, fHeavy, WHITE));
        TextView hint = text("Left and right change a setting. Back closes.", 30, fMed, DIM);
        hint.setPadding(0, 0, 0, 18);
        col.addView(hint);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setVerticalFadingEdgeEnabled(true); // rows fade out at the edge, so it's clear there is more below
        sv.setFadingEdgeLength(90);
        parentList = new LinearLayout(this);
        parentList.setOrientation(LinearLayout.VERTICAL);
        // 24 px each side inside the scroller, unclipped, so a focused row (1.03 larger) keeps its whole white edge
        parentList.setPadding(24, 0, 24, 0);
        parentList.setClipToPadding(false);
        sv.addView(parentList);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, 0, 1);
        slp.leftMargin = slp.rightMargin = -24;
        col.addView(sv, slp);
        parentPanel.addView(col, new FrameLayout.LayoutParams(1100, 960, Gravity.CENTER));
    }

    private void openParent() {
        parentList.removeAllViews();
        parentList.addView(section("Screen time"));
        parentFirst = choiceRow("School days (Sun to Thu)", LIMITS, "limit_school", 90, KidsActivity::limitLabel);
        choiceRow("Weekend (Fri and Sat)", LIMITS, "limit_weekend", 120, KidsActivity::limitLabel);
        choiceRow("Bedtime (until 7 am)", BEDTIMES, "bedtime", 20 * 60 + 30, KidsActivity::bedLabel);
        choiceRow("Bedtime wind-down", new int[]{0, 1}, "winddown", 1, v -> v == 1 ? "On" : "Off");
        parentList.addView(section("Look"));
        choiceRow("Screen size (fit the TV's edges)", FITS, "fit", 100, v -> v + "%");
        int[] worlds = new int[Worlds.ALL.length + 1];
        for (int i = 0; i < worlds.length; i++) worlds[i] = i - 1;
        choiceRow("World", worlds, "world", -1, v -> v < 0 ? "A new one every day" : Worlds.ALL[v].name);
        List<File> clips = wallpapers();
        wallpaperPick(clips); // brings the remembered clip's place in the list up to date before the row shows it
        int[] wp = new int[clips.size() + 2];
        for (int i = 0; i < wp.length; i++) wp[i] = i - 2;
        choiceRow("Wallpaper", wp, "wallpaper", -1, v -> v == -2 ? "Any of the family's" : v < 0 || v >= clips.size() ? "Matches the world" : clipName(clips.get(v)));
        choiceRow("Darken the wallpaper", new int[]{0, 8, 16, 24, 32}, "dim", 8, v -> v == 0 ? "Off" : v + "%");
        choiceRow("Cards", new int[]{0, 1, 2}, "style", 0, v -> v == 0 ? "Smoky glass" : v == 1 ? "Bright glass" : "No boxes");
        choiceRow("Apps along the bottom", new int[]{0, 1}, "icons", 0, v -> v == 0 ? "App icons" : "Big banners");
        choiceRow("Character of the day", new int[]{1, 0}, "char_card", 1, v -> v == 1 ? "Show" : "Hide");
        choiceRow("Name of Allah or hadith", new int[]{1, 0}, "word_card", 1, v -> v == 1 ? "Show" : "Hide");
        choiceRow("Hadith source (Bukhari, Muslim…)", new int[]{1, 0}, "word_source", 1, v -> v == 1 ? "Show" : "Hide");
        TextView arrangeHint = text("To move an app along the bottom, or take it off, hold OK on it.", 30, fMed, DIM);
        arrangeHint.setPadding(28, 6, 28, 6);
        parentList.addView(arrangeHint);
        parentList.addView(section("Reward stars"));
        TextView starsHint = text("Each star buys " + Rules.STAR_MINUTES + " minutes of extra time when time is up.", 30, fMed, DIM);
        starsHint.setPadding(28, 0, 28, 6);
        parentList.addView(starsHint);
        int[] counts = new int[31];
        for (int i = 0; i < counts.length; i++) counts[i] = i;
        String[] kids = Rules.kids(this);
        for (int i = 0; i < kids.length; i++)
            choiceRow("★  " + kids[i], counts, "stars_" + i, 0, v -> v + (v == 1 ? " star" : " stars"));

        parentList.addView(section("Extra time now"));
        LinearLayout extra = new LinearLayout(this);
        extra.setPadding(28, 10, 0, 6);
        int[][] gifts = {{15}, {30}, {60}};
        for (int[] g : gifts) {
            TextView b = button(g[0] == 60 ? "+1 hour" : "+" + g[0] + " min", 30);
            b.setOnClickListener(v -> { Rules.giveExtra(this, g[0]); toastIn(b, "Added"); tick(); });
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
            bl.rightMargin = 16;
            extra.addView(b, bl);
        }
        TextView reset = button("Reset today", 30);
        reset.setOnClickListener(v -> { Rules.setUsed(this, 0); Rules.prefs(this).edit().remove("extra_until").apply(); toastIn(reset, "Reset"); tick(); });
        extra.addView(reset);
        parentList.addView(extra);

        TextView watched = text("Watched today: " + minutes(Rules.used(this)), 30, fMed, DIM);
        watched.setPadding(28, 14, 0, 4);
        parentList.addView(watched);

        parentList.addView(section("More"));

        actionRow("Their apps", "Choose which apps they can open", this::openApps);
        actionRow("Change PIN", "", () -> askPin("set", this::openParent));
        setupText = text(setupSummary(), 30, fMed, DIM);
        setupText.setPadding(28, 12, 28, 12);
        parentList.addView(setupText);
        actionRow("Leave Kids Corner", "Turns kids mode off until it's opened again", this::leave);

        show(parentPanel);
        parentFirst.requestFocus();
        ui.removeCallbacks(parentIdle);
        ui.postDelayed(parentIdle, PARENT_IDLE_MS);
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
        TextView value = text("", 32, fHeavy, WHITE);
        value.setPadding(24, 2, 24, 4);
        value.setBackground(solid(0x1AFFFFFF, 30, 0));
        row.addView(value);
        Runnable draw = () -> {
            int cur = Rules.prefs(this).getInt(key, def), at = 0;
            for (int i = 0; i < values.length; i++) if (values[i] == cur) at = i;
            // the arrow that can't go any further dims, so the ends of the list are clear
            String mid = "  " + label.of(cur) + "  ";
            android.text.SpannableString sp = new android.text.SpannableString("◀" + mid + "▶");
            if (at == 0) sp.setSpan(new android.text.style.ForegroundColorSpan(ENDED), 0, 1, 0);
            if (at == values.length - 1) sp.setSpan(new android.text.style.ForegroundColorSpan(ENDED), sp.length() - 1, sp.length(), 0);
            value.setText(sp);
        };
        draw.run();
        row.setOnFocusChangeListener((v, has) -> focusRow(row, has, 1.03f, 22));
        row.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
            if (ok && e.getRepeatCount() > 0) return true;
            int dir = code == KeyEvent.KEYCODE_DPAD_RIGHT || ok ? 1 : code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 0;
            if (dir == 0) return false;
            int cur = Rules.prefs(this).getInt(key, def), at = 0;
            for (int i = 0; i < values.length; i++) if (values[i] == cur) at = i;
            // OK steps forward like the family home's settings, going round to the first value after the last
            at = ok ? (at + 1) % values.length : Math.max(0, Math.min(values.length - 1, at + dir));
            Rules.prefs(this).edit().putInt(key, values[at]).apply();
            draw.run();
            if (key.equals("world")) { applyWorld(); refreshTiles(); }
            if (key.equals("fit")) fitStage();
            if (key.equals("style") || key.equals("icons") || key.equals("dim") || key.equals("char_card") || key.equals("word_card")) applyLook();
            if (key.equals("word_source")) drawGoodWord();
            if (key.equals("wallpaper")) {
                List<File> clips = wallpapers(); // remembered by name, so a new clip in the folder never swaps it
                Rules.prefs(this).edit().putString("wallpaper_name", values[at] >= 0 && values[at] < clips.size() ? clips.get(values[at]).getName() : "").apply();
                stopClip(); playClip();
            }
            drawStars();
            if (restPanel.getVisibility() == View.VISIBLE) drawRestStars(Rules.resting(this).equals("bed"));
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
        if (!sub.isEmpty()) row.addView(text(sub, 30, fMed, DIM));
        row.setOnFocusChangeListener((v, has) -> focusRow(row, has, 1.03f, 22));
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
                + (u ? "" : "\nWithout app watching the kids lock can't send him back from other apps.")
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
        col.setPadding(56, 44, 56, 20);
        col.setBackground(sheet(44));
        col.addView(text("Their apps", 52, fHeavy, WHITE));
        TextView hint = text("OK ticks or unticks an app. YouTube, the Play Store, Settings and browsers are never allowed.", 30, fMed, DIM);
        col.addView(hint);
        ScrollView sv = new ScrollView(this);
        sv.setPadding(0, 30, 0, 30);
        sv.setClipToPadding(false);
        sv.setVerticalScrollBarEnabled(false);
        sv.setVerticalFadingEdgeEnabled(true);
        sv.setFadingEdgeLength(90);
        sv.setClipChildren(false); // a focused cell (1.05 larger) is not cut back to the grid's edge
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(4);
        appGrid.setClipChildren(false);
        sv.addView(appGrid);
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        appsPanel.addView(col, new FrameLayout.LayoutParams(1456, 900, Gravity.CENTER));
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
            cell.setPadding(14, 22, 14, 20);
            FrameLayout iconBox = new FrameLayout(this);
            iconBox.setBackground(solid(0x14FFFFFF, 30, 0));
            ImageView icon = new ImageView(this);
            Drawable known = iconCache.get("raw:" + pkg);
            if (known != null) icon.setImageDrawable(known);
            else bg.execute(() -> { // the picker opens at once; the icons arrive a moment later, off the main thread
                Drawable d;
                try { d = r.loadIcon(getPackageManager()); } catch (Exception e) { return; }
                ui.post(() -> { iconCache.put("raw:" + pkg, d); icon.setImageDrawable(d); });
            });
            iconBox.addView(icon, new FrameLayout.LayoutParams(104, 104, Gravity.CENTER));
            cell.addView(iconBox, new LinearLayout.LayoutParams(144, 144));
            TextView name = text(r.loadLabel(pm).toString(), 30, fBold, WHITE);
            name.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, -2);
            nl.topMargin = 14;
            cell.addView(name, nl);
            TextView mark = text("", 30, fBold, WHITE);
            mark.setPadding(20, 0, 20, 2);
            LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-2, -2);
            ml.topMargin = 8;
            cell.addView(mark, ml);
            Runnable draw = () -> {
                boolean on = Rules.apps(this).contains(pkg);
                mark.setText(on ? "✓  allowed" : "not allowed");
                mark.setTextColor(on ? 0xFF8CFF7A : DIM);
                mark.setBackground(solid(on ? 0x2E7CFC6A : 0x14FFFFFF, 26, 0));
                icon.setImageAlpha(on ? 255 : 115); // the picture only, no offscreen layer
            };
            draw.run();
            cell.setOnFocusChangeListener((v, has) -> focusRow(cell, has, 1.05f, 30));
            cell.setOnClickListener(v -> { Rules.toggleApp(this, pkg); draw.run(); });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 316; lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.setMargins(10, 0, 10, 20); // even on both sides, so the grid sits centred in the card
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

    /** A view fading in or out is drawn straight onto the screen, not into a spare offscreen picture every frame (same as TV Home). */
    private static void noAlphaLayers(View v) {
        v.forceHasOverlappingRendering(false);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) noAlphaLayers(g.getChildAt(i));
        }
    }

    private void closePanels() {
        for (View v : new View[]{pinPanel, parentPanel, appsPanel, cartoonsPanel}) hide(v);
        focusAfterPanel();
    }

    private FrameLayout panel() {
        FrameLayout p = new FrameLayout(this);
        p.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xF20A0718, 0xFC070512})); // full screen: no dithering
        p.setVisibility(View.GONE);
        p.setClickable(true);
        stage.addView(p, new FrameLayout.LayoutParams(1920, 1080));
        return p;
    }

    private void show(View p) {
        noAlphaLayers(p);
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
        ink(t, color);
        // Baloo's own line height is 1.7 em, with a lot of air above and below the letters.
        // Wrapped lines sit closer (0.8 of it), and each text box gives back some of that air.
        t.setLineSpacing(0, 0.8f);
        trim(t, size);
        return t;
    }

    /**
     * Sets a text's colour. Light text gets a soft dark shadow so it stays readable over a bright
     * wallpaper or tile; dark text (on a focused white or gold button) gets none.
     * The shadow is drawn with the letters themselves, so it adds no extra layer.
     */
    private static void ink(TextView t, int color) {
        t.setTextColor(color);
        float light = (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f;
        if (light > 0.45f) t.setShadowLayer(8, 0, 2, 0xB3000000);
        else t.setShadowLayer(0, 0, 0, 0);
    }

    /** Takes back the empty space Baloo leaves above and below its letters, so cards stay compact. */
    private static void trim(TextView t, float size) {
        t.setPadding(0, -Math.round(size * 0.2f), 0, -Math.round(size * 0.26f));
    }

    private TextView button(String s, float size) {
        TextView b = text(s, size, fBold, WHITE);
        b.setFocusable(true);
        b.setPadding(34, 12, 34, 12);
        b.setGravity(Gravity.CENTER);
        b.setBackground(solid(0x26FFFFFF, 40, 0));
        b.setOnFocusChangeListener((v, has) -> {
            b.setBackground(solid(has ? WHITE : 0x26FFFFFF, 40, 0));
            ink(b, has ? 0xFF161028 : WHITE);
            rest(b, has ? 1.06f : 1f).animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(160).start(); // like the gold buttons
        });
        return b;
    }

    /** A focused row or cell: a soft fill, a clear white edge and a little lift, so focus never rests on a tint alone. */
    private static void focusRow(View v, boolean has, float scale, float radius) {
        v.setBackground(has ? solid(0x26FFFFFF, radius, 0xE6) : null);
        rest(v, has ? scale : 1f).animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f).setDuration(160).start();
    }

    /** A gradient drawn with dithering, so dark fades stay smooth on the TV instead of showing stripes. */
    @SuppressWarnings("deprecation")
    private static GradientDrawable grad(GradientDrawable.Orientation o, int[] colors) {
        GradientDrawable d = new GradientDrawable(o, colors);
        d.setDither(true);
        return d;
    }

    private static GradientDrawable glass(float radius) {
        // light at the top, clearer below, with a fine bright edge: the video shows through
        GradientDrawable d = grad(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x40FFFFFF, 0x1FFFFFFF});
        d.setStroke(2, 0x52FFFFFF);
        d.setCornerRadius(radius);
        return d;
    }

    /** The card every grown-up panel sits on: deep violet, lit from the top left, with a fine edge. */
    private static GradientDrawable sheet(float radius) {
        GradientDrawable d = grad(GradientDrawable.Orientation.TL_BR, new int[]{0xFF241B45, 0xFF150F2B});
        d.setCornerRadius(radius);
        d.setStroke(2, 0x30FFFFFF);
        return d;
    }

    /** A small coloured heading inside a panel: "SCREEN TIME". */
    private TextView section(String s) {
        TextView t = text(s.toUpperCase(Locale.ROOT), 30, fBold, world == null ? 0xFFFFD45A : world.accent);
        t.setLetterSpacing(0.12f);
        t.setPadding(28, 26, 28, 4);
        return t;
    }

    /** Gold buttons for spending reward stars. */
    private TextView goldButton(String s, float size) {
        TextView b = text(s, size, fBold, 0xFFFFE08A);
        b.setFocusable(true);
        b.setPadding(36, 14, 36, 14);
        b.setGravity(Gravity.CENTER);
        b.setBackground(solid(0x2EFFC233, 40, 0));
        b.setOnFocusChangeListener((v, has) -> {
            b.setBackground(solid(has ? 0xFFFFD45A : 0x2EFFC233, 40, 0));
            ink(b, has ? 0xFF2A1A00 : 0xFFFFE08A);
            rest(b, has ? 1.06f : 1f).animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(180).start();
        });
        return b;
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
