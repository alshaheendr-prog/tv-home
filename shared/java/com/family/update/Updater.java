package com.family.update;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * Keeps the family apps up to date without the PC. Shared by the home screen, Adhan and Kids Corner: each app
 * updates only itself, because Android lets an app replace itself without asking (an app with
 * UPDATE_PACKAGES_WITHOUT_USER_ACTION updating its own package), while updating another app would need a tap.
 *
 * About four times a day it reads the "latest" release of github.com/alshaheendr-prog/tv-home. When that build is
 * newer than the one installed, it downloads this app's APK and installs it at a quiet moment: while the TV is
 * off (the box asleep), or between 01:00 and 02:30, when no prayer falls in Kuwait. "Check now" in Parent settings
 * installs straight away. Android itself refuses an APK that isn't signed with the family key, and we refuse any
 * build that isn't newer than the one installed, so it can never go backwards.
 */
public final class Updater {
    private Updater() { }

    static final String RELEASE = "https://api.github.com/repos/alshaheendr-prog/tv-home/releases/tags/latest";
    static final String PREFS = "updater";
    static final int JOB_PERIODIC = 7101, JOB_IDLE = 7102, JOB_NIGHT = 7103, JOB_NOW = 7104;
    static final String ACTION_DONE = "com.family.update.DONE";
    /** Sent by the home screen's "Check now" to Adhan and Kids Corner (signature permission, family apps only). */
    public static final String ACTION_NOW = "com.family.update.NOW";
    public static final String PERMISSION_NOW = "com.family.permission.UPDATE_NOW";
    public static final String[] FAMILY = { "com.family.tvhome", "com.family.adhan", "com.family.kids" };

    /** The release file for each app, as the build workflow names them. */
    static String assetFor(String pkg) {
        switch (pkg) {
            case "com.family.tvhome": return "tv-home.apk";
            case "com.family.adhan": return "family-adhan.apk";
            case "com.family.kids": return "kids-corner.apk";
            default: return null;
        }
    }

    // ---- scheduling -------------------------------------------------------------------------------------------

