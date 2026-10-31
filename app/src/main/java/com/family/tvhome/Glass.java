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
        Accent.register(this);
    }

    // Without a glow the glass is drawn straight onto the canvas each frame: on this box a cached bitmap
    // under changing text left light grey patches, so the cards use no bitmaps at all.
    private final Paint tintP = new Paint(Paint.ANTI_ALIAS_FLAG), frostP = new Paint(Paint.ANTI_ALIAS_FLAG),
            sheenP = new Paint(Paint.ANTI_ALIAS_FLAG), edgeP = new Paint(Paint.ANTI_ALIAS_FLAG),
            accentP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;
    private final RectF box = new RectF(), edgeBox = new RectF();

    private int bodyAccent = 1; // the accent the body shader was built for (1 = not built yet)

    private void prepareDirect(Rect b) {
        box.set(b);
        edgeBox.set(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1);
        bodyAccent = 1;
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
            // tint, the wallpaper's colour wash, the frost and the sheen are all vertical, so they are
            // blended here into one gradient: the glass costs one fill per frame instead of four
            int acc = Accent.strength > 0.01f ? (Math.round(0x24 * Accent.strength) << 24) | (Accent.color & 0xFFFFFF) : 0;
            if (acc != bodyAccent) { bodyAccent = acc; buildBody(acc); }
            canvas.drawRoundRect(box, radius, radius, tintP);
            canvas.drawRoundRect(edgeBox, radius - 1, radius - 1, edgeP);
            return;
        }
        if (cache == null) return;
        Rect b = getBounds();
        canvas.drawBitmap(cache, b.left - (int) glow, b.top - (int) glow, blit);
    }

    private void buildBody(int accent) {
        float h = box.height(), sheen = Math.min(h * 0.5f, 140) / h;
        float[] pos = {0, sheen / 2, sheen, (sheen + 1) / 2, 1};
        int[] cols = new int[pos.length];
        for (int i = 0; i < pos.length; i++) {
            float t = pos[i];
            float[] c = argb(tint);
            if (accent != 0) c = over(c, argb(accent));
            c = over(c, new float[]{(0x29 + (0x0D - 0x29) * t) / 255f, 1, 1, 1});                      // frost
            c = over(c, new float[]{t < sheen ? 0x1C / 255f * (1 - t / sheen) : 0, 1, 1, 1});            // sheen
            cols[i] = (Math.round(c[0] * 255) << 24) | (Math.round(c[1] * 255) << 16) | (Math.round(c[2] * 255) << 8) | Math.round(c[3] * 255);
        }
        tintP.setShader(new LinearGradient(0, box.top, 0, box.bottom, cols, pos, Shader.TileMode.CLAMP));
    }

    private static float[] argb(int c) {
        return new float[]{(c >>> 24) / 255f, (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f};
    }

    /** src painted over dst, both {a, r, g, b} with straight (not premultiplied) colour. */
    private static float[] over(float[] d, float[] s) {
        float a = s[0] + d[0] * (1 - s[0]);
        if (a <= 0) return new float[]{0, 0, 0, 0};
        float[] o = {a, 0, 0, 0};
        for (int k = 1; k < 4; k++) o[k] = (s[k] * s[0] + d[k] * d[0] * (1 - s[0])) / a;
        return o;
    }

    @Override public void setAlpha(int alpha) {
        this.alpha = alpha;
        blit.setAlpha(alpha);
        for (Paint p : new Paint[]{tintP, frostP, sheenP, edgeP}) p.setAlpha(alpha);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter cf) { blit.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
