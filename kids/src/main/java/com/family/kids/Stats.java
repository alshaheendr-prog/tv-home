package com.family.kids;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * A week of history for the family stats page in TV Home: seconds watched per Kuwait day,
 * seconds per app, and the reward stars each child spent. Kept in its own small preferences file
 * and trimmed to the last eight days.
 */
final class Stats {
    static final int DAYS = 7;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("stats", Context.MODE_PRIVATE); }

    /** yyyy-MM-dd in Kuwait time, daysAgo days back from today. */
    static String day(int daysAgo) {
        Calendar cal = Calendar.getInstance(Rules.KUWAIT);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(Rules.KUWAIT);
        return f.format(cal.getTime());
    }

    static void addSeconds(Context c, String pkg, long seconds) {
        SharedPreferences p = prefs(c);
        String d = day(0);
        SharedPreferences.Editor e = p.edit().putLong("t|" + d, p.getLong("t|" + d, 0) + seconds);
        if (pkg != null && !pkg.isEmpty()) e.putLong("a|" + d + "|" + pkg, p.getLong("a|" + d + "|" + pkg, 0) + seconds);
        if (!d.equals(p.getString("trimmed", ""))) { trim(p, e); e.putString("trimmed", d); }
        e.apply();
    }

    static void starSpent(Context c, int kid) {
        SharedPreferences p = prefs(c);
        String k = "s|" + day(0) + "|" + kid;
        p.edit().putInt(k, p.getInt(k, 0) + 1).apply();
    }

    static long seconds(Context c, String day) { return prefs(c).getLong("t|" + day, 0); }
    static int starsSpent(Context c, String day, int kid) { return prefs(c).getInt("s|" + day + "|" + kid, 0); }

    /** The app watched longest that day, or "" if none. */
    static String topApp(Context c, String day) {
        String best = ""; long most = 0;
        String prefix = "a|" + day + "|";
        for (Map.Entry<String, ?> en : prefs(c).getAll().entrySet()) {
            if (!en.getKey().startsWith(prefix) || !(en.getValue() instanceof Long)) continue;
            long s = (Long) en.getValue();
            if (s > most) { most = s; best = en.getKey().substring(prefix.length()); }
        }
        return best;
    }

    private static void trim(SharedPreferences p, SharedPreferences.Editor e) {
        String oldest = day(DAYS);
        for (String k : p.getAll().keySet()) {
            String[] parts = k.split("\\|");
            if (parts.length >= 2 && parts[1].length() == 10 && parts[1].compareTo(oldest) < 0) e.remove(k);
        }
    }
}
