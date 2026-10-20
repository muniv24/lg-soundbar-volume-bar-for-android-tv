package com.muniv24.volumebar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/** The app's only screen: is the bar ready, turn it on or off, show a test, and set it up. */
public final class SettingsActivity extends Activity {
    /** How long to wait for the user to press OK on the TV's "Allow debugging?" question. */
    private static final int APPROVE_MS = 60000;
    private static final int GREEN = 0xFF5AD282, AMBER = 0xFFFFBE50;

    private TextView status;
    private Button toggle, allow, auto;
    private boolean settingUp;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.settings);
        status = findViewById(R.id.status);
        toggle = findViewById(R.id.toggle);
        allow = findViewById(R.id.allow);
        auto = findViewById(R.id.auto);

        toggle.setOnClickListener(v -> {
            Prefs.setOn(this, !Prefs.isOn(this));
            refresh();
        });
        findViewById(R.id.test).setOnClickListener(v -> startForegroundService(
                new Intent(this, VolumeService.class).setAction(VolumeService.ACTION_TEST)));
        allow.setOnClickListener(v -> openPermissionScreen());
        auto.setOnClickListener(v -> setUpAutomatically());
        toggle.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        Prefs.sync(this); // also picks up a permission given while we were away
        refresh();
    }

    private void refresh() {
        boolean ready = Prefs.canDraw(this);
        if (!settingUp) say(ready ? getString(R.string.status_ready) : getString(R.string.status_needs_permission),
                ready ? GREEN : AMBER);
        allow.setVisibility(ready ? View.GONE : View.VISIBLE);
        auto.setVisibility(ready ? View.GONE : View.VISIBLE);
        toggle.setText(Prefs.isOn(this) ? R.string.bar_on : R.string.bar_off);
    }

    /** Most TVs have Android's own permission screen; some (such as Xiaomi) don't. */
    private void openPermissionScreen() {
        Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException noScreen) {
            say(getString(R.string.no_screen), AMBER);
            auto.requestFocus();
        }
    }

    /** Gives the app its permission through the TV's own debugging, with no computer. */
    private void setUpAutomatically() {
        if (settingUp) return;
        settingUp = true;
        say(getString(R.string.auto_working), AMBER);
        String grant = "appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow";
        new Thread(() -> {
            String problem = null;
            try (LocalAdb adb = new LocalAdb(this, APPROVE_MS)) {
                adb.shell(grant);
            } catch (ConnectException off) {
                problem = getString(R.string.auto_debugging_off);
            } catch (SocketTimeoutException | java.io.EOFException noAnswer) {
                problem = getString(R.string.auto_no_answer);
            } catch (Exception e) {
                problem = getString(R.string.auto_failed, e.getMessage());
            }
            String result = problem;
            runOnUiThread(() -> finishSetup(result));
        }, "setup").start();
    }

    private void finishSetup(String problem) {
        settingUp = false;
        if (problem == null && Prefs.canDraw(this)) {
            Prefs.sync(this);
            refresh();
            say(getString(R.string.auto_done), GREEN);
            findViewById(R.id.test).requestFocus();
        } else {
            refresh();
            say(problem != null ? problem : getString(R.string.auto_no_answer), AMBER);
        }
    }

    private void say(String text, int color) {
        status.setText(text);
        status.setTextColor(color);
    }
}
