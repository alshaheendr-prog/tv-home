package com.family.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

/** A soft oval of light in the focused app's own colour, drawn behind the dock's icons, that glides to follow focus. */
final class BrandGlow extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cx = -1, strength;
    private int color = 0xFFFFFFFF;
    private ValueAnimator anim;
    private float shaderR = -1;
    private int filterColor;

    /** Glide to x (in the dock's coordinates) and color; a negative x fades the glow away. */
    void moveTo(float x, int c) {
        if (anim != null) anim.cancel();
        final float fromX = cx < 0 ? x : cx, toX = x < 0 ? cx : x, fromS = strength, toS = x < 0 ? 0 : 1;
        final int toC = Accent.mix(c | 0xFF000000, Accent.color, 0.3f * Accent.strength); // a touch of the wallpaper's colour
        color = toC; // the colour changes at once (one new gradient per move); only position and strength glide
        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(420);
        anim.setInterpolator(new DecelerateInterpolator(2f));
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            cx = fromX + (toX - fromX) * f;
            strength = fromS + (toS - fromS) * f;
            invalidateSelf();
        });
        anim.start();
    }

    @Override public void draw(Canvas c) {
        if (strength <= 0.01f || cx < 0) return;
        Rect b = getBounds();
        float cy = b.top + b.height() * 0.62f, r = b.height() * 1.1f;
        if (r != shaderR || color != filterColor) {
            shaderR = r;
            filterColor = color;
            int rgb = color & 0xFFFFFF;
            paint.setShader(new RadialGradient(0, 0, r, new int[]{0x66000000 | rgb, 0x22000000 | rgb, rgb}, new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP));
        }
        paint.setAlpha(Math.round(255 * strength));
        c.save();
        c.translate(b.left + cx, cy);
        c.scale(1.8f, 1f); // a wide, low oval
        c.drawCircle(0, 0, r, paint);
        c.restore();
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
