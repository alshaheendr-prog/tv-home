package com.family.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The wallpaper's own colour, picked from a frame of the clip that is playing. The glass cards, their glow and
 * the dock glow lean a little towards it, so each video looks as if the screen were designed around it.
 * Drawables read it while they draw (no shaders are made per frame); a change glides over about a second and a half.
 */
final class Accent {
    private Accent() { }

    /** The current colour (opaque) and how much of it to use (0 = none, for grey or dark clips). */
    static int color = 0xFFF1D58A;
    static float strength;

    private static final List<WeakReference<Drawable>> USERS = new ArrayList<>();
    private static ValueAnimator anim;

    static void register(Drawable d) { USERS.add(new WeakReference<>(d)); }

    /** Glide to a new colour; strength 0 fades the tint away. Call on the UI thread. */
    static void glideTo(int target, float targetStrength, Runnable done) {
        if (anim != null) anim.cancel();
        final int fromC = strength <= 0.01f ? target : color;
        final float fromS = strength;
        final int toC = targetStrength <= 0.01f ? color : target;
        final ArgbEvaluator argb = new ArgbEvaluator();
        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(1600);
        anim.setInterpolator(new DecelerateInterpolator(1.5f));
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            color = (int) argb.evaluate(f, fromC, toC);
            strength = fromS + (targetStrength - fromS) * f;
            invalidateAll();
            if (f >= 1 && done != null) done.run();
        });
        anim.start();
    }

    private static void invalidateAll() {
        for (Iterator<WeakReference<Drawable>> it = USERS.iterator(); it.hasNext(); ) {
            Drawable d = it.next().get();
            if (d == null) it.remove(); else d.invalidateSelf();
        }
    }

    /** a mixed towards b by t (0..1), keeping a's alpha. */
    static int mix(int a, int b, float t) {
        if (t <= 0) return a;
        int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /**
     * The clip's leading colour from a small frame: the most common hue among the colourful pixels, then made
     * gentle (medium saturation, bright) so it tints rather than paints. Returns {color, strength*1000},
     * or null when the frame is black (the copy failed) or too grey to have a colour.
     */
    static int[] pick(Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        final int BINS = 24;
        float[] weight = new float[BINS], sr = new float[BINS], sg = new float[BINS], sb = new float[BINS];
        float[] hsv = new float[3];
        float total = 0, light = 0;
        for (int p : px) {
            Color.colorToHSV(p, hsv);
            light += hsv[2];
            if (hsv[2] < 0.12f || hsv[1] < 0.12f) continue;
            float k = hsv[1] * hsv[2];
            int bin = Math.min(BINS - 1, (int) (hsv[0] / 360f * BINS));
            weight[bin] += k; total += k;
            sr[bin] += Color.red(p) * k; sg[bin] += Color.green(p) * k; sb[bin] += Color.blue(p) * k;
        }
        if (light / px.length < 0.03f) return null; // an all-black copy: nothing to learn
        if (total / px.length < 0.04f) return new int[]{0, 0}; // a grey or monochrome clip: no tint
        int best = 0;
        for (int i = 1; i < BINS; i++) if (weight[i] + 0.5f * (weight[(i + 1) % BINS] + weight[(i + BINS - 1) % BINS])
                > weight[best] + 0.5f * (weight[(best + 1) % BINS] + weight[(best + BINS - 1) % BINS])) best = i;
        float r = 0, g = 0, bl = 0, wsum = 0;
        for (int d = -1; d <= 1; d++) {
            int i = (best + d + BINS) % BINS;
            r += sr[i]; g += sg[i]; bl += sb[i]; wsum += weight[i];
        }
        int c = Color.rgb(Math.round(r / wsum), Math.round(g / wsum), Math.round(bl / wsum));
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.max(0.35f, Math.min(0.7f, hsv[1]));
        hsv[2] = 0.95f;
        // colourful clips get the full tint, softly colourful ones a lighter touch
        float s = Math.min(1f, (total / px.length) / 0.15f);
        return new int[]{Color.HSVToColor(hsv), Math.round(s * 1000)};
    }
}
