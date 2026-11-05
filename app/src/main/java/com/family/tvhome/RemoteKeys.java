package com.family.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Remote buttons: Prime Video opens Stremio, the Apps button opens Kodi, and YouTube goes through the parent PIN first.
 * Netflix is left alone. The remote's own key codes differ between boxes, so Parent settings "learns" each button
 * once (press it when asked) and the codes are kept in the home screen's prefs.
 * Switched on by Update TV (enabled_accessibility_services).
 */
public class RemoteKeys extends AccessibilityService {

    /** Learnable buttons: pref key, what it does, and the app it opens (null = YouTube through the PIN). */
    static final String[][] BUTTONS = {
            {"rk_prime", "Prime Video button", "Opens Stremio", "com.stremio.one"},
            {"rk_yt", "YouTube button", "Asks for the PIN first", null},
            {"rk_apps", "Apps button", "Opens Kodi", "org.xbmc.kodi"},
    };

    interface Learned { void on(int keyCode); }

    static volatile boolean running;
    private static volatile String learning;   // pref key being learned, or null
    private static volatile Learned learnedCb;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private int swallowUp = -1;                  // key whose release we also eat, so the app behind never sees half a press

    /** Waits for the next remote button and calls back on the main thread with its code. */
    static void learn(String key, Learned cb) { learnedCb = cb; learning = key; }
    static void stopLearning() { learning = null; learnedCb = null; }

    static int code(SharedPreferences p, String key) {
        return p.getInt(key, key.equals("rk_apps") ? KeyEvent.KEYCODE_ALL_APPS : 0);
    }

    @Override
    protected void onServiceConnected() {
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        running = true;
    }

    @Override
    public boolean onUnbind(Intent intent) { running = false; return super.onUnbind(intent); }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (k == swallowUp) { swallowUp = -1; return true; }
            return false;
        }
        if (e.getAction() != KeyEvent.ACTION_DOWN || e.getRepeatCount() > 0) return k == swallowUp;
        if (isEveryday(k)) return false;   // arrows, OK, Back, Home, volume and power always behave normally

        String l = learning;
        if (l != null) {
            Learned cb = learnedCb;
            stopLearning();
            prefs.edit().putInt(l, k).apply();
            swallowUp = k;
            if (cb != null) ui.post(() -> cb.on(k));
            return true;
        }

        for (String[] b : BUTTONS) {
            if (code(prefs, b[0]) != k) continue;
            swallowUp = k;
            ui.post(() -> run(b));
            return true;
        }
        return false;
    }

    private void run(String[] b) {
        if (b[3] == null) {
            // YouTube: open the home screen's PIN pad; it starts YouTube after the right PIN.
            Intent i = new Intent(this, MainActivity.class).putExtra("panel", "pin")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { startActivity(i); } catch (Exception ignored) { }
            return;
        }
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(b[3]);
        if (i == null) i = pm.getLaunchIntentForPackage(b[3]);
        if (i == null) {
            // the app isn't on the box: the press was already taken, so say so on the home screen instead of nothing
            i = new Intent(this, MainActivity.class).putExtra("missing", b[2].replace("Opens ", ""))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { startActivity(i); } catch (Exception ignored) { }
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    private static boolean isEveryday(int k) {
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_VOLUME_UP: case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_POWER: case KeyEvent.KEYCODE_SLEEP: case KeyEvent.KEYCODE_WAKEUP:
                return true;
            default:
                return false;
        }
    }
}
