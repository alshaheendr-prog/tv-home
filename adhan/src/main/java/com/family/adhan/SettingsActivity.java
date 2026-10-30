package com.family.adhan;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/** The Adhan app's own screen: today's times, sound switches, panel position and setup checks. */
public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private Typeface fMedium, fBold, fHeavy;
    private float k = 1;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("adhan", MODE_PRIVATE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        k = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;
        fMedium = getResources().getFont(R.font.baloo_medium);
        fBold = getResources().getFont(R.font.baloo_bold);
        fHeavy = getResources().getFont(R.font.baloo_extrabold);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF0B0D14);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(px(140), px(70), px(140), px(70));
        sv.addView(list);
        setContentView(sv);
        AdhanService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private int px(float v) { return Math.round(v * k); }

    private TextView text(String s, float size, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTypeface(tf);
        t.setTextColor(color);
        return t;
    }

    private void row(String label, String value, Runnable onClick) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(px(28), px(16), px(28), px(16));
        r.setFocusable(true);
        r.setClickable(true);
        r.addView(text(label, 32, fBold, 0xFFF6F3FF), new LinearLayout.LayoutParams(0, -2, 1));
        TextView v = text(value, 30, fMedium, 0xFFFFC24A);
        r.addView(v);
        r.setOnFocusChangeListener((view, has) -> {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(px(18));
            d.setColor(has ? 0x26FFFFFF : Color.TRANSPARENT);
            if (has) d.setStroke(px(3), 0xFFFFC24A);
            view.setBackground(d);
        });
        r.setOnClickListener(x -> { onClick.run(); render(); });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = px(6);
        list.addView(r, p);
    }

    private void header(String s) {
        TextView t = text(s, 26, fMedium, 0xB8F6F3FF);
        t.setLetterSpacing(.1f);
        t.setPadding(px(28), px(34), 0, px(6));
        list.addView(t);
    }

    private boolean usageAllowed() {
        AppOpsManager ops = getSystemService(AppOpsManager.class);
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private void render() {
        int focusedIndex = list.indexOfChild(getCurrentFocus());
        list.removeAllViews();
        list.addView(text("Adhan · أذان", 64, fHeavy, 0xFFF6F3FF));
        Calendar now = Calendar.getInstance(AdhanService.KUWAIT);
        int[] t = PrayerTimes.forDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        StringBuilder sb = new StringBuilder("Kuwait City today:  ");
        for (int i = 0; i < 6; i++) sb.append(PrayerTimes.EN[i]).append(' ').append(PrayerTimes.clock(t[i])).append(i < 5 ? "   ·   " : "");
        TextView times = text(sb.toString(), 26, fMedium, 0xB8F6F3FF);
        times.setPadding(0, px(6), 0, px(10));
        list.addView(times);

        boolean overlay = AdhanService.canOverlay(this), usage = usageAllowed();
        if (!overlay || !usage) {
            header("SETUP NEEDED");
            TextView help = text("Run these once from your computer, then open this app again:\n"
                    + (overlay ? "" : "adb shell appops set com.family.adhan SYSTEM_ALERT_WINDOW allow\n")
                    + (usage ? "" : "adb shell appops set com.family.adhan GET_USAGE_STATS allow\n"),
                    26, fMedium, 0xFFFF8F8F);
            help.setPadding(px(28), 0, px(28), 0);
            list.addView(help);
        }

        header("HOME SCREEN PANEL");
        boolean panel = prefs.getBoolean("panel", true);
        row("Show prayer times on the home screen", panel ? "On" : "Off", () -> {
            prefs.edit().putBoolean("panel", !prefs.getBoolean("panel", true)).apply();
            refreshService();
        });
        String[] pos = {"tl", "tr", "bl", "br"}, names = {"Top left", "Top right", "Bottom left", "Bottom right"};
        String cur = prefs.getString("position", "tl");
        int ci = 0;
        for (int i = 0; i < 4; i++) if (pos[i].equals(cur)) ci = i;
        final int next = (ci + 1) % 4;
        row("Panel position", names[ci], () -> { prefs.edit().putString("position", pos[next]).apply(); refreshService(); });

        header("WIDGET CARD");
        row("Show the widget card (verse, hadith, reminders, character)", prefs.getBoolean("card", true) ? "On" : "Off", () -> {
            prefs.edit().putBoolean("card", !prefs.getBoolean("card", true)).apply();
            refreshService();
        });
        String ccur = prefs.getString("card_position", "tr");
        int cci = 1;
        for (int i = 0; i < 4; i++) if (pos[i].equals(ccur)) cci = i;
        final int cnext = (cci + 1) % 4;
        row("Card position", names[cci], () -> { prefs.edit().putString("card_position", pos[cnext]).apply(); refreshService(); });

        header("WALLPAPER");
        String[] wm = {"return", "hourly", "off"}, wn = {"Every time I return home", "Every hour", "Off"};
        String wcur = prefs.getString("wallpaper_mode", "return");
        int wi = 0;
        for (int i = 0; i < 3; i++) if (wm[i].equals(wcur)) wi = i;
        final int wnext = (wi + 1) % 3;
        row("New random wallpaper", wn[wi], () -> prefs.edit().putString("wallpaper_mode", wm[wnext]).apply());
        java.io.File[] clips = AdhanService.clipsDir().listFiles((d, n) -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".mp4"));
        int count = clips == null ? 0 : clips.length;
        row("Swap the wallpaper now (" + count + " clips in Movies/TVWallpapers)", "▶", () ->
                startForegroundService(new Intent(this, AdhanService.class).setAction("swap")));
        TextView wnote = text("In Projectivy, set the video wallpaper to Movies/Projectivy/wallpaper.mp4 once. This app swaps a random clip into that file.", 24, fMedium, 0xB8F6F3FF);
        wnote.setPadding(px(28), px(10), px(28), 0);
        list.addView(wnote);
        if (!android.os.Environment.isExternalStorageManager()) {
            TextView need = text("Needs file access: adb shell appops set com.family.adhan MANAGE_EXTERNAL_STORAGE allow", 24, fMedium, 0xFFFF8F8F);
            need.setPadding(px(28), px(6), px(28), 0);
            list.addView(need);
        }

        header("ADHAN SOUND");
        for (int i = 0; i < 6; i++) {
            if (i == 1) continue;
            final String key = "sound_" + PrayerTimes.KEYS[i];
            boolean on = prefs.getBoolean(key, true);
            row(PrayerTimes.EN[i] + "  " + PrayerTimes.AR[i], on ? "Sound on" : "Popup only", () ->
                    prefs.edit().putBoolean(key, !prefs.getBoolean(key, true)).apply());
        }
        row("Play the adhan now (test)", "▶", () -> {
            Intent i = new Intent(this, AdhanService.class).setAction("test");
            startForegroundService(i);
        });
        TextView note = text("To use your favourite adhan recording, copy it to the box as Download/adhan.mp3. Until then a soft chime plays.", 24, fMedium, 0xB8F6F3FF);
        note.setPadding(px(28), px(14), px(28), 0);
        list.addView(note);

        if (focusedIndex >= 0 && focusedIndex < list.getChildCount()) list.getChildAt(focusedIndex).requestFocus();
        else for (int i = 0; i < list.getChildCount(); i++) if (list.getChildAt(i).isFocusable()) { list.getChildAt(i).requestFocus(); break; }
    }

    private void refreshService() {
        startForegroundService(new Intent(this, AdhanService.class).setAction("refresh"));
    }
}
