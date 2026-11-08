package com.family.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import java.io.File;

/**
 * Two messages arrive here: Android's answer after an install (ACTION_DONE, from our own install session), and
 * "Check now" from the home screen's Parent settings (ACTION_NOW, family apps only, by signature permission).
 */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String action = i.getAction();
        if (Updater.ACTION_NOW.equals(action)) { Updater.checkNow(c); return; }
        if (!Updater.ACTION_DONE.equals(action)) return;

        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (st == PackageInstaller.STATUS_SUCCESS) {
            // usually this process has already been replaced by the new build before this arrives
            new File(c.getCacheDir(), "update.apk").delete();
            Updater.prefs(c).edit().remove("ready").apply();
            Updater.status(c, "Updated");
        } else if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Android wants a tap after all (Update TV hasn't switched updates on yet). Never pop a dialog up by
            // itself over what the family is watching: say so in Parent settings and leave the download ready.
            Updater.status(c, "Run Update TV once");
        } else {
            Updater.status(c, "Didn't install, tries later");
            new File(c.getCacheDir(), "update.apk").delete();
            Updater.prefs(c).edit().remove("ready").apply();
        }
    }
}
