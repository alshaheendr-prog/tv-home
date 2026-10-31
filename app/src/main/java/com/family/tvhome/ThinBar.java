package com.family.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** A hairline progress bar: a faint track with a softly glowing fill. Used under countdowns inside the glass cards. */
public class ThinBar extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;

    public ThinBar(Context c) {
        super(c);
        track.setColor(0x1FFFFFFF);
        setColor(0x8CFFFFFF);
    }

    public void setColor(int color) {
        fill.setColor(color);
        fill.setShadowLayer(6, 0, 0, color & 0x66FFFFFF);
        invalidate();
    }

    public void setProgress(float p) {
        p = Math.max(0, Math.min(1, p));
        if (Math.abs(p - progress) < 0.0005f) return;
        progress = p;
        invalidate();
    }

    /** No width of its own: it takes whatever its row gives it, and never stretches a row that wraps its content. */
    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getMode(w) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(w) : 0,
                MeasureSpec.getMode(h) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(h) : 3);
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), r = h / 2f, w = getWidth();
        c.drawRoundRect(0, 0, w, h, r, r, track);
        if (progress > 0) c.drawRoundRect(0, 0, Math.max(h, w * progress), h, r, r, fill);
    }
}
