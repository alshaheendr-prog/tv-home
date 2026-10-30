package com.family.adhan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the adhan service when the box turns on or the app is updated. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        AdhanService.start(c);
    }
}
