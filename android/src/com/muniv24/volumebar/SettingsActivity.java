package com.muniv24.volumebar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** The app's only screen: is the bar ready, turn it on or off, and show a test. */
public final class SettingsActivity extends Activity {
    private TextView status, command;
    private Button toggle, allow;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.settings);
        status = findViewById(R.id.status);
        command = findViewById(R.id.command);
        toggle = findViewById(R.id.toggle);
        allow = findViewById(R.id.allow);

        toggle.setOnClickListener(v -> {
            Prefs.setOn(this, !Prefs.isOn(this));
            refresh();
        });
        findViewById(R.id.test).setOnClickListener(v -> startForegroundService(
                new Intent(this, VolumeService.class).setAction(VolumeService.ACTION_TEST)));
        allow.setOnClickListener(v -> askPermission());
        toggle.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        Prefs.sync(this); // also picks up a permission given while we were away
        refresh();
    }

    private void refresh() {
        boolean ready = Prefs.canDraw(this);
        status.setText(ready ? R.string.status_ready : R.string.status_needs_permission);
        status.setTextColor(ready ? 0xFF5AD282 : 0xFFFFBE50);
        allow.setVisibility(ready ? View.GONE : View.VISIBLE);
        toggle.setText(Prefs.isOn(this) ? R.string.bar_on : R.string.bar_off);
    }

    // Most TVs have no screen for this permission; then the adb command is shown instead.
    private void askPermission() {
        Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException noScreen) {
            status.setText(R.string.status_no_screen);
            command.setVisibility(View.VISIBLE);
        }
    }
}