    /** Safe to call often (every app start): sets up the regular check once and leaves it alone after that. */
    public static void schedule(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(JOB_PERIODIC) != null) return;
        js.schedule(new JobInfo.Builder(JOB_PERIODIC, new ComponentName(c, UpdateJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(6 * 3600_000L, 2 * 3600_000L)
                .setPersisted(true)
                .build());
    }

    /** Check and install straight away (Parent settings). Runs in the background; the status line tells how it went. */
    public static void checkNow(Context c) {
        status(c, "Checking…");
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        js.schedule(new JobInfo.Builder(JOB_NOW, new ComponentName(c, UpdateJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(10_000)
                .build());
    }

    /** Asks Adhan and Kids Corner to check now too (they each update themselves). */
    public static void checkFamilyNow(Context c) {
        for (String p : FAMILY) {
            if (p.equals(c.getPackageName())) continue;
            Intent i = new Intent(ACTION_NOW).setPackage(p);
            try { c.sendBroadcast(i, PERMISSION_NOW); } catch (Exception ignored) { }
        }
    }

    /** A downloaded update waits for the next quiet moment: the box going idle, or the night window. */
    private static void scheduleInstall(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        ComponentName job = new ComponentName(c, UpdateJob.class);
        if (js.getPendingJob(JOB_IDLE) == null)
            js.schedule(new JobInfo.Builder(JOB_IDLE, job).setRequiresDeviceIdle(true).setPersisted(true).build());
        if (js.getPendingJob(JOB_NIGHT) == null) {
            long wait = msUntil(1, 10);
            js.schedule(new JobInfo.Builder(JOB_NIGHT, job)
                    .setMinimumLatency(wait).setOverrideDeadline(wait + 45 * 60_000L).setPersisted(true).build());
        }
    }

    private static long msUntil(int hour, int minute) {
        Calendar now = Calendar.getInstance(), at = (Calendar) now.clone();
        at.set(Calendar.HOUR_OF_DAY, hour); at.set(Calendar.MINUTE, minute); at.set(Calendar.SECOND, 0);
        if (!at.after(now)) at.add(Calendar.DAY_OF_YEAR, 1);
        return at.getTimeInMillis() - now.getTimeInMillis();
    }

    // ---- the check ------------------------------------------------------------------------------------------

    /** Runs on a background thread (UpdateJob). now = the family asked from Parent settings. */
    static void run(Context c, boolean now) {
        try {
            String pkg = c.getPackageName();
            long mine = installedBuild(c);
            File apk = new File(c.getCacheDir(), "update.apk");
            SharedPreferences p = prefs(c);
            long ready = p.getLong("ready", 0);

            // 1. A build already downloaded on an earlier check: install it if now is a quiet moment.
            if (ready > mine && apk.length() > 0) {
                if (now || quiet(c)) install(c, apk, ready);
                else { status(c, "Installs tonight"); scheduleInstall(c); }
                return;
            }

            // 2. Ask GitHub which build is the latest.
            JSONObject rel = new JSONObject(get(RELEASE));
            long latest = buildNumber(rel.optString("name"));
            if (latest <= mine) {
                p.edit().remove("ready").apply();
                apk.delete();
                status(c, "Up to date");
                return;
            }
            String url = null; long size = -1;
            JSONArray assets = rel.optJSONArray("assets");
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (assetFor(pkg).equals(a.optString("name")) && "uploaded".equals(a.optString("state", "uploaded"))) {
                    url = a.optString("browser_download_url"); size = a.optLong("size", -1);
                }
            }
            if (url == null || url.isEmpty()) { status(c, "New build not ready yet"); return; }

            // 3. Download it, then make sure it really is this app and really is newer before keeping it.
            status(c, "Downloading #" + latest + "…");
            File part = new File(c.getCacheDir(), "update.part");
            download(url, part);
            if (size > 0 && part.length() != size) { part.delete(); status(c, "Download failed, tries later"); return; }
            PackageInfo info = c.getPackageManager().getPackageArchiveInfo(part.getPath(), 0);
            if (info == null || !pkg.equals(info.packageName) || info.getLongVersionCode() <= mine) {
                part.delete(); status(c, "Download failed, tries later"); return;
            }
            apk.delete();
            if (!part.renameTo(apk)) { part.delete(); return; }
            p.edit().putLong("ready", info.getLongVersionCode()).apply();

            if (now || quiet(c)) install(c, apk, info.getLongVersionCode());
            else { status(c, "Installs tonight"); scheduleInstall(c); }
        } catch (Exception e) {
            status(c, "Offline, tries later");
        }
    }

    /** The TV is off (box asleep), or it's 01:00 to 02:30: after Isha and well before Fajr all year in Kuwait. */
    static boolean quiet(Context c) {
        PowerManager pm = c.getSystemService(PowerManager.class);
        if (pm != null && !pm.isInteractive()) return true;
        Calendar now = Calendar.getInstance();
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return m >= 60 && m <= 150;
    }

    private static void install(Context c, File apk, long build) throws Exception {
        PackageInstaller pi = c.getPackageManager().getPackageInstaller();
        for (PackageInstaller.SessionInfo s : pi.getMySessions()) {
            try { pi.abandonSession(s.getSessionId()); } catch (Exception ignored) { }
        }
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(c.getPackageName());
        sp.setSize(apk.length());
        if (Build.VERSION.SDK_INT >= 31) sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        int id = pi.createSession(sp);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream out = s.openWrite("base.apk", 0, apk.length())) {
                byte[] b = new byte[64 * 1024];
                for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
                s.fsync(out);
            }
            status(c, "Installing #" + build + "…");
            Intent done = new Intent(c, UpdateReceiver.class).setAction(ACTION_DONE).putExtra("build", build);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            s.commit(PendingIntent.getBroadcast(c, id, done, flags).getIntentSender());
        }
        // From here Android replaces the app; this process ends and the new build starts by itself.
    }

    // ---- small helpers --------------------------------------------------------------------------------------

    static long buildNumber(String releaseName) {
        // the workflow names the release "Latest build #123"
        int i = releaseName == null ? -1 : releaseName.lastIndexOf('#');
        if (i < 0) return -1;
        try { return Long.parseLong(releaseName.substring(i + 1).trim()); } catch (NumberFormatException e) { return -1; }
    }

    static long installedBuild(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return Long.MAX_VALUE; // never happens for our own package; if it did, never install
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(20_000);
        h.setReadTimeout(60_000);
        h.setRequestProperty("User-Agent", "family-tv-updater");
        h.setInstanceFollowRedirects(true); // GitHub hands the file over from its download server
        if (h.getResponseCode() != 200) { h.disconnect(); throw new Exception("HTTP " + h.getResponseCode()); }
        return h;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection h = open(url);
        try (InputStream in = h.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[16 * 1024];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            return out.toString("UTF-8");
        } finally { h.disconnect(); }
    }

    private static void download(String url, File to) throws Exception {
        HttpURLConnection h = open(url);
        try (InputStream in = h.getInputStream(); OutputStream out = new FileOutputStream(to)) {
            byte[] b = new byte[64 * 1024];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
        } finally { h.disconnect(); }
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static void status(Context c, String s) {
        prefs(c).edit().putString("status", s).putLong("status_at", System.currentTimeMillis()).apply();
    }

    /** One short line for Parent settings, e.g. "#212 · Up to date". */
    public static String statusLine(Context c) {
        String s = prefs(c).getString("status", null);
        return "#" + installedBuild(c) + " · " + (s == null ? "On" : s);
    }
}
