package com.family.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Player symbols drawn as white shapes, centred in the button: the emoji versions came out as orange squares on the box. */
public class PlayerIcon extends Drawable {
    public static final int PREV = 0, PLAY = 1, PAUSE = 2, NEXT = 3;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private int kind;

    public PlayerIcon(int kind) { this.kind = kind; p.setColor(0xFFFFFFFF); }

    public void setKind(int k) { if (k != kind) { kind = k; invalidateSelf(); } }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY(), s = 15f; // half height of the symbol
        path.reset();
        switch (kind) {
            case PLAY:
                path.moveTo(cx - s * 0.75f, cy - s);
                path.lineTo(cx + s * 1.05f, cy);
                path.lineTo(cx - s * 0.75f, cy + s);
                path.close();
                break;
            case PAUSE:
                r.set(cx - s * 0.8f, cy - s, cx - s * 0.25f, cy + s);
                c.drawRoundRect(r, 3, 3, p);
                r.set(cx + s * 0.25f, cy - s, cx + s * 0.8f, cy + s);
                c.drawRoundRect(r, 3, 3, p);
                return;
            case PREV:
            case NEXT:
                float d = kind == NEXT ? 1f : -1f, h = s * 0.8f;
                for (int i = 0; i < 2; i++) {
                    float x0 = cx - d * h * (1.15f - i * 1.0f);
                    path.moveTo(x0, cy - h);
                    path.lineTo(x0 + d * h, cy);
                    path.lineTo(x0, cy + h);
                    path.close();
                }
                float bx = cx + d * h * 0.95f;
                r.set(Math.min(bx, bx + d * 4), cy - h, Math.max(bx, bx + d * 4), cy + h);
                c.drawRect(r, p);
                break;
        }
        c.drawPath(path, p);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
