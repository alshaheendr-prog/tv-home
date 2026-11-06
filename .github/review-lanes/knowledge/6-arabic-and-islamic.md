# Lane 6 knowledge: Arabic and Islamic content

## How to use this file

- This is the lane's core: read it in full before every review, then the brief `../6-arabic-and-islamic.md`.
- History lives in [archive/6-arabic-and-islamic.md](archive/6-arabic-and-islamic.md): past findings, open questions and the full source library. Search it (by file, area or PR) when the PR touches something with history; don't read it in full.
- Research online only what the PR touches that this file and the archive don't already cover (a new API, permission, library, font, codec or behaviour). Open and cite what you use. Mark anything unverified as *(unverified)*.
- After every review, send the PR's thread short, dated additions: a **Past findings** block and new **sources** and **open questions** go to the archive; **Lessons learned** and any new verified fact that every review needs go into this core file.
- Keep this core lean: when you add a lesson, merge it with an older one on the same point instead of only appending. Aim to keep this file under about 25 KB.
- Source tags like `[pt]` in this file point to entries in the archive's Source library. Never delete a source that is still cited.
- When you verify an open question, move it from the archive into this core file with its source. Cite only pages you actually opened; if a page could not be fetched, say so instead of citing it.
- Religious facts (a verse, a hadith's wording, number or grade, a fasting rule, an official date or time) go in here **only** with a link to an authoritative page you opened: Kuwait Ministry of Awqaf or a dated Kuwaiti newspaper report of it, Tanzil or the King Fahd Complex for Quran text, sunnah.com or dorar.net for hadith. Never write one from memory. Anything not yet checked is marked **unverified** and listed under Open questions (in the archive).

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
- Tanzil's terms of use (verbatim copies only, no changes) and its version history: **not yet re-read** this round (fetch budget ran out); see Open questions in the archive.
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

Added by the PR #23 review (2026-11-06):
- Section 8 (Fonts): the bundled `amiri_bold.ttf` is a **subset** (1104 cmap entries). It has no U+0030-0039, U+002E, U+003A, U+00B0, U+00B7 and no Latin letters. It does have U+0660-0669, U+060C, parentheses, `!` and ZWNJ. Plex Arabic medium and semibold have the Latin digits and `. : °` (fontTools, 2026-11-06).
- Section 7 (Digits): Android 14 (API 34) ships ICU 72.1 / CLDR 42 / Unicode 15.0; Android 15 ships ICU 75.1 / CLDR 45 [android-icu]. So a bare `Locale("ar")` gives Arabic-Indic digits on the box today, and the CLDR 46 change would only arrive with a later OS.
- Section 9 (Rendering): UAX #9 rule W4 says a single common separator (`:` `,` `.`) between two Arabic-Indic numbers becomes AN, so "١١:٣٢" stays together in RTL text [uax9-w4]. `MixText` (app) puts IBM Plex Arabic on whole Arabic runs inside Latin TextViews and never splits a word; span boundaries are hard shaping breaks [devto].

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

Added by the PR #25 review (2026-11-06):
- 2026-11-06: bundled `amiri_bold.ttf` (app and adhan, same md5 678c6a37…) has U+0028 `(` but **not U+0029 `)`**; it has U+FD3E/FD3F ﴾﴿ and U+06DD. GSUB has no `rtlm`, so HarfBuzz draws `(` for both brackets in RTL.
- 2026-11-06: The bundled Amiri Bold subset encloses digits after U+06DD END OF AYAH (HarfBuzz test): use ۝ + Arabic-Indic digits for verse numbers, not ASCII brackets.

## Past findings and sources

Past findings (every PR and audit), open questions and the full annotated source library are in [archive/6-arabic-and-islamic.md](archive/6-arabic-and-islamic.md). Search it when a PR touches an area with history; don't read it in full.

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

- 2026-11-05: When badge or label priority is reordered, walk every Islamic day from the 8th to the 10th of Dhul Hijjah and the 28th to the 30th of Ramadan. A "tomorrow" check can hide today's bigger day.
- 2026-11-05: The end of Ramadan is known only after the sighting on the evening of the 29th, so any "tomorrow" text on the 29th must be tentative. Dhul Hijjah is fixed when the month starts, so Eid al-Adha lines can be definite.
- 2026-11-05: A neutral character (… : ·) at the end of an Arabic run in an LTR line takes the line's direction (UBA rules N1/N2), so it is drawn where the Arabic begins.

- 2026-11-06: When a view's typeface changes to Amiri, check every string that view can show for Latin text, because the bundled Amiri is a subset.
- 2026-11-06: Look up the box's ICU/CLDR version in developer.android.com's internationalization table before rating a locale-data risk.

- 2026-11-06: A whole verse list can be checked mechanically: compare every Arabic string with the source package character for character, then with a second King Fahd copy by letters only (marks stripped), because encodings differ. fawazahmed0/quran-api and hadith-api on raw.githubusercontent.com load from the container (alquran.cloud, tanzil.net, quranenc.com and jsDelivr are blocked).
- 2026-11-06: fawazahmed0/hadith-api uses sunnah.com numbers; for Muslim use `arabicnumber` ("2564.03" = 2564c), not `hadithnumber` (a running count). Its `grades` field carries al-Albani's grade for the Sunan.
- 2026-11-06: Before showing brackets, digits or punctuation in Amiri, check the bundled subset's cmap for **both** characters of a pair: it has `(` but not `)`. Shape a sample with uharfbuzz to see what the TV will draw.
- 2026-11-06: A setting that changes religious dates must reach every app that shows them; separate apps keep separate preferences.
