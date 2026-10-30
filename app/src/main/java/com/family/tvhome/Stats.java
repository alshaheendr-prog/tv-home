package com.family.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** The weekly family stats: Quran listening kept here, screen time read from Kids Corner. */
final class Stats {
    private Stats() { }

    static final TimeZone KUWAIT = TimeZone.getTimeZone("Asia/Kuwait");
    static final Uri KIDS_WEEK = Uri.parse("content://com.family.kids.stats/week");

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("stats", Context.MODE_PRIVATE); }

    static String day(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(KUWAIT);
        return f.format(millis);
    }

    /** The last seven days, today first. */
    static List<String> week() {
        List<String> out = new ArrayList<>();
        Calendar c = Calendar.getInstance(KUWAIT);
        for (int i = 0; i < 7; i++) { out.add(day(c.getTimeInMillis())); c.add(Calendar.DAY_OF_MONTH, -1); }
        return out;
    }

    private static JSONObject today(SharedPreferences p, String key) {
        try { return new JSONObject(p.getString(key, "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    /** One surah started: counts per surah and per reciter for today. */
    static void surahPlayed(Context c, int surah, String reciter) {
        SharedPreferences p = prefs(c);
        String key = "q_" + day(System.currentTimeMillis());
        JSONObject d = today(p, key);
        try {
            JSONObject s = d.optJSONObject("s"); if (s == null) { s = new JSONObject(); d.put("s", s); }
            s.put("" + surah, s.optInt("" + surah) + 1);
            JSONObject r = d.optJSONObject("r"); if (r == null) { r = new JSONObject(); d.put("r", r); }
            if (reciter != null && !reciter.isEmpty()) r.put(reciter, r.optInt(reciter) + 1);
        } catch (Exception ignored) { }
        p.edit().putString(key, d.toString()).apply();
        prune(p);
    }

    /** Seconds of Quran heard today. */
    static void listened(Context c, int seconds) {
        SharedPreferences p = prefs(c);
        String key = "q_" + day(System.currentTimeMillis());
        JSONObject d = today(p, key);
        try { d.put("sec", d.optInt("sec") + seconds); } catch (Exception ignored) { }
        p.edit().putString(key, d.toString()).apply();
    }

    private static void prune(SharedPreferences p) {
        List<String> keep = new ArrayList<>();
        for (String d : week()) keep.add("q_" + d);
        SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) if (k.startsWith("q_") && !keep.contains(k)) e.remove(k);
        e.apply();
    }

    /** {total minutes, surahs played, top surahs (number -> plays), top reciters (name -> plays)} for the week. */
    static Object[] quranWeek(Context c) {
        SharedPreferences p = prefs(c);
        int sec = 0, plays = 0;
        Map<String, Integer> surahs = new LinkedHashMap<>(), reciters = new LinkedHashMap<>();
        for (String d : week()) {
            JSONObject o = today(p, "q_" + d);
            sec += o.optInt("sec");
            JSONObject s = o.optJSONObject("s");
            if (s != null) for (Iterator<String> it = s.keys(); it.hasNext(); ) {
                String k = it.next(); int v = s.optInt(k); plays += v;
                surahs.put(k, (surahs.containsKey(k) ? surahs.get(k) : 0) + v);
            }
            JSONObject r = o.optJSONObject("r");
            if (r != null) for (Iterator<String> it = r.keys(); it.hasNext(); ) {
                String k = it.next(); reciters.put(k, (reciters.containsKey(k) ? reciters.get(k) : 0) + r.optInt(k));
            }
        }
        return new Object[]{sec / 60, plays, top(surahs, 5), top(reciters, 3)};
    }

    static List<Map.Entry<String, Integer>> top(Map<String, Integer> m, int n) {
        List<Map.Entry<String, Integer>> l = new ArrayList<>(m.entrySet());
        Collections.sort(l, (a, b) -> b.getValue() - a.getValue());
        return l.subList(0, Math.min(n, l.size()));
    }

    /** Per row name over the last seven days: {child, minutes, top app, stars spent, stars held now}. Kids Corner puts the shared screen time under "Kids" and each child's stars under their name. Null if Kids Corner can't share it yet. */
    static List<String[]> kidsWeek(Context c) {
        List<String> days = week();
        Map<String, int[]> totals = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> apps = new LinkedHashMap<>();
        try (Cursor cur = c.getContentResolver().query(KIDS_WEEK, null, null, null, null)) {
            if (cur == null) return null;
            int ci = cur.getColumnIndex("child"), di = cur.getColumnIndex("day"), mi = cur.getColumnIndex("minutes"),
                    ai = cur.getColumnIndex("top_app"), si = cur.getColumnIndex("stars"), li = cur.getColumnIndex("stars_left");
            if (mi < 0) return null;
            while (cur.moveToNext()) {
                if (di >= 0 && !days.contains(cur.getString(di))) continue;
                String child = ci >= 0 && cur.getString(ci) != null ? cur.getString(ci) : "Kids";
                int[] t = totals.get(child);
                if (t == null) { t = new int[]{0, 0, li >= 0 ? cur.getInt(li) : 0}; totals.put(child, t); apps.put(child, new LinkedHashMap<>()); } // newest row first: stars held now
                int min = cur.getInt(mi);
                t[0] += min;
                if (si >= 0) t[1] += cur.getInt(si);
                String app = ai >= 0 ? cur.getString(ai) : null;
                if (app != null && !app.isEmpty()) { Map<String, Integer> a = apps.get(child); a.put(app, (a.containsKey(app) ? a.get(app) : 0) + min); }
            }
        } catch (Exception e) {
            return null;
        }
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : totals.entrySet()) {
            List<Map.Entry<String, Integer>> a = top(apps.get(e.getKey()), 1);
            out.add(new String[]{e.getKey(), "" + e.getValue()[0], a.isEmpty() ? "" : a.get(0).getKey(), "" + e.getValue()[1], "" + e.getValue()[2]});
        }
        return out;
    }

    static String hm(int minutes) {
        if (minutes < 60) return minutes + " min";
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }
}
