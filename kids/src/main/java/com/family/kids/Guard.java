package com.family.kids;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Runs while kids mode is on. Every second and a half it counts screen time, and if anything
 * other than Kids Corner or one of his apps comes to the front (the Home button, the YouTube button,
 * Settings), it brings Kids Corner straight back. When time is up it does the same, and Kids Corner
 * shows the rest screen. A small note floats over his app five minutes and one minute before the end.
 */
public class Guard extends Service {
    static final String CHANNEL = "kids";
    private static final long TICK = 1500;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private UsageStatsManager usm;
    private PowerManager pm;
    private WindowManager wm;
    private String foreground = "";
    private long lastEventQuery, lastTick, lastBringBack, carry;
    private final Set<String> passThrough = new HashSet<>();
    private int warned = Integer.MAX_VALUE; // the last warning shown, in minutes left
    private String warnedDay = "";
    private TextView note;

    static void start(Context c) {
        try { c.startForegroundService(new Intent(c, Guard.class)); } catch (Exception ignored) { }
    }

    static boolean canOverlay(Context c) { return Settings.canDrawOverlays(c); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Kids Corner", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Kids Corner is on")
                .setSmallIcon(R.drawable.icon)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        // The keyboard and system dialogs may come to the front at any time; they're never pulled away.
        passThrough.add(getPackageName());
        passThrough.add("android");
        passThrough.add("com.android.systemui");
        passThrough.add("com.google.android.permissioncontroller");
        passThrough.add("com.android.permissioncontroller");
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            for (InputMethodInfo i : imm.getInputMethodList()) passThrough.add(i.getPackageName());
        } catch (Exception ignored) { }
        lastTick = System.currentTimeMillis();
        tick();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        hideNote();
        super.onDestroy();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long delta = Math.min(now - lastTick, 5000);
        lastTick = now;
        if (!Rules.on(this)) { stopSelf(); return; }

        updateForeground();
        String rest = Rules.resting(this);
        boolean ours = foreground.equals(getPackageName());

        // Screen time counts while the screen is on and he isn't on the rest screen.
        if (pm.isInteractive() && rest.isEmpty()) {
            carry += delta;
            if (carry >= 1000) { Rules.setUsed(this, Rules.used(this) + carry / 1000); carry %= 1000; }
        }

        if (!rest.isEmpty()) {
            if (!ours && !passThrough.contains(foreground)) bringBack();
        } else if (!passThrough.contains(foreground) && !allowed(foreground)) {
            bringBack();
        }

        warnIfNearTheEnd(ours, rest);
        ui.postDelayed(this::tick, TICK);
    }

    private boolean allowed(String pkg) {
        if (pkg.isEmpty()) return true; // nothing seen yet
        if (Rules.NEVER.contains(pkg)) return false;
        return Rules.apps(this).contains(pkg);
    }

    private void bringBack() {
        long now = System.currentTimeMillis();
        if (now - lastBringBack < 2000) return;
        lastBringBack = now;
        try {
            startActivity(new Intent(this, KidsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception ignored) { }
    }

    private void updateForeground() {
        if (usm == null) return;
        long now = System.currentTimeMillis();
        long from = lastEventQuery == 0 ? now - 6 * 3600 * 1000L : lastEventQuery - 2000;
        try {
            UsageEvents ev = usm.queryEvents(from, now);
            UsageEvents.Event e = new UsageEvents.Event();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) foreground = e.getPackageName();
            }
        } catch (Exception ignored) { }
        lastEventQuery = now;
    }

    // ------------------------------------------------------------------ "5 minutes left" note

    private void warnIfNearTheEnd(boolean ours, String rest) {
        String day = Rules.dayKey();
        if (!day.equals(warnedDay)) { warnedDay = day; warned = Integer.MAX_VALUE; }
        long left = Rules.left(this);
        if (!rest.isEmpty() || left == Long.MAX_VALUE || System.currentTimeMillis() < Rules.extraUntil(this)) return;
        int mins = left <= 60 ? 1 : left <= 300 ? 5 : Integer.MAX_VALUE;
        if (mins >= warned) return;
        warned = mins;
        if (!ours && pm.isInteractive()) showNote(mins == 1 ? "One more minute, then it's rest time!" : "5 minutes left today");
    }

    private void showNote(String text) {
        if (!canOverlay(this)) return;
        hideNote();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float k = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;
        note = new TextView(this);
        note.setText(text);
        note.setTextColor(0xFFFFFFFF);
        note.setTypeface(getResources().getFont(R.font.baloo_bold));
        note.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40 * k);
        note.setPadding(Math.round(44 * k), Math.round(14 * k), Math.round(44 * k), Math.round(14 * k));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC1A1033);
        bg.setStroke(Math.round(3 * k), 0xFFFFC233);
        bg.setCornerRadius(60 * k);
        note.setBackground(bg);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Math.round(60 * k);
        try {
            wm.addView(note, lp);
            note.setAlpha(0);
            note.setTranslationY(-40 * k);
            note.animate().alpha(1).translationY(0).setDuration(450).start();
            ui.postDelayed(this::hideNote, 7000);
        } catch (Exception e) { note = null; }
    }

    private void hideNote() {
        if (note == null) return;
        View v = note;
        note = null;
        try { wm.removeView(v); } catch (Exception ignored) { }
    }
}
