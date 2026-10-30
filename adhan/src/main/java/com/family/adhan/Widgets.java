package com.family.adhan;

import android.content.Context;
import android.icu.util.IslamicCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Decides what the rotating widget card shows right now. Each item is {kicker, Arabic, English, reference}. */
final class Widgets {
    private Widgets() { }

    private static String cachedDay = "";
    private static String[] cachedCountdown;

    static List<String[]> items(Context c, Calendar now, int[] t, String[] ayah, double temp, double dust) {
        List<String[]> out = new ArrayList<>();
        int doy = now.get(Calendar.DAY_OF_YEAR) + now.get(Calendar.YEAR) * 400;
        int min = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        // Weather alerts come first when they matter.
        if (!Double.isNaN(dust) && dust > 150)
            out.add(new String[]{"WEATHER ALERT · تنبيه الطقس", "غبار كثيف في الكويت. أغلق النوافذ.", "Heavy dust in Kuwait. Keep the windows closed.", ""});
        if (!Double.isNaN(temp) && temp >= 45)
            out.add(new String[]{"WEATHER ALERT · تنبيه الطقس", "حر شديد، " + Math.round(temp) + "°. اشرب الماء وابقَ في الظل.",
                    "Extreme heat, " + Math.round(temp) + "°. Drink water and stay out of the sun.", ""});

        // Friday
        int dow = now.get(Calendar.DAY_OF_WEEK);
        if (t != null && ((dow == Calendar.FRIDAY && min < t[4]) || (dow == Calendar.THURSDAY && min >= t[4]))) {
            out.add(new String[]{"FRIDAY · الجمعة", "لا تنسَ قراءة سورة الكهف", "Don't forget to read Surat Al-Kahf.", ""});
            if (dow == Calendar.FRIDAY && min < t[2] + 60)
                out.add(new String[]{"JUMU'AH · صلاة الجمعة", "صلاة الجمعة " + PrayerTimes.clock(t[2]), "Jumu'ah prayer at " + PrayerTimes.clock(t[2]) + ".", ""});
        }

        // Morning and evening adhkar
        if (t != null && min >= t[0] && min < t[1] + 180)
            out.add(new String[]{"MORNING ADHKAR · أذكار الصباح", "سبحان الله وبحمده", "SubhanAllahi wa bihamdihi, 100 times in the morning.", "Muslim"});
        if (t != null && min >= t[3] && min < t[4] + 60)
            out.add(new String[]{"EVENING ADHKAR · أذكار المساء", "أعوذ بكلمات الله التامات من شر ما خلق",
                    "I seek refuge in the perfect words of Allah from the evil of what He created. (3 times)", "Muslim"});

        // Verse and hadith of the day
        String[] a = ayah != null ? ayah : Content.AYAH[Math.floorMod(doy, Content.AYAH.length)];
        out.add(new String[]{"VERSE OF THE DAY · آية اليوم", a[0], a[1], a[2]});
        String[] h = Content.HADITH[Math.floorMod(doy, Content.HADITH.length)];
        out.add(new String[]{"HADITH OF THE DAY · حديث اليوم", h[0], h[1], h[2]});

        // Ramadan and Eid
        String[] cd = countdown(now);
        if (cd != null) out.add(cd);

        // Character of the day
        String[] ch = Content.CHARACTERS[Math.floorMod(doy * 7, Content.CHARACTERS.length)];
        out.add(new String[]{"CHARACTER OF THE DAY · شخصية اليوم  ·  " + ch[0].toUpperCase(Locale.ROOT), ch[2], ch[1] + ": " + ch[3], ""});
        return out;
    }

    private static String ar(int n) { return String.format(new Locale("ar"), "%d", n); }

    /** Days until the next of Ramadan, Eid al-Fitr and Eid al-Adha (Umm al-Qura calendar). */
    private static String[] countdown(Calendar now) {
        String key = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
        if (key.equals(cachedDay)) return cachedCountdown;
        cachedDay = key;
        cachedCountdown = null;
        try {
            IslamicCalendar ic = new IslamicCalendar(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
            ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(now.getTimeInMillis());
            int m = ic.get(IslamicCalendar.MONTH), d = ic.get(IslamicCalendar.DAY_OF_MONTH);
            if (m == IslamicCalendar.RAMADAN) {
                cachedCountdown = new String[]{"RAMADAN · رمضان", "رمضان كريم، اليوم " + ar(d) + " من رمضان", "Ramadan Kareem. Today is day " + d + " of Ramadan.", ""};
                return cachedCountdown;
            }
            for (int i = 1; i <= 400; i++) {
                ic.add(IslamicCalendar.DAY_OF_MONTH, 1);
                int mm = ic.get(IslamicCalendar.MONTH), dd = ic.get(IslamicCalendar.DAY_OF_MONTH);
                String[] hit = null;
                if (mm == IslamicCalendar.RAMADAN && dd == 1) hit = new String[]{"رمضان", "Ramadan"};
                else if (mm == IslamicCalendar.SHAWWAL && dd == 1) hit = new String[]{"عيد الفطر", "Eid al-Fitr"};
                else if (mm == IslamicCalendar.DHU_AL_HIJJAH && dd == 10) hit = new String[]{"عيد الأضحى", "Eid al-Adha"};
                if (hit != null) {
                    cachedCountdown = new String[]{"COUNTDOWN · العد التنازلي", hit[0] + " بعد " + ar(i) + " يوم",
                            hit[1] + " in " + i + (i == 1 ? " day" : " days") + ".", ""};
                    break;
                }
            }
        } catch (Exception ignored) { }
        return cachedCountdown;
    }
}
