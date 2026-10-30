package com.family.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/**
 * Pip, the little buddy who lives in each world. Drawn in code (no borrowed art):
 * a round body dressed for the world, eyes that follow whatever tile has focus, and a sleepy face at rest time.
 */
final class Mascot extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private Worlds.World world = Worlds.ALL[0];
    private float lookX, lookY, eyeX, eyeY;
    private boolean asleep;
    private long nextBlink = SystemClock.uptimeMillis() + 2500, blinkStart;
    private Typeface face;

    Mascot(Context c, Typeface face) {
        super(c);
        this.face = face;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    void setWorld(Worlds.World w) { world = w; invalidate(); }
    void setAsleep(boolean a) { asleep = a; invalidate(); }

    /** Where to look, from -1 (left/up) to 1 (right/down). */
    void lookAt(float x, float y) { lookX = Math.max(-1, Math.min(1, x)); lookY = Math.max(-1, Math.min(1, y)); invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = Math.min(w, h) / 300f;
        float cx = w / 2, cy = h * 0.56f, rad = 95 * s; // leaves room for hair, caps and the sleepy Z inside the view
        // ease the eyes toward the target so they glide rather than jump
        eyeX += (lookX - eyeX) * 0.18f;
        eyeY += (lookY - eyeY) * 0.18f;
        boolean moving = Math.abs(lookX - eyeX) > 0.01f || Math.abs(lookY - eyeY) > 0.01f;

        // soft shadow
        fill.setShader(null);
        fill.setColor(0x33000000);
        c.drawOval(cx - rad * 0.8f, h * 0.93f - 10 * s, cx + rad * 0.8f, h * 0.93f + 10 * s, fill);

        drawBackAccessory(c, cx, cy, rad, s);

        // body
        fill.setShader(new RadialGradient(cx - rad * 0.35f, cy - rad * 0.45f, rad * 1.5f,
                lighten(world.body), world.bodyDark, Shader.TileMode.CLAMP));
        if (world.id.equals("block")) {
            r.set(cx - rad, cy - rad, cx + rad, cy + rad);
            c.drawRoundRect(r, 14 * s, 14 * s, fill);
        } else {
            c.drawCircle(cx, cy, rad, fill);
        }
        fill.setShader(null);

        drawFrontAccessory(c, cx, cy, rad, s);

        // cheeks
        if (!world.id.equals("block")) {
            fill.setColor(0x55FF5A7A);
            c.drawOval(cx - rad * 0.78f, cy + rad * 0.12f, cx - rad * 0.42f, cy + rad * 0.32f, fill);
            c.drawOval(cx + rad * 0.42f, cy + rad * 0.12f, cx + rad * 0.78f, cy + rad * 0.32f, fill);
        }

        // eyes
        float ex = rad * 0.34f, ey = cy - rad * 0.12f, ew = rad * 0.2f, eh = rad * 0.3f;
        long now = SystemClock.uptimeMillis();
        if (now > nextBlink) { blinkStart = now; nextBlink = now + 2500 + (long) (Math.random() * 3500); }
        float blink = now - blinkStart < 150 ? 0.12f : 1f;
        if (asleep) {
            line.setColor(0xFF2A1A12);
            line.setStrokeWidth(7 * s);
            for (int i = -1; i <= 1; i += 2) {
                r.set(cx + i * ex - ew, ey - eh * 0.2f, cx + i * ex + ew, ey + eh * 0.6f);
                c.drawArc(r, 20, 140, false, line);
            }
            fill.setColor(0xFFFFFFFF);
            fill.setTypeface(face);
            fill.setTextSize(46 * s);
            c.drawText("z", cx + rad * 0.85f, cy - rad * 0.85f, fill);
            fill.setTextSize(64 * s);
            c.drawText("Z", cx + rad * 1.05f, cy - rad * 1.15f, fill);
        } else {
            for (int i = -1; i <= 1; i += 2) {
                float x = cx + i * ex;
                fill.setColor(0xFFFFFFFF);
                if (world.id.equals("block")) {
                    c.drawRect(x - ew, ey - eh * blink, x + ew, ey + eh * blink, fill);
                    fill.setColor(0xFF1B1B2A);
                    float px = x + eyeX * ew * 0.45f, py = ey + eyeY * eh * 0.45f * blink;
                    c.drawRect(px - ew * 0.5f, py - eh * 0.45f * blink, px + ew * 0.5f, py + eh * 0.45f * blink, fill);
                } else {
                    c.drawOval(x - ew, ey - eh * blink, x + ew, ey + eh * blink, fill);
                    fill.setColor(0xFF1B1B2A);
                    float px = x + eyeX * ew * 0.42f, py = ey + eyeY * eh * 0.4f * blink;
                    c.drawOval(px - ew * 0.58f, py - eh * 0.58f * blink, px + ew * 0.58f, py + eh * 0.58f * blink, fill);
                    fill.setColor(0xFFFFFFFF);
                    c.drawCircle(px - ew * 0.2f, py - eh * 0.25f * blink, ew * 0.2f, fill);
                }
            }
        }

        // mouth
        line.setColor(0xFF3A1A12);
        line.setStrokeWidth(6 * s);
        if (asleep) {
            fill.setColor(0xFF3A1A12);
            c.drawCircle(cx, cy + rad * 0.42f, 9 * s, fill);
        } else if (world.id.equals("block")) {
            fill.setColor(0xFF2A1A12);
            c.drawRect(cx - rad * 0.22f, cy + rad * 0.3f, cx + rad * 0.22f, cy + rad * 0.44f, fill);
        } else {
            r.set(cx - rad * 0.22f, cy + rad * 0.12f, cx + rad * 0.22f, cy + rad * 0.48f);
            c.drawArc(r, 20, 140, false, line);
        }

        // redraw while the eyes glide or blink; otherwise wait for the next blink
        if (moving || now - blinkStart < 150) postInvalidateDelayed(16);
        else if (!asleep) postInvalidateDelayed(Math.max(16, nextBlink - now));
    }

    private void drawBackAccessory(Canvas c, float cx, float cy, float rad, float s) {
        switch (world.id) {
            case "power": // spiky tufts of hair
                fill.setColor(0xFF1C1424);
                path.reset();
                path.moveTo(cx - rad * 0.9f, cy - rad * 0.3f);
                path.lineTo(cx - rad * 1.05f, cy - rad * 1.15f);
                path.lineTo(cx - rad * 0.45f, cy - rad * 0.8f);
                path.lineTo(cx - rad * 0.25f, cy - rad * 1.45f);
                path.lineTo(cx + rad * 0.1f, cy - rad * 0.9f);
                path.lineTo(cx + rad * 0.55f, cy - rad * 1.35f);
                path.lineTo(cx + rad * 0.55f, cy - rad * 0.75f);
                path.lineTo(cx + rad * 1.1f, cy - rad * 0.95f);
                path.lineTo(cx + rad * 0.9f, cy - rad * 0.3f);
                path.close();
                c.drawPath(path, fill);
                break;
            case "speed": // swept-back spikes
                fill.setColor(world.bodyDark);
                for (int i = 0; i < 3; i++) {
                    path.reset();
                    float y = cy - rad * 0.55f + i * rad * 0.5f;
                    path.moveTo(cx - rad * 0.4f, y - rad * 0.28f);
                    path.lineTo(cx - rad * 1.45f, y + rad * 0.05f);
                    path.lineTo(cx - rad * 0.4f, y + rad * 0.3f);
                    path.close();
                    c.drawPath(path, fill);
                }
                break;
            case "dream": // a star on a little stalk
                line.setColor(world.bodyDark);
                line.setStrokeWidth(7 * s);
                c.drawLine(cx, cy - rad * 0.95f, cx + rad * 0.15f, cy - rad * 1.35f, line);
                star(c, cx + rad * 0.17f, cy - rad * 1.45f, 26 * s, world.accent);
                break;
        }
    }

    private void drawFrontAccessory(Canvas c, float cx, float cy, float rad, float s) {
        switch (world.id) {
            case "mushroom": // a spotted cap
                fill.setShader(new LinearGradient(0, cy - rad * 1.2f, 0, cy - rad * 0.2f, 0xFFFF5A4E, 0xFFC92A20, Shader.TileMode.CLAMP));
                r.set(cx - rad * 1.12f, cy - rad * 1.25f, cx + rad * 1.12f, cy + rad * 0.25f);
                c.drawArc(r, 180, 180, true, fill);
                fill.setShader(null);
                fill.setColor(0xFFFFF6E8);
                c.drawCircle(cx, cy - rad * 0.85f, rad * 0.2f, fill);
                c.drawCircle(cx - rad * 0.65f, cy - rad * 0.55f, rad * 0.15f, fill);
                c.drawCircle(cx + rad * 0.65f, cy - rad * 0.55f, rad * 0.15f, fill);
                break;
            case "block": // grass on top of the block
                fill.setColor(0xFF7A5230);
                c.drawRect(cx - rad, cy - rad, cx + rad, cy - rad * 0.62f, fill);
                fill.setColor(0xFF6BCB4A);
                for (int i = 0; i < 8; i++) {
                    float x = cx - rad + i * rad / 4;
                    c.drawRect(x, cy - rad, x + rad / 4, cy - rad * (i % 2 == 0 ? 0.7f : 0.78f), fill);
                }
                break;
            case "power": // a glowing ball held up
                fill.setShader(new RadialGradient(cx + rad * 1.05f, cy + rad * 0.15f, rad * 0.35f, 0xFFFFE9A8, 0xFFFF8A00, Shader.TileMode.CLAMP));
                c.drawCircle(cx + rad * 1.0f, cy + rad * 0.3f, rad * 0.3f, fill);
                fill.setShader(null);
                star(c, cx + rad * 1.0f, cy + rad * 0.3f, 9 * s, 0xFFD9342B);
                break;
        }
    }

    private void star(Canvas c, float x, float y, float r, int color) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 5 * i - Math.PI / 2;
            float rr = i % 2 == 0 ? r : r * 0.45f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
        fill.setColor(color);
        c.drawPath(path, fill);
    }

    private static int lighten(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return 0xFF000000 | (Math.min(255, r + 60) << 16) | (Math.min(255, g + 60) << 8) | Math.min(255, b + 60);
    }
}
