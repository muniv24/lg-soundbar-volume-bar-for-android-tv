package com.muniv24.volumebar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/** The on-screen bar: a dark rounded card with "Volume 29%" and a bar under it, top right. */
final class VolumeBarView extends View {
    // Sizes for a 1920-wide screen; scaled to the real screen.
    private static final float W = 400, H = 120, RADIUS = 24, PAD = 32, TEXT = 34, BAR_H = 8;

    private final float k;
    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private String label = "";
    private int percent;

    VolumeBarView(Context c) {
        super(c);
        k = c.getResources().getDisplayMetrics().widthPixels / 1920f;
        card.setColor(0xE6202020);
        text.setColor(Color.WHITE);
        text.setTextSize(TEXT * k);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        track.setColor(0x40FFFFFF);
        fill.setColor(Color.WHITE);
        setVisibility(GONE);
    }

    /** Adds the bar to the screen, hidden. Needs the "show over other apps" permission. */
    void attach(WindowManager wm) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                Math.round(W * k), Math.round(H * k),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = Math.round(64 * k);
        p.y = Math.round(64 * k);
        wm.addView(this, p);
    }

    void show(int percent, boolean muted) {
        this.percent = muted ? 0 : Math.max(0, Math.min(100, percent));
        label = muted ? getContext().getString(R.string.muted)
                : getContext().getString(R.string.volume, this.percent);
        setVisibility(VISIBLE);
        invalidate();
    }

    void hide() {
        setVisibility(GONE);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = PAD * k;
        r.set(0, 0, w, h);
        c.drawRoundRect(r, RADIUS * k, RADIUS * k, card);
        c.drawText(label, pad, pad + TEXT * k * 0.8f, text);

        float top = h - pad - BAR_H * k, round = BAR_H * k / 2;
        r.set(pad, top, w - pad, top + BAR_H * k);
        c.drawRoundRect(r, round, round, track);
        r.right = pad + (w - 2 * pad) * percent / 100f;
        if (percent > 0) c.drawRoundRect(r, round, round, fill);
    }
}
