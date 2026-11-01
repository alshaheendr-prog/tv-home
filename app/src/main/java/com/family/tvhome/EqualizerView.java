package com.family.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** The "now reciting" sound bars: four soft gold pills that rise and fall smoothly, drawn in one view every frame.
 *  Each bar mixes two slow waves so the motion looks natural, and they settle low when the recitation is paused. */
public class EqualizerView extends View {
    static final int BARS = 4;
    static final float[] SPEED = {2.1f, 2.9f, 2.4f, 3.3f}, SPEED2 = {3.7f, 1.9f, 4.3f, 2.6f}, PHASE = {0f, 1.7f, 3.1f, 4.4f};
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private boolean playing;
    private float level = 0.25f; // eases toward 1 while playing, 0.25 when paused
    private long last;
    private float t;

    public EqualizerView(Context c) { super(c); p.setColor(0xFFF1D58A); }

    public void setPlaying(boolean on) { playing = on; last = 0; postInvalidateOnAnimation(); }

    @Override protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        t += dt;
        float target = playing ? 1f : 0.25f;
        level += (target - level) * Math.min(1f, dt * 4f);
        int w = getWidth(), h = getHeight();
        float gap = w * 0.12f, bw = (w - gap * (BARS - 1)) / BARS;
        for (int i = 0; i < BARS; i++) {
            float wave = 0.5f + 0.3f * (float) Math.sin(t * SPEED[i] + PHASE[i]) + 0.2f * (float) Math.sin(t * SPEED2[i] + PHASE[i] * 2);
            float frac = 0.22f + 0.78f * (playing || level > 0.26f ? wave * level : 0.25f);
            float bh = Math.max(bw, h * Math.min(1f, frac));
            float x = i * (bw + gap), top = (h - bh) / 2f;
            r.set(x, top, x + bw, top + bh);
            c.drawRoundRect(r, bw / 2f, bw / 2f, p);
        }
        if (playing || Math.abs(level - target) > 0.005f) postInvalidateOnAnimation();
    }
}
