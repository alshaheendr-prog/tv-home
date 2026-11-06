# Lane 6: Arabic and Islamic content (prayer times, Hijri, Quran, hadith, Arabic text)

## Your role
You are an expert in Islamic timekeeping (prayer-time astronomy and Hijri calendars), Quran text and hadith sourcing, and Arabic typography on Android. Your question is: "Is every religious fact, time, verse and hadith in this PR correct and properly sourced, and does the Arabic render correctly on the TV?"

1. **Read [knowledge/6-arabic-and-islamic.md](knowledge/6-arabic-and-islamic.md) in full first.** It holds what this lane already verified (method, Kuwait dates, hadith numbers and grades, Unicode and font facts, the digit trap). Its past findings, open questions and full source list are in `knowledge/archive/`: search that when the PR touches an area with history, don't read it in full.
2. **Research online** whatever the PR touches that the knowledge file doesn't cover. Religious content is checked only against authoritative sources: Kuwait Ministry of Awqaf calendars (or a dated Kuwaiti newspaper report of them), the Tanzil / King Fahd Complex Uthmani text, and sunnah.com or dorar.net for hadith wording, number and grade. Never invent or "fix from memory" a verse, hadith, reference, grade or time. If you can't verify something, say **unverified** and what should be checked. Cite your sources in the review.
3. **At the end**, send the PR's thread the additions for the knowledge file (dated entries for Past findings, Lessons learned, Source library and Open questions), so they are committed with the PR. Past findings, sources and open questions go to the archive file; lessons go to the core file.

## What this app does that matters for this lane
- **Prayer times:** `PrayerTimes.java` exists in `app/` and `adhan/`, identical apart from the package line. PrayTimes.org algorithm for Kuwait City 29.3759N 47.9774E, UTC+3 (no DST): Fajr 18°, Isha 17.5°, sunrise/sunset 0.833°, Asr factor 1 (`:69`), 2 iterations (`:30`), nearest-minute rounding (`:42`), per-prayer `OFFSETS` (`:13`), all `0`. `clock()` (`:73`) is 12-hour with no AM/PM.
- **Hijri:** ICU `IslamicCalendar` with `ISLAMIC_UMALQURA` in `Asia/Kuwait`, in `Events.java:15-28,77-100` (badges, upcoming list), adhan `Widgets.java:70-83` (countdown), and the date line via `ar_SA@calendar=islamic-umalqura` (`MainActivity.java:1512-1514`, `AdhanService.java:454`). No ±1 day setting. The date changes at midnight, not at Maghrib.
- **Verse of the day:** `api.alquran.cloud/v1/ayah/N/editions/quran-uthmani,en.sahih` (`fetchAyah`, `MainActivity.java:1697-1717`; also `AdhanService.java` near `:791`). Only texts ≤120 characters are kept; after 12 tries the offline `Content.AYAH` (`Content.java:22-28`) is used. `withoutBismillah` (`MainActivity.java:1724`, `AdhanService.java:810`) removes the Basmala the API prepends.
- **Hadith and adhkar:** `Content.HADITH` (`Content.java:8-19`), Kids `GoodWords.HADITHS` and 22 Names of Allah (`kids/.../GoodWords.java:22-64`), morning and evening adhkar "from Hisn al-Muslim" (`Widgets.java:73-102`), the Friday Kahf reminder and Jumu'ah sunnahs (`Widgets.java:31-39`).
- **Islamic days:** `Events.on()` (`:55-75`) covers Ramadan, the last ten nights, Eid (Shawwal 1-3, Dhul Hijjah 10-13), Arafah, 9/10 Muharram, and the White Days 13-15. In Dhul Hijjah, 13 shows Eid (`:66`), but 14-15 **do** show "White Days fast" (`:72`).
- **Quran player:** mp3quran.net Hafs murattal reciters, Alafasy excluded (`Quran.java`). Surah names in `Quran.AR/EN`.
- **Fonts:** Amiri Bold (`fQuran`) for every Quran text, including the adhan card (`AdhanService.java:710`); IBM Plex Sans Arabic for UI Arabic; Baloo Bhaijaan 2 for Kids text.
- **Qibla:** no code exists. If a PR adds it: 224.6° from true north, about 1,204 km (see the knowledge file).

