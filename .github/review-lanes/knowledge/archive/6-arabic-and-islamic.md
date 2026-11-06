# Lane 6 knowledge: Arabic and Islamic content: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22 (audit update 1): Must: the `badge()` reorder put "Eid al-Adha tomorrow" ahead of today's "Day of Arafah" on 9 Dhul Hijjah (`Events.java:45`). Should: "Eid al-Fitr tomorrow" on 29 Ramadan states Umm al-Qura's prediction as fact, but Kuwait sights the moon that evening (`:44`). Nice: a trailing "…" after Arabic in LTR strings (`MainActivity.java:2824`, `:2977`). Fine: the Quran pauses only when the next prayer changes (`nextPrayer` skips sunrise); adhan banner LIFT 56 px in a 136 px band, Plex Arabic 50 px; the chime fallback; the new Arabic strings.

- **2026-11-05, PR #20 (merged):** no Must fix. Should fix: adhan-card English cut at 4 lines (fixed); hadith numbers on edited lines (fixed: Bukhari 6116, Abu Dawud 5081 as words of Abu al-Darda). `withoutBismillah` verified on 2:1, 9:1, 1:1, 95:1, 2:2. Amiri Bold covers all Quranic marks and ﷺ; Baloo lacks U+06DF, U+06E5, U+06E6, ﷺ. `OFFSETS` checked against published Kuwait times (Al-Anba's Ramadan 1447 timetable "according to the Al-Ajairi calendar", awgatalsalat.com for 6 Nov 2026): exact most days, else ±1 min, so they stay 0; summer unchecked; awqaf.gov.kw unreachable. Kuwait doesn't shift Isha in Ramadan. Left open: `Content.AYAH` partial verses labelled whole; ﷺ in Kids Baloo; no Hijri offset; 9 Muharram labelled "Ashura fast"; Latin digits in Arabic (`Events.java:59`); grade of رضيت بالله ربا; missing hadith numbers elsewhere.
- **2026-11-05, PR #21 (docs):** Should fix (docs): `badge()` shows "Fast of Ashura tomorrow" only on 8 Muharram; 13 Dhul Hijjah shows Eid and 14-15 do show White Days; the رضيت … نبياً risk now gives its grade and why. Added: Kahf and Shawwal sources, Eid 1447 in Kuwait, four new known risks (kids grades, يوم grammar, White Days in Ramadan/Shawwal, the 3-times label).
- **2026-11-05, knowledge build (no PR):** رضينا بالله ربا = Abu Dawud 5072, Da'if per al-Albani (sunnah.com); 9 Muharram is Tasu'a (Muslim 1134a); Tashreeq ban (Muslim 1141a); 1 Muharram 1448 in Kuwait = 16 June 2026 = Umm al-Qura; Plex Arabic covers every Quranic mark and ﷺ, so it's a valid fallback for Kids' ﷺ; CLDR 46 makes plain `ar` use Western digits.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (main d772bcc): Must: random verse of the day shows out-of-context short ayat (seed math: 77:30 on 9 Nov 2026, 100:2 on 13 Nov 2026) → curated ayah-number list. Should: Hijri offset before Ramadan 1448 (Umm al-Qura 8 Feb 2027); Tasu'a label; weak رضيت dhikr ungraded; hadith numbers (table in audit-2026-11/lane-6.md); offline verses plain/partial; same verse two days running (seed overlap); evening adhkar Maghrib (home) vs Asr (adhan card); digits/grammar (AdhanService:400 Western "10", يوم); kids تهادوا hasan, Ar-Raheem meaning, As-Saboor not established; "الإمساك" label on the Fajr time. Nice: API surah spellings, 120 vs 170 limit, ﴿ ﴾ for Quran only, ﷺ in Baloo, half hadith in Kids, midnight Hijri.


