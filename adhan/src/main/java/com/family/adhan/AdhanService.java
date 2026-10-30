package com.family.adhan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.icu.util.ULocale;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Runs in the background. Shows a floating adhan panel while the home screen is in front,
 * and at each prayer time shows a popup and plays the adhan.
 */
public class AdhanService extends Service {
    static final TimeZone KUWAIT = TimeZone.getTimeZone("Asia/Kuwait");
    static final String CHANNEL = "adhan";
    static final int GOLD = 0xFFFFC24A, TEXT = 0xFFF6F3FF, MUTED = 0xB8F6F3FF, INK = 0xFF0B0D14;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private SharedPreferences prefs;
    private Typeface fMedium, fBold, fHeavy, fMono;
    private float k = 1; // screen pixels per 1920-wide TV pixel

    private FrameLayout panel, alert;
    private boolean panelShown, alertShown;
    private TextView clock, ampm, hijri, weather, nextName, countdown, alertTitle, alertSub;
    private final TextView[][] rows = new TextView[6][3];
    private final LinearLayout[] rowViews = new LinearLayout[6];

    private int[] today, tomorrow;
    private String cachedDay = "";
    private final Set<String> fired = new HashSet<>();
    private long lastEventQuery;
    private String foreground = "";
    private Set<String> homePackages = new HashSet<>();
    private long lastWeather;
    private MediaPlayer player;

    // widget card
    private FrameLayout card;
    private boolean cardShown;
    private TextView cardKicker, cardAr, cardEn, cardRef;
    private int cardIndex = -1;
    private long cardSwitched;
    private String[] ayahToday; // {ar, en, ref}
    private String ayahDay = "";
    private volatile double tempNow = Double.NaN, dustNow = Double.NaN;

    static void start(Context c) {
        Intent i = new Intent(c, AdhanService.class);
        try { c.startForegroundService(i); } catch (Exception ignored) { }
    }

