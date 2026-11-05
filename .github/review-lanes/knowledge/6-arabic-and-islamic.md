# Lane 6 knowledge: Arabic and Islamic content

## How to use this file
- Read this whole file before every review, then the brief (`../6-arabic-and-islamic.md`).
- Religious facts (a verse, a hadith's wording, number or grade, a fasting rule, an official date or time) go in here **only** with a link to an authoritative page you opened: Kuwait Ministry of Awqaf or a dated Kuwaiti newspaper report of it, Tanzil or the King Fahd Complex for Quran text, sunnah.com or dorar.net for hadith. Never write one from memory. Anything not yet checked is marked **unverified** and listed under Open questions.
- Research online whatever the PR touches that this file doesn't cover. After every review, send the PR thread the additions for this file so they get committed with the PR: append a dated, short entry to **Past findings**, **Lessons learned**, **Source library** and **Open questions**, and move verified items into Core expertise.
- Keep the file under about 400 lines. When it grows past that, condense the oldest Past findings into one line per PR.

## Core expertise

### 1. Prayer-time astronomy (PrayTimes.org method, which `PrayerTimes.java` ports) [pt]
- Inputs: latitude, longitude, time zone, Julian date. Sun position by the US Naval Observatory approximation (`g`, `q`, `L`, `e`, `RA`, declination `D`, equation of time `EqT`), accurate to about 1 arcminute within two centuries of 2000.
- Dhuhr = 12 + TZ − Lng/15 − EqT (solar noon). The page says a "slight margin" is usually added; it doesn't give the number in the text.
- Sunrise/Sunset = Dhuhr ∓ T(0.833°). 0.833° covers refraction (and the sun's radius). For an observer above the surrounding terrain the page suggests a slightly larger angle, but says the constant is usually kept. Kuwait City is near sea level, so no elevation term is needed.
- Fajr = Dhuhr − T(Fajr angle); Isha = Dhuhr + T(Isha angle).
- Asr: majority (Shafi'i, Maliki, Hanbali, Ja'fari) shadow = object + noon shadow → factor 1; Hanafi factor 2.
- Maghrib (Sunni) = sunset; the page notes a 1-3 minute precaution is common.
- Midnight: astronomical = sunset + ½(sunrise − sunset); some use sunset + ½(Fajr − sunset).
- Higher-latitude rules (middle of night, one-seventh, angle-based) matter only where twilight lasts all night. Kuwait (29.4°N) never needs them.
- Method table on the page: MWL 18/17; ISNA 15/15; Egypt 19.5/17.5; Umm al-Qura 18.5 / Isha 90 min after Maghrib (120 min in Ramadan); Karachi 18/18; Tehran 17.7/14; Qum 16/14; France 12/12; Russia 16/15; Singapore 20/18. **Kuwait isn't in PrayTimes' table.**
- Kuwait: Fajr 18°, Isha 17.5°, Asr factor 1. Sources: Aladhan's method list ("Kuwait", method 9) and eSalah ("Kuwait Ministry of Awqaf"), checked by the first review [aladhan-m], [esalah], [gtaf]. Not yet confirmed on awqaf.gov.kw itself (unreachable from the container).
- Rounding: the code rounds each time to the nearest minute. Against the Kuwaiti published times (Al-Ajairi calendar, per Al-Anba's Ramadan 1447 timetable, and awgatalsalat.com for 6 Nov 2026) the computed times match exactly on most days and otherwise differ by ±1 minute either way; no constant offset does better, so `OFFSETS` stays 0 [first review, 2026-11-05]. Summer dates were not checked.
- Kuwait doesn't switch Isha to a fixed interval in Ramadan (unlike Umm al-Qura's 120 min) [first review].

### 2. Hijri calendar
- **ICU `IslamicCalendar`** (Android uses `android.icu`, a copy of ICU4J) [icu]:
  - A new `IslamicCalendar` defaults to the fixed-cycle **civil** calendar. You must call `setCalculationType(ISLAMIC_UMALQURA)` (the code does) or use a locale with `@calendar=islamic-umalqura`.
  - Types: `ISLAMIC` (astronomical approximation, "fairly simplistic"), `ISLAMIC_CIVIL` (Friday epoch, 16 July 622 Julian), `ISLAMIC_TBLA` (same cycle, Thursday epoch 15 July 622), `ISLAMIC_UMALQURA` (Saudi table). ICU itself says religious months depend on sighting and can differ by up to three days from the civil calendar.
  - Umm al-Qura data covers **1300-1600 AH** (`UMALQURA_YEAR_START/END`); outside that the code falls back to an estimate. Fine for this app's lifetime.
- **Java's `HijrahDate`** (JDK) also uses the Umm al-Qura table. Running it here gave: 18 Feb 2026 = 1 Ramadan 1447; 20 Mar 2026 = 1 Shawwal 1447; 27 May 2026 = 10 Dhul Hijjah 1447; 16 Jun 2026 = 1 Muharram 1448; 5 Nov 2026 = 25 Jumada al-Ula 1448 [jdk-hijrah, local run]. Useful as a cross-check of ICU in tests.
- **Kuwait's actual dates in 2026:** Ramadan began 18 Feb, Eid al-Fitr 20 Mar [first review: arynews, moroccoworldnews]; 1 Muharram 1448 was Tuesday 16 June 2026, set by sighting "confirmed on Monday evening" [arabtimes-1448]. All three equal Umm al-Qura. Eid al-Adha 1447 in Kuwait: the public holiday for Arafah and Eid began Tue 26 May 2026 (Arab Times, 19 May), consistent with Umm al-Qura (Arafah 26 May, Eid Wed 27 May) [arabtimes-eid1447].
- **A real one-day gap, 1446 (2025):** the Kuwait Joint Committee for Crescent Sighting predicted Dhul Hijjah 1446 would begin Thursday 29 May 2025, Arafah 6 June, Eid al-Adha Saturday 7 June; it gave the conjunction as 06:02 on 27 May and the crescent as hard to see (43 minutes after sunset) [kt-1446]. Umm al-Qura has 1 Dhul Hijjah = 28 May and 10 Dhul Hijjah = Friday 6 June 2025 [jdk-hijrah]. So Kuwait's own forecast was one day after Umm al-Qura. (Whether Kuwait's final announcement followed the forecast is **unverified**.)
- Kuwait starts months by sighting through that committee. Umm al-Qura is a prediction that matched in 2026 so far but was a day off from Kuwait's own forecast in 1446. A parent-set ±1 day offset, applied everywhere the Hijri date is read (badges, countdown, upcoming list, the date line), is the safe design.
- The app changes the Hijri date at local midnight. The Islamic day begins at Maghrib; showing "tonight is the night of …" after Maghrib would need its own logic (**fiqh source not yet opened**).

### 3. Calendar days and fasting rules used in the UI
- **Ashura and Tasu'a:** the Prophet ﷺ fasted 'Ashura (10 Muharram) and said that next year, in sha Allah, "صُمْنَا الْيَوْمَ التَّاسِعَ" (we will fast the ninth); he died before it came. Sahih Muslim 1134a [sm-1134]. So the 9th is a separate, recommended fast (Tasu'a), not "Ashura". UI text for the 9th should say "Tasu'a fast" or "fast of the 9th (with Ashura)".
- **Days of Tashreeq:** "أَيَّامُ التَّشْرِيقِ أَيَّامُ أَكْلٍ وَشُرْبٍ" (days of eating and drinking); Muslim's chapter heading says fasting them is forbidden and they are days of dhikr. Sahih Muslim 1141a [sm-1141]. The days are 11-13 Dhul Hijjah (dates **unverified** on the page itself; it gives the rule, not the dates). So no "White Days fast" on 13 Dhul Hijjah. What to show on 14-15 Dhul Hijjah is a fiqh choice to check (**unverified**).
- **White Days:** Abu Dharr: "if you fast any part of the month, fast the 13th" (and the 14th and 15th): "إِذَا صُمْتَ شَيْئًا مِنَ الشَّهْرِ فَصُمْ ثَلاَثَ عَشْرَةَ". Sunan an-Nasa'i 2424, graded Hasan (Darussalam) on sunnah.com [nas-2424]. Cite it as "an-Nasa'i 2424 (hasan)".
- **Arafah and Ashura rewards:** fasting the day of 'Arafah "يُكَفِّرُ السَّنَةَ الْمَاضِيَةَ وَالْبَاقِيَةَ" (expiates the past year and the coming one); fasting 'Ashura expiates the past year. Sahih Muslim 1162b [sm-1162].
- **The two Eids:** 'Umar said the Prophet ﷺ forbade fasting two days, the day you break your fast (Eid al-Fitr) and the day you eat from your sacrifice (Eid al-Adha): "نَهَى رَسُولُ اللَّهِ صلى الله عليه وسلم عَنْ صِيَامِهِمَا". Sahih al-Bukhari 1990 [bk-1990]. No fasting badge on either Eid day.
- **Surat al-Kahf on Friday:** al-Hakim 3392 / al-Bayhaqi 6209, sahih per al-Albani (Sahih al-Jami' 6470). Friday night starts at Thursday sunset, so the Thursday-Maghrib-to-Friday-Maghrib window (`app/.../Widgets.java:32`) is right [dorar-146478].
- **Six days of Shawwal:** Muslim 1164 [abuaminaelias-shawwal]. (sunnah.com's sub-number, 1164a, still to confirm.)

### 4. Qibla (only relevant if a PR adds it; no Qibla code exists)
- Initial great-circle bearing θ = atan2(sin Δλ · cos φ₂, cos φ₁ · sin φ₂ − sin φ₁ · cos φ₂ · cos Δλ), normalised to 0-360°. Haversine for distance.
- From Kuwait City (29.3759N, 47.9774E) to the Kaaba (21.4225N, 39.8262E): **224.6° from true north, about 1,204 km** (computed locally 2026-11-05; matches the brief).
- A phone or TV compass shows magnetic north. The local magnetic declination must come from a geomagnetic model (NOAA WMM); don't hard-code a guess (**value for Kuwait unverified**).

### 5. Quran text
- **Source used:** `api.alquran.cloud` edition `quran-uthmani` (Tanzil Uthmani) with `en.sahih`. Ayah numbers 1-6236 (`% 6236` in `fetchAyah`). The API prepends the Basmala to ayah 1 of surahs 2-114 except 9 [alq-2-1, first review].
- **Basmala rules applied by `withoutBismillah`:** 1:1 is the Basmala itself and stays; 9:1 has none; for other ayah-1 texts the first four words are dropped if the first word, stripped of marks, is بسم. Verified on 2:1, 9:1, 1:1, 95:1 (shadda variant) and 2:2 [first review].
- **Encoding facts that matter when comparing or cleaning text** [nuqayah]: Quran texts differ in how they encode the same mark: open tanween U+08F0-08F2 vs ordinary U+064B-064D (ikhfa/idgham), U+06E1 (small high dotless head of khah) vs U+0652 sukun, U+06E4 small high madda vs U+0653 maddah, the dagger alif (U+0670) needs a tatweel after joining letters to sit right, and there is **no Unicode for the lam-hamza-alif ligature** of Quranic orthography. So never compare verses with plain string equality across sources, and never "normalise" displayed Quran text.
- Text derived from Tanzil and tuned for Unicode exists (nuqayah/quran-text, using the King Fahd Complex QPC v13 text and khaledhosny/quran-data) [nuqayah]. khaledhosny/quran-data says itself that its text **has not been formally reviewed** [khaled]: don't use it as an authority.
- Tanzil's terms of use (verbatim copies only, no changes) and its version history: **not yet re-read** this round (fetch budget ran out); see Open questions.
- Display rules the lane enforces (adab and accuracy): show the text exactly as the source gives it; never trim, ellipsize, cut with `maxLines`, or reword; label part of a verse as part ("from 2:286"); keep the reference `Surah n:ayah` with the text. (A written adab source, e.g. al-Nawawi's al-Tibyan on handling the mushaf, is still to be opened.)

### 6. Unicode facts (UnicodeData.txt and Blocks.txt from unicode-org/unicodetools, `dev`) [ucd], [blocks]
- Blocks: Arabic 0600-06FF, Arabic Supplement 0750-077F, Arabic Extended-B 0870-089F, Extended-A 08A0-08FF, Presentation Forms-A FB50-FDFF, Presentation Forms-B FE70-FEFF, Extended-C 10EC0-10EFF.
- Marks (general category Mn, bidi NSM): U+0610 sign sallallahou alayhe wassallam; U+064B-0652 harakat (fathatan … sukun); U+0653 maddah, U+0654/0655 hamza above/below; U+0670 superscript (dagger) alef; U+06D6-06DC small high pause marks (06D6 ligature sad-lam-alef maksura … 06DC small high seen); U+06DF small high rounded zero; U+06E1 small high dotless head of khah; U+06ED small low meem; U+08F0/08F2 open fathatan/kasratan.
- **Not marks:** U+06E5 small waw and U+06E6 small yeh are letters (Lm, bidi AL). U+0671 alef wasla is a letter (Lo). U+06DD end of ayah is a format char (Cf, bidi AN) that takes the digits after it. U+06DE rub el hizb and U+06E9 place of sajdah are symbols (So, ON). A regex that strips "marks" by range (`ۖ-ۭ`) also removes 06E5/06E6/06DD/06DE/06E9; that is fine for matching بسم, wrong for anything shown.
- U+FDFA ﷺ is one ligature letter (Lo) with a compatibility decomposition to the full phrase; U+FDFD is the Bismillah ligature (So).
- Digits: Arabic-Indic U+0660-0669 have bidi class **AN**; Extended (Persian/Urdu) U+06F0-06F9 have class **EN** and different shapes for 4, 5, 6. Use 0660-0669 for Arabic. U+066B/066C are the Arabic decimal and thousands separators (AN).
- Bidi controls: U+200F RLM, U+061C ALM, U+2066 LRI, U+2067 RLI, U+2068 FSI, U+2069 PDI. All are Default_Ignorable_Code_Point [dcp], so it's normal that fonts don't map them.

### 7. Digits on Android: a trap with `new Locale("ar")` [cldr]
- In CLDR up to release 45, plain `ar` defaults to Arabic-Indic digits (`arab`). From **CLDR 46** plain `ar` inherits `latn` (Western digits), while `ar_KW`, `ar_SA` and `ar_EG` keep `arab` (checked on CLDR `main`).
- So `String.format(new Locale("ar"), "%d", n)` (`Widgets.ar()`, both modules) gives Arabic-Indic digits only while the box's ICU data is older than CLDR 46. Which ICU/CLDR this Android 14 box ships, and whether a Mainline update changes it, is **unverified**. Robust choice: `new Locale("ar", "KW")` or `Locale.forLanguageTag("ar-KW-u-nu-arab")`. The date line already uses `ar_SA@calendar=islamic-umalqura`.

### 8. Fonts (cmap checked with fontTools on the bundled files, 2026-11-05)
Checked set: U+0610-061A, 064B-065F, 0670, 0671, 06D6-06ED, 08F0-08F2, FDFA, FDFD, 06DD, 06DE, 06E9, 0660, 06F0, FD3E/FD3F, 060C, 061B, 061F, 066B, 066C.
- `amiri_bold.ttf`, `amiri_regular.ttf`: all present.
- `plex_arabic_semibold.ttf`, `plex_arabic_medium.ttf`: all present (including ﷺ and the Quranic marks).
- `baloo_*` (Kids, and the adhan module's copies): missing U+0610-0614, 0616-061A, 0659-065F, all of 06D6-06ED, 08F0-08F2, **FDFA ﷺ**, FDFD. Fine for kids' plain harakat text; not for Quran text or ﷺ.
- Inter, Chivo Mono, Cormorant, Fraunces, Jost, Outfit: no Arabic at all; Arabic in a TextView with these faces falls back to the system font.
- Amiri: classical Naskh revival of the Bulaq (Amiria) press type, one of the few metal types used for Quran typesetting; version 1 released 2022 and considered mature, no further development planned; OFL [amiri]. A separate "Amiri Quran" face exists (not bundled; not yet researched).

### 9. Rendering pitfalls on Android (from the first review's sources, in the brief)
- Minikin turns off letter-spacing for cursive scripts, so `setLetterSpacing` on Arabic is ignored, but mixed Latin+Arabic kickers still space the Latin part [minikin].
- Span boundaries inside an Arabic word can break joining; never colour single letters or marks [devto].
- Mixed-direction strings need `BidiFormatter.unicodeWrap` or separate TextViews; a forced `TEXT_DIRECTION_LTR` on an Arabic title changes where neutral characters (·, :, digits) go [bidi].

### 10. Hadith sourcing and grading
- Cite collection and number as sunnah.com shows it (e.g. "Sahih Muslim 1134a", "Sunan Abi Dawud 5072"). sunnah.com also gives "in-book" and old USC-MSA numbers; the USC-MSA scheme is marked deprecated there [sm-1134], so don't use it.
- Bukhari and Muslim need no grade. For others, give the grade sunnah.com shows (usually al-Albani's) or dorar.net's.
- **رضينا بالله ربا وبالإسلام دينا وبمحمد رسولا**, morning and evening: Sunan Abi Dawud 5072, graded **Da'if (weak) by al-Albani** on sunnah.com [ad-5072]. The app's adhkar show it as "رضيت … وبمحمد ﷺ نبياً" with source "Abu Dawud" and no grade.
- The **نبياً + يرضيه** wording is in Tirmidhi (3389 per hadithanswers, 3386 per islamqa), Ibn Majah 3870, Ahmad and Nasa'i's Kubra (3 times). Tirmidhi: hasan gharib; al-Albani: weak (Sabiq ibn Najiya is unknown; Ibn al-Marzuban is a mudallis), though al-Nawawi called some chains jayyid. The authentic hadith is Muslim 1884 / Abu Dawud 1529 (وجبت له الجنة), which is not a morning/evening dhikr [islamqa-192204] [hadithanswers-radhitu].
- Tirmidhi 1956 (smiling is charity): hasan gharib per Tirmidhi.
- Hasbiyallah … 7 times: Abu Dawud 5081, words of Abu al-Darda (mawquf) [first review: hadithanswers].
- "Don't get angry": Bukhari 6116, فردد مرارا, "he repeated it several times" [first review: hadithanswers].
- A GitHub dataset (AhmedBaset/hadith-json) has the Arabic of the nine books but its `idInBook` numbers do **not** match sunnah.com's (e.g. the لا تغضب narration sits at `idInBook` 5884 there, while sunnah.com numbers it 6116). Use it only to find wording, never for numbers.

### Added by the whole-app audit (2026-11-05)
- Tirmidhi 1956 تبسمك: Tirmidhi hasan gharib; al-Albani sahih (Sahihah 572) [tohed-t1956].
- Tirmidhi 1954 من لا يشكر الناس: hasan sahih; al-Albani sahih [tohed-t1954].
- Tirmidhi 3895 خيركم خيركم لأهله: hasan sahih; also Ibn Majah 1977 [hadithanswers-ahl].
- Al-Adab al-Mufrad 594 تهادوا تحابوا: hasan per al-Albani [tohed-adab594]. Not sahih, so not for Kids under the owner's rule.
- Tirmidhi 3604 (second narration): أعوذ بكلمات الله التامات … "حين يمسي ثلاث مرات"; Tirmidhi: hasan [tohed-t3604]. So "3 times" comes from Tirmidhi, not Muslim (answers the open question).
- Bukhari 6018 opens with فلا يؤذ جاره, so فليقل خيرا أو ليصمت is an extract ("from"). Bukhari 5027 = خيركم من تعلم القرآن وعلمه. Muslim 223 opens الطهور شطر الإيمان (extract). Muslim 2593 = يا عائشة إن الله رفيق يحب الرفق. Bukhari 5376 = يا غلام سم الله وكل بيمينك وكل مما يليك. Bukhari 6464 opens سددوا وقاربوا [tohed pages].
- Hisn al-Muslim numbers differ by edition: the Mukhtasar numbering on tohed gives 89 اللهم بك أصبحنا, 94 حسبي الله (7), 97 بسم الله الذي لا يضر (3), 98 رضيت بالله (3), 100 أصبحنا وأصبح الملك لله, 102 سبحان الله وبحمده (100), 107 أعوذ بكلمات الله (3, evening); sunnah.com/hisn uses other numbers. Always name the edition [tohed-hisn].
- As-Saboor: comes only from the Tirmidhi list of names, judged mudraj; its status as a Name is ijtihad (counted by al-Khattabi, al-Bayhaqi, Ibn al-Qayyim and others) [islamqa-191329].
- Ar-Rahman vs Ar-Raheem: Ibn al-Qayyim: الرحمن is the attribute, الرحيم is that mercy reaching its recipients; 33:43 "وكان بالمؤمنين رحيما" [islamonline-rahim].
- Umm al-Qura (JDK HijrahDate): 1 Ramadan 1448 = Mon 8 Feb 2027; 1 Shawwal = Tue 9 Mar 2027; 9/10 Dhul Hijjah = 15/16 May 2027; 1 Muharram 1449 = Sun 6 Jun 2027; 10 Muharram = Tue 15 Jun 2027. The Saudi Hijri calendar forecast agrees on 8 Feb [jordannews-r1448].
- Kuwait timetables (secondary site) print suhoor ~10 min before Fajr [khaleejcalc-imsak]; Awqaf's own column is unverified.
- The verse seed `(seed+attempt)*7919 % 6236` with `seed = doy + year*400` overlaps day to day: day N attempt k = day N+1 attempt k−1.

## This box and this code
- **The owner's own rules** (project skill `islamic-content-check`, read 2026-11-05), which reviews apply on top of the above:
  - Kids content uses only sahih hadith, short and simple; drop anything da'if or mawdu' from what the kids see.
  - Adhkar cite Hisn al-Muslim with its chapter/number as well as the hadith source.
  - Names of Allah: give the ayah where the Name appears when possible.
  - Prefer whole short ayat; a partial ayah only if it is a complete thought that keeps the meaning.
  - Prayer times: compare 3 dates (today, a summer date, a winter date) against Awqaf; more than 1-2 minutes needs a reason.
  - Adab: verse text isn't placed over unsuitable images; the adhan sound is the family's own Makkah takbir clip; Alafasy is excluded from the Quran player by choice.
  - The skill lists Baloo Bhaijaan 2 as the Kids font, but Baloo has no ﷺ (U+FDFA) or Quranic marks (section 8). Tell the owner the skill needs that note.
  - By these rules, the weak رضيت/رضينا بالله ربا dhikr (Abu Dawud 5072) shouldn't appear without its grade, and never in Kids.
- `PrayerTimes.java` in `app` and `adhan` are identical apart from the package line (diffed 2026-11-05). Kuwait City 29.3759N 47.9774E, TZ +3, Fajr 18°, sunrise/sunset 0.833°, Isha 17.5°, Asr via `atan(1/(1+tan|lat−decl|))` (factor 1), two iterations, nearest-minute rounding, `OFFSETS` all 0 (`:13`). `clock()` is 12-hour, no AM/PM (`:73`).
- Hijri: `Events.hijri()` (`Events.java:15-28`) and `Events.upcoming()` (`:77-100`) use `IslamicCalendar` + `ISLAMIC_UMALQURA` in Asia/Kuwait; the date line uses `DateFormat` with `ar_SA@calendar=islamic-umalqura` (`MainActivity.java:1512-1514`, `AdhanService.java:454`); the adhan module's `Widgets.java:70-83` counts down to Ramadan and Eid. No offset setting.
- `Events.on()` (`:55-75`): Ramadan (days ≥21 = last ten nights), Shawwal 1-3 Eid, Shawwal 4-30 "Six days of Shawwal" (so no White Days badge in Shawwal), Dhul Hijjah 1-8 first ten, 9 Arafah, 10-13 "Eid al-Adha Mubarak" (`:66`), 14-15 fall through to "White Days fast" (`:72`), Muharram 1 New Year, 9 **and** 10 "Ashura fast" (`:70`), any other month 13-15 White Days. `badge()` says "Fast of Ashura tomorrow" only on 8 Muharram (`:50`); on the 9th today's "Ashura fast" wins (`:36-38`).
- Known risks found in PR #21:
  - Kids `GoodWords.java:48` (Tirmidhi, smile) and `:53` (Al-Adab al-Mufrad, تهادوا) are not Bukhari/Muslim, against the owner's sahih-only rule for kids.
  - Countdown "بعد N يوم" (`adhan/.../Widgets.java:86`, `app/.../Widgets.java:129`) needs أيام for 3-10 and يوماً for 11-99 [kalimah-numbers].
  - `Events.upcoming()` (`:96`) excludes only Dhul Hijjah, so the next White Days it lists can fall in Ramadan or Shawwal.
  - `app/.../Widgets.java:94-95` labels أعوذ بكلمات الله "(3 times)" as Muslim, but the 3 times may come from Tirmidhi (**unverified**).
- Latin digits in Arabic: `"رمضان · اليوم " + d` (`Events.java:59`).
- Verse of the day: `fetchAyah` (`MainActivity.java:1697-1717`) keeps only texts ≤120 chars, 12 tries, else the offline `Content.AYAH`; `withoutBismillah` (`:1724-1733`, and `AdhanService.java:810`).
- Fonts: home verse band, Quran grid and now-playing use Amiri Bold (`fQuran`, `MainActivity.java:198`); the adhan card's Arabic now uses Amiri Bold with RTL direction and font padding (`AdhanService.java:709-716`); adhan strip text uses Plex Arabic (`:288`); Kids uses Baloo, including "The Prophet ﷺ" (`KidsActivity.java:615`) and ﷺ in `GoodWords.java:50`.
- Adhkar (`app/.../Widgets.java:73-102`): sources are collection names only; Hasbiyallah labelled "Abu Dawud 5081, words of Abu al-Darda"; رضيت بالله labelled "Abu Dawud" with no grade.
- `Content.HADITH` (`Content.java:8-19`) and Kids `GoodWords.HADITHS` (`:47-64`) mostly give collection names without numbers; Bukhari 6116 is the exception.
- `Content.AYAH` (`:22-28`) is plain spelling without marks; 13:28, 2:152, 20:114 and 2:286 are parts of verses labelled as whole ones.
- Kids Names of Allah: Ar-Raheem explained as "loves to forgive" (`GoodWords.java:24`), closer to Al-Ghafoor's meaning.
- The Friday reminder runs from Thursday Maghrib to Friday Maghrib; Jumu'ah sunnahs show Friday before Dhuhr; the Jumu'ah line shows the Dhuhr time (`Widgets.java:31-39`).

## Past findings
- **2026-11-05, PR #20 (merged):** no Must fix. Should fix: adhan-card English cut at 4 lines (fixed); hadith numbers on edited lines (fixed: Bukhari 6116, Abu Dawud 5081 as words of Abu al-Darda). `withoutBismillah` verified on 2:1, 9:1, 1:1, 95:1, 2:2. Amiri Bold covers all Quranic marks and ﷺ; Baloo lacks U+06DF, U+06E5, U+06E6, ﷺ. `OFFSETS` checked against published Kuwait times (Al-Anba's Ramadan 1447 timetable "according to the Al-Ajairi calendar", awgatalsalat.com for 6 Nov 2026): exact most days, else ±1 min, so they stay 0; summer unchecked; awqaf.gov.kw unreachable. Kuwait doesn't shift Isha in Ramadan. Left open: `Content.AYAH` partial verses labelled whole; ﷺ in Kids Baloo; no Hijri offset; 9 Muharram labelled "Ashura fast"; Latin digits in Arabic (`Events.java:59`); grade of رضيت بالله ربا; missing hadith numbers elsewhere.
- **2026-11-05, PR #21 (docs):** Should fix (docs): `badge()` shows "Fast of Ashura tomorrow" only on 8 Muharram; 13 Dhul Hijjah shows Eid and 14-15 do show White Days; the رضيت … نبياً risk now gives its grade and why. Added: Kahf and Shawwal sources, Eid 1447 in Kuwait, four new known risks (kids grades, يوم grammar, White Days in Ramadan/Shawwal, the 3-times label).
- **2026-11-05, knowledge build (no PR):** رضينا بالله ربا = Abu Dawud 5072, Da'if per al-Albani (sunnah.com); 9 Muharram is Tasu'a (Muslim 1134a); Tashreeq ban (Muslim 1141a); 1 Muharram 1448 in Kuwait = 16 June 2026 = Umm al-Qura; Plex Arabic covers every Quranic mark and ﷺ, so it's a valid fallback for Kids' ﷺ; CLDR 46 makes plain `ar` use Western digits.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit (main d772bcc): Must: random verse of the day shows out-of-context short ayat (seed math: 77:30 on 9 Nov 2026, 100:2 on 13 Nov 2026) → curated ayah-number list. Should: Hijri offset before Ramadan 1448 (Umm al-Qura 8 Feb 2027); Tasu'a label; weak رضيت dhikr ungraded; hadith numbers (table in audit-2026-11/lane-6.md); offline verses plain/partial; same verse two days running (seed overlap); evening adhkar Maghrib (home) vs Asr (adhan card); digits/grammar (AdhanService:400 Western "10", يوم); kids تهادوا hasan, Ar-Raheem meaning, As-Saboor not established; "الإمساك" label on the Fajr time. Nice: API surah spellings, 120 vs 170 limit, ﴿ ﴾ for Quran only, ﷺ in Baloo, half hadith in Kids, midnight Hijri.


## Lessons learned
- 2026-11-05: Compare computed prayer times with the Kuwaiti published calendar, not with another app; apps share the same algorithm and agree with each other.
- 2026-11-05: Check font coverage with fontTools on the actual bundled files; "Arabic support" in a font's description says nothing about Quranic marks.
- 2026-11-05: Unicode ranges hide letters among marks (06E5, 06E6) and symbols (06DD, 06DE, 06E9). Check general category before stripping anything.
- 2026-11-05: Locale data changes under you (CLDR 46). Don't rely on a bare language tag for digits; pin the region or the numbering system.
- 2026-11-05: A hadith dataset's own numbering can differ from sunnah.com's. Numbers come only from sunnah.com or dorar.net pages.
- 2026-11-05: sunnah.com returns client errors to WebFetch. Dorar sharh pages (found by search), hadithanswers and abuaminaelias work instead. Name the site with every number, because Tirmidhi editions number differently.
- 2026-11-05: The fetch budget is shared with other lanes. Fetch the religious sources first; AOSP, CLDR, ICU and Unicode files are on raw.githubusercontent.com and don't use it.
- 2026-11-05: en.tohed.com hadith pages (`/hadith/<book>/<n>`) load with WebFetch and show Tirmidhi's and al-Albani's grades. Use them when sunnah.com and dorar.net fail.
- 2026-11-05: A random "verse of the day" is a content risk, not just a design choice: short ayat are often fragments or quoted speech. Simulate the seed for the coming weeks and check what it will show.
- 2026-11-05: Check both modules' copies of shared logic (adhkar windows, verse limits, digits): they drift.
- 2026-11-05: pip install fonttools works in the container (pypi is reachable).

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
