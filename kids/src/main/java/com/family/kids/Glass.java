package com.family.kids;

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
 * The family home's smoky glass: a dark tint with a white frost and sheen blended into one gradient,
 * and an edge that is bright on top and fades down. Two fills per frame, no bitmaps, no offscreen layers.
 */
final class Glass extends Drawable {
    private final float radius;
    private final int tint;
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(), edgeBox = new RectF();

    Glass(float radius, int tint) {
        this.radius = radius;
        this.tint = tint;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(2f);
    }

    @Override
    protected void onBoundsChange(Rect b) {
        if (b.width() <= 0 || b.height() <= 0) return;
        box.set(b);
        edgeBox.set(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1);
        float h = b.height(), sheen = Math.min(h * 0.5f, 140) / h;
        float[] pos = {0, sheen / 2, sheen, (sheen + 1) / 2, 1};
        int[] cols = new int[pos.length];
        for (int i = 0; i < pos.length; i++) {
            float t = pos[i];
            float[] c = argb(tint);
            c = over(c, new float[]{(0x29 + (0x0D - 0x29) * t) / 255f, 1, 1, 1});            // frost
            c = over(c, new float[]{t < sheen ? 0x1C / 255f * (1 - t / sheen) : 0, 1, 1, 1});  // sheen
            cols[i] = (Math.round(c[0] * 255) << 24) | (Math.round(c[1] * 255) << 16) | (Math.round(c[2] * 255) << 8) | Math.round(c[3] * 255);
        }
        body.setShader(new LinearGradient(0, b.top, 0, b.bottom, cols, pos, Shader.TileMode.CLAMP));
        edge.setShader(new LinearGradient(0, b.top, 0, b.bottom, new int[]{0xB3FFFFFF, 0x26FFFFFF, 0x40FFFFFF}, new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        if (box.isEmpty()) return;
        canvas.drawRoundRect(box, radius, radius, body);
        canvas.drawRoundRect(edgeBox, radius - 1, radius - 1, edge);
    }

    private static float[] argb(int c) {
        return new float[]{(c >>> 24) / 255f, (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f};
    }

    /** src painted over dst, both {a, r, g, b} with straight colour. */
    private static float[] over(float[] d, float[] s) {
        float a = s[0] + d[0] * (1 - s[0]);
        if (a <= 0) return new float[]{0, 0, 0, 0};
        float[] o = {a, 0, 0, 0};
        for (int k = 1; k < 4; k++) o[k] = (s[k] * s[0] + d[k] * d[0] * (1 - s[0])) / a;
        return o;
    }

    @Override public void setAlpha(int alpha) { body.setAlpha(alpha); edge.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { body.setColorFilter(cf); edge.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
