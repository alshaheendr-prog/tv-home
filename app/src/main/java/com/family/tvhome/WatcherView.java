package com.family.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * The character in the middle of each world. It is drawn natively on top of the video background,
 * and its eyes follow whichever tile has focus. All coordinates are on the 1920x1080 stage.
 */
public class WatcherView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private final long start = System.nanoTime();
    private String theme = "power";
    private float targetX = 960, targetY = 800;
    private float lookX = 960, lookY = 800, par = 0;
    private boolean running = true;

    public WatcherView(Context c) {
        super(c);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setTheme(String id) { theme = id; invalidate(); }

    /** Where the eyes should look, in stage coordinates. */
    public void lookAt(float x, float y) { targetX = x; targetY = y; }

    public void setRunning(boolean r) { running = r; if (r) postInvalidateOnAnimation(); }

    @Override
    protected void onDraw(Canvas c) {
        float t = (System.nanoTime() - start) / 1e9f;
        lookX += (targetX - lookX) * 0.12f;
        lookY += (targetY - lookY) * 0.12f;
        par += (((lookX - 960) / 960f) - par) * 0.06f;
        float sx = getWidth() / 1920f, sy = getHeight() / 1080f;
        c.save();
        c.scale(sx, sy);
        switch (theme) {
            case "hills": hills(c, t); break;
            case "loop": totem(c, t); break;
            case "dream": puffball(c, t); break;
            case "blocks": block(c, t); break;
            default: orb(c, t);
        }
        c.restore();
        if (running) postInvalidateOnAnimation();
    }

    // ---- characters ----

    private void orb(Canvas c, float t) {
        float bx = 780 - par * 50, by = 380 + (float) Math.sin(t) * 10, r = 120;
        fill.setShader(null);
        fill.setColor(Color.argb(64, 255, 190, 60));
        c.drawCircle(bx, by, r * 1.5f, fill);
        fill.setShader(new RadialGradient(bx - 40, by - 50, r, new int[]{0xFFFFF6C8, 0xFFFFB21C, 0xFFB84A00}, new float[]{0, .4f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, r, fill);
        fill.setShader(null);
        float b = blink(t, 4.3f, 0);
        roundEye(c, bx - 38, by + 5, 26, 30, 13, b);
        roundEye(c, bx + 38, by + 5, 26, 30, 13, b);
    }

    private void hills(Canvas c, float t) {
        float fx = 780 - par * 60;
        c.save();
        c.clipRect(0, 0, 1920, 1000); // the brick ground in the video stays in front
        fill.setShader(null);
        fill.setColor(0xFF5CD14E);
        path.reset();
        path.moveTo(fx - 220, 1080);
        path.lineTo(fx - 220, 610);
        oval.set(fx - 220, 390, fx + 220, 830);
        path.arcTo(oval, 180, 180);
        path.lineTo(fx + 220, 1080);
        path.close();
        c.drawPath(path, fill);
        c.restore();
        float b = blink(t, 5, 1);
        tallEye(c, fx - 60, 500, 20, 46, b);
        tallEye(c, fx + 60, 500, 20, 46, b);
    }

    private void totem(Canvas c, float t) {
        float tx = 780 - par * 55;
        fill.setShader(null);
        int[] cols = {0xFFB0581C, 0xFF7D3CFF, 0xFFB0581C, 0xFF7D3CFF};
        for (int i = 0; i < 4; i++) {
            fill.setColor(cols[i]);
            c.drawRect(tx - 70, 420 + i * 110, tx + 70, 530 + i * 110, fill);
        }
        fill.setColor(0xFF1FB04A);
        path.reset();
        path.moveTo(tx - 130, 420); path.lineTo(tx - 70, 380); path.lineTo(tx - 70, 460); path.close();
        path.moveTo(tx + 130, 420); path.lineTo(tx + 70, 380); path.lineTo(tx + 70, 460); path.close();
        c.drawPath(path, fill);
        float b = blink(t, 3.7f, 2);
        roundEye(c, tx - 32, 470, 24, 24, 11, b);
        roundEye(c, tx + 32, 470, 24, 24, 11, b);
    }

    private void puffball(Canvas c, float t) {
        float px = 780 - par * 70, py = 400 + (float) Math.sin(t * 1.6) * 22, r = 140;
        fill.setShader(null);
        fill.setColor(0xFFE43A7A);
        ellipse(c, px - 90, py + 150, 70, 36, -17);
        ellipse(c, px + 90, py + 150, 70, 36, 17);
        fill.setColor(0xFFFFA6CF);
        float wave = (float) Math.sin(t * 3) * 12;
        ellipse(c, px - 150, py + 10 + wave, 48, 36, -34);
        ellipse(c, px + 150, py - 20 - wave, 48, 36, 34);
        fill.setShader(new RadialGradient(px - 50, py - 60, r, 0xFFFFD0E6, 0xFFFF8FC3, Shader.TileMode.CLAMP));
        c.drawCircle(px, py, r, fill);
        fill.setShader(null);
        fill.setColor(Color.argb(115, 255, 80, 140));
        ellipse(c, px - 80, py + 40, 26, 14, 0);
        ellipse(c, px + 80, py + 40, 26, 14, 0);
        float b = blink(t, 4.6f, .5f);
        tallEye(c, px - 34, py - 20, 16, 40, b);
        tallEye(c, px + 34, py - 20, 16, 40, b);
        stroke.setColor(0xFF9A1F4A);
        stroke.setStrokeWidth(5);
        oval.set(px - 16, py + 24, px + 16, py + 56);
        c.drawArc(oval, 11, 158, false, stroke);
    }

    private static final int[] BLOCK = new int[36];
    static {
        long seed = 5;
        for (int i = 0; i < 36; i++) {
            seed = (seed * 16807) % 2147483647;
            BLOCK[i] = (seed / 2147483647.0) < .5 ? 0xFF5B9A42 : 0xFF7CBC5C;
        }
    }

    private void block(Canvas c, float t) {
        float bx = 780 - par * 80, by = 460;
        fill.setShader(null);
        for (int i = 0; i < 36; i++) {
            fill.setColor(BLOCK[i]);
            float x = bx - 108 + (i % 6) * 36, y = by - 108 + (i / 6) * 36;
            c.drawRect(x, y, x + 36, y + 36, fill);
        }
        float b = blink(t, 3.9f, 1.5f);
        squareEye(c, bx - 48, by - 12, 30, 24, 12, b);
        squareEye(c, bx + 48, by - 12, 30, 24, 12, b);
        fill.setColor(0xFF3A2A1A);
        c.drawRect(bx - 36, by + 48, bx + 36, by + 72, fill);
    }

    // ---- eyes ----

    private float[] pupil(float cx, float cy, float mx, float my) {
        float dx = lookX - cx, dy = lookY - cy;
        float dist = (float) Math.max(1, Math.hypot(dx, dy));
        float k = Math.min(1, dist / 300);
        return new float[]{cx + dx / dist * mx * k, cy + dy / dist * my * k};
    }

    private void roundEye(Canvas c, float cx, float cy, float rx, float ry, float pr, float blink) {
        float[] p = pupil(cx, cy, (rx - pr) * .8f, (ry - pr) * .8f);
        c.save();
        c.scale(1, blink, cx, cy);
        fill.setShader(null);
        fill.setColor(Color.WHITE);
        ellipse(c, cx, cy, rx, ry, 0);
        fill.setColor(0xFF111111);
        ellipse(c, p[0], p[1], pr, pr * 1.15f, 0);
        fill.setColor(Color.WHITE);
        c.drawCircle(p[0] - pr * .35f, p[1] - pr * .4f, pr * .3f, fill);
        c.restore();
    }

    private void tallEye(Canvas c, float cx, float cy, float rx, float ry, float blink) {
        float[] p = pupil(cx, cy, rx * .8f, ry * .8f);
        c.save();
        c.scale(1, blink, cx, cy);
        fill.setShader(null);
        fill.setColor(0xFF111111);
        ellipse(c, cx, cy, rx, ry, 0);
        fill.setColor(Color.WHITE);
        ellipse(c, p[0] - (p[0] - cx) * .2f, p[1] - ry * .35f + (p[1] - cy) * .3f, rx * .45f, ry * .32f, 0);
        c.restore();
    }

    private void squareEye(Canvas c, float cx, float cy, float rx, float ry, float g, float blink) {
        float[] p = pupil(cx, cy, (rx - g) * .8f, (ry - g) * .8f);
        c.save();
        c.scale(1, blink, cx, cy);
        fill.setShader(null);
        fill.setColor(Color.WHITE);
        c.drawRect(cx - rx, cy - ry, cx + rx, cy + ry, fill);
        fill.setColor(0xFF1A1A1A);
        float px = Math.round((p[0] - g) / 12) * 12, py = Math.round((p[1] - g) / 12) * 12;
        c.drawRect(px, py, px + g * 2, py + g * 2, fill);
        c.restore();
    }

    private void ellipse(Canvas c, float cx, float cy, float rx, float ry, float deg) {
        c.save();
        c.rotate(deg, cx, cy);
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(oval, fill);
        c.restore();
    }

    private static float blink(float t, float period, float off) {
        float p = (t + off) % period;
        return p < .16f ? Math.max(.05f, Math.abs((float) Math.cos(p / .16f * Math.PI))) : 1;
    }
}
