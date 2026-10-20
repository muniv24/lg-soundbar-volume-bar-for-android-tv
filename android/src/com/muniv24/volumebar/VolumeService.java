package com.muniv24.volumebar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.WindowManager;

/**
 * Listens for volume changes and shows the bar. Runs in the background while the bar is on.
 *
 * When the volume button is held, Android's number runs ahead of the soundbar: the soundbar only
 * takes some of the repeated presses, then reports its real level a moment later. So the bar shows
 * the number as it moves, then reads it once more after the presses stop, and hides after that.
 */
public final class VolumeService extends Service {
    static final String ACTION_TEST = "com.muniv24.volumebar.TEST";

    private static final String VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
    private static final String MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION";
    private static final String EXTRA_STREAM = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREV = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    /** Wait this long after the last press for the soundbar to report its real level. */
    private static final long SETTLE_MS = 900;
    /** Then keep the bar up this long. */
    private static final long SHOW_MS = 2500;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SoundbarVolume volume;
    private WindowManager windows;
    private VolumeBarView bar;

    private final Runnable settle = this::draw;
    private final Runnable hide = () -> {
        if (bar != null) bar.hide();
        if (!Prefs.isOn(this)) stopSelf(); // was only started for a test
    };

    private final BroadcastReceiver onVolume = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.getIntExtra(EXTRA_STREAM, -1) != AudioManager.STREAM_MUSIC) return;
            boolean sameLevel = VOLUME_CHANGED.equals(i.getAction())
                    && i.getIntExtra(EXTRA_VALUE, -1) == i.getIntExtra(EXTRA_PREV, -2);
            if (sameLevel || volume.onTvSpeakers()) return;
            pop();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        goForeground();
        volume = new SoundbarVolume((AudioManager) getSystemService(AUDIO_SERVICE));
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);

        IntentFilter f = new IntentFilter(VOLUME_CHANGED);
        f.addAction(MUTE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(onVolume, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(onVolume, f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        // The permission may be given after the service started, so the bar is added on first chance.
        if (bar == null && Prefs.canDraw(this)) {
            bar = new VolumeBarView(this);
            bar.attach(windows);
        }
        if (intent != null && ACTION_TEST.equals(intent.getAction())) pop();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        unregisterReceiver(onVolume);
        ui.removeCallbacksAndMessages(null);
        if (bar != null) windows.removeView(bar);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    /** Shows the bar now, re-reads the level once the presses stop, then hides it. */
    private void pop() {
        if (bar == null) return;
        draw();
        ui.removeCallbacks(settle);
        ui.removeCallbacks(hide);
        ui.postDelayed(settle, SETTLE_MS);
        ui.postDelayed(hide, SETTLE_MS + SHOW_MS);
    }

    private void draw() {
        bar.show(volume.percent(), volume.muted());
    }

    // Android only lets a service run in the background with a (here silent) notification.
    private void goForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("bar", getString(R.string.channel),
                NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "bar")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle(getString(R.string.app_name))
                .build();
        startForeground(1, n);
    }
}
