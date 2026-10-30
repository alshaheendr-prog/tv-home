package com.family.tvhome;

import android.icu.util.IslamicCalendar;

import java.util.Calendar;

/** The Hijri date (Umm al-Qura, Kuwait time) and the Islamic days worth a small badge: Ramadan, the Eids, Arafah, Ashura, the White Days. */
final class Events {
    private Events() { }

    private static String cachedKey = "";
    private static int[] cachedHijri;

    /** {month (0 = Muharram), day} for the given moment. */
    static int[] hijri(Calendar now) {
        String key = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
        if (key.equals(cachedKey)) return cachedHijri;
        int[] out = {-1, -1};
        try {
            IslamicCalendar ic = new IslamicCalendar(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
            ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(now.getTimeInMillis());
            out = new int[]{ic.get(IslamicCalendar.MONTH), ic.get(IslamicCalendar.DAY_OF_MONTH)};
        } catch (Exception ignored) { }
        cachedKey = key;
        cachedHijri = out;
        return out;
    }

    static boolean isRamadan(Calendar now) { return hijri(now)[0] == IslamicCalendar.RAMADAN; }

    /** Day of Ramadan (1 to 30), or 0 outside Ramadan. */
    static int ramadanDay(Calendar now) { return isRamadan(now) ? hijri(now)[1] : 0; }

    /** {English, Arabic} for today's badge, or null on an ordinary day. Today's event wins; otherwise a fast that starts tomorrow. */
    static String[] badge(Calendar now) {
        int[] h = hijri(now);
        String[] today = on(h[0], h[1]);
        if (today != null) return today;
        Calendar t = (Calendar) now.clone();
        t.add(Calendar.DAY_OF_MONTH, 1);
        int[] n = hijri(t);
        hijri(now); // put today back in the cache
        String[] tomorrow = on(n[0], n[1]);
        if (tomorrow == null) return null;
        if (n[0] == IslamicCalendar.RAMADAN && n[1] == 1) return new String[]{"Ramadan starts tomorrow", "رمضان يبدأ غداً"};
        if (n[0] == IslamicCalendar.SHAWWAL && n[1] == 1) return new String[]{"Eid al-Fitr tomorrow", "عيد الفطر غداً"};
        if (n[0] == IslamicCalendar.DHU_AL_HIJJAH && n[1] == 10) return new String[]{"Eid al-Adha tomorrow", "عيد الأضحى غداً"};
        if (n[0] == IslamicCalendar.DHU_AL_HIJJAH && n[1] == 9) return new String[]{"Fast of Arafah tomorrow", "صيام عرفة غداً"};
        if (n[0] == IslamicCalendar.MUHARRAM && (n[1] == 9 || n[1] == 10)) return new String[]{"Fast of Ashura tomorrow", "صيام عاشوراء غداً"};
        if (n[1] == 13 && n[0] != IslamicCalendar.DHU_AL_HIJJAH) return new String[]{"White Days start tomorrow", "الأيام البيض تبدأ غداً"};
        return null;
    }

    private static String[] on(int m, int d) {
        if (m < 0) return null;
        if (m == IslamicCalendar.RAMADAN) {
            if (d >= 21) return new String[]{"Last ten nights of Ramadan", "العشر الأواخر من رمضان"};
            return new String[]{"Ramadan · day " + d, "رمضان · اليوم " + d};
        }
        if (m == IslamicCalendar.SHAWWAL && d <= 3) return new String[]{"Eid al-Fitr Mubarak", "عيد فطر مبارك"};
        if (m == IslamicCalendar.SHAWWAL) return new String[]{"Six days of Shawwal", "ست من شوال"};
        if (m == IslamicCalendar.DHU_AL_HIJJAH) {
            if (d <= 8) return new String[]{"First ten days of Dhul Hijjah", "العشر الأوائل من ذي الحجة"};
            if (d == 9) return new String[]{"Day of Arafah", "يوم عرفة"};
            if (d <= 13) return new String[]{"Eid al-Adha Mubarak", "عيد أضحى مبارك"};
        }
        if (m == IslamicCalendar.MUHARRAM) {
            if (d == 1) return new String[]{"Islamic New Year", "رأس السنة الهجرية"};
            if (d == 9 || d == 10) return new String[]{"Ashura fast", "صيام عاشوراء"};
        }
        if (d >= 13 && d <= 15) return new String[]{"White Days fast", "صيام الأيام البيض"};
        return null;
    }
}
