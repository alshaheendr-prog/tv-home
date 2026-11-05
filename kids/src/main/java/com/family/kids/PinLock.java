package com.family.kids;

import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * The parent PIN: kept only as a salted hash, and after 5 wrong tries the pad rests,
 * 30 seconds at first and doubling up to 5 minutes, so the 10,000 codes can't be tried one by one.
 */
final class PinLock {
    private static final int FREE_TRIES = 5;
    private static final long FIRST_WAIT = 30_000, MAX_WAIT = 5 * 60_000, FORGET_AFTER = 60 * 60_000;

    private PinLock() { }

    /** Moves an old plain-text PIN over to the hashed form. */
    private static void migrate(SharedPreferences p) {
        String plain = p.getString("pin", "");
        if (plain.isEmpty()) return;
        String salt = salt(p);
        p.edit().putString("pin_h", hash(plain, salt)).remove("pin").apply();
    }

    static boolean isSet(SharedPreferences p) {
        migrate(p);
        return !p.getString("pin_h", "").isEmpty();
    }

    static void set(SharedPreferences p, String pin) {
        String salt = salt(p);
        p.edit().putString("pin_h", hash(pin, salt)).remove("pin").putInt("pin_fails", 0).putLong("pin_until", 0).apply();
    }

    /** Milliseconds left before another try is allowed, or 0. */
    static long waitLeft(SharedPreferences p) {
        long left = p.getLong("pin_until", 0) - System.currentTimeMillis();
        if (left <= 0) return 0;
        return Math.min(left, MAX_WAIT); // a clock that jumped back can't lock the pad for longer
    }

    /** Checks a try. A wrong one counts towards the rest; a right one clears the count. */
    static boolean check(SharedPreferences p, String entered) {
        migrate(p);
        if (waitLeft(p) > 0) return false;
        String want = p.getString("pin_h", "");
        if (!want.isEmpty() && MessageDigest.isEqual(want.getBytes(), hash(entered, salt(p)).getBytes())) {
            p.edit().putInt("pin_fails", 0).putLong("pin_until", 0).apply();
            return true;
        }
        long now = System.currentTimeMillis();
        // An hour with no tries forgets the old wrong ones, so a child's guessing can't keep the pad resting for long.
        int fails = (now - p.getLong("pin_last", 0) > FORGET_AFTER ? 0 : p.getInt("pin_fails", 0)) + 1;
        SharedPreferences.Editor e = p.edit().putInt("pin_fails", fails).putLong("pin_last", now);
        if (fails >= FREE_TRIES) {
            long wait = Math.min(MAX_WAIT, FIRST_WAIT << Math.min(fails - FREE_TRIES, 10));
            e.putLong("pin_until", now + wait);
        }
        e.apply();
        return false;
    }

    /** "Try again in 30 seconds" style text for the wait left. */
    static String waitText(long ms) {
        long s = (ms + 999) / 1000;
        if (s < 60) return "Too many tries. Try again in " + s + " seconds";
        long m = (s + 59) / 60;
        return "Too many tries. Try again in " + m + (m == 1 ? " minute" : " minutes");
    }

    private static String salt(SharedPreferences p) {
        String s = p.getString("pin_salt", "");
        if (!s.isEmpty()) return s;
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        s = hex(b);
        p.edit().putString("pin_salt", s).apply();
        return s;
    }

    private static String hash(String pin, String salt) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            // One salted hash, cheap enough for the UI thread. Extra rounds would not help a 4-digit PIN
            // (10,000 codes are quick to try even then); the wait after wrong tries is what protects it.
            return hex(d.digest((salt + ":" + pin).getBytes("UTF-8")));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
        return sb.toString();
    }
}
