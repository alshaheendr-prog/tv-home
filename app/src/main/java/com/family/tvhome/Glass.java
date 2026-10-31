package com.family.tvhome;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;


/**
 * A frosted glass panel with a soft outer glow, a bright top edge and a faint sheen.
 * It is painted once into a bitmap when its size changes, so drawing it each frame costs one bitmap blit.
 * The glow is drawn outside the bounds, so the parent must not clip its children.
 */
final class Glass extends Drawable {
    private final float radius, glow;
    private final int glowColor, tint;
    private Bitmap cache;
    private final Paint blit = new Paint(Paint.FILTER_BITMAP_FLAG);
    /** glow is the glow's reach in px; tint is an extra fill under the white frost (use a dark tint for busy videos). */
    Glass(float radius, float glow, int glowColor, int tint) {
        this.radius = radius;
        this.glow = glow;
        this.glowColor = glowColor;
        this.tint = tint;
    }

    // Without a glow the glass is drawn straight onto the canvas each frame: on this box a cached bitmap
    // under changing text left light grey patches, so the cards use no bitmaps at all.
    private final Paint tintP = new Paint(Paint.ANTI_ALIAS_FLAG), frostP = new Paint(Paint.ANTI_ALIAS_FLAG),
            sheenP = new Paint(Paint.ANTI_ALIAS_FLAG), edgeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(), edgeBox = new RectF();

    private void prepareDirect(Rect b) {
        int w = b.width(), h = b.height();
        box.set(b);
        edgeBox.set(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1);
        tintP.setColor(tint);
        frostP.setShader(new LinearGradient(0, b.top, 0, b.bottom, 0x29FFFFFF, 0x0DFFFFFF, Shader.TileMode.CLAMP));
        sheenP.setShader(new LinearGradient(0, b.top, 0, b.top + Math.min(h * 0.5f, 140), 0x1CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        edgeP.setStyle(Paint.Style.STROKE);
        edgeP.setStrokeWidth(2f);
        edgeP.setShader(new LinearGradient(0, b.top, 0, b.bottom, new int[]{0xB3FFFFFF, 0x26FFFFFF, 0x40FFFFFF}, new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onBoundsChange(Rect b) {
        if (b.width() <= 0 || b.height() <= 0) return;
        if (glow <= 0) { cache = null; prepareDirect(b); return; }
        if (cache != null && cache.getWidth() == b.width() + 2 * (int) glow && cache.getHeight() == b.height() + 2 * (int) glow) return;
        int g = (int) glow, w = b.width(), h = b.height();
        cache = Bitmap.createBitmap(w + 2 * g, h + 2 * g, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(cache);
        RectF r = new RectF(g, g, g + w, g + h);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (g > 0 && glowColor != 0) {
            p.setColor(glowColor);
            p.setMaskFilter(new BlurMaskFilter(g * 0.7f, BlurMaskFilter.Blur.OUTER));
            c.drawRoundRect(r, radius, radius, p);
            p.setMaskFilter(null);
        }
        p.setColor(tint);
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(new LinearGradient(0, g, 0, g + h, 0x29FFFFFF, 0x0DFFFFFF, Shader.TileMode.CLAMP));
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
        if (glow <= 0) {
            if (box.isEmpty()) return;
            canvas.drawRoundRect(box, radius, radius, tintP);
            canvas.drawRoundRect(box, radius, radius, frostP);
            canvas.drawRoundRect(box, radius, radius, sheenP);
            canvas.drawRoundRect(edgeBox, radius - 1, radius - 1, edgeP);
            return;
        }
        if (cache == null) return;
        Rect b = getBounds();
        canvas.drawBitmap(cache, b.left - (int) glow, b.top - (int) glow, blit);
    }

    @Override public void setAlpha(int alpha) {
        blit.setAlpha(alpha);
        for (Paint p : new Paint[]{tintP, frostP, sheenP, edgeP}) p.setAlpha(alpha);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter cf) { blit.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
