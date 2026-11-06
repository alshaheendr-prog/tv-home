package com.family.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The Quran page's backdrop: deep night blue with a lattice of gold eight-pointed stars that drifts very slowly,
 * and a warm glow that breathes behind the page.
 *
 * Kept light for the 2 GB box: the night-blue ramp and its darker top and bottom are painted once, dithered, into a
 * narrow strip that is tiled across (one opaque quad a frame, no banding in a dark room). The lattice fades out at the
 * top and bottom through its own paint, instead of a full-screen fade drawn over it, and the glow covers only its circle.
 */
public class StarField extends View {
    static final int W = 1920 + 2 * MainActivity.BLEED, H = 1080 + 2 * MainActivity.BLEED, STEP = 150; // reaches past the layout's edges
    static final float GLOW_R = 900;
    private static Bitmap backdrop; // shared by the Quran page and the player: 64 x H, about 0.4 MB
    private final Paint back = new Paint(), line = new Paint(Paint.ANTI_ALIAS_FLAG), glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader lineFade;
    private final Matrix lineShift = new Matrix();
    private float shownShift = Float.NaN;
    private final Path lattice = new Path();
    private final long start = System.currentTimeMillis();

    public StarField(Context c) {
        super(c);
        if (backdrop == null) backdrop = paintBackdrop();
        back.setShader(new BitmapShader(backdrop, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1.6f);
        // the lines are 0x26 gold, fading where the old full-screen fade darkened the top (80%) and bottom (70%)
        lineFade = new LinearGradient(0, 0, 0, H, new int[]{0x08F1D58A, 0x26F1D58A, 0x26F1D58A, 0x0BF1D58A},
                new float[]{0, 0.28f, 0.75f, 1}, Shader.TileMode.CLAMP);
        line.setShader(lineFade);
        glow.setShader(new RadialGradient(W * 0.5f, H * 0.42f, GLOW_R, 0x30F1D58A, 0x00F1D58A, Shader.TileMode.CLAMP));
        glow.setDither(true);
        for (int y = -STEP; y < H + 2 * STEP; y += STEP)
            for (int x = -STEP; x < W + 2 * STEP; x += STEP) {
                star(x, y, 46, 0);
                lattice.addCircle(x + STEP / 2f, y + STEP / 2f, 6, Path.Direction.CW);
            }
    }

    /** The night-blue ramp with the darker top and bottom, painted once with dithering. */
    private static Bitmap paintBackdrop() {
        Bitmap b = Bitmap.createBitmap(64, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint();
        p.setDither(true);
        p.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xFF0B1124, 0xFF0A0F1F, 0xFF070A15}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 64, H, p);
        p.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xCC070A15, 0x00070A15, 0x00070A15, 0xB3070A15},
                new float[]{0, 0.28f, 0.75f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 64, H, p);
        return b;
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
        c.drawRect(0, 0, W, H, back);
        float breathe = 0.75f + 0.25f * (float) Math.sin(t * 0.5f);
        glow.setAlpha(Math.round(255 * breathe)); // one gradient made once; only its strength changes each frame
        float gx = W * 0.5f, gy = H * 0.42f;
        c.drawRect(gx - GLOW_R, Math.max(0, gy - GLOW_R), gx + GLOW_R, Math.min(H, gy + GLOW_R), glow);
        float dx = (t * 6) % STEP, dy = (t * 3) % STEP; // about a pixel every few frames: barely there, but alive
        // the fade stays put on screen while the lattice drifts; it only moves up and down, by whole pixels, so the
        // shader is changed only when that pixel changes (about 3 times a second) instead of rebuilt every frame
        float shift = (float) Math.floor(dy);
        if (shift != shownShift) {
            shownShift = shift;
            lineShift.setTranslate(0, -shift);
            lineFade.setLocalMatrix(lineShift);
        }
        c.save();
        c.translate(dx, dy);
        c.drawPath(lattice, line);
        c.restore();
        postInvalidateOnAnimation();
    }
}
