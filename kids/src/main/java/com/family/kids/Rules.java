package com.family.kids;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Everything the parent decides, kept in one small preferences file:
 * whether kids mode is on, which apps are allowed, the daily limit, bedtime and the PIN.
 * Screen time is counted here too, per Kuwait day.
 */
final class Rules {
    static final TimeZone KUWAIT = TimeZone.getTimeZone("Asia/Kuwait");

    /** Never allowed in kids mode, whatever the parent ticks. */
    static final Set<String> NEVER = new HashSet<>(Arrays.asList(
            "com.google.android.youtube.tv", "com.google.android.youtube", "com.google.android.youtube.tvkids",
            "com.android.vending", "com.android.tv.settings", "com.android.settings",
            "com.family.tvhome", "com.spocky.projengmenu", "com.google.android.apps.tv.launcherx",
            "com.tcl.browser", "com.phlox.tvwebbrowser", "org.mozilla.firefox", "com.android.chrome",
            "com.stremio.one", "com.cloudmosa.puffinTV", "com.esaba.downloader"));

    /** Ticked the first time: the apps with kids profiles, and his cartoons in Kodi. */
    static final Set<String> DEFAULT_APPS = new HashSet<>(Arrays.asList(
            "com.netflix.ninja", "com.crunchyroll.crunchyroid", "com.amazon.amazonvideo.livingroom",
            "net.mbc.shahidTV", "net.mbc.shahid", "org.xbmc.kodi"));

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("kids", Context.MODE_PRIVATE); }

    static boolean on(Context c) { return prefs(c).getBoolean("on", false); }
    static void setOn(Context c, boolean on) { prefs(c).edit().putBoolean("on", on).apply(); }

    static String pin(Context c) { return prefs(c).getString("pin", ""); }
    static void setPin(Context c, String p) { prefs(c).edit().putString("pin", p).apply(); }

    static Set<String> apps(Context c) {
        return new HashSet<>(prefs(c).getStringSet("apps", DEFAULT_APPS));
    }
    static void toggleApp(Context c, String pkg) {
        Set<String> s = apps(c);
        if (!s.remove(pkg)) s.add(pkg);
        prefs(c).edit().putStringSet("apps", s).apply();
    }

    // ------------------------------------------------------------------ limits

    /** Kuwait's weekend is Friday and Saturday. */
    static boolean weekend(Calendar now) {
        int d = now.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.FRIDAY || d == Calendar.SATURDAY;
    }

    /** Minutes allowed today; 0 means no limit. */
    static int limitToday(Context c) {
        Calendar now = Calendar.getInstance(KUWAIT);
        return weekend(now) ? prefs(c).getInt("limit_weekend", 120) : prefs(c).getInt("limit_school", 90);
    }

    /** Bedtime start in minutes after midnight, or -1 for none. Bedtime always ends at 07:00. */
    static int bedtime(Context c) { return prefs(c).getInt("bedtime", 20 * 60 + 30); }
    static final int WAKE = 7 * 60;

    static boolean inBedtime(Context c) {
        int b = bedtime(c);
        if (b < 0) return false;
        Calendar now = Calendar.getInstance(KUWAIT);
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return b > WAKE ? (m >= b || m < WAKE) : (m >= b && m < WAKE);
    }

    static String dayKey() {
        Calendar now = Calendar.getInstance(KUWAIT);
        return now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
    }

    /** Seconds watched today. */
    static long used(Context c) {
        SharedPreferences p = prefs(c);
        return dayKey().equals(p.getString("used_day", "")) ? p.getLong("used", 0) : 0;
    }
    static void setUsed(Context c, long seconds) {
        prefs(c).edit().putString("used_day", dayKey()).putLong("used", seconds).apply();
    }

    /** Seconds left today, or Long.MAX_VALUE with no limit. */
    static long left(Context c) {
        int limit = limitToday(c);
        if (limit <= 0) return Long.MAX_VALUE;
        return Math.max(0, limit * 60L - used(c));
    }

    /** Extra time the parent gave, as a moment until which nothing is blocked. */
    static long extraUntil(Context c) { return prefs(c).getLong("extra_until", 0); }
    static void giveExtra(Context c, int minutes) {
        long from = Math.max(System.currentTimeMillis(), extraUntil(c));
        prefs(c).edit().putLong("extra_until", from + minutes * 60_000L).apply();
    }

    /** Why the TV is resting right now: "", "time" or "bed". */
    static String resting(Context c) {
        if (!on(c) || System.currentTimeMillis() < extraUntil(c)) return "";
        if (inBedtime(c)) return "bed";
        if (left(c) <= 0) return "time";
        return "";
    }
}
