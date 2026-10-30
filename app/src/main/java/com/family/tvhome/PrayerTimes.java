package com.family.tvhome;

/**
 * Prayer times for Kuwait City, calculated on the box (PrayTimes.org method).
 * Kuwait parameters: Fajr 18°, Isha 17.5°, Asr shadow factor 1.
 */
final class PrayerTimes {
    static final double LAT = 29.3759, LNG = 47.9774, TZ = 3;
    static final String[] KEYS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    static final String[] EN = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    static final String[] AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    /** Minutes to add to each time so it matches the Awqaf calendar exactly. */
    static final int[] OFFSETS = {0, 0, 0, 0, 0, 0};

    private PrayerTimes() { }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double fix(double a, double b) { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; }

    /** Returns minutes after midnight (Kuwait time) for the six times, in KEYS order. */
    static int[] forDate(int y, int m, int d) {
        int yy = y, mm = m;
        if (mm <= 2) { yy -= 1; mm += 12; }
        double a = Math.floor(yy / 100.0), b = 2 - a + Math.floor(a / 4);
        final double jd0 = Math.floor(365.25 * (yy + 4716)) + Math.floor(30.6001 * (mm + 1)) + d + b - 1524.5 - LNG / (15 * 24);

        double[] t = {5, 6, 12, 13, 18, 18};
        for (int i = 0; i < 2; i++) {
            double[] p = new double[6];
            for (int k = 0; k < 6; k++) p[k] = t[k] / 24;
            t = new double[]{
                    angleTime(jd0, 18, p[0], true),
                    angleTime(jd0, 0.833, p[1], true),
                    midDay(jd0, p[2]),
                    asrTime(jd0, p[3]),
                    angleTime(jd0, 0.833, p[4], false),
                    angleTime(jd0, 17.5, p[5], false)};
        }
        int[] out = new int[6];
        for (int k = 0; k < 6; k++) out[k] = (int) Math.round((t[k] + TZ - LNG / 15) * 60) + OFFSETS[k];
        return out;
    }

    private static double[] sun(double jd) {
        double D = jd - 2451545;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360);
        double e = 23.439 - 0.00000036 * D;
        double ra = Math.toDegrees(Math.atan2(dcos(e) * dsin(L), dcos(L))) / 15;
        double decl = Math.toDegrees(Math.asin(dsin(e) * dsin(L)));
        double eqt = q / 15 - fix(ra, 24);
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd0, double t) { return fix(12 - sun(jd0 + t)[1], 24); }

    private static double angleTime(double jd0, double angle, double t, boolean ccw) {
        double decl = sun(jd0 + t)[0];
        double noon = midDay(jd0, t);
        double T = Math.toDegrees(Math.acos((-dsin(angle) - dsin(decl) * dsin(LAT)) / (dcos(decl) * dcos(LAT)))) / 15;
        return noon + (ccw ? -T : T);
    }

    private static double asrTime(double jd0, double t) {
        double decl = sun(jd0 + t)[0];
        double angle = -Math.toDegrees(Math.atan(1 / (1 + dtan(Math.abs(LAT - decl)))));
        return angleTime(jd0, angle, t, false);
    }

    static String clock(int minutes) {
        int h = (minutes / 60) % 24, m = minutes % 60;
        int h12 = ((h + 11) % 12) + 1;
        return h12 + ":" + (m < 10 ? "0" : "") + m;
    }
}
