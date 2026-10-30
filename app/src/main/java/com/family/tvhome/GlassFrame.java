package com.family.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A glass card built from plain layers that all draw inside their own bounds:
 * a soft glow, the blurred wallpaper (optional), the frost, then the content.
 * The frame is bigger than the card by the glow's reach on every side, so place it that much further out.
 */
final class GlassFrame extends FrameLayout {
    final int glow;

    GlassFrame(Context c, float radius, int glow, int glowColor, int tint, boolean frosted) {
        super(c);
        this.glow = glow;
        setClipChildren(false);
        setClipToPadding(false);
        if (glow > 0 && glowColor != 0) {
            View g = new View(c);
            g.setBackground(new Glow(radius, glow, glowColor));
            addView(g, new LayoutParams(-1, -1));
        }
        if (frosted) {
            BackdropView b = new BackdropView(c, radius);
            LayoutParams p = new LayoutParams(-1, -1);
            p.setMargins(glow, glow, glow, glow);
            addView(b, p);
        }
        View frost = new View(c);
        frost.setBackground(new Glass(radius, 0, 0, tint));
        LayoutParams p = new LayoutParams(-1, -1);
        p.setMargins(glow, glow, glow, glow);
        addView(frost, p);
    }

    /** Adds the card's content, inset by the glow. Call once, after the constructor. */
    void setContent(View content, boolean fillWidth, boolean fillHeight) {
        LayoutParams p = new LayoutParams(fillWidth ? -1 : -2, fillHeight ? -1 : -2);
        p.setMargins(glow, glow, glow, glow);
        addView(content, p);
    }

    /** The frame is exactly its content plus the glow; the glass layers then fill that size. */
    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        View content = getChildAt(getChildCount() - 1);
        measureChildWithMargins(content, wSpec, 0, hSpec, 0);
        int w = MeasureSpec.getMode(wSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(wSpec) : content.getMeasuredWidth() + 2 * glow;
        int h = MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(hSpec) : content.getMeasuredHeight() + 2 * glow;
        setMeasuredDimension(w, h);
        for (int i = 0; i < getChildCount() - 1; i++) {
            View c = getChildAt(i);
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            c.measure(MeasureSpec.makeMeasureSpec(Math.max(0, w - lp.leftMargin - lp.rightMargin), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.max(0, h - lp.topMargin - lp.bottomMargin), MeasureSpec.EXACTLY));
        }
        if (MeasureSpec.getMode(wSpec) == MeasureSpec.EXACTLY || MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) {
            LayoutParams lp = (LayoutParams) content.getLayoutParams();
            content.measure(MeasureSpec.makeMeasureSpec(lp.width == -1 ? w - 2 * glow : content.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(lp.height == -1 ? h - 2 * glow : content.getMeasuredHeight(), MeasureSpec.EXACTLY));
        }
    }

    // ------------------------------------------------------------ glow

    /** A blurred rounded rectangle, inset by the glow's reach, painted once per size. */
    static final class Glow extends Drawable {
        private final float radius;
        private final int reach, color;
        private Bitmap cache;

        Glow(float radius, int reach, int color) { this.radius = radius; this.reach = reach; this.color = color; }

        @Override protected void onBoundsChange(Rect b) {
            if (b.width() <= 2 * reach || b.height() <= 2 * reach) { cache = null; return; }
            if (cache != null && cache.getWidth() == b.width() && cache.getHeight() == b.height()) return;
            cache = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            p.setMaskFilter(new BlurMaskFilter(reach * 0.7f, BlurMaskFilter.Blur.OUTER));
            new Canvas(cache).drawRoundRect(new RectF(reach, reach, b.width() - reach, b.height() - reach), radius, radius, p);
        }

        @Override public void draw(Canvas canvas) { if (cache != null) canvas.drawBitmap(cache, getBounds().left, getBounds().top, null); }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    // ------------------------------------------------------------ blurred wallpaper

    private static Bitmap backdrop;
    private static float screenW = 1920, screenH = 1080, stageScale = 1;
    private static final List<WeakReference<View>> VIEWS = new ArrayList<>();

    static void setScreen(float w, float h, float scale) { screenW = w; screenH = h; stageScale = scale; }

    /** A small blurred copy of the whole screen's video, or null for none. */
    static void setBackdrop(Bitmap b) {
        backdrop = b;
        for (Iterator<WeakReference<View>> it = VIEWS.iterator(); it.hasNext(); ) {
            View v = it.next().get();
            if (v == null) it.remove(); else v.invalidate();
        }
    }

    /** Shows the part of the blurred wallpaper that sits right behind it, clipped to the card's rounded shape. */
    static final class BackdropView extends View {
        private final int[] loc = new int[2];
        private final RectF dst = new RectF();
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        BackdropView(Context c, float radius) {
            super(c);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
            });
            setClipToOutline(true);
            VIEWS.add(new WeakReference<>(this));
        }

        @Override protected void onDraw(Canvas canvas) {
            Bitmap b = backdrop;
            if (b == null) return;
            getLocationInWindow(loc);
            float k = stageScale;
            dst.set(-loc[0] / k, -loc[1] / k, (screenW - loc[0]) / k, (screenH - loc[1]) / k);
            canvas.drawBitmap(b, null, dst, paint);
        }
    }
}
