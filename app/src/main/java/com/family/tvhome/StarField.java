package com.family.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The Quran page's backdrop: deep night blue with a lattice of gold eight-pointed stars that drifts very slowly,
 * and a warm glow that breathes behind the page. One path, drawn with a moving offset, so it stays light on the box.
 */
public class StarField extends View {
    static final int W = 1920 + 2 * MainActivity.BLEED, H = 1080 + 2 * MainActivity.BLEED, STEP = 150; // reaches past the layout's edges
    private final Paint bg = new Paint(), line = new Paint(Paint.ANTI_ALIAS_FLAG), glow = new Paint(Paint.ANTI_ALIAS_FLAG),
            fade = new Paint();
    private final Path lattice = new Path();
    private final long start = System.currentTimeMillis();

    public StarField(Context c) {
        super(c);
        bg.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xFF0B1124, 0xFF0A0F1F, 0xFF070A15}, null, Shader.TileMode.CLAMP));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1.6f);
        line.setColor(0x26F1D58A);
        glow.setShader(new RadialGradient(W * 0.5f, H * 0.42f, 900, 0x30F1D58A, 0x00F1D58A, Shader.TileMode.CLAMP));
        fade.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xCC070A15, 0x00070A15, 0x00070A15, 0xB3070A15},
                new float[]{0, 0.28f, 0.75f, 1}, Shader.TileMode.CLAMP));
        for (int y = -STEP; y < H + 2 * STEP; y += STEP)
            for (int x = -STEP; x < W + 2 * STEP; x += STEP) {
                star(x, y, 46, 0);
                lattice.addCircle(x + STEP / 2f, y + STEP / 2f, 6, Path.Direction.CW);
            }
    }

    private void star(float cx, float cy, float r, float turn) {
        for (int sq = 0; sq < 2; sq++)
            for (int i = 0; i <= 4; i++) {
                double a = turn + sq * Math.PI / 4 + i * Math.PI / 2;
                float x = cx + (float) (r * Math.cos(a)), y = cy + (float) (r * Math.sin(a));
                if (i == 0) lattice.moveTo(x, y); else lattice.lineTo(x, y);
            }
    }

    @Override
    protected void onDraw(Canvas c) {
        float t = (System.currentTimeMillis() - start) / 1000f;
        c.drawRect(0, 0, W, H, bg);
        float breathe = 0.75f + 0.25f * (float) Math.sin(t * 0.5f);
        glow.setAlpha(Math.round(255 * breathe)); // one gradient made once; only its strength changes each frame
        c.drawRect(0, 0, W, H, glow);
        c.save();
        c.translate((t * 6) % STEP, (t * 3) % STEP); // about a pixel every few frames: barely there, but alive
        c.drawPath(lattice, line);
        c.restore();
        c.drawRect(0, 0, W, H, fade);
        postInvalidateOnAnimation();
    }
}