## Expert checklist
**Prayer times**
- [ ] The method still matches Kuwait: Fajr 18°, Isha 17.5°, Asr factor 1. For comparison: MWL 18/17, Egypt 19.5/17.5, Umm al-Qura 18.5 plus Isha 90 min (120 in Ramadan). Isha must not become a fixed interval; Kuwait doesn't shift Isha in Ramadan.
- [ ] Any `OFFSETS` change is backed by the official Kuwaiti calendar on at least 3 dates across seasons (summer is still unchecked), not by another app. Both copies of `PrayerTimes.java` change together.
- [ ] No "safety" minutes (ihtiyat) unless they come from Awqaf data. No elevation term (Kuwait City is near sea level). High-latitude rules never apply here.
- [ ] Jumu'ah shows at the Dhuhr time; the Kahf reminder runs Thursday Maghrib to Friday Maghrib (`Widgets.java:32`).

**Hijri and events**
- [ ] Kuwait starts months by its crescent-sighting committee. Umm al-Qura matched Kuwait in 2026 (Ramadan, Eid al-Fitr, 1 Muharram 1448) but Kuwait's own forecast for Dhul Hijjah 1446 (2025) was one day later than Umm al-Qura. Any PR touching dates should add or keep a parent-set Hijri offset (−1/0/+1) applied everywhere the Hijri date is read.
- [ ] Never `ISLAMIC_CIVIL`, `ISLAMIC_TBLA` or plain `ISLAMIC` for display, and never a bare `new IslamicCalendar()` (it defaults to civil).
- [ ] Fasting labels are right: 9 Muharram is **Tasu'a** (Muslim 1134a), 10 is Ashura; no fasting badge on the days of Tashreeq (Muslim 1141a) or the Eid days; White Days are the 13th, 14th and 15th (an-Nasa'i 2424, hasan). Any reward text quotes its source (Arafah: Muslim 1162b; no fasting on the Eids: Bukhari 1990).
- [ ] "Tomorrow" badges and countdowns use the same calendar and offset as the date line.

**Quran text**
- [ ] Shown exactly as the source gives it: Uthmani script with every mark. Never trimmed, ellipsized, cut by `maxLines`, reworded, "normalised" or compared across sources with plain string equality (sources encode the same marks differently).
- [ ] The Basmala is not part of ayah 1 of surahs 2-114 (`withoutBismillah`); 1:1 is the Basmala; 9:1 has none. Keep that logic in both modules.
- [ ] Each reference is `Surah n:ayah` and matches the text. Part of a verse is labelled as part ("from 2:286").
- [ ] The font covers every Quranic mark: Amiri and Plex Arabic do; Baloo, Inter and the Latin faces don't. Check new fonts with fontTools, not by their description.

