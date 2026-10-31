package com.family.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * The rich background of a centred panel, drawn only inside its bounds: deep translucent night blue,
 * a warm gold glow rising from the top, a gold hairline along the top edge, and a light-catching border.
 */
final class PanelBg extends Drawable {
    private final float r;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), warm = new Paint(Paint.ANTI_ALIAS_FLAG),
            hair = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG), sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    PanelBg(float radius) {
        r = radius;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(2);
        hair.setStyle(Paint.Style.STROKE);
        hair.setStrokeWidth(2.5f);
    }

    @Override protected void onBoundsChange(Rect b) {
        box.set(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1);
        float w = b.width(), h = b.height(), cx = b.exactCenterX();
        fill.setShader(new LinearGradient(0, b.top, 0, b.bottom, new int[]{0xE61B2140, 0xE0121731, 0xEB0A0D1C}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP));
        warm.setShader(new RadialGradient(cx, b.top - h * 0.15f, Math.max(1, Math.max(w, h) * 0.75f), 0x33F1D58A, 0x00F1D58A, Shader.TileMode.CLAMP));
        sheen.setShader(new LinearGradient(0, b.top, 0, b.top + Math.min(h * 0.35f, 220), 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        edge.setShader(new LinearGradient(0, b.top, 0, b.bottom, new int[]{0x8CFFFFFF, 0x1FFFFFFF, 0x33FFFFFF}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP));
        hair.setShader(new LinearGradient(b.left + r, 0, b.right - r, 0, new int[]{0x00F1D58A, 0xE6F1D58A, 0x00F1D58A}, null, Shader.TileMode.CLAMP));
    }

    @Override public void draw(Canvas c) {
        c.drawRoundRect(box, r, r, fill);
        c.drawRoundRect(box, r, r, warm);
        c.drawRoundRect(box, r, r, sheen);
        c.drawRoundRect(box, r, r, edge);
        Rect b = getBounds();
        c.drawLine(b.left + r, b.top + 1.5f, b.right - r, b.top + 1.5f, hair);
    }

    @Override public void getOutline(Outline o) { o.setRoundRect(getBounds(), r); }
    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
