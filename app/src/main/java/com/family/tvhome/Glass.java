package com.family.tvhome;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A frosted glass panel with a soft outer glow, a bright top edge and a faint sheen.
 * It is painted once into a bitmap when its size changes, so drawing it each frame costs one bitmap blit.
 * The glow is drawn outside the bounds, so the parent must not clip its children.
 * When a blurred copy of the wallpaper is available ({@link #setBackdrop}), the panel shows it through the frost,
 * which is what makes it look like real frosted glass.
 */
final class Glass extends Drawable {
    private final float radius, glow;
    private final int glowColor, tint;
    private Bitmap cache;
    private final Paint blit = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private final RectF dst = new RectF();
    private final int[] loc = new int[2];
    private boolean frosted;

    private static Bitmap backdrop;
    private static float screenW = 1920, screenH = 1080, stageScale = 1;
    private static final Paint BACKDROP = new Paint(Paint.FILTER_BITMAP_FLAG);
    private static final List<WeakReference<Glass>> FROSTED = new ArrayList<>();

    /** Show the blurred wallpaper through this panel. */
    Glass frosted() {
        frosted = true;
        FROSTED.add(new WeakReference<>(this));
        return this;
    }

    static void setScreen(float w, float h, float scale) { screenW = w; screenH = h; stageScale = scale; }

    /** A small, blurred copy of the whole screen's video, or null to stop showing it. */
    static void setBackdrop(Bitmap b) {
        backdrop = b;
        for (Iterator<WeakReference<Glass>> it = FROSTED.iterator(); it.hasNext(); ) {
            Glass g = it.next().get();
            if (g == null) it.remove(); else g.invalidateSelf();
        }
    }

    /** glow is the glow's reach in px; tint is an extra fill under the white frost (use a dark tint for busy videos). */
    Glass(float radius, float glow, int glowColor, int tint) {
        this.radius = radius;
        this.glow = glow;
        this.glowColor = glowColor;
        this.tint = tint;
    }

    @Override
    protected void onBoundsChange(Rect b) {
        if (b.width() <= 0 || b.height() <= 0) return;
        if (cache != null && cache.getWidth() == b.width() + 2 * (int) glow && cache.getHeight() == b.height() + 2 * (int) glow) return;
        int g = (int) glow, w = b.width(), h = b.height();
        cache = Bitmap.createBitmap(w + 2 * g, h + 2 * g, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(cache);
        RectF r = new RectF(g, g, g + w, g + h);
        clip.reset();
        clip.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CW);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (g > 0 && glowColor != 0) {
            p.setColor(glowColor);
            p.setMaskFilter(new BlurMaskFilter(g * 0.7f, BlurMaskFilter.Blur.OUTER));
            c.drawRoundRect(r, radius, radius, p);
            p.setMaskFilter(null);
        }
        p.setColor(tint);
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(new LinearGradient(0, g, 0, g + h, 0x33FFFFFF, 0x12FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, radius, radius, p);
        // sheen on the upper part
        p.setShader(new LinearGradient(0, g, 0, g + Math.min(h * 0.5f, 140), 0x1CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, radius, radius, p);
        // edge: bright on top, fading down, a little light again at the bottom
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setShader(new LinearGradient(0, g, 0, g + h, new int[]{0xB3FFFFFF, 0x26FFFFFF, 0x40FFFFFF}, new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
        RectF e = new RectF(r.left + 1, r.top + 1, r.right - 1, r.bottom - 1);
        c.drawRoundRect(e, radius - 1, radius - 1, p);
    }

    @Override
    public void draw(Canvas canvas) {
        if (cache == null) return;
        Rect b = getBounds();
        Bitmap bd = backdrop;
        if (frosted && bd != null && getCallback() instanceof View) {
            View v = (View) getCallback();
            v.getLocationInWindow(loc);
            float k = stageScale * v.getScaleX();
            dst.set(-loc[0] / k, -loc[1] / k, (screenW - loc[0]) / k, (screenH - loc[1]) / k);
            int save = canvas.save();
            canvas.translate(b.left, b.top);
            canvas.clipPath(clip);
            BACKDROP.setAlpha(blit.getAlpha());
            canvas.drawBitmap(bd, null, dst, BACKDROP);
            canvas.restoreToCount(save);
        }
        canvas.drawBitmap(cache, b.left - (int) glow, b.top - (int) glow, blit);
    }

    @Override public void setAlpha(int alpha) { blit.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { blit.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
