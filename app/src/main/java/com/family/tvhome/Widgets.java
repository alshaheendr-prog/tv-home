package com.family.tvhome;

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
            out.add(new String[]{"WEATHER ALERT · تنبيه الطقس", "حر شديد، " + ar((int) Math.round(temp)) + "°. اشرب الماء وابقَ في الظل.",
                    "Extreme heat, " + Math.round(temp) + "°. Drink water and stay out of the sun.", ""});

        // Friday
        int dow = now.get(Calendar.DAY_OF_WEEK);
        if (t != null && ((dow == Calendar.FRIDAY && min < t[4]) || (dow == Calendar.THURSDAY && min >= t[4]))) {
            out.add(new String[]{"FRIDAY · الجمعة", "لا تنسَ قراءة سورة الكهف", "Don't forget to read Surat Al-Kahf.", ""});
            if (dow == Calendar.FRIDAY && min < t[2])
                out.add(new String[]{"JUMU'AH SUNNAHS · سنن الجمعة", "الغسل والطيب والتبكير إلى الصلاة والإكثار من الصلاة على النبي ﷺ",
                        "Ghusl, perfume, going early to the prayer, and many salawat on the Prophet ﷺ.", "Bukhari 883, 881 · Abu Dawud 1047"});
            if (dow == Calendar.FRIDAY && min < t[2] + 60)
                out.add(new String[]{"JUMU'AH · صلاة الجمعة", "صلاة الجمعة " + arDigits(PrayerTimes.clock(t[2])), "Jumu'ah prayer at " + PrayerTimes.clock(t[2]) + ".", ""});
        }

        // Morning adhkar from Fajr, evening adhkar from Asr: while it is their time they take over the card.
        int mode = adhkarMode(now, t);
        if (mode != 0) {
            for (String[] d : mode == 1 ? MORNING : EVENING)
                out.add(new String[]{mode == 1 ? "MORNING ADHKAR · أذكار الصباح" : "EVENING ADHKAR · أذكار المساء", d[0], d[1], d[2]});
            return out;
        }

        // Verse and hadith of the day
        String[] a = Verses.forDay(now); // a curated list of whole verses, the same in every app (offline too)
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

    /** 1 from Fajr until three hours after sunrise, 2 from Asr until an hour after Maghrib (as in the Adhan app), otherwise 0. */
    static int adhkarMode(Calendar now, int[] t) {
        if (t == null) return 0;
        int min = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (min >= t[0] && min < t[1] + 180) return 1;
        if (min >= t[3] && min < t[4] + 60) return 2;
        return 0;
    }

    // {Arabic, English, source}. The adhkar of Hisn al-Muslim, cited to the hadith collections (sunnah.com numbering) with
    // al-Albani's grade for the Sunan. Check any new one on dorar.net or sunnah.com first: some well-known ones are weak
    // (رضيت بالله ثلاثاً, Abu Dawud 5073 and 5084) or not the Prophet's ﷺ words (Hasbiyallah 7 times, Abu Dawud 5081).
    static final String[][] MORNING = {
            {"أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له",
                    "We have reached the morning and the dominion belongs to Allah. Praise be to Allah; none is worthy of worship but Allah alone.", "from Muslim 2723"},
            {"اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور",
                    "O Allah, by You we reach the morning and the evening, by You we live and die, and to You is the rising.", "Abu Dawud 5068, sahih (al-Albani)"},
            {"اللهم أنت ربي، لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء لك بذنبي، فاغفر لي، فإنه لا يغفر الذنوب إلا أنت",
                    "O Allah, You are my Lord; none is worthy of worship but You. You created me and I am Your servant, true to Your covenant as best I can. I seek refuge in You from the evil I have done. I admit Your favour and my sin, so forgive me: none forgives sins but You.", "Bukhari 6306"},
            {"اللهم فاطر السموات والأرض، عالم الغيب والشهادة، رب كل شيء ومليكه، أشهد أن لا إله إلا أنت، أعوذ بك من شر نفسي وشر الشيطان وشركه",
                    "O Allah, Creator of the heavens and the earth, Knower of the unseen and the seen, Lord and Owner of all things: I bear witness that none is worthy of worship but You. I seek refuge in You from the evil of myself and of Satan.", "from Abu Dawud 5067, sahih (al-Albani)"},
            {"اللهم إني أسألك العافية في الدنيا والآخرة",
                    "O Allah, I ask You for well-being in this world and the Hereafter.", "from Abu Dawud 5074, sahih (al-Albani)"},
            {"بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم",
                    "In the name of Allah, with whose name nothing on earth or in heaven can harm, and He is the All-Hearing, the All-Knowing. (3 times)", "Abu Dawud 5088, sahih (al-Albani)"},
            {"قل هو الله أحد والمعوذتين حين تمسي وحين تصبح ثلاث مرات تكفيك من كل شيء",
                    "Qul huwa Allahu ahad and the two Mu'awwidhat (Al-Falaq and An-Nas), three times in the evening and the morning: they will suffice you against everything.", "from Abu Dawud 5082, hasan (al-Albani)"},
            {"سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته",
                    "Glory and praise be to Allah, as many as His creation, as much as pleases Him, the weight of His Throne and the ink of His words. (3 times)", "from Muslim 2726"},
            {"لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
                    "None is worthy of worship but Allah alone, with no partner. His is the dominion and the praise, and He has power over all things. (100 times a day)", "from Bukhari 3293"},
            {"سبحان الله وبحمده",
                    "Glory be to Allah and praise be to Him. (100 times, morning and evening)", "from Muslim 2692"},
    };
    static final String[][] EVENING = {
            {"أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له",
                    "We have reached the evening and the dominion belongs to Allah. Praise be to Allah; none is worthy of worship but Allah alone.", "from Muslim 2723"},
            {"اللهم بك أمسينا وبك نحيا وبك نموت وإليك النشور",
                    "O Allah, by You we reach the evening, by You we live and die, and to You is the rising.", "Abu Dawud 5068, sahih (al-Albani)"},
            {"أعوذ بكلمات الله التامات من شر ما خلق",
                    "I seek refuge in the perfect words of Allah from the evil of what He has created. (3 times)", "Muslim 2709 · Tirmidhi 3604"},
            {"اللهم أنت ربي، لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء لك بذنبي، فاغفر لي، فإنه لا يغفر الذنوب إلا أنت",
                    "O Allah, You are my Lord; none is worthy of worship but You. You created me and I am Your servant, true to Your covenant as best I can. I seek refuge in You from the evil I have done. I admit Your favour and my sin, so forgive me: none forgives sins but You.", "Bukhari 6306"},
            {"اللهم فاطر السموات والأرض، عالم الغيب والشهادة، رب كل شيء ومليكه، أشهد أن لا إله إلا أنت، أعوذ بك من شر نفسي وشر الشيطان وشركه",
                    "O Allah, Creator of the heavens and the earth, Knower of the unseen and the seen, Lord and Owner of all things: I bear witness that none is worthy of worship but You. I seek refuge in You from the evil of myself and of Satan.", "from Abu Dawud 5067, sahih (al-Albani)"},
            {"اللهم إني أسألك العافية في الدنيا والآخرة",
                    "O Allah, I ask You for well-being in this world and the Hereafter.", "from Abu Dawud 5074, sahih (al-Albani)"},
            {"بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم",
                    "In the name of Allah, with whose name nothing on earth or in heaven can harm, and He is the All-Hearing, the All-Knowing. (3 times)", "Abu Dawud 5088, sahih (al-Albani)"},
            {"قل هو الله أحد والمعوذتين حين تمسي وحين تصبح ثلاث مرات تكفيك من كل شيء",
                    "Qul huwa Allahu ahad and the two Mu'awwidhat (Al-Falaq and An-Nas), three times in the evening and the morning: they will suffice you against everything.", "from Abu Dawud 5082, hasan (al-Albani)"},
            {"سبحان الله وبحمده",
                    "Glory be to Allah and praise be to Him. (100 times, morning and evening)", "from Muslim 2692"},
    };

    // Arabic-Indic digits by hand: Locale("ar") gives Western digits on newer Android (CLDR 46)
    private static String ar(int n) { return arDigits(String.valueOf(n)); }

    /** "in N days" with the Arabic number agreement: غداً, بعد يومين, بعد ٣ أيام (3-10), بعد ١١ يوماً (11 and up). */
    static String inDaysAr(int n) {
        if (n == 1) return "غداً";
        if (n == 2) return "بعد يومين";
        return "بعد " + ar(n) + (n <= 10 ? " أيام" : " يوماً");
    }


    /** Arabic-Indic digits for an Arabic line: Amiri has these, not the Western ones. */
    static String arDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) b.append(c >= '0' && c <= '9' ? (char) ('\u0660' + (c - '0')) : c);
        return b.toString();
    }

    /** Days until the next of Ramadan, Eid al-Fitr and Eid al-Adha (Umm al-Qura calendar). */
    private static String[] countdown(Calendar now) {
        String key = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR) + "/" + Events.offset;
        if (key.equals(cachedDay)) return cachedCountdown;
        cachedDay = key;
        cachedCountdown = null;
        try {
            IslamicCalendar ic = new IslamicCalendar(android.icu.util.TimeZone.getTimeZone("Asia/Kuwait"));
            ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(Events.shift(now.getTimeInMillis()));
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
                    cachedCountdown = new String[]{"COUNTDOWN · العد التنازلي", hit[0] + " " + inDaysAr(i),
                            hit[1] + " in " + i + (i == 1 ? " day" : " days") + ".", ""};
                    break;
                }
            }
        } catch (Exception ignored) { }
        return cachedCountdown;
    }
}
