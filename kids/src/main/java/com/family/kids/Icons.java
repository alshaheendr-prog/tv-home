package com.family.kids;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** App icons drawn edge to edge in a rounded square, the same way as on the family home's row. */
final class Icons {
    private Icons() { }

    static Drawable fullBleed(Resources res, Drawable icon, float cornerFraction) {
        int size = 192;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) icon;
            int pad = size / 4; // the visible part of an adaptive icon is the middle two thirds
            for (Drawable layer : new Drawable[]{a.getBackground(), a.getForeground()}) {
                if (layer == null) continue;
                layer.setBounds(-pad, -pad, size + pad, size + pad);
                layer.draw(c);
            }
        } else {
            // Older icons carry empty margins: cut to the visible part and stretch it over the whole tile.
            int big = size * 2;
            Bitmap raw = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, big, big);
            icon.draw(new Canvas(raw));
            int l = big, t = big, r = -1, btm = -1;
            for (int y = 0; y < big; y += 2) for (int x = 0; x < big; x += 2) {
                if (Color.alpha(raw.getPixel(x, y)) > 40) { l = Math.min(l, x); r = Math.max(r, x); t = Math.min(t, y); btm = Math.max(btm, y); }
            }
            if (r < 0) { l = 0; t = 0; r = big - 1; btm = big - 1; }
            int side = Math.max(r - l, btm - t) + 2, cx = (l + r) / 2, cy = (t + btm) / 2;
            c.drawColor(edgeColor(raw, cx, cy, side / 2)); // round icons leave corners empty: fill them with the icon's edge colour
            int inset = Math.round(side * 0.04f);
            Rect src = new Rect(cx - side / 2 + inset, cy - side / 2 + inset, cx + side / 2 - inset, cy + side / 2 - inset);
            c.drawBitmap(raw, src, new Rect(0, 0, size, size), new Paint(Paint.FILTER_BITMAP_FLAG));
            raw.recycle();
        }
        // corners are rounded once here, so the row never clips each icon every frame
        Bitmap round = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        rp.setShader(new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float rr = size * cornerFraction;
        new Canvas(round).drawRoundRect(new RectF(0, 0, size, size), rr, rr, rp);
        b.recycle();
        return new BitmapDrawable(res, round);
    }

    private static int edgeColor(Bitmap raw, int cx, int cy, int half) {
        long rs = 0, gs = 0, bs = 0, n = 0;
        int in = Math.max(4, half / 6);
        int[][] pts = {{cx, cy - half + in}, {cx, cy + half - in}, {cx - half + in, cy}, {cx + half - in, cy}};
        for (int[] p : pts) {
            int x = Math.max(0, Math.min(raw.getWidth() - 1, p[0])), y = Math.max(0, Math.min(raw.getHeight() - 1, p[1]));
            int px = raw.getPixel(x, y);
            if (Color.alpha(px) < 200) continue;
            rs += Color.red(px); gs += Color.green(px); bs += Color.blue(px); n++;
        }
        return n == 0 ? 0xFF20242E : Color.rgb((int) (rs / n), (int) (gs / n), (int) (bs / n));
    }
}
