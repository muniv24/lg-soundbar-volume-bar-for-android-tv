package com.muniv24.volumebar;

import android.content.Context;
import android.content.Intent;

/** The one setting (bar on or off), and starting or stopping the service to match it. */
final class Prefs {
    private static final String FILE = "volumebar";
    private static final String KEY_ON = "on";

    private Prefs() {}

    static boolean isOn(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ON, true);
    }

    static void setOn(Context c, boolean on) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply();
        sync(c);
    }

    /** Runs the service while the bar is on, and stops it while it is off. */
    static void sync(Context c) {
        Intent service = new Intent(c, VolumeService.class);
        if (!isOn(c)) {
            c.stopService(service);
            return;
        }
        try {
            c.startForegroundService(service);
        } catch (RuntimeException tooEarly) {
            // Android can refuse while the app is in the background; the next boot or open retries.
        }
    }

    static boolean canDraw(Context c) {
        return android.provider.Settings.canDrawOverlays(c);
    }
}