## Source library
Tags used above. Opened 2026-11-05 unless marked.
- [pt] https://praytimes.org/calculation : the full method: sun equations, Dhuhr/sunrise/sunset, 0.833° and elevation note, method angle table, Asr factors, Maghrib precaution, midnight, high-latitude rules.
- [sm-1134] https://sunnah.com/muslim:1134a : Tasu'a, "صمنا اليوم التاسع"; in-book 13/172; USC-MSA deprecated.
- [sm-1141] https://sunnah.com/muslim:1141 : days of Tashreeq are days of eating and drinking; chapter: fasting them forbidden.
- [bk-1990] https://sunnah.com/bukhari:1990 : no fasting on the two Eids.
- [sm-1162] https://sunnah.com/muslim:1162b : Arafah fast expiates two years, Ashura one.
- [skill] `anthropic-skills:islamic-content-check` (the owner's project skill) : the owner's source list and display rules; read 2026-11-05.
- [nas-2424] https://sunnah.com/nasai:2424 : fast the 13th, 14th, 15th; Hasan (Darussalam). (sunnah.com/tirmidhi:761 failed to load.)
- [ad-5072] https://sunnah.com/abudawud:5072 : رضينا بالله ربا … morning and evening; Da'if (al-Albani).
- [arabtimes-1448] https://www.arabtimesonline.com/news/16th-june-marks-start-of-islamic-new-year-in-kuwait/ : 1 Muharram 1448 = 16 June 2026 in Kuwait, by sighting.
- [kt-1446] https://www.khaleejtimes.com/world/gulf/kuwait-moon-sighting-eid-al-adha : Kuwait Joint Committee for Crescent Sighting forecast for Dhul Hijjah 1446 (2025), one day after Umm al-Qura. Forecast, not the final announcement.
- [icu] https://raw.githubusercontent.com/unicode-org/icu/main/icu4j/main/core/src/main/java/com/ibm/icu/util/IslamicCalendar.java : calculation types, epochs, civil default, Umm al-Qura range 1300-1600.
- [jdk-hijrah] local `java.time.chrono.HijrahDate` (OpenJDK 21) : Umm al-Qura cross-check dates listed in section 2.
- [ucd] https://raw.githubusercontent.com/unicode-org/unicodetools/main/unicodetools/data/ucd/dev/UnicodeData.txt : general category and bidi class of every code point above.
- [blocks] https://raw.githubusercontent.com/unicode-org/unicodetools/main/unicodetools/data/ucd/dev/Blocks.txt : Arabic block ranges.
- [dcp] https://raw.githubusercontent.com/unicode-org/unicodetools/main/unicodetools/data/ucd/dev/DerivedCoreProperties.txt : bidi controls are Default_Ignorable.
- [cldr] https://raw.githubusercontent.com/unicode-org/cldr/release-45/common/main/ar.xml and .../release-46/common/main/ar.xml, .../main/common/main/ar_KW.xml : default numbering system change.
- [nuqayah] https://raw.githubusercontent.com/nuqayah/quran-text/master/README.md : Tanzil-derived Unicode-clean text; encoding differences; no lam-hamza-alif ligature in Unicode.
- [khaled] https://raw.githubusercontent.com/khaledhosny/quran-data/master/README.md : "not formally reviewed".
- [amiri] https://raw.githubusercontent.com/aliftype/amiri/main/README.md : Amiri's origin, maturity, OFL.
- Checked by the first review on 2026-11-05 (listed in the brief, not re-opened this round): [aladhan-m] https://api.aladhan.com/v1/methods ; Aladhan timings for 15-01, 05-11 and 21-06-2026 ; [esalah] https://esalah.com/prayer-times/kuwait/?lang=en ; [gtaf] https://gtaf.org/prayer-times/kuwait/kuwait-city/ ; ICU CalculationType docs ; arynews.tv Ramadan 2026 and moroccoworldnews Eid al-Fitr 2026 ; [alq-2-1] https://api.alquran.cloud/v1/ayah/8/editions/quran-uthmani,en.sahih ; hadithanswers.com (Bukhari 6116, Abu Dawud 5081) ; [minikin] android.googlesource.com minikin e8721b2 ; [devto] dev.to Arabic tajweed rendering ; [bidi] learn.microsoft.com BidiFormatter ; Al-Anba Ramadan 1447 timetable and awgatalsalat.com (6 Nov 2026).
- [dorar-146478] https://dorar.net/hadith/sharh/146478 : Kahf on Friday, sahih per al-Albani, Friday night starts at Thursday sunset. Checked 2026-11-05.
- [abuaminaelias-shawwal] abuaminaelias.com, six days of Shawwal: Muslim 1164. Checked 2026-11-05.
- [arabtimes-eid1447] arabtimesonline.com, 19 May 2026: Kuwait holiday for Arafah and Eid 1447 from Tue 26 May. Checked 2026-11-05.
- [islamqa-192204] https://islamqa.info/en/answers/192204 : رضيت بالله ربا, its narrations and grades; the authentic Muslim 1884 / Abu Dawud 1529. Checked 2026-11-05.
- [hadithanswers-radhitu] hadithanswers.com, رضيت بالله ربا: Tirmidhi 3389 and its grade. Checked 2026-11-05.
- [kalimah-numbers] kalimah-center.com/arabic-numbers-grammar : number-noun agreement (أيام for 3-10, يوماً for 11-99). Checked 2026-11-05.
- Not reachable on 2026-11-05: awqaf.gov.kw (first review); tanzil.net, sunnah.com and aladhan.com by curl (proxy only allows GitHub raw); the WebFetch budget ran out before tanzil.net/docs/text_updates and the GCC Eid al-Adha 2026 reports.
- [tohed-t1956] https://en.tohed.com/hadith/tirmidhi/1956 ; [tohed-t1954] https://en.tohed.com/hadith/tirmidhi/1954 ; [tohed-t3604] https://en.tohed.com/hadith/tirmidhi/3604 ; [tohed-adab594] https://en.tohed.com/hadith/al-adab-al-mufrad/594 ; tohed Bukhari 6018, 5027, 6464, 5376 and Muslim 223, 2593 (same URL pattern) ; [tohed-hisn] https://en.tohed.com/hadith/hisnul-muslim/chapter/862/sub/25214/
- [hadithanswers-ahl] https://hadithanswers.com/another-hadith-stating-the-best-of-you-is-the-best-to-his-wife-family/
- [islamqa-191329] https://islamqa.info/ar/answers/191329 (As-Saboor)
- [islamonline-rahim] https://fiqh.islamonline.net/ (من أسماء الله الحسنى الرحمن الرحيم)
- [jordannews-r1448] https://www.jordannews.jo/Section-20/Middle-East/Date-of-the-First-Day-of-Ramadan-2027-According-to-Astronomical-Calculations-52316
- [khaleejcalc-imsak] https://khaleejcalculators.com/ramadan-imsakiyah/kuwait (secondary; it names both Umm al-Qura and "Kuwait Awqaf" as its method, so it is not an authority)
- Not reachable 2026-11-05: dorar.net/hadith/sharh pages (client error), api.alquran.cloud by curl (proxy), GitHub API for nuqayah/quran-text (not in session scope).

- [mwn-eid1447] https://www.moroccoworldnews.com/2026/03/283198/kuwait-confirms-eid-al-fitr-2026-for-march-20/ : the Shawwal moon was not seen on 29 Ramadan 1447, Ramadan ran 30 days, Eid was Fri 20 Mar 2026 in Kuwait. Checked 2026-11-05.
- [tohed-t3604] re-checked 2026-11-05: حين يمسي ثلاث مرات, hasan per Tirmidhi.

## Open questions to research next
- Kuwait Awqaf: the official method statement and a summer-month timetable (June-August) to extend the OFFSETS check. Try awqaf.gov.kw again, or a dated Kuwaiti newspaper table.
- Answered 2026-11-05: Eid al-Adha 1447 in Kuwait matched Umm al-Qura (holiday from Tue 26 May) [arabtimes-eid1447].
- Answered 2026-11-05: Kahf on Friday = al-Hakim 3392 / al-Bayhaqi 6209, sahih per al-Albani, Thursday sunset to Friday [dorar-146478]; six of Shawwal = Muslim 1164 [abuaminaelias-shawwal]. Still open: the Jumu'ah sunnahs in `Widgets.java:35` (ghusl, perfume, going early, salawat).
- sunnah.com numbers for Tirmidhi رضيت … نبياً and Muslim 1164a.
- al-Albani's grade for Tirmidhi 1956 (smile) and تهادوا تحابوا.
- The Tirmidhi number for the "3 times" narration of أعوذ بكلمات الله.
- Numbers for the remaining `Content.HADITH` and `GoodWords.HADITHS` entries, with grades for the non-Bukhari/Muslim ones (Tirmidhi smile; Al-Adab al-Mufrad تهادوا تحابوا; خيركم خيركم لأهله; من لا يشكر الناس).
- Answered 2026-11-05: رضيت بالله … نبيا is in Tirmidhi, Ibn Majah 3870, Ahmad and Nasa'i's Kubra; weak per al-Albani (section 10).
- Which days to show as White Days in Dhul Hijjah (14-15 only? 14-16?) and the source.
- Tanzil's current text version and terms of use (tanzil.net/docs/text_updates, tanzil.net/download).
- Amiri Quran vs Amiri: does the bundled Amiri Bold place pause marks and small high letters correctly at 34-38 px on the TV? KFGQPC Uthmanic Hafs font licence for app use.
- ICU/CLDR version on this box (Android 14) and whether `Locale("ar")` gives Arabic-Indic digits there.
- An adab source on displaying Quran text (e.g. not on a screen that is cut off or used as decoration).
- Magnetic declination for Kuwait City from NOAA WMM, only if a Qibla feature is ever proposed.
- Kuwait's sighting committee date for Ramadan 1448 (expected Umm al-Qura 8 Feb 2027). Check in late January 2027.
- Sunnah.com numbers to confirm: the eight Bukhari/Muslim pairs in the S4 table, the Muslim number for أحب الأعمال أدومها, the hadith numbers for the five morning adhkar, and the Jumu'ah sunnahs.
- The fiqh source for when the evening adhkar start (after Asr vs after Maghrib), and which one Hisn al-Muslim's author follows.
- Awqaf Kuwait's own imsak column (minutes before Fajr) and a summer prayer timetable from an authoritative Kuwaiti source.
- Owner decision: hasan hadith in Kids (تهادوا تحابوا), As-Saboor, and the curated verse list.

- Which committee announces Eid al-Fitr in Kuwait, and at what time that evening. Name the Kuwait Sharia Crescent Sighting Committee from a Kuwaiti source such as KUNA.
- Umm al-Qura years where Ramadan has 29 days in 1448-1450, to test the day-29 wording. 1448 has 29 days: 1 Ramadan is 8 Feb 2027 and 1 Shawwal is 9 Mar 2027.

