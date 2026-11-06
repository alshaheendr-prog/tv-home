package com.family.kids;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

/**
 * Plays his cartoons full screen. OK pauses, left and right skip 10 seconds, Back goes home.
 * It remembers where each episode stopped, plays the next one when an episode ends,
 * and closes by itself when screen time is up.
 */
public class PlayerActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private VideoView video;
    private LinearLayout bar;
    private TextView title, time, paused;
    private View fill;
    private String[] paths;
    private int index;
    private SharedPreferences spots;
    private float k = 1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        paths = getIntent().getStringArrayExtra("paths");
        index = getIntent().getIntExtra("index", 0);
        if (paths == null || paths.length == 0) { finish(); return; }
        spots = getSharedPreferences("spots", MODE_PRIVATE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        k = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;
        Typeface bold = getResources().getFont(R.font.baloo_bold), heavy = getResources().getFont(R.font.baloo_extrabold);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        paused = new TextView(this);
        paused.setText("❚❚");
        paused.setTextColor(0xE6FFFFFF);
        paused.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(120));
        // a dark disc and a shadow, so the sign still shows on a white or sky-blue frame
        paused.setShadowLayer(px(16), 0, px(3), 0xB3000000);
        paused.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(0x8C000000);
        paused.setBackground(disc);
        paused.setVisibility(View.GONE);
        root.addView(paused, new FrameLayout.LayoutParams(px(220), px(220), Gravity.CENTER));

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(px(90), px(40), px(90), px(60));
        GradientDrawable barBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xE6000000, 0x8C000000, 0x00000000});
        barBg.setDither(true);
        bar.setBackground(barBg);
        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTypeface(heavy);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(48));
        title.setShadowLayer(px(8), 0, px(2), 0xB3000000);
        bar.addView(title);
        FrameLayout track = new FrameLayout(this);
        GradientDrawable t = new GradientDrawable();
        t.setColor(0x40FFFFFF);
        t.setCornerRadius(px(6));
        track.setBackground(t);
        fill = new View(this);
        GradientDrawable f = new GradientDrawable();
        f.setColor(0xFFFFC233);
        f.setCornerRadius(px(6));
        fill.setBackground(f);
        fill.setPivotX(0);
        track.addView(fill, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, px(12));
        tl.topMargin = px(16);
        bar.addView(track, tl);
        time = new TextView(this);
        time.setTextColor(0xE6FFFFFF);
        time.setShadowLayer(px(8), 0, px(2), 0xB3000000);
        time.setTypeface(bold);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(30));
        time.setPadding(0, px(8), 0, 0);
        bar.addView(time);
        root.addView(bar, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        video.setOnPreparedListener(mp -> {
            int at = spots.getInt(paths[index], 0);
            if (at > 10_000 && at < mp.getDuration() - 30_000) video.seekTo(at);
            video.start();
            paused.setVisibility(View.GONE);
            showBar();
        });
        video.setOnCompletionListener(mp -> {
            spots.edit().remove(paths[index]).apply();
            if (index + 1 < paths.length) { index++; open(); } else finish();
        });
        video.setOnErrorListener((mp, a, c) -> {
            // a file this box can't play: skip to the next one
            if (index + 1 < paths.length) { index++; open(); } else finish();
            return true;
        });
        open();
        tick();
    }

    private int px(float v) { return Math.round(v * k); }

    private void open() {
        title.setText(KidsActivity.title(new File(paths[index])));
        video.setVideoPath(paths[index]);
    }

    private void tick() {
        if (!Rules.resting(this).isEmpty()) { save(); finish(); return; }
        if (bar.getAlpha() > 0) drawProgress();
        if (video.isPlaying() && video.getCurrentPosition() % 5000 < 1000) save();
        ui.postDelayed(this::tick, 1000);
    }

    private void drawProgress() {
        int d = video.getDuration(), p = video.getCurrentPosition();
        fill.setScaleX(d > 0 ? Math.min(1f, p / (float) d) : 0);
        time.setText(clock(p) + "  /  " + clock(d) + (index + 1 < paths.length ? "     Next: " + KidsActivity.title(new File(paths[index + 1])) : ""));
    }

    private static String clock(int ms) {
        if (ms <= 0) return "0:00";
        int s = ms / 1000, m = s / 60, h = m / 60;
        String ss = (s % 60 < 10 ? "0" : "") + (s % 60);
        return h > 0 ? h + ":" + (m % 60 < 10 ? "0" : "") + (m % 60) + ":" + ss : m + ":" + ss;
    }

    private void save() {
        if (paths == null) return;
        try { spots.edit().putInt(paths[index], video.getCurrentPosition()).apply(); } catch (Exception ignored) { }
    }

    private final Runnable hideBar = () -> { if (video.isPlaying()) bar.animate().alpha(0).setDuration(400).start(); };

    private void showBar() {
        drawProgress();
        bar.animate().alpha(1).setDuration(200).start();
        ui.removeCallbacks(hideBar);
        ui.postDelayed(hideBar, 3500);
    }

    /** A held arrow repeats every 50 ms: skip on the first press, then only every 8th repeat (about 2.5 jumps a second). */
    private static boolean skipHeld(KeyEvent e) { return e.getRepeatCount() == 0 || e.getRepeatCount() % 8 == 0; }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY: case KeyEvent.KEYCODE_MEDIA_PAUSE:
                // a held OK repeats every 50 ms: only the first press counts, so holding it never flickers pause on and off.
                // The TV remote's own Play only plays and Pause only pauses.
                if (e.getRepeatCount() == 0) {
                    boolean pause = code == KeyEvent.KEYCODE_MEDIA_PAUSE || (code != KeyEvent.KEYCODE_MEDIA_PLAY && video.isPlaying());
                    if (pause && video.isPlaying()) { video.pause(); save(); paused.setVisibility(View.VISIBLE); }
                    else if (!pause && !video.isPlaying()) { video.start(); paused.setVisibility(View.GONE); }
                }
                showBar();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (skipHeld(e)) video.seekTo(Math.max(0, video.getCurrentPosition() - (e.getRepeatCount() > 0 ? 30_000 : 10_000)));
                showBar();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (skipHeld(e)) video.seekTo(Math.max(0, Math.min(video.getDuration() - 1000, video.getCurrentPosition() + (e.getRepeatCount() > 0 ? 30_000 : 10_000))));
                showBar();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
                showBar();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (index + 1 < paths.length) { save(); index++; open(); }
                return true;
        }
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onPause() {
        save();
        video.pause();
        paused.setVisibility(View.VISIBLE); // back from a system dialog it is still paused, so show the sign
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
