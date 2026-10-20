package com.muniv24.volumebar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the bar when the TV starts, and after the app is updated. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Prefs.sync(c);
    }
}
