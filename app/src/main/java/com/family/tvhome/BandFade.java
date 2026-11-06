package com.family.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * The shade behind the verse band: about 40% black over the whole text block, easing to clear above and below it
 * (six stops, so the kicker and the last line sit on the full shade), and clear towards the left, where there are no
 * words. One gradient multiplied by the other, built once per size; dithered, so it never bands over a dark clip.
 */
class BandFade extends Drawable {
    private static final int SHADE = 0x66000000;
    private final Paint paint = new Paint();

    BandFade() { paint.setDither(true); }

    @Override
    protected void onBoundsChange(Rect b) {
        int h = b.height(), w = b.width();
        if (h <= 0 || w <= 0) return;
        float edge = Math.min(0.45f, MainActivity.LINE_FADE / (float) h); // the clear-to-full ramp is the band's padding
        Shader down = new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{0, SHADE, SHADE, SHADE, SHADE, 0},
                new float[]{0, edge, 0.35f, 0.65f, 1 - edge, 1}, Shader.TileMode.CLAMP);
        Shader across = new LinearGradient(b.left, 0, b.left + w * 0.35f, 0, 0x00000000, 0xFF000000, Shader.TileMode.CLAMP);
        paint.setShader(new ComposeShader(down, across, PorterDuff.Mode.DST_IN));
    }

    @Override public void draw(Canvas c) { c.drawRect(getBounds(), paint); }
    @Override public void setAlpha(int a) { paint.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
