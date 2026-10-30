package com.family.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** A small arc from sunrise to sunset with the sun where it is now; after sunset a pale moon crosses below the horizon line. */
public class SunArc extends View {
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG), ground = new Paint(Paint.ANTI_ALIAS_FLAG),
            sun = new Paint(Paint.ANTI_ALIAS_FLAG), glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int rise, set, now;

    public SunArc(Context c) {
        super(c);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(2);
        arc.setColor(0x66FFFFFF);
        arc.setPathEffect(new DashPathEffect(new float[]{4, 6}, 0));
        ground.setColor(0x4DFFFFFF);
        ground.setStrokeWidth(2);
    }

    /** All in minutes after midnight. */
    public void set(int rise, int set, int now) {
        this.rise = rise; this.set = set; this.now = now;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 10, base = h - 8;
        RectF oval = new RectF(pad, base - (h - 16) , w - pad, base + (h - 16));
        c.drawArc(oval, 180, 180, false, arc);
        c.drawLine(0, base, w, base, ground);
        if (set <= rise) return;
        boolean day = now >= rise && now <= set;
        double f;
        if (day) f = (now - rise) / (double) (set - rise);
        else {
            int night = 1440 - (set - rise);
            int since = now > set ? now - set : now + 1440 - set;
            f = since / (double) night;
        }
        double a = Math.PI * (1 - f);
        float rx = (w - 2 * pad) / 2, ry = h - 16;
        float x = w / 2 + (float) (Math.cos(a) * rx);
        float y = day ? base - (float) (Math.sin(a) * ry) : base + 4;
        int color = day ? 0xFFF1D58A : 0xCCDDE6FF;
        glow.setShader(new RadialGradient(x, y, 20, color & 0x80FFFFFF, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, 20, glow);
        sun.setColor(color);
        c.drawCircle(x, y, day ? 8 : 6, sun);
    }
}
