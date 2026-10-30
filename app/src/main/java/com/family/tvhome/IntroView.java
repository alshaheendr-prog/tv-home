package com.family.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.Random;

/**
 * The start-up intro: ten short animated scenes, one picked at random each time the box turns on.
 * Everything is drawn on a 1920x1080 canvas with plain shapes and gradients (no blur filters), so it stays smooth on the box.
 * Each scene lasts about five and a half seconds and ends on the gold Bismillah.
 */
public class IntroView extends View {
    public static final int COUNT = 10;
    public static final long LENGTH = 5600;
    public static final String[] NAMES = {
            "Golden Bismillah", "Star pattern", "Crescent rising", "Arches opening", "Alshaheen",
            "Ghanim and Noha", "Kuwait at dawn", "Glass cards", "Gold particles", "Night to sunrise"};

    static final int W = 1920, H = 1080, CX = 960, CY = 540;
    static final int GOLD = 0xFFF1D58A, GOLD_DEEP = 0xFFB8893A, NIGHT = 0xFF070B16, NAVY = 0xFF0E1630;
    static final String BISM = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";
    static final String WELCOME = "WELCOME HOME  ·  أهلاً بكم";

    private final int mode;
    private final long start = System.currentTimeMillis();
    private final Random rnd = new Random(7);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), title = new Paint(Paint.ANTI_ALIAS_FLAG),
            kicker = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] sx = new float[220], sy = new float[220], sp = new float[220], ss = new float[220];
    private Path star, skyline, crescent;
    private float[] partFrom, partTo;
    private float titleW;

    public IntroView(Context c, int mode, Typeface quran, Typeface latin) {
        super(c);
        this.mode = mode;
        title.setTypeface(quran);
        title.setTextSize(96);
        title.setTextAlign(Paint.Align.CENTER);
        titleW = title.measureText(BISM);
        title.setShader(new LinearGradient(CX - titleW / 2, 0, CX + titleW / 2, 0,
                new int[]{GOLD_DEEP, GOLD, 0xFFFFF3D0, GOLD, GOLD_DEEP}, null, Shader.TileMode.CLAMP));
        title.setShadowLayer(36, 0, 0, 0x80F1D58A);
        kicker.setTypeface(latin);
        kicker.setTextSize(22);
        kicker.setLetterSpacing(0.22f);
        kicker.setTextAlign(Paint.Align.CENTER);
        kicker.setColor(0xB3FFFFFF);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        for (int i = 0; i < sx.length; i++) {
            sx[i] = rnd.nextFloat() * W; sy[i] = rnd.nextFloat() * H;
            sp[i] = rnd.nextFloat() * 6.28f; ss[i] = 1.2f + rnd.nextFloat() * 2.4f;
        }
        build();
    }

    // ------------------------------------------------------------------ timing helpers

    private static float clamp(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    /** Progress of a phase that runs from a to b (in parts of the whole intro). */
    private static float seg(float t, float a, float b) { return clamp((t - a) / (b - a)); }
    private static float easeOut(float v) { return 1 - (1 - v) * (1 - v) * (1 - v); }
    private static float easeInOut(float v) { return v < 0.5f ? 4 * v * v * v : 1 - (float) Math.pow(-2 * v + 2, 3) / 2; }
    private static int alpha(int color, float a) { return (Math.round(Color.alpha(color) * clamp(a)) << 24) | (color & 0xFFFFFF); }
    private static int mix(int a, int b, float t) {
        t = clamp(t);
        return Color.argb(Math.round(Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t),
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    // ------------------------------------------------------------------ shapes built once

    /** An eight-pointed star (two overlapping squares), the Rub el Hizb. */
    private static Path eightStar(float cx, float cy, float r, float turn) {
        Path s = new Path();
        for (int sq = 0; sq < 2; sq++) {
            for (int i = 0; i <= 4; i++) {
                double a = turn + sq * Math.PI / 4 + i * Math.PI / 2;
                float x = cx + (float) (r * Math.cos(a)), y = cy + (float) (r * Math.sin(a));
                if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
            }
        }
        return s;
    }

    private void build() {
        switch (mode) {
            case 1:
                star = new Path();
                for (int k = 0; k < 4; k++) star.addPath(eightStar(CX, CY, 150 + k * 95, k * 0.2f));
                star.addCircle(CX, CY, 560, Path.Direction.CW);
                star.addCircle(CX, CY, 120, Path.Direction.CW);
                break;
            case 2: {
                Path outer = new Path(); outer.addCircle(0, 0, 120, Path.Direction.CW);
                Path bite = new Path(); bite.addCircle(48, -26, 104, Path.Direction.CW);
                crescent = new Path();
                crescent.op(outer, bite, Path.Op.DIFFERENCE);
                break;
            }
            case 6: {
                // An original silhouette of a Gulf city skyline: two sphere towers on the sea, a tall spire and towers.
                skyline = new Path();
                float base = 820;
                skyline.addRect(0, base - 40, W, base + 2, Path.Direction.CW);
                float[][] blocks = {{120, 120, 150}, {290, 70, 210}, {390, 90, 120}, {1180, 80, 260}, {1290, 60, 180},
                        {1380, 110, 320}, {1520, 70, 200}, {1620, 90, 150}, {1740, 60, 230}, {1830, 90, 120}};
                for (float[] b : blocks) skyline.addRect(b[0], base - b[2], b[0] + b[1], base, Path.Direction.CW);
                // the tall spire tower
                skyline.addRect(1450, base - 470, 1474, base - 300, Path.Direction.CW);
                skyline.addRect(1458, base - 560, 1466, base - 470, Path.Direction.CW);
                skyline.addCircle(1462, base - 455, 26, Path.Direction.CW);
                // the sphere towers
                skyline.addRect(706, base - 560, 718, base, Path.Direction.CW);
                skyline.addCircle(712, base - 380, 70, Path.Direction.CW);
                skyline.addCircle(712, base - 500, 38, Path.Direction.CW);
                skyline.addRect(840, base - 420, 852, base, Path.Direction.CW);
                skyline.addCircle(846, base - 290, 56, Path.Direction.CW);
                skyline.addRect(930, base - 200, 938, base, Path.Direction.CW);
                break;
            }
            case 8: buildParticles(); break;
            default: break;
        }
    }

    /** Points sampled from the Bismillah itself, for the particles to fly into. */
    private void buildParticles() {
        int bw = 960, bh = 200;
        Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(b);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(title.getTypeface());
        tp.setTextSize(48);
        tp.setTextAlign(Paint.Align.CENTER);
        c.drawText(BISM, bw / 2f, bh / 2f + 16, tp);
        int n = 0;
        float[] to = new float[2 * 4000];
        for (int y = 0; y < bh; y += 2) for (int x = 0; x < bw; x += 2) {
            if (n >= 4000) break;
            if (((b.getPixel(x, y) >>> 24) & 0xFF) > 120) { to[2 * n] = CX + (x - bw / 2f) * 2; to[2 * n + 1] = CY + (y - bh / 2f) * 2; n++; }
        }
        b.recycle();
        partTo = new float[2 * n];
        System.arraycopy(to, 0, partTo, 0, 2 * n);
        partFrom = new float[2 * n];
        for (int i = 0; i < n; i++) {
            double a = rnd.nextFloat() * Math.PI * 2, r = 700 + rnd.nextFloat() * 600;
            partFrom[2 * i] = CX + (float) (Math.cos(a) * r);
            partFrom[2 * i + 1] = CY + (float) (Math.sin(a) * r * 0.6);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void onDraw(Canvas c) {
        float t = clamp((System.currentTimeMillis() - start) / (float) LENGTH);
        float sec = (System.currentTimeMillis() - start) / 1000f;
        switch (mode) {
            case 0: sweep(c, t); break;
            case 1: starPattern(c, t, sec); break;
            case 2: crescent(c, t, sec); break;
            case 3: arches(c, t); break;
            case 4: family(c, t, sec); break;
            case 5: children(c, t, sec); break;
            case 6: dawn(c, t); break;
            case 7: glass(c, t); break;
            case 8: particles(c, t); break;
            default: sunrise(c, t, sec); break;
        }
        postInvalidateOnAnimation();
    }

    private void backdrop(Canvas c, int inner, int outer) {
        p.setShader(new RadialGradient(CX, CY, 1150, inner, outer, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
    }

    private void glow(Canvas c, float x, float y, float r, int color) {
        p.setShader(new RadialGradient(x, y, Math.max(1, r), color, color & 0xFFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, p);
        p.setShader(null);
    }

    /** The Bismillah and the welcome line, rising gently into place. */
    private void drawTitle(Canvas c, float a, float y) {
        if (a <= 0) return;
        float e = easeOut(a);
        title.setAlpha(Math.round(255 * e));
        c.drawText(BISM, CX, y + 24 * (1 - e), title);
        kicker.setAlpha(Math.round(180 * seg(a, 0.35f, 1)));
        c.drawText(WELCOME, CX, y + 90, kicker);
        // a hairline under the welcome line that draws out from the centre
        float half = 150 * easeOut(seg(a, 0.5f, 1));
        line.setStrokeWidth(2);
        line.setShader(null);
        line.setColor(alpha(GOLD, 0.55f * seg(a, 0.5f, 1)));
        if (half > 1) c.drawLine(CX - half, y + 122, CX + half, y + 122, line);
    }

    private void stars(Canvas c, float sec, float a, int count) {
        if (a <= 0) return;
        p.setColor(Color.WHITE);
        for (int i = 0; i < count; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(sec * 2.2f + sp[i]);
            p.setAlpha(Math.round(255 * a * (0.25f + 0.75f * tw)));
            c.drawCircle(sx[i], sy[i] * 0.75f, ss[i], p);
        }
    }

    // 1. Golden Bismillah: the words are revealed right to left by a moving band of light.
    private void sweep(Canvas c, float t) {
        backdrop(c, NAVY, NIGHT);
        float r = easeInOut(seg(t, 0.08f, 0.62f));
        float right = CX + titleW / 2 + 40, left = CX - titleW / 2 - 40;
        float edge = right - (right - left) * r;
        c.save();
        c.clipRect(edge, 0, W, H);
        title.setAlpha(255);
        c.drawText(BISM, CX, CY, title);
        c.restore();
        if (r > 0 && r < 1) {
            glow(c, edge, CY - 30, 220, 0x66F1D58A);
            glow(c, edge, CY - 30, 60, 0xCCFFF3D0);
            p.setColor(GOLD);
            for (int i = 0; i < 26; i++) {
                float dx = (sp[i] / 6.28f) * 160, dy = (ss[i] - 2.4f) * 70 + (float) Math.sin(i * 1.7f) * 40;
                p.setAlpha(Math.round(200 * (1 - dx / 160)));
                c.drawCircle(edge + dx, CY - 30 + dy, 2 + (i % 3), p);
            }
        }
        float k = seg(t, 0.6f, 0.9f);
        kicker.setAlpha(Math.round(180 * k));
        c.drawText(WELCOME, CX, CY + 90, kicker);
        line.setStrokeWidth(2);
        line.setColor(alpha(GOLD, 0.55f * k));
        float half = 150 * easeOut(k);
        if (half > 1) c.drawLine(CX - half, CY + 122, CX + half, CY + 122, line);
    }

    // 2. Star pattern: an eight-pointed star lattice draws itself line by line, then turns softly behind the words.
    private void starPattern(Canvas c, float t, float sec) {
        backdrop(c, 0xFF101A36, NIGHT);
        float d = easeInOut(seg(t, 0.02f, 0.6f));
        c.save();
        c.rotate(sec * 4, CX, CY);
        PathMeasure pm = new PathMeasure(star, false);
        Path part = new Path();
        do {
            float len = pm.getLength();
            pm.getSegment(0, len * d, part, true);
        } while (pm.nextContour());
        line.setShader(new LinearGradient(CX - 560, CY - 560, CX + 560, CY + 560, GOLD_DEEP, GOLD, Shader.TileMode.CLAMP));
        line.setStrokeWidth(2.5f);
        line.setAlpha(Math.round(255 * (0.85f - 0.55f * seg(t, 0.6f, 0.85f))));
        c.drawPath(part, line);
        line.setShader(null);
        c.restore();
        glow(c, CX, CY, 420 * easeOut(seg(t, 0.45f, 0.8f)), 0xCC070B16);
        drawTitle(c, seg(t, 0.55f, 0.9f), CY + 10);
    }

    // 3. Crescent rising: a crescent climbs through a twinkling sky and settles above the words.
    private void crescent(Canvas c, float t, float sec) {
        p.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xFF050914, 0xFF0D1836, 0xFF1F2A55}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        stars(c, sec, seg(t, 0, 0.4f), 160);
        float r = easeOut(seg(t, 0.05f, 0.6f));
        float y = 1000 - (1000 - 330) * r;
        glow(c, CX, y, 320, 0x55F1D58A);
        c.save();
        c.translate(CX, y);
        c.rotate(-20 + 20 * r);
        p.setShader(new LinearGradient(-120, -120, 120, 120, 0xFFFFF3D0, GOLD_DEEP, Shader.TileMode.CLAMP));
        c.drawPath(crescent, p);
        p.setShader(null);
        c.restore();
        drawTitle(c, seg(t, 0.55f, 0.88f), 640);
    }

    // 4. Arches opening: two carved doors under a pointed arch swing open onto warm light.
    private void arches(Canvas c, float t) {
        c.drawColor(NIGHT);
        float aw = 560, top = 150, bottom = 940, l = CX - aw / 2, r = CX + aw / 2;
        Path arch = new Path();
        arch.moveTo(l, bottom);
        arch.lineTo(l, 420);
        arch.quadTo(l, 230, CX, top);
        arch.quadTo(r, 230, r, 420);
        arch.lineTo(r, bottom);
        arch.close();
        float o = easeInOut(seg(t, 0.15f, 0.6f));
        // light behind the doors
        c.save();
        c.clipPath(arch);
        p.setShader(new RadialGradient(CX, 560, 700, new int[]{0xFFFFF0C8, 0xFFE0B25A, 0xFF3A2A12}, new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP));
        p.setAlpha(Math.round(255 * o));
        c.drawRect(l, top, r, bottom, p);
        p.setShader(null);
        p.setAlpha(255);
        // the two doors fold away to the sides
        float dw = aw / 2 * (1 - o);
        p.setShader(new LinearGradient(l, 0, CX, 0, 0xFF1C1408, 0xFF2E2210, Shader.TileMode.MIRROR));
        c.drawRect(l, top, l + dw, bottom, p);
        c.drawRect(r - dw, top, r, bottom, p);
        p.setShader(null);
        line.setStrokeWidth(2);
        line.setColor(alpha(GOLD, 0.5f * (1 - o)));
        for (int i = 1; i < 4; i++) {
            float yy = top + (bottom - top) * i / 4f;
            c.drawLine(l + 30, yy, l + dw - 30, yy, line);
            c.drawLine(r - dw + 30, yy, r - 30, yy, line);
        }
        c.restore();
        // the arch frame and two smaller side arches
        line.setStrokeWidth(4);
        line.setColor(alpha(GOLD, 0.8f));
        c.drawPath(arch, line);
        line.setStrokeWidth(2);
        line.setColor(alpha(GOLD, 0.35f));
        for (int s = -1; s <= 1; s += 2) {
            c.save();
            c.translate(s * 480, 140);
            c.scale(0.6f, 0.7f, CX, bottom);
            c.drawPath(arch, line);
            c.restore();
        }
        // the light spills out, then the words appear over it
        float spill = seg(t, 0.45f, 0.85f);
        glow(c, CX, 560, 300 + 900 * easeOut(spill), alpha(0xFFF1D58A, 0.35f * spill));
        p.setColor(alpha(NIGHT, 0.55f * spill));
        c.drawRect(0, 0, W, H, p);
        drawTitle(c, seg(t, 0.6f, 0.92f), CY + 10);
    }

    // 5. Alshaheen: a falcon (shaheen) glides in on a trail of gold and gives way to the family name.
    private void family(Canvas c, float t, float sec) {
        backdrop(c, 0xFF1A1630, NIGHT);
        stars(c, sec, 0.6f, 120);
        float f = easeInOut(seg(t, 0, 0.55f));
        // the flight path: a long, gentle curve from the left edge to the centre
        float x = -200 + (CX + 200) * f;
        float y = 760 - 320 * f + (float) Math.sin(f * Math.PI) * -120;
        float fade = 1 - seg(t, 0.5f, 0.68f);
        // the gold trail behind it
        p.setColor(GOLD);
        for (int i = 0; i < 40; i++) {
            float back = f - i * 0.012f;
            if (back < 0) break;
            float tx = -200 + (CX + 200) * back, ty = 760 - 320 * back + (float) Math.sin(back * Math.PI) * -120;
            p.setAlpha(Math.round(200 * fade * (1 - i / 40f)));
            c.drawCircle(tx, ty + (float) Math.sin(i * 0.9f + sec * 3) * 6, 3.5f * (1 - i / 50f), p);
        }
        if (fade > 0) {
            glow(c, x, y, 220, alpha(0xFFF1D58A, 0.35f * fade));
            c.save();
            c.translate(x, y);
            c.rotate(-12 + 12 * f);
            float flap = 0.55f + 0.45f * (float) Math.cos(sec * 7);
            p.setShader(new LinearGradient(-120, 0, 120, 0, 0xFFFFF3D0, GOLD_DEEP, Shader.TileMode.CLAMP));
            p.setAlpha(Math.round(255 * fade));
            c.drawPath(falcon(flap), p);
            p.setShader(null);
            c.restore();
        }
        float n = seg(t, 0.5f, 0.85f);
        if (n > 0) {
            float e = easeOut(n);
            glow(c, CX, CY - 40, 520, alpha(0xFFF1D58A, 0.25f * e));
            title.setTextSize(150);
            title.setAlpha(Math.round(255 * e));
            float sc = 0.92f + 0.08f * e;
            c.save();
            c.scale(sc, sc, CX, CY);
            c.drawText("آل شاهين", CX, CY + 10, title);
            c.restore();
            title.setTextSize(96);
            kicker.setAlpha(Math.round(190 * seg(t, 0.65f, 0.95f)));
            c.drawText("THE ALSHAHEEN FAMILY  ·  WELCOME HOME", CX, CY + 110, kicker);
            float half = 190 * easeOut(seg(t, 0.7f, 1));
            line.setStrokeWidth(2);
            line.setShader(null);
            line.setColor(alpha(GOLD, 0.55f));
            if (half > 1) c.drawLine(CX - half, CY + 142, CX + half, CY + 142, line);
        }
    }

    /** A simple falcon seen from below, wings open; flap squeezes the wings from 0 (closed) to 1 (open). */
    private static Path falcon(float flap) {
        Path b = new Path();
        float wy = -70 * flap;
        b.moveTo(0, -38);
        b.quadTo(14, -30, 16, -6);          // head and right shoulder
        b.quadTo(70, -10 + wy * 0.4f, 150, wy);  // right wing leading edge
        b.quadTo(100, 10, 34, 22);         // right wing trailing edge
        b.lineTo(28, 62);                  // tail, right
        b.lineTo(0, 54);
        b.lineTo(-28, 62);                 // tail, left
        b.lineTo(-34, 22);
        b.quadTo(-100, 10, -150, wy);      // left wing trailing edge
        b.quadTo(-70, -10 + wy * 0.4f, -16, -6);
        b.quadTo(-14, -30, 0, -38);
        b.close();
        return b;
    }

    // 6. Ghanim and Noha: two little lights, one gold and one rose, dance around each other and burst into their names.
    private void children(Canvas c, float t, float sec) {
        backdrop(c, 0xFF1C1A3E, 0xFF080A18);
        float d = easeInOut(seg(t, 0, 0.55f));
        float ang = sec * 2.6f, rad = 420 * (1 - d) + 40;
        float ax = CX + (float) Math.cos(ang) * rad * 1.4f, ay = CY - 40 + (float) Math.sin(ang) * rad * 0.6f;
        float bx = CX - (float) Math.cos(ang) * rad * 1.4f, by = CY - 40 - (float) Math.sin(ang) * rad * 0.6f;
        float lights = 1 - seg(t, 0.52f, 0.62f);
        if (lights > 0) {
            for (int i = 0; i < 18; i++) {
                float a2 = ang - i * 0.07f, r2 = rad + i * 2;
                float k = (1 - i / 18f) * lights;
                p.setColor(alpha(GOLD, 0.8f * k));
                c.drawCircle(CX + (float) Math.cos(a2) * r2 * 1.4f, CY - 40 + (float) Math.sin(a2) * r2 * 0.6f, 5 * k + 1, p);
                p.setColor(alpha(0xFFF7A8C4, 0.8f * k));
                c.drawCircle(CX - (float) Math.cos(a2) * r2 * 1.4f, CY - 40 - (float) Math.sin(a2) * r2 * 0.6f, 5 * k + 1, p);
            }
            glow(c, ax, ay, 120, alpha(0xFFF1D58A, 0.8f * lights));
            glow(c, bx, by, 120, alpha(0xFFF7A8C4, 0.8f * lights));
            p.setColor(alpha(Color.WHITE, lights));
            c.drawCircle(ax, ay, 12, p);
            c.drawCircle(bx, by, 12, p);
        }
        // the burst: sparkles fly out from the centre where the lights met
        float burst = seg(t, 0.55f, 0.95f);
        if (burst > 0 && burst < 1) {
            for (int i = 0; i < 60; i++) {
                double a = sp[i];
                float r = 80 + 700 * easeOut(burst) * (0.5f + ss[i] / 4.8f);
                p.setColor(alpha(i % 2 == 0 ? GOLD : 0xFFF7A8C4, 1 - burst));
                c.drawCircle(CX + (float) Math.cos(a) * r, CY - 40 + (float) Math.sin(a) * r * 0.7f, ss[i] + 1, p);
            }
        }
        float n = seg(t, 0.56f, 0.86f);
        if (n > 0) {
            float e = easeOut(n);
            glow(c, CX, CY - 40, 600, alpha(0xFFF1D58A, 0.2f * e));
            title.setTextSize(130);
            title.setAlpha(Math.round(255 * e));
            c.save();
            float sc = 0.85f + 0.15f * e;
            c.scale(sc, sc, CX, CY - 40);
            c.drawText("غانم و نهى", CX, CY, title);
            c.restore();
            title.setTextSize(96);
            kicker.setTextSize(30);
            kicker.setAlpha(Math.round(230 * seg(t, 0.68f, 0.95f)));
            c.drawText("GHANIM  &  NOHA", CX, CY + 96, kicker);
            kicker.setTextSize(22);
            kicker.setAlpha(Math.round(170 * seg(t, 0.75f, 1)));
            c.drawText("WELCOME HOME  ·  أهلاً بكم", CX, CY + 146, kicker);
        }
    }

    // 7. Kuwait at dawn: the city stands dark against the night, the sky warms and the sun lifts out of the sea.
    private void dawn(Canvas c, float t) {
        float d = easeInOut(seg(t, 0, 0.85f));
        p.setShader(new LinearGradient(0, 0, 0, 820, new int[]{mix(0xFF060A18, 0xFF2A3A6E, d), mix(0xFF0D1430, 0xFFC4705A, d), mix(0xFF141B3A, 0xFFF6C27A, d)},
                new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, 820, p);
        p.setShader(null);
        stars(c, 0, 1 - seg(t, 0, 0.5f), 120);
        float sunY = 900 - 260 * d;
        glow(c, 1100, sunY, 520, alpha(0xFFFFD58A, 0.6f * d));
        p.setColor(mix(0xFFE8803A, 0xFFFFE6B0, d));
        c.drawCircle(1100, sunY, 70, p);
        // the sea and the sun's path on it
        p.setShader(new LinearGradient(0, 820, 0, H, mix(0xFF0A1024, 0xFF6A4A4A, d), 0xFF05070E, Shader.TileMode.CLAMP));
        c.drawRect(0, 820, W, H, p);
        p.setShader(null);
        p.setColor(alpha(0xFFFFE0A0, 0.5f * d));
        for (int i = 0; i < 9; i++) {
            float yy = 840 + i * 26, w = 140 - i * 12;
            c.drawRect(1100 - w, yy, 1100 + w, yy + 4, p);
        }
        p.setColor(mix(0xFF03050C, 0xFF1A1420, d));
        c.drawPath(skyline, p);
        drawTitle(c, seg(t, 0.55f, 0.9f), 330);
    }

    // 8. Glass cards: the home screen's glass panels fly in, lock into place and light up the words.
    private void glass(Canvas c, float t) {
        backdrop(c, 0xFF1B2A4A, 0xFF0A0F1E);
        RectF[] rs = {new RectF(80, 56, 640, 346), new RectF(1280, 56, 1840, 346), new RectF(300, 700, 1620, 800), new RectF(80, 880, 1840, 1040)};
        float[][] from = {{-900, -300, -18}, {900, -300, 18}, {0, 600, 8}, {0, 500, -6}};
        for (int i = 0; i < rs.length; i++) {
            float g = easeOut(seg(t, 0.05f + i * 0.08f, 0.45f + i * 0.08f));
            if (g <= 0) continue;
            c.save();
            c.translate(from[i][0] * (1 - g), from[i][1] * (1 - g));
            c.rotate(from[i][2] * (1 - g), rs[i].centerX(), rs[i].centerY());
            float dim = 1 - 0.6f * seg(t, 0.6f, 0.85f);
            p.setShader(new LinearGradient(0, rs[i].top, 0, rs[i].bottom, alpha(0x33FFFFFF, g * dim), alpha(0x0DFFFFFF, g * dim), Shader.TileMode.CLAMP));
            c.drawRoundRect(rs[i], 34, 34, p);
            p.setShader(null);
            line.setStrokeWidth(2);
            line.setColor(alpha(0x66FFFFFF, g * dim));
            c.drawRoundRect(rs[i], 34, 34, line);
            c.restore();
        }
        float flash = seg(t, 0.5f, 0.62f) * (1 - seg(t, 0.62f, 0.8f));
        glow(c, CX, CY - 20, 700, alpha(0xFFFFF3D0, 0.25f * flash));
        drawTitle(c, seg(t, 0.55f, 0.88f), CY - 10);
    }

    // 9. Gold particles: a cloud of gold dust swirls in and settles into the Bismillah.
    private void particles(Canvas c, float t) {
        backdrop(c, 0xFF12182E, NIGHT);
        int n = partTo.length / 2;
        float g = easeInOut(seg(t, 0.05f, 0.7f));
        float[] pts = new float[partTo.length];
        for (int i = 0; i < n; i++) {
            float lag = (i % 17) / 17f * 0.25f;
            float k = easeOut(clamp((g - lag) / (1 - lag)));
            float swirl = (1 - k) * 1.4f;
            float fx = partFrom[2 * i] - CX, fy = partFrom[2 * i + 1] - CY;
            float rx = (float) (fx * Math.cos(swirl) - fy * Math.sin(swirl)) + CX;
            float ry = (float) (fx * Math.sin(swirl) + fy * Math.cos(swirl)) + CY;
            pts[2 * i] = rx + (partTo[2 * i] - rx) * k;
            pts[2 * i + 1] = ry + (partTo[2 * i + 1] - ry) * k;
        }
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(3.2f);
        p.setColor(alpha(GOLD, 1 - 0.8f * seg(t, 0.72f, 0.9f)));
        c.drawPoints(pts, p);
        float land = seg(t, 0.66f, 0.88f);
        glow(c, CX, CY, 600, alpha(0xFFF1D58A, 0.18f * land * (1 - seg(t, 0.88f, 1))));
        if (land > 0) {
            title.setAlpha(Math.round(255 * land));
            c.drawText(BISM, CX, CY + 34, title);
            kicker.setAlpha(Math.round(180 * seg(t, 0.78f, 1)));
            c.drawText(WELCOME, CX, CY + 124, kicker);
        }
    }

    // 10. Night to sunrise: the stars fade, the horizon glows and the sun rises with long soft rays.
    private void sunrise(Canvas c, float t, float sec) {
        float d = easeInOut(seg(t, 0.05f, 0.8f));
        p.setShader(new LinearGradient(0, 0, 0, H, new int[]{mix(0xFF03050C, 0xFF243A6A, d), mix(0xFF080D20, 0xFF9A6A6A, d), mix(0xFF0E1428, 0xFFF4B870, d)},
                new float[]{0, 0.65f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        stars(c, sec, 1 - seg(t, 0.1f, 0.6f), 220);
        float sunY = 1200 - 420 * d;
        c.save();
        c.rotate(sec * 3, CX, sunY);
        for (int i = 0; i < 12; i++) {
            c.save();
            c.rotate(i * 30, CX, sunY);
            Path ray = new Path();
            ray.moveTo(CX - 18, sunY);
            ray.lineTo(CX, sunY - 1300);
            ray.lineTo(CX + 18, sunY);
            ray.close();
            p.setShader(new LinearGradient(CX, sunY, CX, sunY - 1300, alpha(0xFFFFE6B0, 0.28f * d), 0x00FFE6B0, Shader.TileMode.CLAMP));
            c.drawPath(ray, p);
            p.setShader(null);
            c.restore();
        }
        c.restore();
        glow(c, CX, sunY, 600, alpha(0xFFFFD58A, 0.55f * d));
        p.setColor(mix(0xFFE07840, 0xFFFFF0C8, d));
        c.drawCircle(CX, sunY, 110, p);
        p.setColor(alpha(NIGHT, 0.35f * seg(t, 0.55f, 0.85f)));
        c.drawRect(0, 0, W, H, p);
        drawTitle(c, seg(t, 0.55f, 0.88f), 400);
    }
}