    static boolean canOverlay(Context c) { return Settings.canDrawOverlays(c); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("adhan", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        k = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;
        fMedium = getResources().getFont(R.font.baloo_medium);
        fBold = getResources().getFont(R.font.baloo_bold);
        fHeavy = getResources().getFont(R.font.baloo_extrabold);
        fMono = getResources().getFont(R.font.chivo_mono_bold);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Adhan", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Adhan times")
                .setContentText("Kuwait City prayer times")
                .setSmallIcon(R.drawable.icon)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        findHomePackages();
        buildPanel();
        buildAlert();
        buildCard();
        tick();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "test".equals(intent.getAction())) fireAdhan(3);
        if (intent != null && "refresh".equals(intent.getAction())) { setPanel(false); setCard(false); }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        stopSound();
        try { if (panelShown) wm.removeView(panel); } catch (Exception ignored) { }
        try { if (cardShown) wm.removeView(card); } catch (Exception ignored) { }
        try { if (alertShown) wm.removeView(alert); } catch (Exception ignored) { }
        super.onDestroy();
    }

    // ------------------------------------------------------------------ views

    private float px(float tvPx) { return tvPx * k; }

    private TextView text(String s, float size, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private static GradientDrawable rounded(int color, float r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(r);
        return d;
    }

    private WindowManager.LayoutParams overlayParams(int gravity) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = gravity;
        return p;
    }

    private void buildPanel() {
        panel = new FrameLayout(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) px(26);
        box.setPadding(pad, (int) px(20), pad, (int) px(20));
        GradientDrawable bg = rounded(0xE00A0C16, px(28));
        bg.setStroke(Math.max(1, (int) px(2)), 0x33FFFFFF);
        box.setBackground(bg);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.BOTTOM);
        clock = text("--:--", 64, fHeavy, TEXT);
        ampm = text("", 26, fBold, MUTED);
        ampm.setPadding((int) px(8), 0, 0, (int) px(8));
        weather = text("", 40, fBold, TEXT);
        weather.setGravity(Gravity.END);
        top.addView(clock);
        top.addView(ampm);
        top.addView(weather, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(top);
        hijri = text("", 26, fBold, GOLD);
        hijri.setPadding(0, (int) px(4), 0, (int) px(12));
        box.addView(hijri);

        nextName = text("", 30, fBold, TEXT);
        box.addView(nextName);
        countdown = text("--:--:--", 52, fMono, GOLD);
        countdown.setPadding(0, (int) px(2), 0, (int) px(10));
        box.addView(countdown);

        // The six times as a compact 3 x 2 grid.
        LinearLayout line = null;
        for (int i = 0; i < 6; i++) {
            if (i % 3 == 0) {
                line = new LinearLayout(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = (int) px(6);
                box.addView(line, lp);
            }
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, (int) px(6), 0, (int) px(8));
            TextView en = text(PrayerTimes.EN[i], 20, fBold, TEXT);
            TextView ar = text(PrayerTimes.AR[i], 20, fMedium, MUTED);
            TextView tm = text("", 26, fMono, TEXT);
            tm.setPadding(0, (int) px(4), 0, 0);
            cell.addView(en);
            cell.addView(ar);
            cell.addView(tm);
            rows[i][0] = en; rows[i][1] = ar; rows[i][2] = tm;
            rowViews[i] = cell;
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
            cp.leftMargin = (int) px(3); cp.rightMargin = (int) px(3);
            line.addView(cell, cp);
        }
        panel.addView(box, new FrameLayout.LayoutParams((int) px(440), -2));
        panel.setAlpha(0);
    }

    private int panelGravity() {
        switch (prefs.getString("position", "tl")) {
            case "tr": return Gravity.TOP | Gravity.END;
            case "bl": return Gravity.BOTTOM | Gravity.START;
            case "br": return Gravity.BOTTOM | Gravity.END;
            default: return Gravity.TOP | Gravity.START;
        }
    }

    private WindowManager.LayoutParams panelParams() {
        WindowManager.LayoutParams p = overlayParams(panelGravity());
        p.x = (int) px(56);
        p.y = (int) px(44);
        return p;
    }

    private void applyPosition() {
        if (panelShown) try { wm.updateViewLayout(panel, panelParams()); } catch (Exception ignored) { }
    }

    private void setPanel(boolean show) {
        if (!canOverlay(this)) return;
        if (show && !prefs.getBoolean("panel", true)) show = false;
        if (show == panelShown) return;
        if (show) {
            try { wm.addView(panel, panelParams()); panelShown = true; } catch (Exception e) { return; }
            panel.animate().alpha(1).setDuration(250).start();
        } else {
            panelShown = false;
            panel.animate().alpha(0).setDuration(150).withEndAction(() -> {
                try { wm.removeView(panel); } catch (Exception ignored) { }
            }).start();
        }
    }

    private void buildAlert() {
        alert = new FrameLayout(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding((int) px(70), (int) px(44), (int) px(70), (int) px(48));
        GradientDrawable bg = rounded(0xF00A0C16, px(36));
        bg.setStroke((int) Math.max(2, px(3)), GOLD);
        box.setBackground(bg);
        alertTitle = text("", 72, fHeavy, GOLD);
        alertTitle.setGravity(Gravity.CENTER);
        box.addView(alertTitle);
        alertSub = text("", 40, fBold, TEXT);
        alertSub.setGravity(Gravity.CENTER);
        alertSub.setPadding(0, (int) px(10), 0, 0);
        box.addView(alertSub);
        alert.addView(box);
    }

    private void showAlert(int i) {
        if (!canOverlay(this)) return;
        alertTitle.setText("حان الآن موعد أذان " + PrayerTimes.AR[i]);
        alertSub.setText("It's time for " + PrayerTimes.EN[i] + " · " + PrayerTimes.clock(today[i]));
        if (!alertShown) {
            try { wm.addView(alert, overlayParams(Gravity.CENTER)); alertShown = true; } catch (Exception ignored) { return; }
        }
        alert.setAlpha(0);
        alert.animate().alpha(1).setDuration(300).start();
    }

    private void hideAlert() {
        if (!alertShown) return;
        alertShown = false;
        alert.animate().alpha(0).setDuration(300).withEndAction(() -> {
            try { wm.removeView(alert); } catch (Exception ignored) { }
        }).start();
    }

    // ------------------------------------------------------------------ the clock

    private void tick() {
        Calendar now = Calendar.getInstance(KUWAIT);
        int y = now.get(Calendar.YEAR), m = now.get(Calendar.MONTH) + 1, d = now.get(Calendar.DAY_OF_MONTH);
        String key = y + "-" + m + "-" + d;
        if (!key.equals(cachedDay)) {
            cachedDay = key;
            fired.clear();
            today = PrayerTimes.forDate(y, m, d);
            Calendar tm = (Calendar) now.clone();
            tm.add(Calendar.DAY_OF_MONTH, 1);
            tomorrow = PrayerTimes.forDate(tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH));
            for (int i = 0; i < 6; i++) rows[i][2].setText(PrayerTimes.clock(today[i]));
            try {
                DateFormat hf = DateFormat.getDateInstance(DateFormat.LONG, new ULocale("ar_SA@calendar=islamic-umalqura"));
                hf.setTimeZone(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
                SimpleDateFormat gf = new SimpleDateFormat("EEE d MMM", Locale.UK);
                gf.setTimeZone(KUWAIT);
                hijri.setText(gf.format(now.getTime()) + "  ·  " + hf.format(now.getTime()));
            } catch (Exception ignored) { }
        }
        int h = now.get(Calendar.HOUR_OF_DAY), mi = now.get(Calendar.MINUTE), s = now.get(Calendar.SECOND);
        int nowSec = h * 3600 + mi * 60 + s;

        // Adhan time?
        for (int i = 0; i < 6; i++) {
            if (i == 1) continue; // sunrise has no adhan
            String id = PrayerTimes.KEYS[i];
            if (today[i] == h * 60 + mi && !fired.contains(id)) { fired.add(id); fireAdhan(i); }
        }

        if (panelShown || alertShown) {
            clock.setText(((h + 11) % 12 + 1) + ":" + (mi < 10 ? "0" : "") + mi);
            ampm.setText(h < 12 ? "AM" : "PM");
            int next = -1;
            for (int i = 0; i < 6; i++) if (i != 1 && today[i] * 60 > nowSec) { next = i; break; }
            int target = next >= 0 ? today[next] * 60 : tomorrow[0] * 60 + 86400;
            if (next < 0) next = 0;
            int left = target - nowSec;
            nextName.setText("Next: " + PrayerTimes.EN[next] + "  " + PrayerTimes.AR[next]);
            countdown.setText(String.format(Locale.US, "%02d:%02d:%02d", left / 3600, left % 3600 / 60, left % 60));
            for (int i = 0; i < 6; i++) {
                boolean isNext = i == next, passed = !isNext && today[i] * 60 <= nowSec;
                rowViews[i].setBackground(isNext ? rounded(GOLD, px(10)) : null);
                rowViews[i].setAlpha(passed ? .5f : 1f);
                int c = isNext ? INK : TEXT;
                rows[i][0].setTextColor(c);
                rows[i][1].setTextColor(isNext ? INK : MUTED);
                rows[i][2].setTextColor(c);
            }
        }

        updateForeground();
        boolean home = homePackages.contains(foreground);
        setPanel(home);
        setCard(home);
        if (cardShown && System.currentTimeMillis() - cardSwitched > 15000) nextCard(now);
        if (!key.equals(ayahDay)) { ayahDay = key; fetchAyah(now.get(Calendar.DAY_OF_YEAR) + y * 400); }
        if (System.currentTimeMillis() - lastWeather > 30 * 60 * 1000) { lastWeather = System.currentTimeMillis(); fetchWeather(); }
        ui.postDelayed(this::tick, 1000 - (System.currentTimeMillis() % 1000));
    }

    // ------------------------------------------------------------------ which app is in front

    private void findHomePackages() {
        Set<String> out = new HashSet<>();
        out.add("com.spocky.projengmenu"); // Projectivy
        for (ResolveInfo r : getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
            String p = r.activityInfo.packageName;
            if (!p.equals("com.android.tv.settings") && !p.startsWith("com.android.settings")) out.add(p);
        }
        homePackages = out;
    }

    private void updateForeground() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
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

    // ------------------------------------------------------------------ adhan

    private void fireAdhan(int i) {
        if (today == null) return;
        showAlert(i);
        boolean sound = prefs.getBoolean("sound_" + PrayerTimes.KEYS[i], true);
        if (sound) playSound(); else ui.postDelayed(this::hideAlert, 60_000);
    }

    /** The adhan recording: Download/adhan.mp3 (or fajr.mp3 for Fajr) if the parent added one, otherwise a chime. */
    private Uri soundUri() {
        File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        for (String name : new String[]{"adhan.mp3", "adhan.m4a", "adhan.wav"}) {
            File f = new File(dl, name);
            if (f.canRead()) return Uri.fromFile(f);
        }
        return Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chime);
    }

    private void playSound() {
        stopSound();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs).build();
        am.requestAudioFocus(focus); // pauses Netflix or YouTube while the adhan plays
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(attrs);
            mp.setDataSource(this, soundUri());
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(p -> {
                am.abandonAudioFocusRequest(focus);
                ui.postDelayed(this::hideAlert, 4000);
                stopSound();
            });
            mp.setOnErrorListener((p, w, x) -> { am.abandonAudioFocusRequest(focus); hideAlert(); return true; });
            mp.prepareAsync();
            player = mp;
        } catch (Exception e) {
            am.abandonAudioFocusRequest(focus);
            ui.postDelayed(this::hideAlert, 60_000);
        }
    }

    private void stopSound() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    // ------------------------------------------------------------------ weather (Open-Meteo, no key needed)

    private void fetchWeather() {
        new Thread(() -> {
            try {
                JSONObject cur = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=29.3759&longitude=47.9774&current=temperature_2m,weather_code,is_day&timezone=Asia%2FKuwait")).getJSONObject("current");
                long temp = Math.round(cur.getDouble("temperature_2m"));
                String icon = weatherIcon(cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1);
                tempNow = cur.getDouble("temperature_2m");
                try { dustNow = new JSONObject(get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=29.3759&longitude=47.9774&current=dust"))
                        .getJSONObject("current").getDouble("dust"); } catch (Exception ignored) { }
                ui.post(() -> weather.setText((dustNow > 150 ? "🌫️" : icon) + " " + temp + "°"));
            } catch (Exception e) {
                lastWeather = System.currentTimeMillis() - 25 * 60 * 1000; // try again in 5 minutes
            }
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

    // ------------------------------------------------------------------ widget card

    private void buildCard() {
        card = new FrameLayout(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) px(30), (int) px(22), (int) px(30), (int) px(24));
        GradientDrawable bg = rounded(0xE00A0C16, px(28));
        bg.setStroke(Math.max(1, (int) px(2)), 0x33FFFFFF);
        box.setBackground(bg);
        cardKicker = text("", 20, fBold, GOLD);
        cardKicker.setLetterSpacing(.08f);
        box.addView(cardKicker);
        cardAr = text("", 34, fBold, TEXT);
        cardAr.setTextDirection(View.TEXT_DIRECTION_RTL);
        cardAr.setGravity(Gravity.END);
        cardAr.setLineSpacing(0, 1.15f);
        cardAr.setMaxLines(4);
        cardAr.setPadding(0, (int) px(12), 0, 0);
        box.addView(cardAr, new LinearLayout.LayoutParams(-1, -2));
        cardEn = text("", 26, fMedium, TEXT);
        cardEn.setLineSpacing(0, 1.1f);
        cardEn.setMaxLines(4);
        cardEn.setPadding(0, (int) px(10), 0, 0);
        box.addView(cardEn);
        cardRef = text("", 20, fMedium, MUTED);
        cardRef.setPadding(0, (int) px(10), 0, 0);
        box.addView(cardRef);
        card.addView(box, new FrameLayout.LayoutParams((int) px(560), -2));
        card.setAlpha(0);
    }

    private WindowManager.LayoutParams cardParams() {
        int g;
        switch (prefs.getString("card_position", "tr")) {
            case "tl": g = Gravity.TOP | Gravity.START; break;
            case "bl": g = Gravity.BOTTOM | Gravity.START; break;
            case "br": g = Gravity.BOTTOM | Gravity.END; break;
            default: g = Gravity.TOP | Gravity.END;
        }
        WindowManager.LayoutParams p = overlayParams(g);
        p.x = (int) px(56);
        p.y = (int) px(44);
        return p;
    }

    private void setCard(boolean show) {
        if (!canOverlay(this)) return;
        if (show && !prefs.getBoolean("card", true)) show = false;
        if (show == cardShown) return;
        if (show) {
            nextCard(Calendar.getInstance(KUWAIT));
            try { wm.addView(card, cardParams()); cardShown = true; } catch (Exception e) { return; }
            card.animate().alpha(1).setDuration(250).start();
        } else {
            cardShown = false;
            card.animate().alpha(0).setDuration(150).withEndAction(() -> {
                try { wm.removeView(card); } catch (Exception ignored) { }
            }).start();
        }
    }

    private void nextCard(Calendar now) {
        java.util.List<String[]> items = Widgets.items(this, now, today, ayahToday, tempNow, dustNow);
        if (items.isEmpty()) return;
        cardIndex = (cardIndex + 1) % items.size();
        String[] it = items.get(cardIndex); // {kicker, ar, en, ref}
        cardSwitched = System.currentTimeMillis();
        Runnable fill = () -> {
            cardKicker.setText(it[0]);
            cardAr.setText(it[1]);
            cardAr.setVisibility(it[1].isEmpty() ? View.GONE : View.VISIBLE);
            cardEn.setText(it[2]);
            cardRef.setText(it[3]);
            cardRef.setVisibility(it[3].isEmpty() ? View.GONE : View.VISIBLE);
        };
        if (!cardShown) { fill.run(); return; }
        View box = card.getChildAt(0);
        box.animate().alpha(0).setDuration(250).withEndAction(() -> {
            fill.run();
            box.animate().alpha(1).setDuration(350).start();
        }).start();
    }

    /** Verse of the day from the Qur'an service (Uthmani text with Sahih International), a short one if possible. */
    private void fetchAyah(int seed) {
        new Thread(() -> {
            for (int attempt = 0; attempt < 12; attempt++) {
                int n = (int) (((long) (seed + attempt) * 7919L) % 6236) + 1;
                try {
                    JSONObject d = new JSONObject(get("https://api.alquran.cloud/v1/ayah/" + n + "/editions/quran-uthmani,en.sahih"));
                    org.json.JSONArray arr = d.getJSONArray("data");
                    JSONObject ar = arr.getJSONObject(0), en = arr.getJSONObject(1);
                    String text = ar.getString("text");
                    if (text.length() > 170 && attempt < 11) continue;
                    JSONObject surah = ar.getJSONObject("surah");
                    String ref = surah.getString("englishName") + " " + surah.getInt("number") + ":" + ar.getInt("numberInSurah")
                            + "  ·  " + surah.getString("name");
                    String[] a = {text, en.getString("text"), ref};
                    ui.post(() -> ayahToday = a);
                    return;
                } catch (Exception e) {
                    break; // offline: Widgets falls back to the built-in verses
                }
            }
        }).start();
    }

    private static String weatherIcon(int code, boolean day) {
        if (code == 0) return day ? "☀️" : "🌙";
        if (code <= 2) return day ? "🌤️" : "☁️";
        if (code == 3) return "☁️";
        if (code <= 48) return "🌫️";
        if (code <= 67 || (code >= 80 && code <= 82)) return "🌧️";
        if (code <= 77) return "❄️";
        if (code >= 95) return "⛈️";
        return "🌦️";
    }
}
