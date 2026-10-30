package com.family.tvhome;

import android.graphics.Bitmap;

/** A quick box blur (three passes, which looks Gaussian) for the small wallpaper copy behind the glass. */
final class Blur {
    private Blur() { }

    static Bitmap blur(Bitmap src, int radius) {
        int w = src.getWidth(), h = src.getHeight();
        int[] a = new int[w * h], b = new int[w * h];
        src.getPixels(a, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            box(a, b, w, h, radius, true);
            box(b, a, w, h, radius, false);
        }
        // a touch darker and calmer, so white text on the glass stays readable
        for (int i = 0; i < a.length; i++) {
            int c = a[i];
            int r = (c >> 16 & 255) * 82 / 100, g = (c >> 8 & 255) * 82 / 100, bl = (c & 255) * 82 / 100;
            a[i] = 0xFF000000 | r << 16 | g << 8 | bl;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(a, 0, w, 0, 0, w, h);
        return out;
    }

    private static void box(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h, lines = horizontal ? h : w;
        int div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[idx(line, clamp(i, len), w, horizontal)];
                sr += c >> 16 & 255; sg += c >> 8 & 255; sb += c & 255;
            }
            for (int i = 0; i < len; i++) {
                out[idx(line, i, w, horizontal)] = 0xFF000000 | (sr / div) << 16 | (sg / div) << 8 | (sb / div);
                int add = in[idx(line, clamp(i + r + 1, len), w, horizontal)];
                int sub = in[idx(line, clamp(i - r, len), w, horizontal)];
                sr += (add >> 16 & 255) - (sub >> 16 & 255);
                sg += (add >> 8 & 255) - (sub >> 8 & 255);
                sb += (add & 255) - (sub & 255);
            }
        }
    }

    private static int clamp(int i, int len) { return i < 0 ? 0 : i >= len ? len - 1 : i; }

    private static int idx(int line, int i, int w, boolean horizontal) { return horizontal ? line * w + i : i * w + line; }
}
