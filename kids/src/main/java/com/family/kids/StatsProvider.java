package com.family.kids;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * Read-only week of stats for TV Home's family stats page:
 * content://com.family.kids.stats/week, guarded by a signature permission.
 *
 * Rows, newest day first. The children share the TV, so screen time is one "Kids" row a day
 * (minutes, top_app); each child also gets a row a day with the stars they spent (minutes 0)
 * and stars_left, how many they hold now.
 */
public class StatsProvider extends ContentProvider {
    static final String[] COLUMNS = {"child", "day", "minutes", "top_app", "stars", "stars_left"};

    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!"week".equals(uri.getLastPathSegment())) return null;
        android.content.Context c = getContext();
        MatrixCursor out = new MatrixCursor(COLUMNS);
        String[] kids = Rules.kids(c);
        long clear = android.os.Binder.clearCallingIdentity();
        try {
            for (int i = 0; i < Stats.DAYS; i++) {
                String d = Stats.day(i);
                out.addRow(new Object[]{"Kids", d, (int) (Stats.seconds(c, d) / 60), label(Stats.topApp(c, d)), 0, 0});
                for (int k = 0; k < kids.length; k++)
                    out.addRow(new Object[]{kids[k], d, 0, "", Stats.starsSpent(c, d, k), Rules.stars(c, k)});
            }
        } finally {
            android.os.Binder.restoreCallingIdentity(clear);
        }
        return out;
    }

    private String label(String pkg) {
        if (pkg.isEmpty()) return "";
        if (pkg.equals(getContext().getPackageName())) return "Kids Corner";
        try {
            PackageManager pm = getContext().getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { return pkg; }
    }

    @Override public String getType(Uri uri) { return "vnd.android.cursor.dir/vnd.com.family.kids.stats"; }
    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