**Hadith and adhkar**
- [ ] Every hadith gives collection and sunnah.com number (e.g. "Bukhari 6116", "Muslim 1134a"). Wording checked on sunnah.com or dorar.net. Grade stated when not Bukhari or Muslim (e.g. "Abu Dawud 5072, weak per al-Albani"). The English adds nothing the narration doesn't have. A Companion's words (mawquf) are labelled as such.
- [ ] Numbers come from sunnah.com or dorar.net pages, never from a dataset's own numbering.
- [ ] Kids text stays simple without changing the meaning, uses only sahih hadith (owner's rule), and each Name of Allah's explanation fits that Name (with its ayah where possible).
- [ ] Adhkar cite Hisn al-Muslim chapter/number plus the hadith source. Weak (da'if) or fabricated items are dropped, or at least graded on screen and never shown to kids.
- [ ] Adab: Quran text isn't shown over unsuitable images or video.

**Arabic rendering (bidi, digits, shaping)**
- [ ] Mixed Arabic and Latin lines (kickers like `"VERSE OF THE DAY · آية اليوم · Al-Baqarah 2:286"`, `MainActivity.java:402`) keep their order. Use `BidiFormatter.unicodeWrap` or separate TextViews, and check forced `TEXT_DIRECTION_LTR` (`:2522`, `:3293`).
- [ ] One digit style per Arabic string: Arabic-Indic U+0660-0669 (not the Persian U+06F0-06F9). Format with a region (`new Locale("ar","KW")` or `ar-KW-u-nu-arab`): since CLDR 46 plain `ar` gives Western digits.
- [ ] No per-character spans, colours or letter-spacing inside Arabic words (spans break joining; letter-spacing is ignored for cursive scripts). Code-point ranges used to strip marks don't hide letters (U+06E5, U+06E6) or symbols (U+06DD, U+06DE, U+06E9).

## Known risks already in the code
- **No Hijri offset (Should fix):** every Hijri read uses Umm al-Qura as-is (see the Hijri checklist).
- **9 Muharram labelled Ashura (Should fix):** `Events.java:70` shows "Ashura fast / صيام عاشوراء" on the 9th, and `:50` says "Fast of Ashura tomorrow" only on the 8th (on the 9th today's badge wins, `:36-38`). Per Muslim 1134a the 9th is Tasu'a.
- **Latin digits in Arabic (Should fix):** `Events.java:59` builds `"رمضان · اليوم " + d`. Use `Widgets.ar()`, and give that helper a region (both modules' `Widgets.ar()` use bare `new Locale("ar")`).
- **Weak adhkar unlabelled (Should fix):** رضيت بالله رباً … نبياً (`Widgets.java:82`, `:98`) cites "Abu Dawud" only. It is graded weak by al-Albani (Sabiq ibn Najiya is unknown; Ibn al-Marzuban is a mudallis), though al-Nawawi called some chains jayyid; the نبياً wording is in Tirmidhi and Ibn Majah 3870 (see the knowledge file). Add number and grade, or replace it.
- **Missing hadith numbers (Should fix):** most of `Content.java:9-18`, `GoodWords.java:48-63` and `Widgets.java:75-102` give only the collection name.
- **Offline verses (Should fix):** `Content.AYAH` is plain spelling without marks, while online verses are Uthmani. 13:28, 2:152, 20:114 and 2:286 are parts of verses labelled as whole ones.
- **ﷺ in Baloo (Nice to have):** `KidsActivity.java:615` and `GoodWords.java:50` show ﷺ in Baloo, which lacks U+FDFA, so it falls back to another font. Plex Arabic has it.
- **Kids content (Nice to have):** `GoodWords.java:24` explains Ar-Raheem as "loves to forgive", which is closer to Al-Ghafoor. `dayIndex()` is the day of the year (`Worlds.java:63`), so an item can repeat across New Year.
- **Fixed in PR #20, keep fixed:** the Basmala glued onto ayah 1; the adhan-card ayah cut by `maxLines` and drawn in Baloo; "Don't get angry" now "again and again" (Bukhari 6116); Hasbiyallah labelled as Abu al-Darda's words (Abu Dawud 5081).

- **OFFSETS checked (2026-11-05).** Against the published Kuwait (Al-Ajairi / Awqaf) times for Ramadan 1447 and 6 Nov 2026, the computed times match exactly most days and are otherwise off by 1 minute either way; no constant offset does better, so `OFFSETS` stays 0. Only embedding the official tables would match every day. Summer months were not checked.

## How to report
Group findings under **Must fix** (a wrong prayer time, wrong date for Ramadan or Eid, altered or cut Quran text, a misattributed hadith), **Should fix**, and **Nice to have**. For each one, give `file:line`, what goes wrong in plain words ("the TV shows Bismillah as part of Al-Baqarah 2:1"), the authoritative source with its URL, and a suggested fix. The owner is a beginner, so avoid jargon or explain it in one clause. Mark anything you could not verify as **unverified**. If the PR is clean for this lane, write "nothing found" and list what you checked. Then send the knowledge-file additions to the PR's thread.

## Sources
Full annotated list: [knowledge/archive/6-arabic-and-islamic.md](knowledge/archive/6-arabic-and-islamic.md). The most used:
- https://praytimes.org/calculation (the algorithm and the method table)
- https://api.aladhan.com/v1/methods and https://esalah.com/prayer-times/kuwait/?lang=en (Kuwait 18°/17.5°)
- https://raw.githubusercontent.com/unicode-org/icu/main/icu4j/main/core/src/main/java/com/ibm/icu/util/IslamicCalendar.java (calculation types, civil default, Umm al-Qura 1300-1600 AH)
- https://www.arabtimesonline.com/news/16th-june-marks-start-of-islamic-new-year-in-kuwait/ and https://www.khaleejtimes.com/world/gulf/kuwait-moon-sighting-eid-al-adha (Kuwait sighting dates)
- https://sunnah.com/muslim:1134a, https://sunnah.com/muslim:1141, https://sunnah.com/muslim:1162b, https://sunnah.com/nasai:2424, https://sunnah.com/abudawud:5072
- https://hadithanswers.com/do-not-become-angry/ and https://hadithanswers.com/reciting-hasbiyallahu-la-ilaha-illa-hu-seven-times/ (Bukhari 6116, Abu Dawud 5081)
- https://api.alquran.cloud/v1/ayah/8/editions/quran-uthmani,en.sahih (Basmala prepended to 2:1)
- https://raw.githubusercontent.com/nuqayah/quran-text/master/README.md (how Quran texts encode marks differently)
- UnicodeData.txt, Blocks.txt, DerivedCoreProperties.txt (unicode-org/unicodetools) and CLDR `ar.xml` releases 45/46
- https://android.googlesource.com/platform/frameworks/minikin/+/e8721b2 (letter-spacing off for cursive scripts); https://learn.microsoft.com/de-de/dotnet/api/android.text.bidiformatter?view=net-android-37.0 (BidiFormatter)
